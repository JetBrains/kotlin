/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.dokka.ExperimentalDokkaApi::class)

package org.jetbrains.kotlin.documentation.analyzer.internal

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.links.parent
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.Since
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.model.doc.Throws
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.kotlin.documentation.*
import org.jetbrains.dokka.model.AnnotationTarget as DokkaAnnotationTarget
import org.jetbrains.dokka.model.doc.Deprecated as DeprecatedTag

/** Shorthand to avoid repeating the fully-qualified name everywhere in this package. */
internal typealias DokkaSourceSet = DokkaConfiguration.DokkaSourceSet

/** The `inline`/`infix`/`suspend`/`static`/... modifiers of the subject of [this] property container, in [sourceSet]. */
internal fun <C : Documentable> PropertyContainer<C>.extraModifiersIn(sourceSet: DokkaSourceSet): Set<ExtraModifiers> {
    return this[AdditionalModifiers]?.content?.get(sourceSet).orEmpty()
}

internal val DClasslike.extra: PropertyContainer<DClasslike>
    @Suppress("UNCHECKED_CAST")
    get() = when (this) {
        is DClass -> extra as PropertyContainer<DClasslike>
        is DInterface -> extra as PropertyContainer<DClasslike>
        is DObject -> extra as PropertyContainer<DClasslike>
        is DEnum -> extra as PropertyContainer<DClasslike>
        is DAnnotation -> extra as PropertyContainer<DClasslike>
    }

// region Since / deprecation

private fun <C : DokkaAnnotationTarget> PropertyContainer<C>.annotationIn(
    packageName: String,
    className: String,
    sourceSet: DokkaSourceSet,
): Annotations.Annotation? = this[Annotations]?.directAnnotations?.get(sourceSet)?.firstOrNull {
    it.dri.packageName == packageName && it.dri.classNames == className
}

private fun Annotations.Annotation.stringArgument(name: String): String? =
    (params[name] as? StringValue)?.value?.takeIf { it.isNotEmpty() }

private fun <C : DokkaAnnotationTarget> PropertyContainer<C>.sinceIn(
    documentable: Documentable,
    sourceSet: DokkaSourceSet,
): String? = annotationIn("kotlin", "SinceKotlin", sourceSet)?.stringArgument("version")
    ?: annotationIn("kotlin", "IntroducedAt", sourceSet)?.stringArgument("version")
    ?: documentable.documentation[sourceSet]?.children?.filterIsInstance<Since>()?.firstOrNull()
        ?.root?.plainText()?.trim()?.takeIf { it.isNotEmpty() }

private fun DocTag.plainText(): String = when (this) {
    is Text -> body
    else -> children.joinToString("") { it.plainText() }
}

private fun <C : DokkaAnnotationTarget> PropertyContainer<C>.deprecatedIn(
    documentable: Documentable,
    sourceSet: DokkaSourceSet,
): KdDeprecated? {
    val kotlin = annotationIn("kotlin", "Deprecated", sourceSet)
    val java = annotationIn("java.lang", "Deprecated", sourceSet)
    val tag = documentable.documentation[sourceSet]?.children?.any { it is DeprecatedTag } == true
    if (kotlin == null && java == null && !tag) return null

    val level = when ((kotlin?.params?.get("level") as? EnumValue)?.enumName?.substringAfterLast('.')) {
        "ERROR" -> KdDeprecationLevel.ERROR
        "HIDDEN" -> error("Hidden declaration should have been filtered out: ${documentable.dri}")
        else -> KdDeprecationLevel.WARNING
    }
    val sinceKotlin = annotationIn("kotlin", "DeprecatedSinceKotlin", sourceSet)
    val replacement = (kotlin?.params?.get("replaceWith") as? AnnotationValue)?.annotation
    return KdDeprecated(
        since = java?.stringArgument("since")
            ?: sinceKotlin?.stringArgument(if (level == KdDeprecationLevel.ERROR) "errorSince" else "warningSince"),
        // KdDocumentation is still a placeholder, just as for declaration documentation.
        message = KdDocumentation.EMPTY,
        level = level,
        replaceWith = replacement?.stringArgument("expression")?.let { expression ->
            KdReplaceWith(
                imports = (replacement.params["imports"] as? ArrayValue)?.value.orEmpty().map { (it as StringValue).value },
                expression = KdExpression(text = expression, links = emptyList()),
            )
        },
    )
}

// endregion

// region Element builders

internal fun <C> PropertyContainer<C>.throwsIn(
    documentable: Documentable,
    sourceSet: DokkaSourceSet,
): List<KdThrows> where C : Documentable, C : DokkaAnnotationTarget {
    val declared = this[CheckedExceptions]?.exceptions?.get(sourceSet).orEmpty()
    val annotation = annotationIn("kotlin", "Throws", sourceSet) ?: annotationIn("kotlin.jvm", "Throws", sourceSet)
    val annotated = (annotation?.params?.get("exceptionClasses") as? ArrayValue)?.value.orEmpty()
        .filterIsInstance<ClassValue>().map { it.classDRI }
    val documented = documentable.documentation[sourceSet]?.children.orEmpty().filterIsInstance<Throws>()
        .mapNotNull { it.exceptionAddress }
    // Documentation is a placeholder until KdDocumentation can represent tag contents.
    return (declared + annotated + documented).map { it.toKdElementId() }.distinct().map { KdThrows(it) }
}

internal fun buildFunctionElement(function: DFunction, sourceSet: DokkaConfiguration.DokkaSourceSet): KdFunction {
    val mods = function.extra.extraModifiersIn(sourceSet)
    return KdFunction(
        name = function.name,
        isCompanion = function.extra[IsCompanion] != null,
        isExternal = ExtraModifiers.KotlinOnlyModifiers.External in mods || ExtraModifiers.JavaOnlyModifiers.Native in mods,
        isSuspend = ExtraModifiers.KotlinOnlyModifiers.Suspend in mods,
        isOperator = ExtraModifiers.KotlinOnlyModifiers.Operator in mods,
        isInfix = ExtraModifiers.KotlinOnlyModifiers.Infix in mods,
        isInline = ExtraModifiers.KotlinOnlyModifiers.Inline in mods,
        isTailRec = ExtraModifiers.KotlinOnlyModifiers.TailRec in mods,
        mappedTo = null, // TODO KT-90419
        returnType = function.type.toKdType(sourceSet),
        typeParameters = function.generics.map { it.toKdTypeParameter(sourceSet) },
        valueParameters = function.parameters.map { it.toKdValueParameter(sourceSet) },
        extensionReceiverParameter = function.receiver?.toKdExtensionReceiverParameter(sourceSet),
        contextParameters = function.contextParameters.map { it.toKdContextParameter(sourceSet) },
        overridens = function.extra[OverriddenMembers]?.members?.get(sourceSet).orEmpty().map { it.toKdElementId() }.distinct(),
        visibility = function.visibility[sourceSet]?.toKdVisibility() ?: KdVisibility.PUBLIC,
        modality = function.modifier[sourceSet]?.toKdModality() ?: KdModality.FINAL,
        since = function.extra.sinceIn(function, sourceSet),
        deprecated = function.extra.deprecatedIn(function, sourceSet),
        throws = function.extra.throwsIn(function, sourceSet),
        annotations = function.extra.annotationsIn(sourceSet),
        documentation = KdDocumentation.EMPTY,
    )
}

internal fun buildConstructorElement(constructor: DFunction, sourceSet: DokkaConfiguration.DokkaSourceSet): KdConstructor {
    val mods = constructor.extra.extraModifiersIn(sourceSet)
    return KdConstructor(
        isPrimary = constructor.extra[PrimaryConstructorExtra] != null,
        isExternal = ExtraModifiers.KotlinOnlyModifiers.External in mods || ExtraModifiers.JavaOnlyModifiers.Native in mods,
        mappedTo = null, // TODO KT-90419
        typeParameters = constructor.generics.map { it.toKdTypeParameter(sourceSet) },
        valueParameters = constructor.parameters.map { it.toKdValueParameter(sourceSet) },
        visibility = constructor.visibility[sourceSet]?.toKdVisibility() ?: KdVisibility.PUBLIC,
        since = constructor.extra.sinceIn(constructor, sourceSet),
        deprecated = constructor.extra.deprecatedIn(constructor, sourceSet),
        throws = constructor.extra.throwsIn(constructor, sourceSet),
        annotations = constructor.extra.annotationsIn(sourceSet),
        documentation = KdDocumentation.EMPTY,
    )
}

internal fun buildPropertyElement(
    property: DProperty,
    sourceSet: DokkaConfiguration.DokkaSourceSet,
    getterId: KdElementId?,
    setterId: KdElementId?,
): KdProperty {
    val mods = property.extra.extraModifiersIn(sourceSet)
    return KdProperty(
        name = property.name,
        isInline = ExtraModifiers.KotlinOnlyModifiers.Inline in mods ||
                property.getter?.extra?.extraModifiersIn(sourceSet)?.contains(ExtraModifiers.KotlinOnlyModifiers.Inline) == true &&
                (property.setter == null || property.setter.extra.extraModifiersIn(sourceSet).contains(ExtraModifiers.KotlinOnlyModifiers.Inline)),
        isCompanion = property.extra[IsCompanion] != null,
        isExternal = ExtraModifiers.KotlinOnlyModifiers.External in mods || ExtraModifiers.JavaOnlyModifiers.Native in mods,
        isSuspend = ExtraModifiers.KotlinOnlyModifiers.Suspend in mods, // TODO AA does not support `suspend` for properties
        isMutable = property.extra[IsVar] != null,
        isFromPrimaryConstructor = property.extra[IsAlsoParameter]?.inSourceSets.orEmpty().any { it.sourceSetID == sourceSet.sourceSetID },
        isLateinit = ExtraModifiers.KotlinOnlyModifiers.LateInit in mods,
        constExpression = if (ExtraModifiers.KotlinOnlyModifiers.Const in mods) {
            property.extra[DefaultValue]?.expression?.get(sourceSet)?.let { expression ->
                // TODO add new const values: [UInt], [Chat], [Byte] and so on
                val value = when (expression) {
                    is StringConstant -> KdConstValue.String(expression.value)
                    is IntegerConstant -> KdConstValue.Int(expression.value.toInt())
                    is BooleanConstant -> KdConstValue.Boolean(expression.value)
                    is DoubleConstant -> KdConstValue.Double(expression.value)
                    is FloatConstant -> KdConstValue.Float(expression.value)
                    else -> error("Unsupported const initializer: $expression")
                }
                //
                /**
                 * TODO keep the original expression with links:
                 * const val a = 1
                 * const val b = a
                 */
                KdConstExpression(value, expression.toKdExpression())
            }
        } else null,
        getter = getterId,
        setter = setterId,
        mappedTo = null, // TODO KT-90419
        type = property.type.toKdType(sourceSet),
        typeParameters = property.generics.map { it.toKdTypeParameter(sourceSet) },
        extensionReceiverParameter = property.receiver?.toKdExtensionReceiverParameter(sourceSet),
        contextParameters = property.contextParameters.map { it.toKdContextParameter(sourceSet) },
        overridens = property.extra[OverriddenMembers]?.members?.get(sourceSet).orEmpty().map { it.toKdElementId() }.distinct(),
        visibility = property.visibility[sourceSet]?.toKdVisibility() ?: KdVisibility.PUBLIC,
        modality = property.modifier[sourceSet]?.toKdModality() ?: KdModality.FINAL,
        since = property.extra.sinceIn(property, sourceSet),
        deprecated = property.extra.deprecatedIn(property, sourceSet),
        throws = property.extra.throwsIn(property, sourceSet),
        annotations = property.extra.annotationsIn(sourceSet),
        documentation = KdDocumentation.EMPTY,
    )
}

internal fun buildTypealiasElement(typeAlias: DTypeAlias, sourceSet: DokkaConfiguration.DokkaSourceSet): KdTypealias =
    KdTypealias(
        name = typeAlias.name,
        typeParameters = typeAlias.generics.map { it.toKdTypeParameter(sourceSet) },
        underlyingType = requireNotNull(typeAlias.underlyingType[sourceSet]) { "Missing underlying type for ${typeAlias.dri} in $sourceSet" }.toKdType(sourceSet),
        declarations = emptyList(), // TODO on Dokka's side
        visibility = typeAlias.visibility[sourceSet]?.toKdVisibility() ?: KdVisibility.PUBLIC,
        since = typeAlias.extra.sinceIn(typeAlias, sourceSet),
        deprecated = typeAlias.extra.deprecatedIn(typeAlias, sourceSet),
        annotations = typeAlias.extra.annotationsIn(sourceSet),
        documentation = KdDocumentation.EMPTY,
    )

internal fun buildEnumEntryElement(enumEntry: DEnumEntry, sourceSet: DokkaConfiguration.DokkaSourceSet): KdProperty =
    KdProperty(
        name = enumEntry.name,
        isInline = false,
        isCompanion = false,
        isExternal = false,
        isSuspend = false,
        isMutable = false,
        isFromPrimaryConstructor = false,
        isLateinit = false,
        constExpression = null,
        getter = null,
        setter = null,
        mappedTo = null, // TODO KT-90419
        type = KdClassLikeType(
            qualifiers = listOf(KdClassTypeQualifier(enumEntry.dri.parent.classNames.orEmpty().substringAfterLast('.'), emptyList())),
            classLikeId = enumEntry.dri.parent.toKdElementId(),
            isMarkedNullable = false,
            annotations = emptyList(),
        ),
        typeParameters = emptyList(),
        extensionReceiverParameter = null,
        contextParameters = emptyList(),
        overridens = emptyList(),
        visibility = KdVisibility.PUBLIC,
        modality = KdModality.FINAL,
        since = enumEntry.extra.sinceIn(enumEntry, sourceSet),
        deprecated = enumEntry.extra.deprecatedIn(enumEntry, sourceSet),
        throws = enumEntry.extra.throwsIn(enumEntry, sourceSet),
        annotations = enumEntry.extra.annotationsIn(sourceSet),
        documentation = KdDocumentation.EMPTY,
    )

internal fun buildClassElement(
    node: DClasslike,
    sourceSet: DokkaConfiguration.DokkaSourceSet,
    declarations: List<KdElementId>,
    isCompanion: Boolean,
): KdClass {
    val mods = node.extra.extraModifiersIn(sourceSet)
    val annotations = node.extra.annotationsIn(sourceSet)
    val since = node.extra.sinceIn(node, sourceSet)
    val deprecated = node.extra.deprecatedIn(node, sourceSet)
    val classKind = when (node) {
        is DClass -> KdClassKind.CLASS
        is DInterface -> KdClassKind.INTERFACE
        is DObject -> KdClassKind.OBJECT
        is DEnum -> KdClassKind.ENUM_CLASS
        is DAnnotation -> KdClassKind.ANNOTATION_CLASS
    }
    val modality = (node as? WithAbstraction)?.modifier?.get(sourceSet)?.toKdModality() ?: KdModality.FINAL
    val generics = (node as? WithGenerics)?.generics.orEmpty()
    val superTypes = (node as? WithSupertypes)?.supertypes?.get(sourceSet).orEmpty()
    return KdClass(
        name = node.name.orEmpty(),
        classKind = classKind,
        isCompanion = isCompanion,
        isData = ExtraModifiers.KotlinOnlyModifiers.Data in mods,
        isValue = ExtraModifiers.KotlinOnlyModifiers.Value in mods,
        isFun = ExtraModifiers.KotlinOnlyModifiers.Fun in mods,
        isInner = ExtraModifiers.KotlinOnlyModifiers.Inner in mods,
        isExternal = ExtraModifiers.KotlinOnlyModifiers.External in mods,
        isError = false,
        mappedTo = null, // TODO KT-90419
        typeParameters = generics.map { it.toKdTypeParameter(sourceSet) },
        superTypes = superTypes.map { it.typeConstructor.toKdType(sourceSet) },
        declarations = declarations,
        visibility = node.visibility[sourceSet]?.toKdVisibility() ?: KdVisibility.PUBLIC,
        modality = modality,
        since = since,
        deprecated = deprecated,
        annotations = annotations,
        documentation = KdDocumentation.EMPTY,
    )
}

// endregion

// region Parameters / type parameters

private fun DTypeParameter.toKdTypeParameter(sourceSet: DokkaConfiguration.DokkaSourceSet): KdTypeParameter {
    val variance = when (variantTypeParameter) {
        is Covariance -> KdTypeVariance.OUT
        is Contravariance -> KdTypeVariance.IN
        is Invariance -> null
    }
    return KdTypeParameter(
        name = name,
        isReified = extra.extraModifiersIn(sourceSet).let { ExtraModifiers.KotlinOnlyModifiers.Reified in it },
        upperBounds = bounds.map { it.toKdType(sourceSet) },
        variance = variance,
        annotations = extra.annotationsIn(sourceSet),
        documentation = KdDocumentation.EMPTY,
    )
}

private fun DParameter.toKdValueParameter(sourceSet: DokkaConfiguration.DokkaSourceSet): KdValueParameter {
    val mods = extra.extraModifiersIn(sourceSet)
    val defaultValueExpression = extra[DefaultValue]?.expression?.get(sourceSet)?.let {
        KdDefaultValueExpression.Defined(it.toKdExpression())
    }
    return KdValueParameter(
        name = name.orEmpty(),
        type = type.toKdType(sourceSet),
        isNoinline = ExtraModifiers.KotlinOnlyModifiers.NoInline in mods,
        isCrossinline = ExtraModifiers.KotlinOnlyModifiers.CrossInline in mods,
        isVararg = ExtraModifiers.KotlinOnlyModifiers.VarArg in mods,
        defaultValueExpression = defaultValueExpression,
        annotations = extra.annotationsIn(sourceSet),
        documentation = KdDocumentation.EMPTY,
    )
}

private fun DParameter.toKdExtensionReceiverParameter(sourceSet: DokkaConfiguration.DokkaSourceSet): KdExtensionReceiverParameter =
    KdExtensionReceiverParameter(
        type = type.toKdType(sourceSet),
        annotations = extra.annotationsIn(sourceSet),
        documentation = KdDocumentation.EMPTY,
    )

private fun DParameter.toKdContextParameter(sourceSet: DokkaConfiguration.DokkaSourceSet): KdContextParameter =
    KdContextParameter(
        name = name,
        type = type.toKdType(sourceSet),
        annotations = extra.annotationsIn(sourceSet),
        documentation = KdDocumentation.EMPTY,
    )

private fun Expression.toKdExpression(): KdExpression = KdExpression(
    text = when (this) {
        is ComplexExpression -> value
        is IntegerConstant -> value.toString()
        is StringConstant -> "\"$value\""
        is DoubleConstant -> value.toString()
        is FloatConstant -> "${value}f"
        is BooleanConstant -> value.toString()
        else -> ""
    },
    links = emptyList(),
)

// endregion

// region Visibility / modality

internal fun Visibility.toKdVisibility(): KdVisibility = when (this) {
    is KotlinVisibility.Public -> KdVisibility.PUBLIC
    is KotlinVisibility.Private -> KdVisibility.PRIVATE
    is KotlinVisibility.Protected -> KdVisibility.PROTECTED
    is KotlinVisibility.Internal -> KdVisibility.INTERNAL
    is JavaVisibility.Public -> KdVisibility.PUBLIC
    is JavaVisibility.Private -> KdVisibility.PRIVATE
    // Java's `protected` is visible to the whole package too, unlike Kotlin's `protected`.
    is JavaVisibility.Protected -> KdVisibility.PACKAGE_PROTECTED
    is JavaVisibility.Default -> KdVisibility.PACKAGE_PRIVATE
}

internal fun Modifier.toKdModality(): KdModality = when (this) {
    is KotlinModifier.Abstract -> KdModality.ABSTRACT
    is KotlinModifier.Open -> KdModality.OPEN
    is KotlinModifier.Sealed -> KdModality.SEALED
    is KotlinModifier.Final -> KdModality.FINAL
    is KotlinModifier.Empty -> KdModality.FINAL
    is JavaModifier.Abstract -> KdModality.ABSTRACT
    is JavaModifier.Final -> KdModality.FINAL
    // A Java method/class without `final`/`abstract` is overridable/extensible, same as Kotlin's `open`.
    is JavaModifier.Empty -> KdModality.OPEN
}

// endregion
