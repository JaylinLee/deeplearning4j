/*******************************************************************************
 * Copyright (c) 2015-2018 Skymind, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

//
//  @author raver119@gmail.com
//

#include <op_boilerplate.h>
#if NOT_EXCLUDED(OP_skipgram)

#include <ops/declarable/CustomOperations.h>

namespace nd4j {
    namespace ops {
        CUSTOM_OP_IMPL(cbow, 3, 1, true, 0, 0) {

            return Status::OK();
        }

        DECLARE_TYPES(cbow) {
            getOpDescriptor()
                    ->setAllowedInputTypes(nd4j::DataType::ANY)
                    ->setAllowedOutputTypes({ALL_FLOATS});
        }

        DECLARE_SHAPE_FN(cbow) {
            return SHAPELIST(ShapeBuilders::createScalarShapeInfo(DataType::INT8, block.getWorkspace()));
        }
    }
}

#endif