/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package samples.collections

import samples.*
import kotlin.test.*


@RunWith(Enclosed::class)
class Arrays {

    class ContentOperations {

        @Sample
        fun contentToString() {
            val array = arrayOf("apples", "oranges", "lime")

            assertPrints(array.contentToString(), "[apples, oranges, lime]")
        }

        @Sample
        fun contentDeepToString() {
            val matrix = arrayOf(
                    intArrayOf(3, 7, 9),
                    intArrayOf(0, 1, 0),
                    intArrayOf(2, 4, 8)
            )

            assertPrints(matrix.contentDeepToString(), "[[3, 7, 9], [0, 1, 0], [2, 4, 8]]")
        }

    }

}