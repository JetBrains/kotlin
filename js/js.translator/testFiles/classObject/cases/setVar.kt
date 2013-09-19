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
    var a = 3
    class object {
        var a = -2
    }
}



fun box() : String {
    A.a = 2
    if (A.a != 2) return "A.a != 2"

    val a = A
    a.a = 3
    if (a.a != 3) return "a = A; a.a = 3; a != 3"

    if (A().a != 3) return "A().a != 3"

    val x = A()
    x.a = 4
    if (x.a != 4) return "x = A(); x.a = 4; x.a != 4"

    return "OK"
}
