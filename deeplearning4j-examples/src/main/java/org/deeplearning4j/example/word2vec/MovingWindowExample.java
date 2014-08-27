package org.deeplearning4j.example.word2vec;

import org.deeplearning4j.berkeley.Pair;
import org.deeplearning4j.datasets.iterator.DataSetIterator;
import org.deeplearning4j.models.classifiers.dbn.DBN;
import org.deeplearning4j.iterativereduce.actor.core.DefaultModelSaver;
import org.deeplearning4j.iterativereduce.actor.multilayer.ActorNetworkRunner;
import org.deeplearning4j.iterativereduce.tracker.statetracker.hazelcast.HazelCastStateTracker;
import org.deeplearning4j.linalg.api.activation.Activations;
import org.deeplearning4j.linalg.api.ndarray.INDArray;
import org.deeplearning4j.linalg.dataset.DataSet;
import org.deeplearning4j.linalg.factory.NDArrays;
import org.deeplearning4j.models.featuredetectors.rbm.RBM;
import org.deeplearning4j.scaleout.conf.Conf;
import org.deeplearning4j.text.tokenizerfactory.UimaTokenizerFactory;
import org.deeplearning4j.util.SerializationUtils;
import org.deeplearning4j.util.Viterbi;
import org.deeplearning4j.word2vec.Word2Vec;
import org.deeplearning4j.word2vec.inputsanitation.InputHomogenization;
import org.deeplearning4j.word2vec.iterator.Word2VecDataSetIterator;
import org.deeplearning4j.word2vec.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.word2vec.sentenceiterator.labelaware.LabelAwareListSentenceIterator;
import org.deeplearning4j.word2vec.sentenceiterator.labelaware.LabelAwareSentenceIterator;
import org.deeplearning4j.word2vec.tokenizer.TokenizerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Completed textual moving window example
 */
public class MovingWindowExample {

    private static Logger log = LoggerFactory.getLogger(MovingWindowExample.class);

    public static void main(String[] args) throws Exception {

        InputStream is = new ClassPathResource("sentiment-milliontweets.csv").getInputStream();
        LabelAwareSentenceIterator iterator = new LabelAwareListSentenceIterator(is,",",1,3);
        //getFromOrigin rid of @mentions
        iterator.setPreProcessor(new SentencePreProcessor() {
            @Override
            public String preProcess(String sentence) {
                String base =  new InputHomogenization(sentence).transform();
                base = base.replaceAll("@.*","");
                return base;
            }
        });

        TokenizerFactory tokenizerFactory = new UimaTokenizerFactory();
        File vecModel = new File("tweet-wordvectors.ser");
        Word2Vec vec = vecModel.exists() ? (Word2Vec) SerializationUtils.readObject(vecModel) : new Word2Vec(tokenizerFactory,iterator,5);
        if(!vecModel.exists()) {
            vec.fit();

            log.info("Saving word 2 vec model...");

            SerializationUtils.saveObject(vec,new File("tweet-wordvectors.ser"));
        }

        else
            vec.setTokenizerFactory(tokenizerFactory);

        iterator.reset();

        DataSetIterator iter = new Word2VecDataSetIterator(vec,iterator,Arrays.asList("0","1","2"));
        if(!iter.hasNext())
            throw new IllegalStateException("No data found");

          /*
        Note that this is an example of how to iterate. The parameters are not optimally tuned here, but serve to demonstrate
        how to use bag of words classification
         */
        HazelCastStateTracker tracker = new HazelCastStateTracker(2200);

        Conf c = new Conf();
        c.setFinetuneEpochs(10000);
        c.setPretrainEpochs(10000);
        c.setFinetuneLearningRate(1e-3f);
        c.setPretrainLearningRate(1e-3f);
        c.setLayerSizes(new int[]{iter.inputColumns() / 4,iter.inputColumns() / 4, iter.inputColumns() / 3});
        c.setUseAdaGrad(true);
        c.setMomentum(0.5f);
        //c.setRenderWeighftEpochs(1000);
        c.setnOut(2);
        c.setFunction(Activations.hardTanh());
        c.setSplit(10);
        c.setnIn(vec.getLayerSize() * vec.getWindow());
        c.setHiddenUnit(RBM.HiddenUnit.RECTIFIED);
        c.setVisibleUnit(RBM.VisibleUnit.GAUSSIAN);
        c.setMultiLayerClazz(DBN.class);
        c.setUseRegularization(false);
        c.setL2(2e-4f);
        c.setDeepLearningParams(new Object[]{1,1e-1,1000});
        ActorNetworkRunner runner = new ActorNetworkRunner("master",iter);
        runner.setModelSaver(new DefaultModelSaver(new File("word2vec-modelsaver.ser")));
        runner.setStateTracker(tracker);
        runner.setup(c);
        runner.train();


        iterator.reset();

        //viterbi optimization
        Viterbi viterbi = new Viterbi(NDArrays.create(new double[]{0, 1}));
        iter.reset();
        while(iter.hasNext()) {
            DataSet next = iter.next();

            Pair<Double,INDArray> decoded = viterbi.decode(next.getLabels());
            log.info("Pair " + decoded.getSecond());
        }


    }



}
