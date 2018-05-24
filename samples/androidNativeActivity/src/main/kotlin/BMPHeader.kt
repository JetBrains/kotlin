/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package NativeApplication

import kotlinx.cinterop.*

internal class BMPHeader(val rawPtr: NativePtr) {
    inline fun <reified T : CPointed> memberAt(offset: Long): T {
        return interpretPointed<T>(this.rawPtr + offset)
    }

    val magic get() = memberAt<ShortVar>(0).value.toInt()
    val size get() = memberAt<IntVar>(2).value
    val zero get() = memberAt<IntVar>(6).value
    val width get() = memberAt<IntVar>(18).value
    val height get() = memberAt<IntVar>(22).value
    val bits get() = memberAt<ShortVar>(28).value.toInt()
    val data get() = interpretCPointer<ByteVar>(rawPtr + 54) as CArrayPointer<ByteVar>
}