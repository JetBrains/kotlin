/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.addBackingField
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addTypeParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.builders.declarations.buildReceiverParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.createDispatchReceiverParameterWithClassParent
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.plugin.sandbox.fir.fqn

/*
 * For files that contain at least one class annotated with @GenerateClassFamily,
 * generates four sibling top-level classes that exercise the
 * `IrGeneratedDeclarationsRegistrar.registerClassAsMetadataVisible` API across
 * different shapes:
 *
 *   1. Plain          — no generics, extends Any.
 *   2. WithGeneric<T> — has a type parameter, extends Any.
 *   3. ExtendsSource  — inherits a source-declared class (`SourceBase`).
 *   4. ExtendsPlain   — inherits another plugin-generated class (`Plain`).
 *
 * Each generated class carries a primary no-arg constructor, a `val x: Int = 42`
 * property with a backing field, and `fun foo(): String = "ok"` so the recursive
 * child registration in `registerClassAsMetadataVisible` is exercised in all
 * four scenarios.
 */
class GeneratedTopLevelClassIrGenerator(val context: IrPluginContext) : IrVisitorVoid() {
    companion object {
        private val ANNOTATION_FQN = "GenerateClassFamily".fqn()
        private val SOURCE_BASE_NAME = Name.identifier("SourceBase")
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFile(declaration: IrFile) {
        // Process children first so we don't mutate file.declarations while iterating.
        declaration.acceptChildrenVoid(this)
        val hasMarker = declaration.declarations.filterIsInstance<IrClass>()
            .any { it.annotations.hasAnnotation(ANNOTATION_FQN) }
        if (hasMarker) {
            generateClassFamily(declaration)
        }
    }

    private fun generateClassFamily(file: IrFile) {
        val sourceBase = file.declarations.filterIsInstance<IrClass>()
            .firstOrNull { it.name == SOURCE_BASE_NAME }
            ?: error("@GenerateClassFamily test data must declare a class named SourceBase")

        val plain = buildGeneratedClass(
            file, "Plain", isGeneric = false, superClass = null, includeXAndFoo = true, modality = Modality.OPEN
        )
        val withGeneric = buildGeneratedClass(
            file, "WithGeneric", isGeneric = true, superClass = null, includeXAndFoo = true, modality = Modality.OPEN
        )
        val extendsSource = buildGeneratedClass(
            file, "ExtendsSource", isGeneric = false, superClass = sourceBase, includeXAndFoo = true, modality = Modality.OPEN
        )
        // ExtendsPlain inherits x and foo from Plain (so cross-module reads exercise inheritance
        // through another plugin-generated class). Redeclaring would VerifyError-collide with
        // Plain's final getX() in JVM bytecode.
        val extendsPlain = buildGeneratedClass(
            file, "ExtendsPlain", isGeneric = false, superClass = plain, includeXAndFoo = false, modality = Modality.OPEN
        )

        // Sealed family: MySealed is a sealed class with two final subclasses. SubA/SubB inherit
        // x and foo from MySealed (same reason as ExtendsPlain — avoid getX() VerifyError).
        val mySealed = buildGeneratedClass(
            file, "MySealed", isGeneric = false, superClass = null, includeXAndFoo = true, modality = Modality.SEALED
        )
        val subA = buildGeneratedClass(
            file, "SubA", isGeneric = false, superClass = mySealed, includeXAndFoo = false, modality = Modality.FINAL
        )
        val subB = buildGeneratedClass(
            file, "SubB", isGeneric = false, superClass = mySealed, includeXAndFoo = false, modality = Modality.FINAL
        )
        mySealed.sealedSubclasses = listOf(subA.symbol, subB.symbol)

        for (klass in listOf(plain, withGeneric, extendsSource, extendsPlain, mySealed, subA, subB)) {
            file.declarations += klass
            context.metadataDeclarationRegistrar.registerClassAsMetadataVisible(klass)
        }
    }

    private fun buildGeneratedClass(
        file: IrFile,
        className: String,
        isGeneric: Boolean,
        superClass: IrClass?,
        includeXAndFoo: Boolean,
        modality: Modality,
    ): IrClass {
        val classModality = modality
        val klass = context.irFactory.buildClass {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = Name.identifier(className)
            kind = ClassKind.CLASS
            this.modality = classModality
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            parent = file
        }

        if (isGeneric) {
            klass.addTypeParameter {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                name = Name.identifier("T")
                superTypes += context.irBuiltIns.anyNType
            }
        }

        klass.superTypes = listOf((superClass ?: context.irBuiltIns.anyClass.owner).defaultType)
        klass.thisReceiver = klass.buildReceiverParameter {
            type = klass.symbol.typeWith(klass.typeParameters.map { it.defaultType })
        }

        klass.declarations += buildPrimaryConstructor(klass, superClass)
        if (includeXAndFoo) {
            klass.declarations += buildXProperty(klass)
            klass.declarations += buildFooFunction(klass)
        }

        return klass
    }

    private fun buildPrimaryConstructor(klass: IrClass, superClass: IrClass?): IrConstructor {
        val actualSuperClass = superClass ?: context.irBuiltIns.anyClass.owner
        val superConstructor = actualSuperClass.constructors
            .singleOrNull { c -> c.parameters.none { it.kind == IrParameterKind.Regular } }
            ?: error("No no-arg constructor found in ${actualSuperClass.name}")

        return context.irFactory.buildConstructor {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            returnType = klass.defaultType
            isPrimary = true
        }.apply {
            parent = klass
            body = context.irFactory.createBlockBody(
                startOffset, endOffset,
                listOf(
                    IrDelegatingConstructorCallImpl(
                        startOffset, endOffset, context.irBuiltIns.unitType,
                        superConstructor.symbol, 0,
                    ),
                    IrInstanceInitializerCallImpl(
                        startOffset, endOffset, klass.symbol, context.irBuiltIns.unitType,
                    ),
                )
            )
        }
    }

    private fun buildXProperty(klass: IrClass): IrProperty {
        return context.irFactory.buildProperty {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = Name.identifier("x")
        }.apply {
            parent = klass
            val field = addBackingField {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                type = context.irBuiltIns.intType
                isFinal = true
            }
            field.initializer = context.irFactory.createExpressionBody(
                IrConstImpl.int(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, context.irBuiltIns.intType, 42)
            )
            addGetter {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                returnType = field.type
                origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
            }.apply {
                val dispatch = createDispatchReceiverParameterWithClassParent()
                parameters += dispatch
                body = context.irFactory.createBlockBody(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                    listOf(
                        IrReturnImpl(
                            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                            context.irBuiltIns.nothingType,
                            symbol,
                            IrGetFieldImpl(
                                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                                field.symbol, field.type,
                                IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, dispatch.symbol)
                            )
                        )
                    )
                )
            }
        }
    }

    private fun buildFooFunction(klass: IrClass): IrSimpleFunction {
        return context.irFactory.buildFun {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = Name.identifier("foo")
            returnType = context.irBuiltIns.stringType
            modality = Modality.OPEN
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            parent = klass
            parameters += createDispatchReceiverParameterWithClassParent()
            body = context.irFactory.createBlockBody(
                startOffset, endOffset,
                listOf(
                    IrReturnImpl(
                        startOffset, endOffset,
                        context.irBuiltIns.nothingType,
                        symbol,
                        IrConstImpl.string(
                            startOffset, endOffset, context.irBuiltIns.stringType, "ok"
                        )
                    )
                )
            )
        }
    }
}
