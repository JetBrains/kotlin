/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

import kotlin.reflect.KCallable

@SinceKotlin("1.1")
public class PackageReference(
    override val jClass: Class<*>,
    @Suppress("unused") private val moduleName: String
) : ClassBasedDeclarationContainer {
    override val members: Collection<KCallable<*>>
        get() = throw KotlinReflectionNotSupportedError()

    override fun equals(other: Any?) =
        other is PackageReference && jClass == other.jClass

    override fun hashCode() =
        jClass.hashCode()

    override fun toString() =
        jClass.toString() + Reflection.REFLECTION_NOT_AVAILABLE
}
