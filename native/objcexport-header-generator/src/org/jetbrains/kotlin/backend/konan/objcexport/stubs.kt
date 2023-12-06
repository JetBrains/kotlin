/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("ObjCExportStubKt")

package org.jetbrains.kotlin.backend.konan.objcexport

@Deprecated("Use 'ObjCExportStub' instead", replaceWith = ReplaceWith("ObjCExportStub"))
@Suppress("unused")
typealias Stub<@Suppress("UNUSED_TYPEALIAS_PARAMETER") T> = ObjCExportStub

sealed interface ObjCExportStub {
    /**
     * The ObjC name of this entity;
     * Note: The original 'Kotlin Name' can be found in [origin]
     */
    val name: String

    val comment: ObjCComment?


    /**
     * Leaves breadcrumbs, minimal information, about the origin of this stub.
     * A [origin] can either be
     * - [ObjCExportStubOrigin.Source] indicating that the stub was produced from Source Code (happens inside the IDE)
     * - [ObjCExportStubOrigin.Binary] indicating that the stub was produced by deserializing a klib (Note: CLI only works in this mode)
     * - null: Indicating that we not provide information about the origin of this stub. This can happen e.g.
     * if the stub is just synthetically created by this tool.
     */
    val origin: ObjCExportStubOrigin?
}

val ObjCExportStub.psiOrNull
    get() = when (val origin = origin) {
        is ObjCExportStubOrigin.Source -> origin.psi
        else -> null
    }


abstract class ObjCTopLevel : ObjCExportStub

sealed class ObjCClass : ObjCTopLevel() {
    abstract val attributes: List<String>
    abstract val superProtocols: List<String>
    abstract val members: List<ObjCExportStub>
}

abstract class ObjCProtocol : ObjCClass()

abstract class ObjCInterface : ObjCClass() {
    abstract val categoryName: String?
    abstract val generics: List<ObjCGenericTypeDeclaration>
    abstract val superClass: String?
    abstract val superClassGenerics: List<ObjCNonNullReferenceType>
}

class ObjCComment(val contentLines: List<String>) {
    constructor(vararg contentLines: String) : this(contentLines.toList())
}

data class ObjCClassForwardDeclaration(
    val className: String,
    val typeDeclarations: List<ObjCGenericTypeDeclaration> = emptyList(),
)

class ObjCProtocolImpl(
    override val name: String,
    override val comment: ObjCComment?,
    override val origin: ObjCExportStubOrigin?,
    override val attributes: List<String>,
    override val superProtocols: List<String>,
    override val members: List<ObjCExportStub>,
) : ObjCProtocol()

class ObjCInterfaceImpl(
    override val name: String,
    override val comment: ObjCComment?,
    override val origin: ObjCExportStubOrigin?,
    override val attributes: List<String>,
    override val superProtocols: List<String>,
    override val members: List<ObjCExportStub>,
    override val categoryName: String?,
    override val generics: List<ObjCGenericTypeDeclaration>,
    override val superClass: String?,
    override val superClassGenerics: List<ObjCNonNullReferenceType>,
) : ObjCInterface()

class ObjCMethod(
    override val comment: ObjCComment?,
    override val origin: ObjCExportStubOrigin?,
    val isInstanceMethod: Boolean,
    val returnType: ObjCType,
    val selectors: List<String>,
    val parameters: List<ObjCParameter>,
    val attributes: List<String>,
) : ObjCExportStub {
    override val name: String = buildMethodName(selectors, parameters)
}

class ObjCParameter(
    override val name: String,
    override val origin: ObjCExportStubOrigin?,
    val type: ObjCType,
    val todo: Nothing?
) : ObjCExportStub {
    override val comment: Nothing? = null
}

class ObjCProperty(
    override val name: String,
    override val comment: ObjCComment?,
    override val origin: ObjCExportStubOrigin?,
    val type: ObjCType,
    val propertyAttributes: List<String>,
    val setterName: String? = null,
    val getterName: String? = null,
    val declarationAttributes: List<String> = emptyList(),
) : ObjCExportStub

private fun buildMethodName(selectors: List<String>, parameters: List<ObjCParameter>): String =
    if (selectors.size == 1 && parameters.size == 0) {
        selectors[0]
    } else {
        assert(selectors.size == parameters.size)
        selectors.joinToString(separator = "")
    }
