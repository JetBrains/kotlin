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

class A {
    val a = 3
    class object {
        val a = 2
        val b = 5
    }
}



fun box() : String {
    if (A.a != 2) return "A.a != 2, it: ${A.a}"
    if (A.b != 5) return "A.b != 5, it: ${A.b}"

    val b = A
    if (b.a != 2) return "b = A; b != 2, it: ${b.a}"

    if (A().a != 3) return "A().a != 3, it: ${A().a}"

    return "OK"
}
