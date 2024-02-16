/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.klib

import kotlin.metadata.*
import kotlin.metadata.internal.common.KmModuleFragment
import kotlinx.metadata.klib.*
import org.jetbrains.kotlin.kotlinp.Kotlinp
import org.jetbrains.kotlin.kotlinp.Settings
import org.jetbrains.kotlin.kotlinp.Printer

class KlibKotlinp(
    settings: Settings,
    private val signatureComputer: ExternalSignatureComputer?,
) : Kotlinp(settings) {
    fun renderModule(module: KlibModuleMetadata, printer: Printer): Unit = with(printer) {
        appendLine("library {")
        withIndent {
            appendCommentedLine("module name: ", module.name)
            module.fragments.forEach { renderModuleFragment(it, printer) }
        }
        appendLine("}")
    }

    private fun renderModuleFragment(moduleFragment: KmModuleFragment, printer: Printer): Unit = with(printer) {
        appendLine()
        appendLine("library fragment {")
        withIndent {
            moduleFragment.fqName?.let {
                appendCommentedLine("package name: ", it.ifEmpty { "<root>" })
            }
            if (moduleFragment.className.isNotEmpty()) {
                appendLine()
                moduleFragment.className.sortIfNeeded { it }.forEach {
                    appendCommentedLine("class name: ", it)
                }
            }
            moduleFragment.classes.forEach { appendLine(); renderClass(it, printer) }
            moduleFragment.pkg?.let { appendLine(); renderPackage(it, printer) }
        }
        appendLine("}")
    }

    override fun getAnnotations(clazz: KmClass) = clazz.annotations
    override fun getAnnotations(constructor: KmConstructor) = constructor.annotations
    override fun getAnnotations(function: KmFunction) = function.annotations
    override fun getAnnotations(property: KmProperty) = property.annotations
    override fun getGetterAnnotations(property: KmProperty) = property.getterAnnotations
    override fun getSetterAnnotations(property: KmProperty) = property.setterAnnotations
    override fun getAnnotations(typeParameter: KmTypeParameter) = typeParameter.annotations
    override fun getAnnotations(type: KmType) = type.annotations
    override fun getAnnotations(valueParameter: KmValueParameter) = valueParameter.annotations

    override fun Printer.appendSignatures(clazz: KmClass) = appendSignature { classSignature(clazz) }
    override fun Printer.appendSignatures(constructor: KmConstructor) = appendSignature { constructorSignature(constructor) }
    override fun Printer.appendSignatures(function: KmFunction) = appendSignature { functionSignature(function) }
    override fun Printer.appendSignatures(property: KmProperty) = appendSignature { propertySignature(property) }
    override fun Printer.appendGetterSignatures(property: KmProperty) = appendSignature { propertyGetterSignature(property) }
    override fun Printer.appendSetterSignatures(property: KmProperty) = appendSignature { propertySetterSignature(property) }
    override fun Printer.appendSignatures(typeAlias: KmTypeAlias) = appendSignature { typeAliasSignature(typeAlias) }

    private inline fun Printer.appendSignature(extractSignature: ExternalSignatureComputer.() -> String?) {
        val signature = signatureComputer?.let(extractSignature) ?: return
        appendCommentedLine("signature: ", signature)
    }

    override fun Printer.appendEnumEntries(clazz: KmClass) {
        clazz.klibEnumEntries.forEach { enumEntry ->
            appendLine()
            appendSignature { enumEntrySignature(enumEntry) }
            appendAnnotations(hasAnnotations = null, enumEntry.annotations)
            appendLine(enumEntry.name, ",")
        }
    }

    override fun Printer.appendCompileTimeConstant(property: KmProperty): Printer {
        val compileTimeValue = property.compileTimeValue
        if (compileTimeValue != null) {
            renderAnnotationArgument(compileTimeValue, this)
            return this
        } else {
            return append("...")
        }
    }
}
