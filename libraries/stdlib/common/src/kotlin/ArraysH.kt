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

private val hexArray = CharArray(16) { if (it < 10) '0' + it else 'a' + (it - 10) }

/** Creates a string from bytes represented as hexadecimal numbers. */
fun ByteArray.toHexString(): String {
    val builder = StringBuilder(size * 2)
    for (byte in this) {
        val v = byte.toInt() and 0xFF
        builder.append(hexArray[v.ushr(4)])
        builder.append(hexArray[v and 0x0F])
    }
    return builder.toString()
}