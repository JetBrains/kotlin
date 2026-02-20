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
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedAttribute
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedDeclaration
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedMemberName
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedField
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedType
import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedVisibility
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JsStandardClassIds
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsExport
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsExportDefault
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsExportIgnore
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsImplicitExport
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsStatic
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
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
private fun KaSymbol.getSingleAnnotationArgumentStringForOverriddenDeclaration(annotationClassId: ClassId): String? {
    val argument = (this as? KaAnnotated)?.getSingleAnnotationArgumentString(annotationClassId)
    return when {
        argument != null -> argument
        this is KaCallableSymbol -> allOverriddenSymbols.firstNotNullOfOrNull { it.getSingleAnnotationArgumentString(annotationClassId) }
        else -> null
    }
}

context(_: KaSession)
private fun KaSymbol.getJsNameForOverriddenDeclaration(): String? =
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
    val parentModality = parent?.modality
    if (!(declaration is KaConstructorSymbol && declaration.isPrimary)
        && declaration.visibility == KaSymbolVisibility.PROTECTED
        && (parentModality == KaSymbolModality.FINAL || parentModality == KaSymbolModality.SEALED)
    ) {
        // Protected members inside final classes are effectively private.
        // Protected members inside sealed classes are effectively module-private.
        // The only exception is the primary constructor: we will set its visibility to private during
        // TypeScript export model generation, otherwise, if no (private) primary constructor is exported, there will be
        // a default constructor, which we don't want.
        return false
    }

    if (parent != null) {
        return shouldDeclarationBeExported(parent)
    }

    // FIXME(KT-82224): `containingFile` is always null for declarations deserialized from KLIBs
    return declaration.containingFile?.isJsExport() ?: false
}

internal fun KaAnnotated.getJsName(): String? =
    getSingleAnnotationArgumentString(JsStandardClassIds.Annotations.JsName)

internal val TypeScriptExportConfig.generateNamespacesForPackages: Boolean
    get() = artifactConfiguration.moduleKind != ModuleKind.ES

// TODO: Add memoization?
context(_: KaSession)
internal fun KaNamedSymbol.getExportedFqName(shouldIncludePackage: Boolean, config: TypeScriptExportConfig): FqName {
    val name = Name.identifier(getExportedIdentifier())
    val isEsModules = config.artifactConfiguration.moduleKind == ModuleKind.ES
    return when (val parent = containingDeclaration) {
        is KaNamedSymbol -> parent.getExportedFqName(shouldIncludePackage, config).child(name)
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

internal fun KaAnnotated.isJsImplicitExport(): Boolean =
    annotations.contains(JsImplicitExport)

private fun KaAnnotated.isJsExportIgnore(): Boolean =
    annotations.contains(JsExportIgnore)

internal fun KaAnnotated.isJsExport(): Boolean =
    annotations.contains(JsExport)

internal fun KaAnnotated.isJsStatic(): Boolean =
    annotations.contains(JsStatic)

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

internal val KaNamedClassSymbol.shouldNotBeImplemented: Boolean
    get() = classKind == KaClassKind.INTERFACE && !isExternal || isJsImplicitExport()

context(_: KaSession)
internal fun KaNamedClassSymbol.shouldContainImplementationOfMagicProperty(superTypes: List<KaType>): Boolean {
    return !isExternal && superTypes.any {
        val superClass = it.expandedSymbol ?: return@any false
        superClass.classKind == KaClassKind.INTERFACE && it.shouldAddMagicPropertyOfSuper || superClass.isJsImplicitExport()
    }
}

private const val magicPropertyName = "__doNotUseOrImplementIt"

context(_: KaSession)
internal fun MutableList<ExportedDeclaration>.addMagicInterfaceProperty(klass: KaNamedClassSymbol, config: TypeScriptExportConfig) {
    add(
        ExportedField(
            name = ExportedMemberName.Identifier(magicPropertyName),
            type = klass.generateTagType(config),
            mutable = false,
            isMember = true,
        )
    )
}

context(_: KaSession)
internal fun MutableList<ExportedDeclaration>.addMagicPropertyForInterfaceImplementation(
    klass: KaNamedClassSymbol,
    superTypes: List<KaType>,
    typeParameterScope: TypeParameterScope,
    config: TypeScriptExportConfig,
) {
    val allSuperTypesWithMagicProperty = superTypes.filter { it.shouldAddMagicPropertyOfSuper }
    if (allSuperTypesWithMagicProperty.isEmpty()) return

    var intersectionOfTypes = allSuperTypesWithMagicProperty
        .map {
            ExportedType.PropertyType(
                container = TypeExporter(config, typeParameterScope).exportType(it),
                propertyName = ExportedType.LiteralType.StringLiteralType(magicPropertyName),
            )
        }
        .reduce(ExportedType::IntersectionType)

    if (klass.shouldNotBeImplemented) {
        intersectionOfTypes = ExportedType.IntersectionType(klass.generateTagType(config), intersectionOfTypes)
    }

    add(
        ExportedField(
            name = ExportedMemberName.Identifier(magicPropertyName),
            type = intersectionOfTypes,
            mutable = false,
            isMember = true,
        )
    )
}

context(_: KaSession)
private val KaType.shouldAddMagicPropertyOfSuper: Boolean
    get() {
        val klass = this.expandedSymbol ?: return false
        if (klass.isJsImplicitExport()) return true
        if (!shouldDeclarationBeExported(klass)) return false
        return klass.classKind == KaClassKind.INTERFACE && !(klass is KaNamedClassSymbol && klass.isExternal) || klass.superTypes.any {
            it.shouldAddMagicPropertyOfSuper
        }
    }

context(_: KaSession)
private fun KaNamedClassSymbol.generateTagType(config: TypeScriptExportConfig): ExportedType {
    return ExportedType.InlineInterfaceType(
        listOf(
            ExportedField(
                name = ExportedMemberName.Identifier(getExportedFqName(shouldIncludePackage = true, config).asString()),
                type = ExportedType.Primitive.UniqueSymbol,
                mutable = false,
                isMember = true,
            )
        )
    )
}

internal fun KaDeclarationSymbol.exportedVisibility(parent: KaDeclarationSymbol?): ExportedVisibility =
    when (visibility) {
        KaSymbolVisibility.PROTECTED if (parent?.modality == KaSymbolModality.SEALED || parent?.modality == KaSymbolModality.FINAL) ->
            ExportedVisibility.PRIVATE
        KaSymbolVisibility.PROTECTED -> ExportedVisibility.PROTECTED
        else -> ExportedVisibility.DEFAULT
    }

internal fun <T : ExportedDeclaration> T.withAttributes(source: KaDeclarationSymbol?): T {
    source?.getSingleAnnotationArgumentString(StandardClassIds.Annotations.Deprecated)?.let {
        attributes.add(ExportedAttribute.DeprecatedAttribute(it))
    }
    return this
}