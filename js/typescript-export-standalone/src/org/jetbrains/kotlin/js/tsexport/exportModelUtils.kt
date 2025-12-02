/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaContextParameterApi::class)

package org.jetbrains.kotlin.js.tsexport

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JsStandardClassIds
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsExport
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsExportDefault
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsExportIgnore
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsImplicitExport
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.butIf

private val reservedWords = setOf(
    "break",
    "case",
    "catch",
    "class",
    "const",
    "continue",
    "debugger",
    "default",
    "delete",
    "do",
    "else",
    "enum",
    "export",
    "extends",
    "false",
    "finally",
    "for",
    "function",
    "if",
    "import",
    "in",
    "instanceof",
    "new",
    "null",
    "return",
    "super",
    "switch",
    "this",
    "throw",
    "true",
    "try",
    "typeof",
    "var",
    "void",
    "while",
    "with"
)

private val strictModeReservedWords = setOf(
    "as",
    "implements",
    "interface",
    "let",
    "package",
    "private",
    "protected",
    "public",
    "static",
    "yield"
)

internal val allReservedWords = reservedWords + strictModeReservedWords

private fun KaAnnotated.getSingleAnnotationArgumentString(annotationClassId: ClassId): String? {
    val annotation = annotations[annotationClassId].singleOrNull() ?: return null
    return ((annotation.arguments[0].expression as? KaAnnotationValue.ConstantValue)?.value as? KaConstantValue.StringValue)?.value
}

private fun KaAnnotated.getJsQualifier(): String? =
    getSingleAnnotationArgumentString(JsStandardClassIds.Annotations.JsQualifier)

context(_: KaSession)
private fun KaNamedSymbol.getSingleAnnotationArgumentStringForOverriddenDeclaration(annotationClassId: ClassId): String? {
    val argument = (this as? KaAnnotated)?.getSingleAnnotationArgumentString(annotationClassId)
    return when {
        argument != null -> argument
        this is KaCallableSymbol -> allOverriddenSymbols.firstNotNullOfOrNull { it.getSingleAnnotationArgumentString(annotationClassId) }
        else -> null
    }
}

context(_: KaSession)
private fun KaNamedSymbol.getJsNameForOverriddenDeclaration(): String? =
    getSingleAnnotationArgumentStringForOverriddenDeclaration(JsStandardClassIds.Annotations.JsName)

context(_: KaSession)
internal fun KaNamedSymbol.getJsSymbolForOverriddenDeclaration(): String? =
    getSingleAnnotationArgumentStringForOverriddenDeclaration(JsStandardClassIds.Annotations.JsSymbol)

context(_: KaSession)
internal fun KaNamedSymbol.getExportedIdentifier(): String {
    getJsNameForOverriddenDeclaration()?.let { return it }
    return name.asString()
}

context(_: KaSession)
internal fun shouldDeclarationBeExportedImplicitlyOrExplicitly(declaration: KaDeclarationSymbol): Boolean =
    declaration.isJsImplicitExport() || shouldDeclarationBeExported(declaration)

context(_: KaSession)
internal fun shouldDeclarationBeExported(declaration: KaDeclarationSymbol): Boolean {
    if (declaration.isExpect || declaration.isJsExportIgnore() || !declaration.visibility.isPublicApi) {
        return false
    }
    if (declaration.isExplicitlyExported()) {
        return true
    }

    if (declaration is KaCallableSymbol && declaration.isOverride) {
        return (declaration is KaNamedFunctionSymbol && declaration.isMethodOfAny)
                || declaration.allOverriddenSymbols.any { shouldDeclarationBeExported(it) }
    }

    val parent = declaration.containingDeclaration
    if (parent != null) {
        return shouldDeclarationBeExported(parent)
    }

    // FIXME(KT-82224): `containingFile` is always null for declarations deserialized from KLIBs
    return declaration.containingFile?.isJsExport() ?: false
}

internal val TypeScriptExportConfig.generateNamespacesForPackages: Boolean
    get() = artifactConfiguration.moduleKind != ModuleKind.ES

// TODO: Add memoization?
context(_: KaSession)
internal fun KaNamedSymbol.getExportedFqName(shouldIncludePackage: Boolean, isEsModules: Boolean): FqName {
    val name = Name.identifier(getExportedIdentifier())
    return when (val parent = containingDeclaration) {
        is KaNamedSymbol -> parent.getExportedFqName(shouldIncludePackage, isEsModules).child(name)
        null ->
            getTopLevelQualifier(shouldIncludePackage).child(name)
                .butIf(isEsModules && this is KaNamedClassSymbol && classKind == KaClassKind.OBJECT && !isExternal) {
                    // In ES modules, static members of a top-level object actually live in the <object name>.$metadata.type namespace,
                    // rather than just the <object name> namespace.
                    it.child(Name.identifier($$"$metadata$")).child(Name.identifier("type"))
                }
        else -> FqName.topLevel(name)
    }
}

private fun KaNamedSymbol.getTopLevelQualifier(shouldIncludePackage: Boolean): FqName {
    // TODO(KT-82224): Respect file-level @JsQualifier
    val jsQualifier = (this as? KaAnnotated)?.getJsQualifier()
    if (jsQualifier != null) {
        return FqName(jsQualifier)
    }

    if (!shouldIncludePackage) {
        return FqName.ROOT
    }

    return when (this) {
        is KaClassLikeSymbol -> classId?.packageFqName
        is KaCallableSymbol -> callableId?.packageName
        else -> null
    } ?: FqName.ROOT
}

private fun KaAnnotated.isJsImplicitExport(): Boolean =
    annotations.contains(JsImplicitExport)

private fun KaAnnotated.isJsExportIgnore(): Boolean =
    annotations.contains(JsExportIgnore)

internal fun KaAnnotated.isJsExport(): Boolean =
    annotations.contains(JsExport)

private fun KaAnnotated.isExplicitlyExported(): Boolean =
    annotations.contains(JsExport) || annotations.contains(JsExportDefault)

private val KaSymbolVisibility.isPublicApi: Boolean
    get() = this == KaSymbolVisibility.PUBLIC || this == KaSymbolVisibility.PROTECTED

@OptIn(KaExperimentalApi::class)
context(_: KaSession)
private val KaNamedFunctionSymbol.isMethodOfAny: Boolean
    get() {
        if (receiverParameter != null || contextParameters.isNotEmpty()) return false
        return when (name) {
            OperatorNameConventions.HASH_CODE, OperatorNameConventions.TO_STRING ->
                valueParameters.isEmpty()
            OperatorNameConventions.EQUALS ->
                valueParameters.singleOrNull()?.returnType?.fullyExpandedType?.let { it.isAnyType && it.isNullable } == true
            else -> false
        }
    }

context(_: KaSession)
private val KaCallableSymbol.isOverride: Boolean
    get() = directlyOverriddenSymbols.firstOrNull() != null
