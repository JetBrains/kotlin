/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jspo.compiler.backend

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.toPluginKey
import org.jetbrains.kotlin.ir.expressions.impl.IrAnnotationImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlinx.jspo.compiler.resolve.JsPlainObjectsPluginKey
import kotlin.collections.plus

class GeneratedCompanionJsExportIgnoreTransformer(
    private val pluginContext: IrPluginContext,
) : IrVisitorVoid() {

    private val jsExportIgnoreClass: IrClassSymbol? by lazy {
        val jsExportClassId = ClassId(FqName("kotlin.js"), Name.identifier("JsExport"))
        val jsExportIgnoreClassId = jsExportClassId.createNestedClassId(Name.identifier("Ignore"))
        pluginContext.finderForBuiltins().findClass(jsExportIgnoreClassId)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.origin.toPluginKey<JsPlainObjectsPluginKey>() != null) {
            declaration.excludeFromJsExport()
        }

        visitElement(declaration)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun IrDeclaration.excludeFromJsExport() {
        if (!pluginContext.platform.isJs()) {
            return
        }
        val jsExportIgnore = jsExportIgnoreClass?.owner ?: return
        val jsExportIgnoreCtor = jsExportIgnore.primaryConstructor ?: return

        // Already annotated with JsExport.Ignore.
        if (annotations.any { it.classSymbol == jsExportIgnore.symbol }) return

        annotations += IrAnnotationImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            jsExportIgnore.defaultType,
            jsExportIgnoreCtor.symbol,
            jsExportIgnore.typeParameters.size,
            jsExportIgnoreCtor.typeParameters.size,
        )
    }
}
