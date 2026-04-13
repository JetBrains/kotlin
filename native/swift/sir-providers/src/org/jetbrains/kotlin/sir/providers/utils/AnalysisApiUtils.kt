/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.utils

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KaNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.contextParameters
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

public fun KaSymbolModality.isAbstract(): Boolean = when (this) {
    KaSymbolModality.FINAL, KaSymbolModality.OPEN -> false
    KaSymbolModality.SEALED, KaSymbolModality.ABSTRACT -> true
}

public val KaAnnotated.deprecatedAnnotation: Deprecated?
    get() = this.annotations[StandardClassIds.Annotations.Deprecated].firstOrNull()?.let {
        val arguments = it.arguments.associate { it.name.asString() to it.expression }

        val message = (arguments["message"] as? KaAnnotationValue.ConstantValue?)
            ?.value.toString().removeSurrounding("\"")

        val level = (arguments["level"] as? KaAnnotationValue.EnumEntryValue?)
            ?.callableId?.let {
                require(it.classId == ClassId.topLevel(StandardNames.FqNames.deprecationLevel))
                runCatching { kotlin.DeprecationLevel.valueOf(it.callableName.identifier) }.getOrNull()
            } ?: DeprecationLevel.WARNING

        val replaceWith = (arguments["replaceWith"] as? KaAnnotationValue.NestedAnnotationValue?)
            ?.annotation?.let {
                require(it.classId == ClassId.topLevel(StandardNames.FqNames.replaceWith))
                it.arguments.find { it.name.asString() == "expression" }
                    ?.let { it.expression as KaAnnotationValue.ConstantValue }
                    ?.let { it.value.value as? String }
            } ?: ""

        Deprecated(message, level = level, replaceWith = ReplaceWith(replaceWith))
    }

public val KaAnnotated.throwsAnnotation: Throws?
    get() = this.annotations[ClassId.topLevel(FqName.fromSegments(listOf("kotlin", "Throws")))].firstOrNull()?.let {
        val arguments = it.arguments.associate { it.name.asString() to it.expression }

        val classes = (arguments["exceptionClasses"] as? KaAnnotationValue.ArrayValue?)
            ?.values?.filterIsInstance<KaAnnotationValue.ClassLiteralValue>()

        Throws()
    }

private val ObjCNameClassId = ClassId.topLevel(FqName("kotlin.native.ObjCName"))

public class ObjCNameAnnotation(
    public val objCName: String?,
    public val swiftName: String?,
    public val isExact: Boolean,
) {
    /**
     * The Swift friendly declaration name, this will never be an empty string.
     */
    public val name: String? get() = swiftName?.takeIf { it.isNotEmpty() } ?: objCName

    /**
     * The Swift friendly argument name for this declaration, this might be an empty string.
     */
    public val argumentName: String? get() = swiftName ?: objCName
}

public val KaAnnotated.objCNameAnnotation: ObjCNameAnnotation?
    get() = annotations[ObjCNameClassId].firstOrNull()?.let { annotation ->
        var objCName: String? = null
        var swiftName: String? = null
        var isExact: Boolean? = null
        for (argument in annotation.arguments) {
            when (argument.name.identifier) {
                "name" -> objCName = argument.resolveConstantValue<KaConstantValue.StringValue>()?.value
                "swiftName" -> swiftName = argument.resolveConstantValue<KaConstantValue.StringValue>()?.value
                "exact" -> isExact = argument.resolveConstantValue<KaConstantValue.BooleanValue>()?.value
            }
        }
        if (swiftName == "_") swiftName = "" // In Swift export we convert empty names to _ where needed
        ObjCNameAnnotation(objCName, swiftName, isExact ?: false)
    }

private inline fun <reified T : KaConstantValue> KaNamedAnnotationValue.resolveConstantValue(): T? {
    val constantValue = expression as? KaAnnotationValue.ConstantValue ?: return null
    return constantValue.value as? T
}

@Suppress("OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION")
public val KaAnnotated.requiresOptInAnnotation: RequiresOptIn?
    get() = this.annotations[ClassId.topLevel(FqName("kotlin.RequiresOptIn"))].firstOrNull()?.let {
        val arguments = it.arguments.associate { it.name.asString() to it.expression }

        val message = (arguments["message"] as? KaAnnotationValue.ConstantValue?)
            ?.value?.toString()?.removeSurrounding("\"") ?: ""

        val level = (arguments["level"] as? KaAnnotationValue.EnumEntryValue?)
            ?.callableId?.let {
                require(it.classId == ClassId.topLevel(FqName("kotlin.RequiresOptIn")).createNestedClassId(Name.identifier("Level")))
                when (it.callableName.identifier) {
                    "WARNING" -> RequiresOptIn.Level.WARNING
                    "ERROR" -> RequiresOptIn.Level.ERROR
                    else -> RequiresOptIn.Level.ERROR
                }
            } ?: RequiresOptIn.Level.ERROR

        RequiresOptIn(message, level)
    }

/**
 * Extracts all opt-in requirements for the given declaration.
 * This includes both direct @RequiresOptIn annotations and indirect opt-in requirements
 * from annotations that themselves require opt-in.
 *
 * @return Set of ClassId representing all opt-in markers required for this declaration
 */
context(session: KaSession)
public val KaDeclarationSymbol.allRequiredOptIns: List<ClassId>
    get() = sequence {
        allRequiredOptInClassIds(this@allRequiredOptIns)
    }.distinct().toList().sortedBy { it.asFqNameString() }

private val KaAnnotation.classIdForOptInOrNull: ClassId?
    get() = this.constructorSymbol?.returnType?.symbol?.let { symbol ->
        symbol.classId?.takeIf { symbol.requiresOptInAnnotation != null }
    }

context(session: KaSession)
private suspend fun SequenceScope<ClassId>.allRequiredOptInClassIds(symbol: KaDeclarationSymbol): Unit = when (symbol) {
    is KaCallableSymbol -> allRequiredOptInClassIds(symbol)
    is KaClassLikeSymbol -> allRequiredOptInClassIds(symbol)
    else -> {}
}

context(session: KaSession)
private suspend fun SequenceScope<ClassId>.allRequiredOptInClassIds(symbol: KaClassLikeSymbol): Unit = with(session) {
    // Add supertype opt-in markers
    (symbol as? KaClassSymbol)?.superTypes.orEmpty().mapNotNull { type ->
        type.symbol as? KaClassSymbol
    }.forEach { allRequiredOptInClassIds(it) }

    // Add expanded type opt-in markers
    (symbol as? KaTypeAliasSymbol)?.expandedType?.let { allRequiredOptInClassIds(it) }

    // Add opt-in markers from lexical scope
    symbol.containingDeclaration?.let { allRequiredOptInClassIds(it) }

    // Add own opt-in markers
    symbol.annotations.forEach { it.classIdForOptInOrNull?.let { yield(it) } }
}

@OptIn(KaExperimentalApi::class)
context(session: KaSession)
private suspend fun SequenceScope<ClassId>.allRequiredOptInClassIds(
    type: KaType,
    shouldExpand: Boolean = true,
): Unit = with(session) {
    if (shouldExpand) {
        allRequiredOptInClassIds(type.fullyExpandedType, shouldExpand = false)
        type.abbreviation?.let { allRequiredOptInClassIds(it, shouldExpand = false) }
        return@with
    }
    when (type) {
        is KaFunctionType -> {
            type.contextReceivers.forEach { allRequiredOptInClassIds(it.type) }
            type.receiverType?.let { allRequiredOptInClassIds(it) }
            type.parameterTypes.forEach { allRequiredOptInClassIds(it) }
            allRequiredOptInClassIds(type.returnType)
        }
        is KaClassType -> {
            allRequiredOptInClassIds(type.symbol)
            type.typeArguments.forEach { it.type?.let { allRequiredOptInClassIds(it) } }
        }
        else -> {}
    }
}

@OptIn(KaExperimentalApi::class)
context(session: KaSession)
private suspend fun SequenceScope<ClassId>.allRequiredOptInClassIds(symbol: KaCallableSymbol): Unit = with(session) {
    // Add opt-in markers from lexical scope
    symbol.containingDeclaration?.let { allRequiredOptInClassIds(it) }

    // Add own opt-in markers
    symbol.annotations.forEach { it.classIdForOptInOrNull?.let { yield(it) } }

    // Add opt-in markers from the types used in signature
    symbol.typeParameters.forEach { it.upperBounds.forEach { allRequiredOptInClassIds(it) } }
    symbol.contextParameters.forEach { allRequiredOptInClassIds(it.returnType) }
    symbol.receiverParameter?.let { allRequiredOptInClassIds(it.returnType) }
    if (symbol is KaFunctionSymbol) {
        symbol.valueParameters.forEach { allRequiredOptInClassIds(it.returnType) }
    }
    allRequiredOptInClassIds(symbol.returnType)

    // Add opt-in markers from overridden declarations
    symbol.allOverriddenSymbols.forEach { allRequiredOptInClassIds(it) }
}
