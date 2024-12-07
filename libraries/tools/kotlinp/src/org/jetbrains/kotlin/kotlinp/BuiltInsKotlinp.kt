/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp

import kotlin.metadata.*
import kotlin.metadata.internal.common.*

class BuiltInsKotlinp(settings: Settings) : Kotlinp(settings) {
    fun printBuiltInsFile(metadata: KotlinCommonMetadata?): String = printString {
        if (metadata != null) renderModuleFragment(metadata.kmModuleFragment, this)
        else appendLine("unsupported file")
    }

    fun renderModuleFragment(fragment: KmModuleFragment, printer: Printer): Unit = with(printer) {
        var first = true
        fragment.pkg?.let {
            renderPackage(it, printer)
            first = false
        }
        for (klass in fragment.classes) {
            if (first) first = false else appendLine()
            renderClass(klass, printer)
        }
    }

    override fun getAnnotations(clazz: KmClass) = extension { clazz.annotations }
    override fun getAnnotations(constructor: KmConstructor) = extension { constructor.annotations }
    override fun getAnnotations(function: KmFunction) = extension { function.annotations }
    override fun getAnnotations(property: KmProperty) = extension { property.annotations }
    override fun getGetterAnnotations(property: KmProperty) = extension { property.getterAnnotations }
    override fun getSetterAnnotations(property: KmProperty) = extension { property.setterAnnotations }
    override fun getAnnotations(typeParameter: KmTypeParameter) = extension { typeParameter.annotations }
    override fun getAnnotations(type: KmType) = extension { type.annotations }
    override fun getAnnotations(valueParameter: KmValueParameter) = extension { valueParameter.annotations }

    override fun sortConstructors(constructors: List<KmConstructor>): List<KmConstructor> =
        constructors.sortedBy { render(it, ::renderConstructor) }

    override fun sortFunctions(functions: List<KmFunction>): List<KmFunction> =
        functions.sortedBy { render(it, ::renderFunction) }

    override fun sortProperties(properties: List<KmProperty>): List<KmProperty> =
        properties.sortedBy { render(it, ::renderProperty) }

    override fun Printer.appendEnumEntries(clazz: KmClass) {
        clazz.enumEntries.forEach { enumEntry ->
            appendLine()
            appendLine(enumEntry, ",")
        }
    }

    private inline fun <T> render(declaration: T, block: (T, Printer) -> Unit): String {
        val printer = StringBuilderPrinter()
        block(declaration, printer)
        return printer.toString()
    }

    override fun Printer.appendCompileTimeConstant(property: KmProperty): Printer {
        val compileTimeValue = extension { property.compileTimeValue }
        if (compileTimeValue != null) {
            renderAnnotationArgument(compileTimeValue, this)
            return this
        } else {
            return append("...")
        }
    }

    private inline fun <T> extension(lambda: BuiltInExtensionsAccessor.() -> T): T = with(BuiltInExtensionsAccessor, lambda)
}
