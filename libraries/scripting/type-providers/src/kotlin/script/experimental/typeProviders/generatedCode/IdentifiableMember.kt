/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

/**
 * References a member that can be used as a type
 */
interface IdentifiableMember {
    val name: String
    fun imports(): Set<String> = emptySet()

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        inline operator fun <reified T> invoke(): IdentifiableMember = invoke(typeOf<T>())

        operator fun invoke(kType: KType): IdentifiableMember = KTypeIdentifiable(kType)
        operator fun invoke(kClass: KClass<*>): IdentifiableMember = KClassIdentifiable(kClass)
        operator fun invoke(name: String): IdentifiableMember = NamedIdentifiable(name)
    }
}

private class KTypeIdentifiable(kType: KType) : IdentifiableMember {
    override val name: String = kType.toString()

    private val imports = setOfNotNull(kType.jvmErasure.qualifiedName) +
            kType.arguments.mapNotNull { it.type?.jvmErasure?.qualifiedName }.toSet()

    override fun imports(): Set<String> = imports
}

private class KClassIdentifiable(kClass: KClass<*>) : IdentifiableMember {
    override val name: String = kClass.simpleName ?: kClass.qualifiedName!!
    private val import = kClass.qualifiedName

    override fun imports() = setOfNotNull(import)
}

private class NamedIdentifiable(override val name: String) : IdentifiableMember
