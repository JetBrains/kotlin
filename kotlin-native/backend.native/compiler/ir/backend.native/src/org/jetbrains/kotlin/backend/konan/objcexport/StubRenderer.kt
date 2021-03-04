/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.backend.common.serialization.extractSerializedKdocString
import org.jetbrains.kotlin.backend.common.serialization.metadata.findKDocString

object StubRenderer {
    fun render(stub: Stub<*>): List<String> = render(stub, false)
    internal fun render(stub: Stub<*>, shouldExportKDoc: Boolean): List<String> = collect {
        stub.run {
            val kDoc = if (shouldExportKDoc) {
                this.descriptor?.extractKDocString()
            } else null
            kDoc?.let {
                +"" // Probably makes the output more readable.
                +it // Let's try to keep non-trivial kdoc formatting intact
            }

            this.comment?.let { comment ->
                kDoc?: let { +"" } // Probably makes the output more readable.
                +"/**"
                comment.contentLines.forEach {
                    +" $it"
                }
                +"*/"
            }

            when (this) {
                is ObjCProtocol -> {
                    attributes.forEach {
                        +renderAttribute(it)
                    }
                    +renderProtocolHeader()
                    +"@required"
                    renderMembers(this, shouldExportKDoc)
                    +"@end;"
                }
                is ObjCInterface -> {
                    attributes.forEach {
                        +renderAttribute(it)
                    }
                    +renderInterfaceHeader()
                    renderMembers(this, shouldExportKDoc)
                    +"@end;"
                }
                is ObjCMethod -> {
                    +renderMethod(this)
                }
                is ObjCProperty -> {
                    +renderProperty(this)
                }
                else -> throw IllegalArgumentException("unsupported stub: " + stub::class)
            }
        }
    }

    private fun renderProperty(property: ObjCProperty): String = buildString {
        fun StringBuilder.appendTypeAndName() {
            append(' ')
            append(property.type.render(property.name))
        }

        fun ObjCProperty.getAllAttributes(): List<String> {
            if (getterName == null && setterName == null) return propertyAttributes

            val allAttributes = propertyAttributes.toMutableList()
            getterName?.let { allAttributes += "getter=$it" }
            setterName?.let { allAttributes += "setter=$it" }
            return allAttributes
        }

        fun StringBuilder.appendAttributes() {
            val attributes = property.getAllAttributes()
            if (attributes.isNotEmpty()) {
                append(' ')
                attributes.joinTo(this, prefix = "(", postfix = ")")
            }
        }

        append("@property")
        appendAttributes()
        appendTypeAndName()
        appendPostfixDeclarationAttributes(property.declarationAttributes)
        append(';')
    }

    private fun renderMethod(method: ObjCMethod): String = buildString {
        fun appendStaticness() {
            if (method.isInstanceMethod) {
                append('-')
            } else {
                append('+')
            }
        }

        fun appendReturnType() {
            append(" (")
            append(method.returnType.render())
            append(')')
        }

        fun appendParameters() {
            assert(method.selectors.size == method.parameters.size ||
                   method.selectors.size == 1 && method.parameters.size == 0)

            if (method.selectors.size == 1 && method.parameters.size == 0) {
                append(method.selectors[0])
            } else {
                for (i in 0 until method.selectors.size) {
                    if (i > 0) append(' ')

                    val parameter = method.parameters[i]
                    val selector = method.selectors[i]
                    append(selector)
                    append("(")
                    append(parameter.type.render())
                    append(")")
                    append(parameter.name)
                }
            }
        }

        fun appendAttributes() {
            appendPostfixDeclarationAttributes(method.attributes)
        }

        appendStaticness()
        appendReturnType()
        appendParameters()
        appendAttributes()
        append(';')
    }

    private fun Appendable.appendPostfixDeclarationAttributes(attributes: List<kotlin.String>) {
        if (attributes.isNotEmpty()) this.append(' ')
        attributes.joinTo(this, separator = " ", transform = this@StubRenderer::renderAttribute)
    }

    private fun ObjCProtocol.renderProtocolHeader() = buildString {
        append("@protocol ")
        append(name)
        appendSuperProtocols(this@renderProtocolHeader)
    }

    private fun StringBuilder.appendSuperProtocols(clazz: ObjCClass<ClassDescriptor>) {
        val protocols = clazz.superProtocols
        if (protocols.isNotEmpty()) {
            protocols.joinTo(this, separator = ", ", prefix = " <", postfix = ">")
        }
    }

    private fun ObjCInterface.renderInterfaceHeader() = buildString {
        fun appendSuperClass() {
            if (superClass != null) append(" : $superClass")
            formatGenerics(this, superClassGenerics.map { it.render() })
        }

        fun appendGenerics() {
            formatGenerics(this, generics)
        }

        fun appendCategoryName() {
            if (categoryName != null) {
                append(" (")
                append(categoryName)
                append(')')
            }
        }

        append("@interface ")
        append(name)
        appendGenerics()
        appendCategoryName()
        appendSuperClass()
        appendSuperProtocols(this@renderInterfaceHeader)
    }

    private fun Collector.renderMembers(clazz: ObjCClass<*>, shouldExportKDoc: Boolean) {
        clazz.members.forEach {
            +render(it, shouldExportKDoc)
        }
    }

    private fun renderAttribute(attribute: String) = "__attribute__(($attribute))"

    private fun collect(p: Collector.() -> Unit): List<String> {
        val collector = Collector()
        collector.p()
        return collector.build()
    }

    private class Collector {
        private val collection: MutableList<String> = mutableListOf()
        fun build(): List<String> = collection

        operator fun String.unaryPlus() {
            collection += this
        }

        operator fun List<String>.unaryPlus() {
            collection += this
        }
    }
}

internal fun formatGenerics(buffer: Appendable, generics:List<String>) {
    if (generics.isNotEmpty()) {
        generics.joinTo(buffer, separator = ", ", prefix = "<", postfix = ">")
    }
}

private fun DeclarationDescriptor.extractKDocString(): String? {
    return (this as? DeclarationDescriptorWithSource)?.findKDocString()
            ?: extractSerializedKdocString()
}

