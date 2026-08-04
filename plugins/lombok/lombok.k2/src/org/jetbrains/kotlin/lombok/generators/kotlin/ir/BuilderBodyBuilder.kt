/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators.kotlin.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.ValueRemapper
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.lombok.generators.BuilderDeclarationType
import org.jetbrains.kotlin.lombok.generators.BuilderGeneratorKey
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

/**
 * Fills the bodies with the builder members previously generated (as bare signatures) by
 * [org.jetbrains.kotlin.lombok.generators.BuilderGenerator] for a Kotlin class annotated with `@Builder`.
 *
 * For `@Builder class Entity(val a: A, val b: B)` the following bodies are produced:
 *  - setters `a(a)` / `b(b)`: store the argument into the builder's backing field and return `this`;
 *  - `build()`: invoke the entity constructor with the accumulated fields;
 *  - `builder()`: return a fresh builder instance;
 *  - `toBuilder()`: return a builder pre-filled from the receiver's properties.
 *
 */
object BuilderBodyBuilder : IrBodyBuilder<BuilderGeneratorKey>() {
    override fun IrBlockBodyBuilder.build(
        key: BuilderGeneratorKey,
        declaration: IrSimpleFunction,
    ) {
        val regularParameters = declaration.parameters.filter { it.kind == IrParameterKind.Regular }
        val builderFunctionType = key.type as? BuilderDeclarationType.Function ?: return
        when (builderFunctionType) {
            BuilderDeclarationType.Function.Setter -> buildSetter(declaration, regularParameters.single())
            is BuilderDeclarationType.Function.Build -> buildBuildMethod(declaration, builderFunctionType.builderAnnStartOffset)
            BuilderDeclarationType.Function.Builder -> buildBuilderFactory(declaration)
            BuilderDeclarationType.Function.ToBuilder -> buildToBuilder(declaration)
            is BuilderDeclarationType.SingularFunction -> {
                val fieldName = builderFunctionType.fieldName
                when (builderFunctionType) {
                    is BuilderDeclarationType.SingularFunction.AddSingle -> buildSingularAddSingle(
                        fieldName,
                        declaration,
                        regularParameters
                    )
                    is BuilderDeclarationType.SingularFunction.AddAll -> buildSingularAddAll(
                        fieldName,
                        declaration,
                        regularParameters.single()
                    )
                    is BuilderDeclarationType.SingularFunction.Clear -> buildSingularClear(
                        fieldName,
                        declaration
                    )
                }
            }
        }
    }

    private fun IrBlockBodyBuilder.buildSetter(declaration: IrSimpleFunction, parameter: IrValueParameter) {
        val builderClass = declaration.parent as IrClass
        val thisParameter = declaration.dispatchReceiverParameter!!
        val field = builderClass.findBuilderField(parameter.name) ?: return

        +irSetField(irGet(thisParameter), field, irGet(parameter))
        builderClass.defaultFlagField(parameter.name)?.let { flagField ->
            +irSetField(irGet(thisParameter), flagField, irTrue())
        }
        +irReturn(irGet(thisParameter))
    }

    /**
     * Resolves every constructor parameter's value into a local temporary, in declaration order, before
     * constructing the entity. This lets a `@Builder.Default` field's default expression (which, unlike Java
     * Lombok's, may reference an earlier constructor parameter, e.g. `@Builder.Default val b: Int = a + 1`)
     * be evaluated against the *resolved* value of that earlier parameter — whether it came from an explicit
     * builder setter call or from its own default — rather than re-reading a possibly-unset builder field.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBlockBodyBuilder.buildBuildMethod(declaration: IrSimpleFunction, builderAnnStartOffset: Int?) {
        val builderClass = declaration.parent as IrClass
        val thisParameter = declaration.dispatchReceiverParameter!!
        val entityClass = declaration.returnType.classOrNull!!.owner
        val constructor = entityClass.entityConstructorFor(builderAnnStartOffset)
        val singularFieldNames = builderClass.singularFieldNames()
        val regularParameters = constructor.parameters.filter { it.kind == IrParameterKind.Regular }

        val resolvedValues = mutableMapOf<IrValueSymbol, IrValueSymbol>()
        for (parameter in regularParameters) {
            val field = builderClass.findBuilderField(parameter.name) ?: continue
            val fieldRead = irGetField(irGet(thisParameter), field)
            val value = when {
                parameter.name in singularFieldNames -> buildSingularResult(field, thisParameter, parameter.type)
                else -> builderClass.defaultFlagField(parameter.name)?.let { flagField ->
                    buildDefaultOrSetValue(declaration, parameter, flagField, thisParameter, fieldRead, resolvedValues)
                } ?: fieldRead
            }
            val temp = irTemporary(value, nameHint = parameter.name.identifier)
            resolvedValues[parameter.symbol] = temp.symbol
        }

        val constructorCall = irConstruct(declaration, constructor)
        constructor.parameters.forEachIndexed { index, parameter ->
            if (parameter.kind != IrParameterKind.Regular) return@forEachIndexed
            val tempSymbol = resolvedValues[parameter.symbol] ?: return@forEachIndexed
            constructorCall.arguments[index] = irGet(tempSymbol.owner)
        }

        +irReturn(constructorCall)
    }

    /** `if ($set) field else <default expression, with earlier-parameter references resolved to their temps>`. */
    private fun IrBlockBodyBuilder.buildDefaultOrSetValue(
        declaration: IrSimpleFunction,
        parameter: IrValueParameter,
        flagField: IrField,
        thisParameter: IrValueParameter,
        fieldRead: IrExpression,
        resolvedValues: Map<IrValueSymbol, IrValueSymbol>,
    ): IrExpression {
        val defaultExpression = parameter.defaultValue?.expression ?: return fieldRead
        val copiedDefault = defaultExpression
            .deepCopyWithSymbols(initialParent = declaration)
            .transform(ValueRemapper(resolvedValues), null)
        return irIfThenElse(
            parameter.type,
            irGetField(irGet(thisParameter), flagField),
            fieldRead,
            copiedDefault,
        )
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBlockBodyBuilder.buildBuilderFactory(declaration: IrSimpleFunction) {
        val builderClass = declaration.returnType.classOrNull!!.owner
        +irReturn(irConstruct(declaration, builderClass.builderConstructor()))
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBlockBodyBuilder.buildToBuilder(declaration: IrSimpleFunction) {
        val entityClass = declaration.parent as IrClass
        val thisParameter = declaration.dispatchReceiverParameter!!
        val builderClass = declaration.returnType.classOrNull!!.owner
        val singularFieldNames = builderClass.singularFieldNames()

        val builder = irTemporary(irConstruct(declaration, builderClass.builderConstructor()), nameHint = "builder")
        val fields = builderClass.declarations.mapNotNull {
            if (it is IrProperty && !it.isDefaultFlagField()) {
                it.backingField
            } else {
                null
            }
        }
        for (field in fields) {
            val property = entityClass.findDeclaration<IrProperty> { it.name == field.name } ?: continue
            val getter = property.getter ?: continue
            val entityValue = irCall(getter.symbol).apply { arguments[0] = irGet(thisParameter) }
            val newValue = if (field.name in singularFieldNames) {
                copySingularValue(field, entityValue)
            } else {
                entityValue
            }
            +irSetField(irGet(builder), field, newValue)
            // The entity's current value must be preserved verbatim on `build()`, not silently re-defaulted.
            builderClass.defaultFlagField(field.name)?.let { flagField ->
                +irSetField(irGet(builder), flagField, irTrue())
            }
        }

        +irReturn(irGet(builder))
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.findBuilderField(name: Name): IrField? =
        declarations.firstNotNullOfOrNull { declaration ->
            val field = (declaration as? IrProperty)?.backingField
            field?.takeIf { it.name == name }
        }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.singularFieldNames(): Set<Name> =
        declarations.asSequence()
            .filterIsInstance<IrSimpleFunction>()
            .mapNotNull { (it.origin as? GeneratedByPlugin)?.pluginKey as? BuilderGeneratorKey }
            .map { it.type }
            .filterIsInstance<BuilderDeclarationType.SingularFunction>()
            .map { it.fieldName }
            .toSet()

    private fun IrProperty.isDefaultFlagField(): Boolean =
        ((origin as? GeneratedByPlugin)?.pluginKey as? BuilderGeneratorKey)?.type is BuilderDeclarationType.DefaultFlagField

    /** The hidden `$set` flag field backing a `@Builder.Default` field named [fieldName], if one was generated. */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.defaultFlagField(fieldName: Name): IrField? =
        declarations.firstNotNullOfOrNull { declaration ->
            val property = declaration as? IrProperty ?: return@firstNotNullOfOrNull null
            val type = ((property.origin as? GeneratedByPlugin)?.pluginKey as? BuilderGeneratorKey)?.type
            (type as? BuilderDeclarationType.DefaultFlagField)
                ?.takeIf { it.fieldName == fieldName }
                ?.let { property.backingField }
        }

    // -------------------------------- @Singular support --------------------------------
    //
    // A `@Singular` builder field holds a nullable, lazily-initialized *mutable* backing collection
    // (`null` == "not yet created"); `item()`/`items()` mutate it in place and `clear...()` reuses it.
    // `build()` always produces a non-null, genuinely immutable result (matching Lombok's own
    // size-based 0/1/else switch); `toBuilder()` never aliases the entity's own collection.

    private enum class SingularKind { COLLECTION, SET, ITERABLE, MAP, TABLE }

    private class SingularCollectionInfo(val kind: SingularKind, val typeArguments: List<IrType>)

    /** The Guava immutable class to construct for a `@Singular` field whose *entity-declared* type is Guava. */
    private enum class GuavaCollectionKind(val classId: ClassId) {
        LIST(LombokNames.IMMUTABLE_LIST_ID),
        SET(LombokNames.IMMUTABLE_SET_ID),
        SORTED_SET(LombokNames.IMMUTABLE_SORTED_SET_ID),
        MAP(LombokNames.IMMUTABLE_MAP_ID),
        BI_MAP(LombokNames.IMMUTABLE_BI_MAP_ID),
        SORTED_MAP(LombokNames.IMMUTABLE_SORTED_MAP_ID),
    }

    /**
     * Classifies the *entity's own declared* property/constructor-parameter type (NOT the builder's backing
     * field type, which is always a plain Kotlin `Mutable*` after `toBackingMutableCollectionType()`).
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrType.guavaCollectionKindOrNull(): GuavaCollectionKind? =
        when (classOrNull?.owner?.classId) {
            LombokNames.IMMUTABLE_LIST_ID, LombokNames.IMMUTABLE_COLLECTION_ID -> GuavaCollectionKind.LIST
            LombokNames.IMMUTABLE_SET_ID -> GuavaCollectionKind.SET
            LombokNames.IMMUTABLE_SORTED_SET_ID -> GuavaCollectionKind.SORTED_SET
            LombokNames.IMMUTABLE_MAP_ID -> GuavaCollectionKind.MAP
            LombokNames.IMMUTABLE_BI_MAP_ID -> GuavaCollectionKind.BI_MAP
            LombokNames.IMMUTABLE_SORTED_MAP_ID -> GuavaCollectionKind.SORTED_MAP
            else -> null
        }

    /** `item(e)` (or `item(k, v)` for maps, `item(rowKey, columnKey, value)` for tables) — mutates the (lazily-created) backing collection in place. */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBlockBodyBuilder.buildSingularAddSingle(
        fieldName: Name,
        declaration: IrSimpleFunction,
        parameters: List<IrValueParameter>,
    ) {
        val builderClass = declaration.parent as IrClass
        val thisParameter = declaration.dispatchReceiverParameter!!
        val field = builderClass.findBuilderField(fieldName) ?: return
        val info = singularCollectionInfo(field.type) ?: return
        val builtIns = pluginContext.irBuiltIns
        val backing = irImplicitCast(irGetField(irGet(thisParameter), field), field.type.makeNotNull())

        val addCall = when (info.kind) {
            SingularKind.MAP -> irCall(
                builtIns.mutableMapClass.owner.getSimpleFunction("put")!!,
                info.typeArguments[1].makeNullable(),
                typeArgumentsCount = 0
            ).apply {
                arguments[0] = backing
                arguments[1] = irGet(parameters[0])
                arguments[2] = irGet(parameters[1])
            }
            SingularKind.TABLE -> irCall(
                tableFunction(field.file, "put") ?: return,
                info.typeArguments[2].makeNullable(),
                typeArgumentsCount = 0
            ).apply {
                arguments[0] = backing
                arguments[1] = irGet(parameters[0])
                arguments[2] = irGet(parameters[1])
                arguments[3] = irGet(parameters[2])
            }
            else -> irCallOp(
                builtIns.mutableCollectionClass.owner.getSimpleFunction("add")!!,
                builtIns.booleanType,
                backing,
                irGet(parameters.single())
            )
        }
        +ensureInitialized(thisParameter, field, info)
        +addCall
        +irReturn(irGet(thisParameter))
    }

    /** `items(collection)` — mutates the (lazily-created) backing collection in place; a `null` argument is ignored. */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBlockBodyBuilder.buildSingularAddAll(
        fieldName: Name,
        declaration: IrSimpleFunction,
        parameter: IrValueParameter,
    ) {
        val builderClass = declaration.parent as IrClass
        val thisParameter = declaration.dispatchReceiverParameter!!
        val field = builderClass.findBuilderField(fieldName) ?: return
        val info = singularCollectionInfo(field.type) ?: return
        val builtIns = pluginContext.irBuiltIns

        // When the parameter is nullable (a nullable field type or `@Singular(ignoreNullCollections = true)`),
        // a `null` collection must be silently ignored — matching Lombok's `ignoreNullCollections` semantics.
        val nullable = parameter.type.isNullable()
        val argument = if (nullable) irImplicitCast(irGet(parameter), parameter.type.makeNotNull()) else irGet(parameter)
        val backing = irImplicitCast(irGetField(irGet(thisParameter), field), field.type.makeNotNull())
        val addAllCall = when (info.kind) {
            SingularKind.MAP -> irCallOp(builtIns.mutableMapClass.owner.getSimpleFunction("putAll")!!, builtIns.unitType, backing, argument)
            SingularKind.TABLE -> irCallOp(tableFunction(field.file, "putAll") ?: return, builtIns.unitType, backing, argument)
            SingularKind.COLLECTION,
            SingularKind.SET,
            SingularKind.ITERABLE
                -> irCallOp(builtIns.mutableCollectionClass.owner.getSimpleFunction("addAll")!!, builtIns.booleanType, backing, argument)
        }
        val mutate = irComposite(resultType = builtIns.unitType) {
            +ensureInitialized(thisParameter, field, info)
            +addAllCall
        }

        if (nullable) {
            +irIfThen(builtIns.unitType, irNotEquals(irGet(parameter), irNull()), mutate)
        } else {
            +mutate
        }
        +irReturn(irGet(thisParameter))
    }

    /** `clearItems()` — reuses the existing backing collection in place; a never-created field stays `null`. */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBlockBodyBuilder.buildSingularClear(fieldName: Name, declaration: IrSimpleFunction) {
        val builderClass = declaration.parent as IrClass
        val thisParameter = declaration.dispatchReceiverParameter!!
        val field = builderClass.findBuilderField(fieldName) ?: return
        val info = singularCollectionInfo(field.type) ?: return
        val builtIns = pluginContext.irBuiltIns
        val clearSymbol = when (info.kind) {
            SingularKind.MAP -> builtIns.mutableMapClass.owner.getSimpleFunction("clear")!!
            SingularKind.TABLE -> tableFunction(field.file, "clear") ?: return
            SingularKind.COLLECTION,
            SingularKind.SET,
            SingularKind.ITERABLE
                -> builtIns.mutableCollectionClass.owner.getSimpleFunction("clear")!!
        }

        val tmp = irTemporary(irGetField(irGet(thisParameter), field))
        +irIfThen(
            builtIns.unitType,
            irNotEquals(irGet(tmp), irNull()),
            irCallOp(clearSymbol, builtIns.unitType, irImplicitCast(irGet(tmp), field.type.makeNotNull())),
        )
        +irReturn(irGet(thisParameter))
    }

    /** Lazily creates the backing collection the first time a field is mutated; a no-op once it exists. */
    private fun IrBlockBodyBuilder.ensureInitialized(
        thisParameter: IrValueParameter,
        field: IrField,
        info: SingularCollectionInfo,
    ): IrStatement =
        irIfThen(
            pluginContext.irBuiltIns.unitType,
            irEqualsNull(irGetField(irGet(thisParameter), field)),
            irSetField(irGet(thisParameter), field, newMutableBacking(info, field.file)),
        )

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBlockBodyBuilder.singularCollectionInfo(type: IrType): SingularCollectionInfo? {
        val simpleType = type as? IrSimpleType ?: return null
        val classifier = simpleType.classifier as? IrClassSymbol ?: return null
        val typeArguments = simpleType.arguments.map { it.typeOrNull ?: return null }
        val builtIns = pluginContext.irBuiltIns
        val kind = when (classifier) {
            builtIns.mapClass, builtIns.mutableMapClass -> SingularKind.MAP
            builtIns.setClass, builtIns.mutableSetClass -> SingularKind.SET
            builtIns.iterableClass, builtIns.mutableIterableClass -> SingularKind.ITERABLE
            builtIns.listClass, builtIns.mutableListClass, builtIns.collectionClass, builtIns.mutableCollectionClass ->
                SingularKind.COLLECTION
            else -> if (classifier.owner.classId == LombokNames.TABLE_ID) SingularKind.TABLE else return null
        }
        return SingularCollectionInfo(kind, typeArguments)
    }

    /** Resolves a method declared on the Guava `Table` interface (`put`/`putAll`/`clear`), used for a `@Singular` table field's mutable backing. */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBlockBodyBuilder.tableFunction(file: IrFile, name: String): IrSimpleFunctionSymbol? =
        pluginContext.finderForSource(file).findClass(LombokNames.TABLE_ID)?.owner?.getSimpleFunction(name)

    private fun IrBlockBodyBuilder.emptyCollection(info: SingularCollectionInfo): IrExpression {
        val name = when (info.kind) {
            SingularKind.MAP -> "emptyMap"
            SingularKind.SET -> "emptySet"
            else -> "emptyList"
        }
        val symbol = pluginContext
            .finderForBuiltins()
            .findFunctions(CallableId(StandardClassIds.BASE_COLLECTIONS_PACKAGE, Name.identifier(name)))
            .first()
        return irCallWithSubstitutedType(symbol, info.typeArguments)
    }

    /**
     * `build()`'s per-field result: a size-based 3-way switch matching Lombok's own bytecode —
     * `0 -> canonical empty`, `1 -> canonical singleton` (List/Set only), `else -> defensive copy
     * wrapped unmodifiable`. Always non-null even though the field itself may never be initialized.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBlockBodyBuilder.buildSingularResult(
        field: IrField,
        thisParameter: IrValueParameter,
        entityParameterType: IrType,
    ): IrExpression {
        val info = singularCollectionInfo(field.type) ?: return irGetField(irGet(thisParameter), field)
        if (info.kind == SingularKind.TABLE) {
            return buildTableSingularResult(field, thisParameter, info)
        }
        entityParameterType.guavaCollectionKindOrNull()?.let { guavaKind ->
            return buildGuavaSingularResult(field, thisParameter, info, guavaKind)
        }
        val builtIns = pluginContext.irBuiltIns
        // Built from `info`'s (already-substituted, builder-scoped) type arguments rather than the entity
        // constructor's own declared parameter type, which references the entity class's own type parameters
        // — foreign to the scope this expression is constructed in for a generic entity class.
        val resultType = when (info.kind) {
            SingularKind.MAP -> builtIns.mapClass.typeWith(info.typeArguments)
            SingularKind.SET -> builtIns.setClass.typeWith(info.typeArguments)
            else -> builtIns.listClass.typeWith(info.typeArguments)
        }

        val fieldTmp = irTemporary(irGetField(irGet(thisParameter), field), nameHint = "singular")
        fun nonNullField() = irImplicitCast(irGet(fieldTmp), field.type.makeNotNull())

        val sizeGetter = if (info.kind == SingularKind.MAP) builtIns.mapClass.owner.getPropertyGetter("size")!!
        else builtIns.collectionClass.owner.getPropertyGetter("size")!!
        val sizeTmp = irTemporary(
            irIfNull(builtIns.intType, irGet(fieldTmp), irInt(0), irCallOp(sizeGetter, builtIns.intType, nonNullField())),
            nameHint = "size",
        )

        val branches = mutableListOf<IrBranch>(irBranch(irEquals(irGet(sizeTmp), irInt(0)), emptyCollection(info)))
        if (info.kind != SingularKind.MAP) {
            branches += irBranch(irEquals(irGet(sizeTmp), irInt(1)), singleElementCollection(info, nonNullField()))
        }
        branches += irElseBranch(unmodifiableDefensiveCopy(info, field.file, nonNullField()))

        return irWhen(resultType, branches)
    }

    /** `build()`'s per-field result for a Guava-declared `@Singular` field: `field == null ? Guava.of() : Guava.copyOf(field)`. */
    private fun IrBlockBodyBuilder.buildGuavaSingularResult(
        field: IrField,
        thisParameter: IrValueParameter,
        info: SingularCollectionInfo,
        guavaKind: GuavaCollectionKind,
    ): IrExpression {
        val builtIns = pluginContext.irBuiltIns
        val expectedParamClassifiers = if (info.kind == SingularKind.MAP) {
            setOf(builtIns.mapClass, builtIns.mutableMapClass)
        } else {
            setOf(builtIns.collectionClass, builtIns.mutableCollectionClass)
        }
        return buildImmutableOfCopyOfResult(field, thisParameter, info, guavaKind.classId, expectedParamClassifiers)
    }

    /** `build()`'s per-field result for a `@Singular` table field: `field == null ? ImmutableTable.of() : ImmutableTable.copyOf(field)`. */
    private fun IrBlockBodyBuilder.buildTableSingularResult(
        field: IrField,
        thisParameter: IrValueParameter,
        info: SingularCollectionInfo,
    ): IrExpression {
        val tableClassifier = pluginContext.finderForSource(field.file).findClass(LombokNames.TABLE_ID)
            ?: return irGetField(irGet(thisParameter), field)
        return buildImmutableOfCopyOfResult(field, thisParameter, info, LombokNames.IMMUTABLE_TABLE_ID, setOf(tableClassifier))
    }

    /**
     * `build()`'s per-field result for an entity-declared immutable Guava type (`ImmutableList`/`ImmutableSet`/
     * `ImmutableMap`/`ImmutableTable` and friends): `field == null ? Immutable.of() : Immutable.copyOf(field)`.
     * [copyOfParamClassifiers] selects the `copyOf` overload whose sole parameter has one of these classifiers
     * (distinguishing e.g. `copyOf(Collection)` from `copyOf(Iterable)`/`copyOf(SortedSet)` overloads).
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBlockBodyBuilder.buildImmutableOfCopyOfResult(
        field: IrField,
        thisParameter: IrValueParameter,
        info: SingularCollectionInfo,
        immutableClassId: ClassId,
        copyOfParamClassifiers: Set<IrClassSymbol>,
    ): IrExpression {
        val immutableClass = pluginContext.finderForSource(field.file).findClass(immutableClassId)
            ?: return irGetField(irGet(thisParameter), field)
        val resultType = immutableClass.typeWith(info.typeArguments)
        val staticFunctions = immutableClass.owner.declarations.filterIsInstance<IrSimpleFunction>()

        val ofFunction = staticFunctions.first {
            it.name.asString() == "of" && it.parameters.none { p -> p.kind == IrParameterKind.Regular }
        }
        val copyOfFunction = staticFunctions.first { function ->
            function.name.asString() == "copyOf" &&
                    function.parameters.singleOrNull { it.kind == IrParameterKind.Regular }
                        ?.let { (it.type as? IrSimpleType)?.classifier in copyOfParamClassifiers } == true
        }

        val fieldTmp = irTemporary(irGetField(irGet(thisParameter), field), nameHint = "singular")
        val nonNullField = irImplicitCast(irGet(fieldTmp), field.type.makeNotNull())

        return irIfNull(
            resultType,
            irGet(fieldTmp),
            irCallWithSubstitutedType(ofFunction.symbol, info.typeArguments),
            irCallWithSubstitutedType(copyOfFunction.symbol, info.typeArguments).apply { arguments[0] = nonNullField },
        )
    }

    /** `listOf(element)` / `setOf(element)`, `element` being the collection's sole entry. */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBlockBodyBuilder.singleElementCollection(info: SingularCollectionInfo, source: IrExpression): IrExpression {
        val builtIns = pluginContext.irBuiltIns
        val elementType = info.typeArguments.single()
        val iterator = irCallOp(
            builtIns.iterableClass.owner.getSimpleFunction("iterator")!!,
            builtIns.iteratorClass.typeWith(elementType),
            source,
        )
        val element = irCallOp(builtIns.iteratorClass.owner.getSimpleFunction("next")!!, elementType, iterator)
        val name = if (info.kind == SingularKind.SET) "setOf" else "listOf"
        val symbol = pluginContext
            .finderForBuiltins()
            .findFunctions(CallableId(StandardClassIds.BASE_COLLECTIONS_PACKAGE, Name.identifier(name)))
            .first { function ->
                val regular = function.owner.parameters.filter { it.kind == IrParameterKind.Regular }
                regular.size == 1 && regular.single().varargElementType == null
            }
        return irCallWithSubstitutedType(symbol, info.typeArguments).apply { arguments[0] = element }
    }

    /** A fresh mutable copy of [source], wrapped unmodifiable (falls back to the plain copy if the JDK wrapper can't be resolved). */
    private fun IrBlockBodyBuilder.unmodifiableDefensiveCopy(
        info: SingularCollectionInfo,
        file: IrFile,
        source: IrExpression,
    ): IrExpression =
        unmodifiableWrap(info, file, freshMutableCopy(info, file, source))

    /** A fresh `ArrayList`/`LinkedHashSet`/`LinkedHashMap`/`HashBasedTable` filled via `addAll`/`putAll` from [source]. */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBlockBodyBuilder.freshMutableCopy(info: SingularCollectionInfo, file: IrFile, source: IrExpression): IrExpression {
        val builtIns = pluginContext.irBuiltIns
        // Resolved up front (rather than inside `irBlock`'s lambda) so a failed lookup can bail out via a
        // plain `return`, without relying on non-local-return semantics inside the block-building lambda.
        val tablePutAll = if (info.kind == SingularKind.TABLE) tableFunction(file, "putAll") ?: return source else null
        val backing = newMutableBacking(info, file)
        return irBlock(resultType = backing.type) {
            val copy = irTemporary(backing)
            val addAllCall = when (info.kind) {
                SingularKind.MAP ->
                    irCallOp(builtIns.mutableMapClass.owner.getSimpleFunction("putAll")!!, builtIns.unitType, irGet(copy), source)
                SingularKind.TABLE ->
                    irCallOp(tablePutAll!!, builtIns.unitType, irGet(copy), source)
                SingularKind.COLLECTION,
                SingularKind.SET,
                SingularKind.ITERABLE
                    -> irCallOp(builtIns.mutableCollectionClass.owner.getSimpleFunction("addAll")!!, builtIns.booleanType, irGet(copy), source)
            }
            +addAllCall
            +irGet(copy)
        }
    }

    /** `java.util.Collections.unmodifiableList/Set/Map(mutableCopy)`; returns [mutableCopy] unchanged if unresolved. */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBlockBodyBuilder.unmodifiableWrap(info: SingularCollectionInfo, file: IrFile, mutableCopy: IrExpression): IrExpression {
        val name = when (info.kind) {
            SingularKind.MAP -> "unmodifiableMap"
            SingularKind.SET -> "unmodifiableSet"
            SingularKind.COLLECTION, SingularKind.ITERABLE -> "unmodifiableList"
            // Table fields never reach this path: `buildSingularResult` routes them through `buildTableSingularResult` instead.
            SingularKind.TABLE -> shouldNotBeCalled()
        }
        val collectionsClass = ClassId(FqName("java.util"), Name.identifier("Collections"))
        val symbol = pluginContext.finderForSource(file).findClass(collectionsClass)?.owner?.getSimpleFunction(name)
            ?: return mutableCopy
        return irCallWithSubstitutedType(symbol, info.typeArguments).apply { arguments[0] = mutableCopy }
    }

    /** A fresh, empty `ArrayList<T>`/`LinkedHashSet<T>`/`LinkedHashMap<K, V>`/`HashBasedTable<R, C, V>` matching [info]'s shape. */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBlockBodyBuilder.newMutableBacking(info: SingularCollectionInfo, file: IrFile): IrExpression {
        if (info.kind == SingularKind.TABLE) {
            val hashBasedTableClass = pluginContext.finderForSource(file).findClass(LombokNames.HASH_BASED_TABLE_ID)!!
            val createFunction = hashBasedTableClass.owner.declarations.filterIsInstance<IrSimpleFunction>()
                .first { it.name.asString() == "create" && it.parameters.none { p -> p.kind == IrParameterKind.Regular } }
            return irCallWithSubstitutedType(createFunction.symbol, info.typeArguments)
        }
        val name = when (info.kind) {
            SingularKind.MAP -> "LinkedHashMap"
            SingularKind.SET -> "LinkedHashSet"
            SingularKind.COLLECTION, SingularKind.ITERABLE -> "ArrayList"
            SingularKind.TABLE -> shouldNotBeCalled() // handled above
        }
        val classSymbol = pluginContext
            .finderForBuiltins()
            .findClass(ClassId(StandardClassIds.BASE_COLLECTIONS_PACKAGE, Name.identifier(name)))!!
        val constructor = classSymbol.owner.constructors.first { it.parameters.none { p -> p.kind == IrParameterKind.Regular } }
        return irCallConstructor(constructor.symbol, info.typeArguments).apply { type = classSymbol.typeWith(info.typeArguments) }
    }

    /**
     * `toBuilder()`'s per-singular-field value: a fresh mutable copy of the entity's own (immutable)
     * collection, never an alias of it — or `null` if the entity's value is itself `null`.
     */
    private fun IrBlockBodyBuilder.copySingularValue(field: IrField, entityValue: IrExpression): IrExpression {
        val info = singularCollectionInfo(field.type) ?: return entityValue
        if (!entityValue.type.isNullable()) {
            return freshMutableCopy(info, field.file, entityValue)
        }
        val tmp = irTemporary(entityValue)
        val nonNullSource = irImplicitCast(irGet(tmp), entityValue.type.makeNotNull())
        return irIfNull(field.type, irGet(tmp), irNull(), freshMutableCopy(info, field.file, nonNullSource))
    }

    private val IrBlockBodyBuilder.pluginContext: IrPluginContext
        get() = context as IrPluginContext

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.builderConstructor(): IrConstructor =
        primaryConstructor ?: constructors.first()

    /**
     * The entity constructor this builder's [build] should invoke. `@Builder` may annotate a
     * secondary constructor/method instead of the class/primary constructor, so the primary-or-first
     * fallback isn't always correct; [builderAnnStartOffset] (from [BuilderDeclarationType.Function.Build])
     * pins down exactly which one by matching against the start offset of that same
     * constructor's own `@Builder` annotation call, read back here off its IR annotations.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.entityConstructorFor(builderAnnStartOffset: Int?): IrConstructor {
        return constructors.firstOrNull { constructor ->
            constructor.getAnnotation(LombokNames.BUILDER)?.let {
                it.startOffset == builderAnnStartOffset
            } ?: false
        } ?: builderConstructor()
    }

    /**
     * Builds `constructor(...)` for the value this function returns, i.e. the entity in `build` or the
     * builder in `builder`/`toBuilder`.
     *
     * Both the call's type arguments and its result type are taken from [declaration]'s return type rather
     * than from the constructor's own declaration. This matters for generic classes (KT-83334):
     *  - the type arguments must be present, otherwise a constructor call with zero type arguments for a
     *    generic class crashes JVM synthetic-accessor lowering;
     *  - the result type must reference the type parameter that is in scope at the call site (this
     *    function's / its class's parameter), not the constructed class's own parameter. For a factory
     *    like `fun <T> builder(): FooBuilder<T>` the constructor's declared return type `FooBuilder<T of
     *    FooBuilder>` mentions an out-of-scope parameter and fails IR validation.
     */
    private fun IrBlockBodyBuilder.irConstruct(declaration: IrSimpleFunction, constructor: IrConstructor): IrConstructorCall =
        irCallConstructor(constructor.symbol, declaration.constructedTypeArguments()).apply {
            type = declaration.returnType
        }

    private fun IrSimpleFunction.constructedTypeArguments(): List<IrType> =
        (returnType as? IrSimpleType)?.arguments?.map { it.typeOrFail } ?: emptyList()
}
