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

package org.jetbrains.ring
//-----------------------------------------------------------------------------//

actual class Random actual constructor() {
    actual companion object {
        actual var seedInt = 0
        actual fun nextInt(boundary: Int): Int {
            seedInt = (3 * seedInt + 11) % boundary
            return seedInt
        }

        actual var seedDouble: Double = 0.1
        actual fun nextDouble(boundary: Double): Double {
            seedDouble = (7.0 * seedDouble + 7.0) % boundary
            return seedDouble
        }
    }
}