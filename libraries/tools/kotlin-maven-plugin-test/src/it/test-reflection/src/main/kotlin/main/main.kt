/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package main

import kotlin.reflect.*
import kotlin.reflect.full.*

fun topLevelFun() {}

class A(val prop: String)

val Int.extProp: Int get() = this

fun box(): String {
    val u = ::topLevelFun
    u()

    fun localFun() {}
    val l = ::localFun
    l()

    val ext = Int::extProp
    if (ext.get(42) != 42) return "Fail ext: ${ext.get(42)}"

    val a = A::class
    if (a.memberProperties.size != 1) return "Fail: ${a.memberProperties}"

    val p = A::prop
    if (p.name != "prop") return "Fail name: ${p.name}"

    return p.get(A("OK"))
}
