/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package foo

fun bar(i: Int = 0): Int = if (i == 7) i else bar(i - 1)

fun box(): String {
    val a = bar(10)
    if (a != 7) return "bar(10) = $a, but expected 7"

    fun boo(i: Int = 0): Int = if (i == 4) i else boo(i - 1)
    val b = boo(17)
    if (b != 4) return "boo(17) = $b, but expected 4"

    fun f() = 1
    val v = 3
    fun baz(i: Int = 0): Int = if (i == v) f() + v else baz(i - 1)

    val c = baz(10)
    if (c != 4) return "baz(10) = $c, but expected 4"

    return "OK"
}
