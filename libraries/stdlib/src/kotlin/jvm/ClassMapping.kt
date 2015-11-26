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
@file:kotlin.jvm.JvmName("ClassMapping")
@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package kotlin.jvm

import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass

@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun <T: Any> KClass<T>.getJava(): Class<T> = this.java

@Deprecated("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)
public fun <T : Any> Class<T>.getKotlin(): KClass<T> = Reflection.createKotlinClass(this) as KClass<T>

