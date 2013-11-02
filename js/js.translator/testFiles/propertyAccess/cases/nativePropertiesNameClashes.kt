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

val PACKAGE = "Kotlin.modules.JS_TESTS.foo"

native fun eval(e: String): Any? = noImpl

class A
native val Any.__proto__: String = noImpl
native val A.__proto__: String = noImpl

fun box(): String {
    val a = A()
    val any: Any = a
    val protoA = eval("$PACKAGE.A.prototype")
    if (a.__proto__ != any.__proto__ || a.__proto__ != protoA)
        return "a.__proto__ != any.__proto__ /*${a.__proto__ != any.__proto__}*/ || a.__proto__ != $PACKAGE.A.prototype /*${a.__proto__ != protoA}*/"

    return "OK"
}
