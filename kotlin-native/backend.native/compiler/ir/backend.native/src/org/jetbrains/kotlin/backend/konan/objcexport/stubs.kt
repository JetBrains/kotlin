/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.source.PsiSourceElement

class ObjCComment(val contentLines: List<String>) {
    constructor(vararg contentLines: String) : this(contentLines.toList())
}

data class ObjCClassForwardDeclaration(
        val className: String,
        val typeDeclarations: List<ObjCGenericTypeDeclaration> = emptyList()
)

abstract class Stub<out D : DeclarationDescriptor>(val name: String, val comment: ObjCComment? = null) {
    abstract val descriptor: D?
    open val psi: PsiElement?
        get() = ((descriptor as? DeclarationDescriptorWithSource)?.source as? PsiSourceElement)?.psi
    open val isValid: Boolean
        get() = descriptor?.module?.isValid ?: true
}

abstract class ObjCTopLevel<out D : DeclarationDescriptor>(name: String) : Stub<D>(name)

abstract class ObjCClass<out D : DeclarationDescriptor>(name: String,
                                                        val attributes: List<String>) : ObjCTopLevel<D>(name) {
    abstract val superProtocols: List<String>
    abstract val members: List<Stub<*>>
}

abstract class ObjCProtocol(name: String,
                            attributes: List<String>) : ObjCClass<ClassDescriptor>(name, attributes)

class ObjCProtocolImpl(
        name: String,
        override val descriptor: ClassDescriptor,
        override val superProtocols: List<String>,
        override val members: List<Stub<*>>,
        attributes: List<String> = emptyList()) : ObjCProtocol(name, attributes)

abstract class ObjCInterface(name: String,
                             val generics: List<ObjCGenericTypeDeclaration>,
                             val categoryName: String?,
                             attributes: List<String>) : ObjCClass<ClassDescriptor>(name, attributes) {
    abstract val superClass: String?
    abstract val superClassGenerics: List<ObjCNonNullReferenceType>
}

class ObjCInterfaceImpl(
        name: String,
        generics: List<ObjCGenericTypeDeclaration> = emptyList(),
        override val descriptor: ClassDescriptor? = null,
        override val superClass: String? = null,
        override val superClassGenerics: List<ObjCNonNullReferenceType> = emptyList(),
        override val superProtocols: List<String> = emptyList(),
        categoryName: String? = null,
        override val members: List<Stub<*>> = emptyList(),
        attributes: List<String> = emptyList()
) : ObjCInterface(name, generics, categoryName, attributes)

class ObjCMethod(
        override val descriptor: DeclarationDescriptor?,
        val isInstanceMethod: Boolean,
        val returnType: ObjCType,
        val selectors: List<String>,
        val parameters: List<ObjCParameter>,
        val attributes: List<String>,
        comment: ObjCComment? = null
) : Stub<DeclarationDescriptor>(buildMethodName(selectors, parameters), comment)

class ObjCParameter(name: String,
                    override val descriptor: ParameterDescriptor?,
                    val type: ObjCType) : Stub<ParameterDescriptor>(name)

class ObjCProperty(name: String,
                   override val descriptor: DeclarationDescriptorWithSource?,
                   val type: ObjCType,
                   val propertyAttributes: List<String>,
                   val setterName: String? = null,
                   val getterName: String? = null,
                   val declarationAttributes: List<String> = emptyList()) : Stub<DeclarationDescriptorWithSource>(name) {

    @Deprecated("", ReplaceWith("this.propertyAttributes"), DeprecationLevel.WARNING)
    val attributes: List<String> get() = propertyAttributes
}

private fun buildMethodName(selectors: List<String>, parameters: List<ObjCParameter>): String =
        if (selectors.size == 1 && parameters.size == 0) {
            selectors[0]
        } else {
            assert(selectors.size == parameters.size)
            selectors.joinToString(separator = "")
        }
