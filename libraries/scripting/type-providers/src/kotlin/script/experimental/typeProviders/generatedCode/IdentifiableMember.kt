/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf
import kotlin.script.experimental.typeProviders.generatedCode.impl.appendJoined
import kotlin.script.experimental.typeProviders.generatedCode.impl.GeneratedInterface

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

fun IdentifiableMember.withParameters(vararg parameters: IdentifiableMember): IdentifiableMember {
    return GenericIdentifiableMember(this, parameters.toList())
}

fun IdentifiableMember.withParameters(vararg parameters: KClass<*>): IdentifiableMember {
    return GenericIdentifiableMember(this, parameters.map { IdentifiableMember(it) })
}

fun KClass<*>.withParameters(vararg parameters: IdentifiableMember): IdentifiableMember {
    return IdentifiableMember(this).withParameters(*parameters)
}

fun KClass<*>.withParameters(vararg parameters: KClass<*>): IdentifiableMember {
    return IdentifiableMember(this).withParameters(*parameters)
}

fun IdentifiableMember.optional(): IdentifiableMember {
    return OptionalIdentifiableMember(this)
}

fun KClass<*>.optional(): IdentifiableMember {
    return IdentifiableMember(this).optional()
}

fun IdentifiableMember.asInterface(): GeneratedInterface = CastedInterfaceMember(this)

private class GenericIdentifiableMember(
    private val member: IdentifiableMember,
    private val parameters: List<IdentifiableMember>
) : IdentifiableMember {
    override val name: String = buildString {
        append(member.name)
        append("<")
        appendJoined(parameters, ", ") {
            append(it.name)
        }
        append(">")
    }

    override fun imports(): Set<String> = (parameters + member).map { it.imports() }.flatten().toSet()
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

private class OptionalIdentifiableMember(private val member: IdentifiableMember) : IdentifiableMember {
    override val name: String = "${member.name}?"
    override fun imports() = member.imports()
}

private class CastedInterfaceMember(private val member: IdentifiableMember) : GeneratedInterface, IdentifiableMember by member