/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaContextParameterApi::class, KaNonPublicApi::class, KtNonPublicApi::class)

package org.jetbrains.kotlin.js.tsexport

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.backend.js.tsexport.*
import org.jetbrains.kotlin.js.config.ModuleKind
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsExport
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsExportDefault
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsExportIgnore
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsImplicitExport
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsNoRuntime
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsStatic
import org.jetbrains.kotlin.psi.KtNonPublicApi
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
        this is KaCallableSymbol -> allOverriddenSymbols.firstNotNullOfOrNull {
            when (this) {
                is KaPropertyGetterSymbol -> (it as? KaPropertySymbol)?.getter?.getSingleAnnotationArgumentString(annotationClassId)
                is KaPropertySetterSymbol -> (it as? KaPropertySymbol)?.setter?.getSingleAnnotationArgumentString(annotationClassId)
                else -> it.getSingleAnnotationArgumentString(annotationClassId)
            }
        }
        else -> null
    }
}

context(_: KaSession)
internal fun KaSymbol.getJsNameForOverriddenDeclaration(): String? =
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

internal fun KaAnnotated.isJsNoRuntime(): Boolean =
    annotations.contains(JsNoRuntime)

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

private val KaCallableSymbol.isOverride: Boolean
    get() = when (this) {
        is KaNamedFunctionSymbol -> isOverride
        is KaPropertySymbol -> isOverride
        else -> false
    }

private const val ownImplementableSymbolName = "Symbol"
private const val notImplementablePropertyName = "__doNotUseOrImplementIt"

internal fun MutableList<ExportedDeclaration>.addOwnJsSymbolDeclaration() =
    add(
        ExportedField(
            name = ExportedMemberName.Identifier(ownImplementableSymbolName),
            type = ExportedType.Primitive.UniqueSymbol,
            mutable = false,
            isMember = false,
            isStatic = true,
        )
    )

context(_: KaSession)
internal fun MutableList<ExportedDeclaration>.addImplementableSymbolProperty(klass: KaNamedClassSymbol, config: TypeScriptExportConfig) =
    add(
        ExportedField(
            name = ExportedMemberName.SymbolReference(
                "${
                    klass.getExportedFqName(shouldIncludePackage = config.generateNamespacesForPackages, config).asString()
                }.$ownImplementableSymbolName"
            ),
            type = ExportedType.LiteralType.BooleanLiteralType(true),
            mutable = false,
            isMember = true,
            isStatic = false,
        )
    )

private fun KaNamedClassSymbol.shouldContainNotImplementableProperty(
    config: TypeScriptExportConfig,
    hasNonExportedAbstractMembers: Boolean,
): Boolean =
    hasNonExportedAbstractMembers || isJsImplicitExport() ||
            classId?.packageFqName == StandardNames.COLLECTIONS_PACKAGE_FQ_NAME ||
            (!config.implementableInterfaces && classKind == KaClassKind.INTERFACE && !isExternal && !isJsNoRuntime())

@OptIn(KaExperimentalApi::class)
context(_: KaSession)
internal fun MutableList<ExportedDeclaration>.addSuperTypesSpecialProperties(
    klass: KaNamedClassSymbol,
    superTypes: List<KaType>,
    typeParameterScope: TypeParameterScope,
    config: TypeScriptExportConfig,
    hasNonExportedAbstractMembers: Boolean,
) {
    val allSuperTypesWithBrandProperty = klass.collectAllImplementableAndNotImplementableInterfaces(superTypes, config)
    val typeItselfShouldNotBeImplemented = klass.shouldContainNotImplementableProperty(config, hasNonExportedAbstractMembers)

    val (implementableSuperTypes, notImplementableSuperTypes) = allSuperTypesWithBrandProperty.partition { it.value }

    for ((superType, _) in implementableSuperTypes) {
        addImplementableSymbolProperty(superType, config)
    }

    if (notImplementableSuperTypes.isEmpty()) {
        if (typeItselfShouldNotBeImplemented) addNotImplementableProperty(klass, config)
        return
    }

    val intersectionOfTypes = notImplementableSuperTypes
        .map { (superType, _) ->
            // TODO: rework it to stricter types instead of `any` for type parameters
            val superTypeWithDynamicArguments = typeCreator.classType(superType) {
                for (i in superType.typeParameters.indices) {
                    invariantTypeArgument(dynamicType())
                }
            }
            ExportedType.PropertyType(
                TypeExporter(config, typeParameterScope).exportType(superTypeWithDynamicArguments),
                ExportedType.LiteralType.StringLiteralType(notImplementablePropertyName),
            )
        }
        .reduce(ExportedType::IntersectionType)
        .butIf(typeItselfShouldNotBeImplemented) {
            ExportedType.IntersectionType(
                klass.generateNotImplementableBrandType(config),
                it
            )
        }

    add(
        ExportedField(
            name = ExportedMemberName.Identifier(notImplementablePropertyName),
            type = intersectionOfTypes,
            mutable = false,
            isMember = true,
        )
    )
}

context(_: KaSession)
private fun MutableList<ExportedDeclaration>.addNotImplementableProperty(klass: KaNamedClassSymbol, config: TypeScriptExportConfig) {
    add(
        ExportedField(
            name = ExportedMemberName.Identifier(notImplementablePropertyName),
            type = klass.generateNotImplementableBrandType(config),
            mutable = false,
            isMember = true,
        )
    )
}

context(_: KaSession)
private fun KaNamedClassSymbol.generateNotImplementableBrandType(config: TypeScriptExportConfig): ExportedType {
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

/**
 * With this method we're collecting all the super types that may contain either implementable or non-implementable properties
 * - For interfaces we're looking only parents that contain not-implementable properties
 * - For classes we're looking for both kinds of super-types
 *
 * We're collecting such information to copy those properties into the current declaration to generate a valid TypeScript definition
 *
 * As an example:
 * ```kotlin
 * @JsExport interface Foo
 * @JsExport interface Bar
 *
 * class NotExportedParent : Foo, Bar
 *
 * @JsExport
 * class ExportedChild : NotExportedParent
 * ```
 *
 * For such a class we should do the following:
 * 1. Implementable interfaces and no strict implicit export
 * ```typescript
 * declare interface Foo { readonly [Foo.Symbol]: true }
 * declare namespace Foo { const Symbol: unique symbol; }
 *
 * declare interface Bar { readonly [Bar.Symbol]: true }
 * declare namespace Bar { const Symbol: unique symbol; }
 *
 * declare  class ExportedChild implements Foo, Bar {
 *   readonly [Foo.Symbol]: true;
 *   readonly [Bar.Symbol]: true;
 * }
 * ```
 * 2. Implementable interfaces and strict implicit export
 * ```typescript
 * declare interface Foo { readonly [Foo.Symbol]: true }
 * declare namespace Foo { const Symbol: unique symbol; }
 *
 * declare interface Bar { readonly [Bar.Symbol]: true }
 * declare namespace Bar { const Symbol: unique symbol; }
 *
 * declare interface NotExportedParent extends Foo, Bar {
 *    readonly __doNotUseOrImplementIt: {
 *      readonly "NotExportedParent": unique symbol;
 *    };
 * }
 *
 * declare class ExportedChild implements NotExportedParent {
 *   readonly [Foo.Symbol]: true;
 *   readonly [Bar.Symbol]: true;
 *   readonly __doNotUseOrImplementIt: {
 *      readonly "NotExportedParent": unique symbol;
 *   };
 * }
 * ```
 *
 * 3. Not-implementable interfaces and no strict implicit export
 * ```typescript
 * declare interface Foo {
 *    readonly __doNotUseOrImplementIt: {
 *      readonly "Foo": unique symbol;
 *    };
 * }
 *
 * declare interface Bar {
 *    readonly __doNotUseOrImplementIt: {
 *      readonly "Bar": unique symbol;
 *    };
 * }
 *
 * declare class ExportedChild implements Foo, Bar {
 *   readonly __doNotUseOrImplementIt: Foo["__doNotUseOrImplementIt"] & Bar["__doNotUseOrImplementIt"]
 * }
 * ```
 *
 * 3. Not-implementable interfaces and strict implicit export
 * ```typescript
 * declare interface Foo {
 *    readonly __doNotUseOrImplementIt: {
 *      readonly "Foo": unique symbol;
 *    };
 * }
 *
 * declare interface Bar {
 *    readonly __doNotUseOrImplementIt: {
 *      readonly "Bar": unique symbol;
 *    };
 * }
 *
 * declare interface NotExportedParent extends Foo, Bar {
 *    readonly __doNotUseOrImplementIt: Foo["__doNotUseOrImplementIt"] & Bar["__doNotUseOrImplementIt"] & {
 *      readonly "NotExportedParent": unique symbol;
 *    };
 * }
 *
 * declare class ExportedChild implements NotExportedParent {
 *   readonly __doNotUseOrImplementIt: NotExportedParent["__doNotUseOrImplementIt"]
 * }
 * ```
 *
 * Because of such complications, I believe we should remove the strict-implicit export in future (since it's unstable and we don't have a plan to support it in future)
 */
// TODO: think about per class memoization
context(_: KaSession)
private fun KaNamedClassSymbol.collectAllImplementableAndNotImplementableInterfaces(
    superTypes: Iterable<KaType>,
    config: TypeScriptExportConfig,
): Collection<Map.Entry<KaNamedClassSymbol, Boolean>> {
    fun MutableList<KaNamedClassSymbol>.enqueueSuperTypes(superTypes: Iterable<KaType>) =
        superTypes.mapNotNullTo(this) { superType ->
            (superType.expandedSymbol as? KaNamedClassSymbol)?.takeIf { !it.isExternal }
        }

    // If we're processing an interface:
    // - If it's not implementable, we just need to add its direct not-implementable super types to generate a correct type for __doNotUseOrImplementIt
    // - If it's implementable, we don't need to generate anything, since it's already receiving all the properties through the inheritance
    // If we're processing a class, we should collect:
    // - All the implementable interfaces in the hierarchy to add the correct Symbol
    // - All the nearest not-implementable interfaces to generate a correct type for __doNotUseOrImplementIt
    val result = linkedMapOf<KaNamedClassSymbol, Boolean>()
    val stack = mutableListOf<KaNamedClassSymbol>()
    stack.enqueueSuperTypes(superTypes)

    while (stack.isNotEmpty()) {
        val processedClass = stack.removeLast().takeIf { it !in result } ?: continue

        if (processedClass.isJsImplicitExport()) {
            result[processedClass] = false
            continue
        }

        if (!shouldDeclarationBeExported(processedClass)) continue

        if (processedClass.hasNonExportedAbstractMembers()) {
            result[processedClass] = false
            continue
        }

        if (processedClass.classKind == KaClassKind.INTERFACE && !processedClass.isJsNoRuntime()) {
            if (config.implementableInterfaces) {
                if (classKind == KaClassKind.INTERFACE) continue
                result[processedClass] = true
            } else {
                result[processedClass] = false
            }
        }

        if (result.isNotEmpty()) {
            stack.enqueueSuperTypes(processedClass.superTypes)
        }
    }

    return result.entries
}

internal fun KaDeclarationSymbol.exportedVisibility(parent: KaDeclarationSymbol?): ExportedVisibility =
    when (visibility) {
        KaSymbolVisibility.PROTECTED if (parent?.modality == KaSymbolModality.SEALED || parent?.modality == KaSymbolModality.FINAL) ->
            ExportedVisibility.PRIVATE
        KaSymbolVisibility.PROTECTED -> ExportedVisibility.PROTECTED
        else -> ExportedVisibility.DEFAULT
    }

context(_: KaSession)
internal fun <T : ExportedDeclaration> T.withAttributes(source: KaDeclarationSymbol?, ignoreDoc: Boolean = false): T {
    if (source == null) return this
    if (this is ExportedConstructor && visibility == ExportedVisibility.PRIVATE) return this

    source.getSingleAnnotationArgumentString(StandardClassIds.Annotations.Deprecated)?.let {
        attributes.add(ExportedAttribute.DeprecatedAttribute(it))
    }

    if (source.annotations.contains(JsExportDefault)) {
        attributes.add(ExportedAttribute.DefaultExport)
    }

    if (!ignoreDoc) addDocumentationAttributes(source)

    return this
}


/**
 * We only process interfaces because it's impossible to cover the following case:
 * an abstract class that extends another abstract class which contains an ignored abstract member
 * but the current inheritor overrides them all and converts them into non-abstract members.
 *
 * Example code:
 * ```kotlin
 * @JsExport
 * abstract class A {
 *   @JsExport.Ignore
 *   abstract fun a(): Int
 * }
 *
 *
 * @JsExport
 * interface B {
 *   @JsExport.Ignore
 *   fun b(): Int
 * }
 *
 * @JsExport
 * abstract class ProblemOne : A(), B {
 *    override fun a(): Int = 1
 *    // Here we should generate our magic `__doNotUseItOrImplementIt` both as abstract and non-abstract member
 *    // It's impossible to express in TypeScript
 * }
 * ```
 */
context(_: KaSession)
internal fun KaNamedClassSymbol.hasNonExportedAbstractMembers(): Boolean =
    classKind == KaClassKind.INTERFACE && declaredMemberScope.callables.any { !it.isOverride && it.isJsExportIgnore() }
