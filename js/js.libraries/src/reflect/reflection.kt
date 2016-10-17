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

// a package is omitted to get declarations directly under the module

import kotlin.reflect.KClass
import kotlin.reflect.js.internal.KClassImpl

@JsName("getKClass")
internal fun <T : Any> getKClass(jClass: JsClass<T>): KClass<T> = getOrCreateKClass(jClass)
@JsName("getKClassFromExpression")
internal fun <T : Any> getKClassFromExpression(e: T): KClass<T> = getOrCreateKClass(e.jsClass)

private fun <T : Any> getOrCreateKClass(jClass: JsClass<T>): KClass<T> {
    val metadata = jClass.asDynamic().`$metadata$`

    return if (metadata != null) {
        if (metadata.`$kClass$` == null) {
            val kClass = KClassImpl(jClass)
            metadata.`$kClass$` = kClass
            kClass
        }
        else {
            metadata.`$kClass$`
        }
    }
    else {
        KClassImpl(jClass)
    }
}

