/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.cexport

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.referenceFunction

/**
 * Bind CExported elements to their IR counterparts.
 *
 **/
@OptIn(ObsoleteDescriptorBasedAPI::class)
internal class CAdapterCodegenElementsBuilder(
    private val symbolTable: SymbolTable,
) {
    fun buildAllCodegenElementsRecursively(elements: CAdapterExportedElements): List<CAdapterCodegenElement> {
        val codegenElements = mutableListOf<CAdapterCodegenElement>()
        val top = elements.scopes.first()
        assert(top.kind == ScopeKind.TOP)
        top.generateCAdapters(codegenElements, this::buildCodegenAdapter)
        return codegenElements.toList()
    }

    private fun ExportedElementScope.generateCAdapters(
        codegenElements: MutableList<CAdapterCodegenElement>,
        builder: (ExportedElement) -> CAdapterCodegenElement
    ) {
        codegenElements += this.elements.map { builder(it) }
        this.scopes.forEach { it.generateCAdapters(codegenElements, builder) }
    }

    private fun buildCodegenAdapter(element: ExportedElement): CAdapterCodegenElement {
        when {
            element.isFunction -> {
                val symbol = symbolTable.referenceFunction(element.declaration as FunctionDescriptor)
                return CAdapterCodegenElement.Function(symbol, element)
            }
            element.isClass -> {
                val symbol = symbolTable.descriptorExtension.referenceClass(element.declaration as ClassDescriptor)
                return CAdapterCodegenElement.Class(symbol, element)
            }
            element.isEnumEntry -> {
                val symbol = symbolTable.descriptorExtension.referenceEnumEntry(element.declaration as ClassDescriptor)
                return CAdapterCodegenElement.EnumEntry(symbol, element)
            }
            else -> error("Unexpected kind ${element.kind} of ExportedElement: $element")
        }
    }
}