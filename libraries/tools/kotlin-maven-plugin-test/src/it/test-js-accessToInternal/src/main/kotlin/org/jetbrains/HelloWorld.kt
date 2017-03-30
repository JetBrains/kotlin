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

package org.jetbrains

internal val CONST = "CONST"

open class PublicClass {
    internal fun foo(): String = "foo"
    internal val bar: String = "bar"
    open internal fun baz(): String = "PublicClass.baz()"
}

internal data class InternalDataClass(val x: Int, val y: Int)

internal fun box(): String {
    return "OK"
}