/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package kotlin.js

import kotlin.reflect.KClass
import kotlin.reflect.js.internal.KClassImpl

@native
interface JsClass<T : Any> {
    val name: String
}

@native
@Deprecated("Use class literal and extension property `js` instead.", replaceWith = ReplaceWith("T::class.js"))
fun <T : Any> jsClass(): JsClass<T> = noImpl

@Deprecated("Use class literal and extension property `js` instead.", replaceWith = ReplaceWith("this::class.js"))
val <T : Any> T.jsClass: JsClass<T>
    get() = js("Object").getPrototypeOf(this).constructor

val <T : Any> KClass<T>.js: JsClass<T>
    get() = (this as KClassImpl<T>).jClass

val <T : Any> JsClass<T>.kotlin: KClass<T>
    get() = KClassImpl<T>(this)
