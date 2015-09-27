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

package kotlin.jvm

import kotlin.jvm.internal.ClassBasedDeclarationContainer
import kotlin.jvm.internal.Intrinsic
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass

/**
 * Returns a Java [Class] instance corresponding to the given [KClass] instance.
 */
@Intrinsic("kotlin.KClass.java.property")
public val <T : Any> KClass<T>.java: Class<T>
    get() = (this as ClassBasedDeclarationContainer).jClass as Class<T>

/**
 * Returns a [KClass] instance corresponding to the given Java [Class] instance.
 */
public val <T : Any> Class<T>.kotlin: KClass<T>
    get() = Reflection.createKotlinClass(this) as KClass<T>
