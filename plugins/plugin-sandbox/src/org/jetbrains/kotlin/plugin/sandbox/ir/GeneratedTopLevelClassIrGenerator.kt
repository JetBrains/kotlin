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
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
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
import org.jetbrains.kotlin.ir.types.IrType
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

        // Test (6): nested class inside a source-declared outer.
        val sourceWithNested = file.declarations.filterIsInstance<IrClass>()
            .firstOrNull { it.name.identifier == "SourceWithNested" }
        if (sourceWithNested != null) {
            val nested = buildGeneratedClass(
                sourceWithNested, "Nested", isGeneric = false, superClass = null,
                includeXAndFoo = true, modality = Modality.FINAL,
            )
            sourceWithNested.declarations += nested
            context.metadataDeclarationRegistrar.registerClassAsMetadataVisible(nested)
        }

        // Test (7): companion object inside a source-declared outer.
        val sourceWithCompanion = file.declarations.filterIsInstance<IrClass>()
            .firstOrNull { it.name.identifier == "SourceWithCompanion" }
        if (sourceWithCompanion != null) {
            val companion = buildGeneratedClass(
                sourceWithCompanion, "Companion", isGeneric = false, superClass = null,
                includeXAndFoo = true, modality = Modality.FINAL, classKind = ClassKind.OBJECT, isCompanion = true,
            )
            sourceWithCompanion.declarations += companion
            context.metadataDeclarationRegistrar.registerClassAsMetadataVisible(companion)
        }

        // Test (8): top-level plugin-generated class with its own nested + companion.
        // Build outer first, then the nested + companion as children, all wired up before the
        // single registerClassAsMetadataVisible call on the outermost. The registrar walks
        // outer.declarations and transitively builds FIR for the inner classes.
        val withNestedFamily = buildGeneratedClass(
            file, "WithNestedFamily", isGeneric = false, superClass = null,
            includeXAndFoo = false, modality = Modality.OPEN,
        )
        val inner = buildGeneratedClass(
            withNestedFamily, "Inner", isGeneric = false, superClass = null,
            includeXAndFoo = true, modality = Modality.FINAL,
        )
        val withNestedFamilyCompanion = buildGeneratedClass(
            withNestedFamily, "Companion", isGeneric = false, superClass = null,
            includeXAndFoo = false, modality = Modality.FINAL, classKind = ClassKind.OBJECT, isCompanion = true,
        )
        // Companion needs a `fromCompanion()` function so we can test the cross-module call.
        withNestedFamilyCompanion.declarations += buildFromCompanionFunction(withNestedFamilyCompanion)
        withNestedFamily.declarations += inner
        withNestedFamily.declarations += withNestedFamilyCompanion
        file.declarations += withNestedFamily
        context.metadataDeclarationRegistrar.registerClassAsMetadataVisible(withNestedFamily)

        // (9) inner-no-generics inside outer-no-generics (source-declared).
        val sopl = file.declarations.filterIsInstance<IrClass>()
            .firstOrNull { it.name.identifier == "SourceOuterPlain" }
        if (sopl != null) {
            val innerPlainInPlain = buildGeneratedClass(
                sopl, "InnerPlain", isGeneric = false, superClass = null,
                includeXAndFoo = true, modality = Modality.FINAL, isInner = true,
            )
            sopl.declarations += innerPlainInPlain
            context.metadataDeclarationRegistrar.registerClassAsMetadataVisible(innerPlainInPlain)
        }

        // (10) inner-no-generics inside outer-generic (source-declared).
        val sogen = file.declarations.filterIsInstance<IrClass>()
            .firstOrNull { it.name.identifier == "SourceOuterGeneric" }
        if (sogen != null) {
            val innerPlainInGeneric = buildGeneratedClass(
                sogen, "InnerPlain", isGeneric = false, superClass = null,
                includeXAndFoo = true, modality = Modality.FINAL, isInner = true,
            )
            sogen.declarations += innerPlainInGeneric
            context.metadataDeclarationRegistrar.registerClassAsMetadataVisible(innerPlainInGeneric)
        }

        // (11) inner-generic inside outer-no-generics (source-declared).
        val sopl2 = file.declarations.filterIsInstance<IrClass>()
            .firstOrNull { it.name.identifier == "SourceOuterPlain2" }
        if (sopl2 != null) {
            val innerGenericInPlain = buildGeneratedClass(
                sopl2, "InnerGeneric", isGeneric = true, superClass = null,
                includeXAndFoo = true, modality = Modality.FINAL, isInner = true,
            )
            sopl2.declarations += innerGenericInPlain
            context.metadataDeclarationRegistrar.registerClassAsMetadataVisible(innerGenericInPlain)
        }

        // (12) both inner and outer generic (source-declared outer).
        val sopg = file.declarations.filterIsInstance<IrClass>()
            .firstOrNull { it.name.identifier == "SourceOuterGenericPaired" }
        if (sopg != null) {
            val innerGenericInGeneric = buildGeneratedClass(
                sopg, "InnerGeneric", isGeneric = true, superClass = null,
                includeXAndFoo = true, modality = Modality.FINAL, isInner = true,
            )
            sopg.declarations += innerGenericInGeneric
            context.metadataDeclarationRegistrar.registerClassAsMetadataVisible(innerGenericInGeneric)
        }

        // (13) outer is also plugin-generated, with its own inner. Register only the outer;
        // the registrar builds the inner FIR transitively through buildFirClassRecursively's
        // IrClass branch.
        val withInnerFamily = buildGeneratedClass(
            file, "WithInnerFamily", isGeneric = true, superClass = null,
            includeXAndFoo = false, modality = Modality.OPEN,
        )
        val innerInGenerated = buildGeneratedClass(
            withInnerFamily, "Inner", isGeneric = true, superClass = null,
            includeXAndFoo = true, modality = Modality.FINAL, isInner = true,
        )
        withInnerFamily.declarations += innerInGenerated
        file.declarations += withInnerFamily
        context.metadataDeclarationRegistrar.registerClassAsMetadataVisible(withInnerFamily)

        // (14) Deep generic inner chain: WithDeepInnerFamily<A>.Inner<B>.DeeplyInner<C>, all
        // plugin-generated. Register only the outermost; the registrar builds Inner and DeeplyInner
        // transitively. Distinct type-parameter names (A/B/C) avoid name collisions across levels.
        // Inner and DeeplyInner each get a `self(): <thisType>` whose return type references the
        // inner generic class, exercising the order-sensitive `fillFromPossiblyInnerType` path so
        // the three captured/own type parameters must be registered in the canonical order.
        val deepOuter = buildGeneratedClass(
            file, "WithDeepInnerFamily", isGeneric = true, superClass = null,
            includeXAndFoo = false, modality = Modality.OPEN, typeParamName = "A",
        )
        val deepInner = buildGeneratedClass(
            deepOuter, "Inner", isGeneric = true, superClass = null,
            includeXAndFoo = false, modality = Modality.FINAL, isInner = true, typeParamName = "B",
            includeSelf = true,
        )
        val deeplyInner = buildGeneratedClass(
            deepInner, "DeeplyInner", isGeneric = true, superClass = null,
            includeXAndFoo = true, modality = Modality.FINAL, isInner = true, typeParamName = "C",
            includeSelf = true,
        )
        // Functions returning the type parameter at each level of the chain: own `C`, captured `B`
        // from `Inner`, and captured `A` from `WithDeepInnerFamily`. Verifies that captured
        // type-parameter references in member signatures are registered so dependent modules can
        // call these with the correct argument/return types.
        deeplyInner.declarations += buildIdentityFunction(deeplyInner, "idC", deeplyInner.typeParameters.single())
        deeplyInner.declarations += buildIdentityFunction(deeplyInner, "idB", deepInner.typeParameters.single())
        deeplyInner.declarations += buildIdentityFunction(deeplyInner, "idA", deepOuter.typeParameters.single())
        deepInner.declarations += deeplyInner
        deepOuter.declarations += deepInner
        file.declarations += deepOuter
        context.metadataDeclarationRegistrar.registerClassAsMetadataVisible(deepOuter)

        generateInnerBoxFamily(file)
    }

    /**
     * (16) Generates `class Outer<T> { inner class InnerBox(val value: T) : Box<T> }`, where `Box`
     * is the source-declared interface. The inner class's supertype `Box<T>` references the outer
     * class's (captured) type parameter, so registering it exercises IR -> cone type conversion of a
     * supertype whose argument is a captured type parameter — it must resolve to the outer class's
     * FIR type parameter, not be dropped or mis-substituted.
     */
    private fun generateInnerBoxFamily(file: IrFile) {
        val boxInterface = file.declarations.filterIsInstance<IrClass>()
            .firstOrNull { it.name.identifier == "Box" } ?: return
        val boxValueProperty = boxInterface.declarations.filterIsInstance<IrProperty>()
            .single { it.name.identifier == "value" }
        val boxValueGetter = boxValueProperty.getter ?: error("Box.value must have a getter")

        val outer = buildGeneratedClass(
            file, "Outer", isGeneric = true, superClass = null,
            includeXAndFoo = false, modality = Modality.OPEN, typeParamName = "T",
        )
        // The captured type: the outer class's own type parameter `T`.
        val capturedType = outer.typeParameters.single().defaultType

        val innerBox = context.irFactory.buildClass {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = Name.identifier("InnerBox")
            kind = ClassKind.CLASS
            modality = Modality.FINAL
            visibility = DescriptorVisibilities.PUBLIC
            isInner = true
        }.apply {
            parent = outer
        }
        // : Box<T> — supertype argument is the captured outer type parameter.
        innerBox.superTypes = listOf(boxInterface.symbol.typeWith(capturedType))
        innerBox.thisReceiver = innerBox.buildReceiverParameter {
            type = innerInstanceType(innerBox)
        }

        // Primary constructor `(value: T)` with the outer instance as dispatch receiver.
        lateinit var constructorValueParameter: IrValueParameter
        val constructor = context.irFactory.buildConstructor {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            returnType = innerBox.thisReceiver!!.type
            isPrimary = true
        }.apply {
            parent = innerBox
            parameters += buildReceiverParameter {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                kind = IrParameterKind.DispatchReceiver
                type = outer.thisReceiver!!.type
            }.also { it.parent = this }
            constructorValueParameter = addValueParameter("value", capturedType)
            val anyConstructor = context.irBuiltIns.anyClass.owner.constructors
                .single { c -> c.parameters.none { it.kind == IrParameterKind.Regular } }
            body = context.irFactory.createBlockBody(
                startOffset, endOffset,
                listOf(
                    IrDelegatingConstructorCallImpl(
                        startOffset, endOffset, context.irBuiltIns.unitType,
                        anyConstructor.symbol, 0,
                    ),
                    IrInstanceInitializerCallImpl(
                        startOffset, endOffset, innerBox.symbol, context.irBuiltIns.unitType,
                    ),
                )
            )
        }

        // `override val value: T` backed by a field initialized from the constructor parameter.
        val valueProperty = context.irFactory.buildProperty {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = Name.identifier("value")
        }.apply {
            parent = innerBox
            overriddenSymbols = listOf(boxValueProperty.symbol)
            val field = addBackingField {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                type = capturedType
                isFinal = true
            }
            field.initializer = context.irFactory.createExpressionBody(
                IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, constructorValueParameter.symbol)
            )
            addGetter {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                returnType = capturedType
                origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
            }.apply {
                overriddenSymbols = listOf(boxValueGetter.symbol)
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

        innerBox.declarations += constructor
        innerBox.declarations += valueProperty
        outer.declarations += innerBox
        file.declarations += outer
        context.metadataDeclarationRegistrar.registerClassAsMetadataVisible(outer)
    }

    private fun buildFromCompanionFunction(klass: IrClass): IrSimpleFunction {
        return context.irFactory.buildFun {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = Name.identifier("fromCompanion")
            returnType = context.irBuiltIns.stringType
            modality = Modality.FINAL
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
                            startOffset, endOffset, context.irBuiltIns.stringType, "from-companion"
                        )
                    )
                )
            )
        }
    }

    private fun buildGeneratedClass(
        parentDeclaration: IrDeclarationParent,
        className: String,
        isGeneric: Boolean,
        superClass: IrClass?,
        includeXAndFoo: Boolean,
        modality: Modality,
        classKind: ClassKind = ClassKind.CLASS,
        isCompanion: Boolean = false,
        isInner: Boolean = false,
        typeParamName: String? = null,
        includeSelf: Boolean = false,
    ): IrClass {
        val classModality = modality
        val classKindValue = classKind
        val classIsCompanion = isCompanion
        val classIsInner = isInner
        val klass = context.irFactory.buildClass {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = Name.identifier(className)
            kind = classKindValue
            this.modality = classModality
            visibility = DescriptorVisibilities.PUBLIC
            this.isCompanion = classIsCompanion
            this.isInner = classIsInner
        }.apply {
            parent = parentDeclaration
        }

        // IR convention (matching fir2ir's `setTypeParameters`): an inner class declares ONLY its
        // own type parameters. Captured outer parameters are NOT copied into `typeParameters`;
        // instead the class's instance type carries them as trailing arguments (see `innerInstanceType`).
        if (isGeneric) {
            klass.addTypeParameter {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                // Distinct names across an inner chain (callers pass A/B/C) avoid shadowing.
                name = Name.identifier(typeParamName ?: if (isInner) "S" else "T")
                superTypes += context.irBuiltIns.anyNType
            }
        }

        klass.superTypes = listOf((superClass ?: context.irBuiltIns.anyClass.owner).defaultType)
        klass.thisReceiver = klass.buildReceiverParameter {
            type = innerInstanceType(klass)
        }

        klass.declarations += buildPrimaryConstructor(klass, superClass, isInner)
        if (includeXAndFoo) {
            klass.declarations += buildXProperty(klass)
            klass.declarations += buildFooFunction(klass)
        }
        if (includeSelf) {
            klass.declarations += buildSelfFunction(klass)
        }

        return klass
    }

    /**
     * The instance type of [klass]: the class applied to its own type parameters followed by the
     * own type parameters of every enclosing class up the inner-class chain (nearest first). This
     * matches the IR convention produced by fir2ir's `setThisReceiver`: an inner class's
     * `typeParameters` are own-only, but its type carries `[own…, enclosing-own…]` arguments.
     */
    private fun innerInstanceType(klass: IrClass): IrType {
        val arguments = mutableListOf<IrType>()
        klass.typeParameters.mapTo(arguments) { it.defaultType }
        if (klass.isInner) {
            var outer: IrClass? = klass.parent as? IrClass
            while (outer != null) {
                outer.typeParameters.mapTo(arguments) { it.defaultType }
                outer = if (outer.isInner) outer.parent as? IrClass else null
            }
        }
        return klass.symbol.typeWith(arguments)
    }

    /**
     * Builds `fun self(): <thisType>` returning the dispatch receiver. Its return type is the inner
     * instance type (e.g. `DeeplyInner<C, B, A>`), so mapping it exercises the order-sensitive
     * `buildPossiblyInnerType` (JVM) / `fillFromPossiblyInnerType` (metadata) paths, which require
     * the inner class's type parameters to be modeled correctly across the whole enclosing chain.
     */
    /**
     * Builds `fun <functionName>(value: T): T = value`, where `T` is [typeParameter] — which may be
     * own to [klass] or captured from an enclosing class. Such a signature references a type
     * parameter that belongs to a *different* class in the inner chain, exercising the cross-class
     * type-parameter resolution that the registrar must reproduce so dependent modules can call it.
     */
    private fun buildIdentityFunction(klass: IrClass, functionName: String, typeParameter: IrTypeParameter): IrSimpleFunction {
        val valueType = typeParameter.defaultType
        return context.irFactory.buildFun {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = Name.identifier(functionName)
            returnType = valueType
            modality = Modality.FINAL
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            parent = klass
            parameters += createDispatchReceiverParameterWithClassParent()
            val valueParameter = addValueParameter("value", valueType)
            body = context.irFactory.createBlockBody(
                startOffset, endOffset,
                listOf(
                    IrReturnImpl(
                        startOffset, endOffset,
                        context.irBuiltIns.nothingType,
                        symbol,
                        IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, valueParameter.symbol)
                    )
                )
            )
        }
    }

    private fun buildSelfFunction(klass: IrClass): IrSimpleFunction {
        val selfType = klass.thisReceiver!!.type
        return context.irFactory.buildFun {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            name = Name.identifier("self")
            returnType = selfType
            modality = Modality.FINAL
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            parent = klass
            val dispatch = createDispatchReceiverParameterWithClassParent()
            parameters += dispatch
            body = context.irFactory.createBlockBody(
                startOffset, endOffset,
                listOf(
                    IrReturnImpl(
                        startOffset, endOffset,
                        context.irBuiltIns.nothingType,
                        symbol,
                        IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, dispatch.symbol)
                    )
                )
            )
        }
    }

    private fun buildPrimaryConstructor(klass: IrClass, superClass: IrClass?, isInner: Boolean): IrConstructor {
        val actualSuperClass = superClass ?: context.irBuiltIns.anyClass.owner
        val superConstructor = actualSuperClass.constructors
            .singleOrNull { c -> c.parameters.none { it.kind == IrParameterKind.Regular } }
            ?: error("No no-arg constructor found in ${actualSuperClass.name}")

        return context.irFactory.buildConstructor {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            returnType = klass.thisReceiver!!.type
            isPrimary = true
        }.apply {
            parent = klass
            if (isInner) {
                // Inner-class constructor receives the outer instance as a dispatch receiver, typed
                // as the outer's instance type (which itself carries any further-enclosing args).
                val outer = klass.parent as IrClass
                parameters += buildReceiverParameter {
                    startOffset = SYNTHETIC_OFFSET
                    endOffset = SYNTHETIC_OFFSET
                    kind = IrParameterKind.DispatchReceiver
                    type = outer.thisReceiver!!.type
                }.also { it.parent = this }
            }
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
