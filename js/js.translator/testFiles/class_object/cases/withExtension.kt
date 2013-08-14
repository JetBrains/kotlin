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

open class A {
    val a = 3
    fun foo(): Int {
        return 5
    }
    class object: A() {
        val c = a
    }
}

class B {
    class object: A() {
    }
}

fun box() : String {
    if (A.a != 3) return "A.a != 3"
    if (A.foo() != 5) return "A.foo() != 5"

    val a = A
    if (a.c != 3) return "a = A; a.c != 3"

    if (A().a != 3) return "A().a != 3"

    if (B.a != 3) return "B.a != 3"
    val b = B
    if (b.foo() != 5) return "b = B; b.foo() != 5"
    return "OK"
}
