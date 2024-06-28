/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi

object StubRenderer {
    fun render(stub: ObjCExportStub): List<String> = render(stub, false)

    private fun findPositionToInsertGeneratedCommentLine(kDoc: List<String>, generatedCommentLine: String): Int {
        val generatedWords = generatedCommentLine.trim().split(" ").map { it.trim() }
        if (generatedWords.size >= 2 && generatedWords[0] == "@param") {
            for (i in kDoc.indices.reversed()) {
                val kDocLineWords = kDoc[i].trim().split(" ").map { it.trim() }.filter { it.isNotEmpty() }.filterNot { it == "*" }
                if (kDocLineWords.size >= 2 && kDocLineWords[0] == generatedWords[0] && kDocLineWords[1] == generatedWords[1]) {
                    return i + 1  // position after last `@param` kDoc line, describing same parameter as in generatedCommentLine
                }
            }
        }
        return kDoc.size
    }

    @InternalKotlinNativeApi
    fun render(stub: ObjCExportStub, shouldExportKDoc: Boolean): List<String> = collect {
        stub.run {
            val (kDocEnding, commentBlockEnding) = if (comment?.contentLines == null) {
                Pair("*/", null)  // Close kDoc with `*/`, and print nothing after empty comment
            } else {
                Pair("", "*/")  // Don't terminate kDoc, though close comment block with `*/`
            }
            val kDoc = if (shouldExportKDoc) {
                origin?.kdoc?.let {
                    if (it.startsWith("/**") && it.endsWith("*/")) {
                        // Nested comment is allowed inside of preformatted ``` block in kdoc but not in ObjC
                        val kdocClean = "/**${it.substring(3, it.length - 2).replace("*/", "**").replace("/*", "**")}$kDocEnding"
                        kdocClean.lines().map {
                            it.trim().let {
                                if (it.isNotEmpty() && it[0] == '*') " $it"
                                else it
                            }
                        }
                    } else null
                }
            } else null

            val kDocAndComment = kDoc?.filterNot { it.isEmpty() }.orEmpty().toMutableList()
            comment?.contentLines?.let { commentLine ->
                if (!kDoc.isNullOrEmpty()) kDocAndComment.add(" *")  // Separator between nonempty kDoc and nonempty comment
                commentLine.forEach { kDocAndComment.add(findPositionToInsertGeneratedCommentLine(kDocAndComment, it), " * $it") }
            }
            if (kDocAndComment.isNotEmpty()) {
                +"" // Probably makes the output more readable.
                if (kDoc.isNullOrEmpty()) +"/**"  // Start comment block, in case kDoc was empty
                kDocAndComment.forEach {
                    +it
                }
                commentBlockEnding?.let { +it }
            }

            when (this) {
                is ObjCProtocol -> {
                    attributes.forEach {
                        +renderAttribute(it)
                    }
                    +renderProtocolHeader()
                    +"@required"
                    renderMembers(this, shouldExportKDoc)
                    +"@end"
                }
                is ObjCInterface -> {
                    attributes.forEach {
                        +renderAttribute(it)
                    }
                    +renderInterfaceHeader()
                    renderMembers(this, shouldExportKDoc)
                    +"@end"
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
            assert(
                method.selectors.size == method.parameters.size ||
                    method.selectors.size == 1 && method.parameters.size == 0
            )

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

    private fun StringBuilder.appendSuperProtocols(clazz: ObjCClass) {
        val protocols = clazz.superProtocols
        if (protocols.isNotEmpty()) {
            protocols.joinTo(this, separator = ", ", prefix = " <", postfix = ">")
        }
    }

    private fun ObjCInterface.renderInterfaceHeader() = buildString {
        fun appendSuperClass() {
            if (superClass != null) append(" : $superClass")
            formatGenerics(this, superClassGenerics)
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

    private fun Collector.renderMembers(clazz: ObjCClass, shouldExportKDoc: Boolean) {
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

fun formatGenerics(buffer: Appendable, generics: List<Any>) {
    if (generics.isNotEmpty()) {
        generics.joinTo(buffer, separator = ", ", prefix = "<", postfix = ">")
    }
}
