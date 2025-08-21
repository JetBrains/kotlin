/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.klib

import kotlinx.metadata.klib.*
import org.jetbrains.kotlin.kotlinp.Kotlinp
import org.jetbrains.kotlin.kotlinp.Printer
import org.jetbrains.kotlin.kotlinp.Settings
import kotlin.metadata.*
import kotlin.metadata.internal.common.KmModuleFragment

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
            moduleFragment.classes.sortIfNeeded { it.sortedBy(KmClass::name) }.forEach { appendLine(); renderClass(it, printer) }
            moduleFragment.pkg?.let { appendLine(); renderPackage(it, printer) }
        }
        appendLine("}")
    }

    override fun getAnnotations(typeParameter: KmTypeParameter): List<KmAnnotation> = typeParameter.annotations
    override fun getAnnotations(type: KmType): List<KmAnnotation> = type.annotations

    override fun Printer.appendSignatures(clazz: KmClass) = appendSignature { classSignature(clazz) }
    override fun Printer.appendSignatures(constructor: KmConstructor) = appendSignature { constructorSignature(constructor) }
    override fun Printer.appendSignatures(function: KmFunction) = appendSignature { functionSignature(function) }
    override fun Printer.appendSignatures(property: KmProperty) = appendSignature { propertySignature(property) }
    override fun Printer.appendGetterSignatures(property: KmProperty) = appendSignature { propertyGetterSignature(property) }
    override fun Printer.appendSetterSignatures(property: KmProperty) = appendSignature { propertySetterSignature(property) }
    override fun Printer.appendSignatures(typeAlias: KmTypeAlias) = appendSignature { typeAliasSignature(typeAlias) }
    override fun Printer.appendSignatures(enumEntry: KmEnumEntry) = appendSignature { enumEntrySignature(enumEntry) }

    private inline fun Printer.appendSignature(extractSignature: ExternalSignatureComputer.() -> String?) {
        val signature = signatureComputer?.let(extractSignature) ?: return
        appendCommentedLine("signature: ", signature)
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
