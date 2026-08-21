/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators.kotlin.ir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.lombok.generators.BuilderGeneratorKey
import org.jetbrains.kotlin.lombok.generators.EqualsAndHashCodeGeneratorKey
import org.jetbrains.kotlin.lombok.generators.LombokDeclarationKey
import org.jetbrains.kotlin.lombok.generators.ToStringGeneratorKey
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KClass

class LombokIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.acceptChildrenVoid(IrBodyBuilderVisitor(pluginContext))
    }
}

class IrBodyBuilderVisitor(private val context: IrPluginContext) : IrVisitorVoid() {
    private val bodyBuilders: Map<KClass<out LombokDeclarationKey>, IrBodyBuilder<out LombokDeclarationKey>> = mapOf(
        ToStringGeneratorKey::class to ToStringBodyBuilder,
        EqualsAndHashCodeGeneratorKey::class to EqualsAndHashCodeIrBodyBuilder,
        BuilderGeneratorKey::class to BuilderBodyBuilder,
    )

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        val generatorKey = (declaration.origin as? GeneratedByPlugin)?.pluginKey
        val bodyBuilder = generatorKey?.let { bodyBuilders[it::class] }

        if (bodyBuilder != null) {
            declaration.body = DeclarationIrBuilder(context, declaration.symbol).irBlockBody {
                when (bodyBuilder) {
                    is ToStringBodyBuilder -> {
                        with(bodyBuilder) {
                            build(generatorKey as ToStringGeneratorKey, declaration)
                        }
                    }
                    is EqualsAndHashCodeIrBodyBuilder -> {
                        with(bodyBuilder) {
                            build(generatorKey as EqualsAndHashCodeGeneratorKey, declaration)
                        }
                    }
                    is BuilderBodyBuilder -> {
                        with(bodyBuilder) {
                            build(generatorKey as BuilderGeneratorKey, declaration)
                        }
                    }
                }
            }
        } else {
            declaration.acceptChildrenVoid(this)
        }
    }
}

sealed class IrBodyBuilder<T : GeneratedDeclarationKey> {
    abstract fun IrBlockBodyBuilder.build(key: T, declaration: IrSimpleFunction)

    /** The builders are always invoked from [IrBodyBuilderVisitor], which passes its [IrPluginContext] to the body builder. */
    protected val IrBuilderWithScope.pluginContext: IrPluginContext
        get() = context as IrPluginContext

    /**
     * The `java.util.Arrays` function Lombok routes an array property through, so that it is rendered, compared
     * and hashed by content rather than by identity: the shallow overload named [primitiveArrayName] for a
     * primitive array, and the deep one named [objectArrayName] for the rest - an object array is always treated
     * deeply, even a one-dimensional one.
     *
     * Shared so that `toString` and `equals`/`hashCode` cannot drift apart on where that split falls.
     *
     * Returns `null` for a non-array [type], and also if `java.util.Arrays` can't be resolved, in which case the
     * caller falls back to whatever it does for an ordinary property.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    protected fun IrBuilderWithScope.findArraysFunctionByContent(
        type: IrType?,
        primitiveArrayName: Name,
        objectArrayName: Name,
        parameterCount: Int,
    ): IrSimpleFunction? {
        val builtIns = context.irBuiltIns
        val classifier = type?.classifierOrNull ?: return null
        val isPrimitiveArray = classifier in builtIns.primitiveArraysToPrimitiveTypes
        if (!isPrimitiveArray && classifier != builtIns.arrayClass) return null

        val arraysClass = pluginContext.finderForBuiltins().findClass(LombokNames.JAVA_ARRAYS_ID) ?: return null
        val name = if (isPrimitiveArray) primitiveArrayName else objectArrayName
        return arraysClass.owner.functions.firstOrNull { function ->
            function.name == name &&
                    function.parameters.size == parameterCount &&
                    function.parameters.all { it.type.classifierOrNull == classifier }
        }
    }
}
