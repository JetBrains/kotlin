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
    ss: DokkaSourceSet,
): Annotations.Annotation? = this[Annotations]?.directAnnotations?.get(ss)?.firstOrNull {
    it.dri.packageName == packageName && it.dri.classNames == className
}

private fun Annotations.Annotation.stringArgument(name: String): String? =
    (params[name] as? StringValue)?.value?.takeIf { it.isNotEmpty() }

private fun <C : DokkaAnnotationTarget> PropertyContainer<C>.sinceIn(
    documentable: Documentable,
    ss: DokkaSourceSet,
): String? = annotationIn("kotlin", "SinceKotlin", ss)?.stringArgument("version")
    ?: annotationIn("kotlin", "IntroducedAt", ss)?.stringArgument("version")
    ?: documentable.documentation[ss]?.children?.filterIsInstance<Since>()?.firstOrNull()
        ?.root?.plainText()?.trim()?.takeIf { it.isNotEmpty() }

private fun DocTag.plainText(): String = when (this) {
    is Text -> body
    else -> children.joinToString("") { it.plainText() }
}

private fun <C : DokkaAnnotationTarget> PropertyContainer<C>.deprecatedIn(
    documentable: Documentable,
    ss: DokkaSourceSet,
): KdDeprecated? {
    val kotlin = annotationIn("kotlin", "Deprecated", ss)
    val java = annotationIn("java.lang", "Deprecated", ss)
    val tag = documentable.documentation[ss]?.children?.any { it is DeprecatedTag } == true
    if (kotlin == null && java == null && !tag) return null

    val level = when ((kotlin?.params?.get("level") as? EnumValue)?.enumName?.substringAfterLast('.')) {
        "ERROR" -> KdDeprecationLevel.ERROR
        "HIDDEN" -> error("Hidden declaration should have been filtered out: ${documentable.dri}")
        else -> KdDeprecationLevel.WARNING
    }
    val sinceKotlin = annotationIn("kotlin", "DeprecatedSinceKotlin", ss)
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
    ss: DokkaSourceSet,
): List<KdThrows> where C : Documentable, C : DokkaAnnotationTarget {
    val declared = this[CheckedExceptions]?.exceptions?.get(ss).orEmpty()
    val annotation = annotationIn("kotlin", "Throws", ss) ?: annotationIn("kotlin.jvm", "Throws", ss)
    val annotated = (annotation?.params?.get("exceptionClasses") as? ArrayValue)?.value.orEmpty()
        .filterIsInstance<ClassValue>().map { it.classDRI }
    val documented = documentable.documentation[ss]?.children.orEmpty().filterIsInstance<Throws>()
        .mapNotNull { it.exceptionAddress }
    // Documentation is a placeholder until KdDocumentation can represent tag contents.
    return (declared + annotated + documented).map { it.toKdElementId() }.distinct().map { KdThrows(it) }
}

internal fun buildFunctionElement(f: DFunction, ss: DokkaConfiguration.DokkaSourceSet): KdFunction {
    val mods = f.extra.extraModifiersIn(ss)
    return KdFunction(
        name = f.name,
        isCompanion = f.extra[IsCompanion] != null,
        isExternal = ExtraModifiers.KotlinOnlyModifiers.External in mods || ExtraModifiers.JavaOnlyModifiers.Native in mods,
        isSuspend = ExtraModifiers.KotlinOnlyModifiers.Suspend in mods,
        isOperator = ExtraModifiers.KotlinOnlyModifiers.Operator in mods,
        isInfix = ExtraModifiers.KotlinOnlyModifiers.Infix in mods,
        isInline = ExtraModifiers.KotlinOnlyModifiers.Inline in mods,
        isTailRec = ExtraModifiers.KotlinOnlyModifiers.TailRec in mods,
        mappedTo = null, // TODO KT-90419
        returnType = f.type.toKdType(ss),
        typeParameters = f.generics.map { it.toKdTypeParameter(ss) },
        valueParameters = f.parameters.map { it.toKdValueParameter(ss) },
        extensionReceiverParameter = f.receiver?.toKdExtensionReceiverParameter(ss),
        contextParameters = f.contextParameters.map { it.toKdContextParameter(ss) },
        overridens = f.extra[OverriddenMembers]?.members?.get(ss).orEmpty().map { it.toKdElementId() }.distinct(),
        visibility = f.visibility[ss]?.toKdVisibility() ?: KdVisibility.PUBLIC,
        modality = f.modifier[ss]?.toKdModality() ?: KdModality.FINAL,
        since = f.extra.sinceIn(f, ss),
        deprecated = f.extra.deprecatedIn(f, ss),
        throws = f.extra.throwsIn(f, ss),
        annotations = f.extra.annotationsIn(ss),
        documentation = KdDocumentation.EMPTY,
    )
}

internal fun buildConstructorElement(c: DFunction, ss: DokkaConfiguration.DokkaSourceSet): KdConstructor {
    val mods = c.extra.extraModifiersIn(ss)
    return KdConstructor(
        isPrimary = c.extra[PrimaryConstructorExtra] != null,
        isExternal = ExtraModifiers.KotlinOnlyModifiers.External in mods || ExtraModifiers.JavaOnlyModifiers.Native in mods,
        mappedTo = null, // TODO KT-90419
        typeParameters = c.generics.map { it.toKdTypeParameter(ss) },
        valueParameters = c.parameters.map { it.toKdValueParameter(ss) },
        visibility = c.visibility[ss]?.toKdVisibility() ?: KdVisibility.PUBLIC,
        since = c.extra.sinceIn(c, ss),
        deprecated = c.extra.deprecatedIn(c, ss),
        throws = c.extra.throwsIn(c, ss),
        annotations = c.extra.annotationsIn(ss),
        documentation = KdDocumentation.EMPTY,
    )
}

internal fun buildPropertyElement(
    p: DProperty,
    ss: DokkaConfiguration.DokkaSourceSet,
    getterId: KdElementId?,
    setterId: KdElementId?,
): KdProperty {
    val mods = p.extra.extraModifiersIn(ss)
    return KdProperty(
        name = p.name,
        isInline = ExtraModifiers.KotlinOnlyModifiers.Inline in mods ||
                p.getter?.extra?.extraModifiersIn(ss)?.contains(ExtraModifiers.KotlinOnlyModifiers.Inline) == true &&
                (p.setter == null || p.setter.extra.extraModifiersIn(ss).contains(ExtraModifiers.KotlinOnlyModifiers.Inline)),
        isCompanion = p.extra[IsCompanion] != null,
        isExternal = ExtraModifiers.KotlinOnlyModifiers.External in mods || ExtraModifiers.JavaOnlyModifiers.Native in mods,
        isSuspend = ExtraModifiers.KotlinOnlyModifiers.Suspend in mods, // TODO AA does not support `suspend` for properties
        isMutable = p.extra[IsVar] != null,
        isFromPrimaryConstructor = p.extra[IsAlsoParameter]?.inSourceSets.orEmpty().any { it.sourceSetID == ss.sourceSetID },
        isLateinit = ExtraModifiers.KotlinOnlyModifiers.LateInit in mods,
        constExpression = if (ExtraModifiers.KotlinOnlyModifiers.Const in mods) {
            p.extra[DefaultValue]?.expression?.get(ss)?.let { expression ->
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
        type = p.type.toKdType(ss),
        typeParameters = p.generics.map { it.toKdTypeParameter(ss) },
        extensionReceiverParameter = p.receiver?.toKdExtensionReceiverParameter(ss),
        contextParameters = p.contextParameters.map { it.toKdContextParameter(ss) },
        overridens = p.extra[OverriddenMembers]?.members?.get(ss).orEmpty().map { it.toKdElementId() }.distinct(),
        visibility = p.visibility[ss]?.toKdVisibility() ?: KdVisibility.PUBLIC,
        modality = p.modifier[ss]?.toKdModality() ?: KdModality.FINAL,
        since = p.extra.sinceIn(p, ss),
        deprecated = p.extra.deprecatedIn(p, ss),
        throws = p.extra.throwsIn(p, ss),
        annotations = p.extra.annotationsIn(ss),
        documentation = KdDocumentation.EMPTY,
    )
}

internal fun buildTypealiasElement(t: DTypeAlias, ss: DokkaConfiguration.DokkaSourceSet): KdTypealias =
    KdTypealias(
        name = t.name,
        typeParameters = t.generics.map { it.toKdTypeParameter(ss) },
        underlyingType = requireNotNull(t.underlyingType[ss]) { "Missing underlying type for ${t.dri} in $ss" }.toKdType(ss),
        declarations = emptyList(), // TODO on Dokka's side
        visibility = t.visibility[ss]?.toKdVisibility() ?: KdVisibility.PUBLIC,
        since = t.extra.sinceIn(t, ss),
        deprecated = t.extra.deprecatedIn(t, ss),
        annotations = t.extra.annotationsIn(ss),
        documentation = KdDocumentation.EMPTY,
    )

internal fun buildEnumEntryElement(e: DEnumEntry, ss: DokkaConfiguration.DokkaSourceSet): KdProperty =
    KdProperty(
        name = e.name,
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
            qualifiers = listOf(KdClassTypeQualifier(e.dri.parent.classNames.orEmpty().substringAfterLast('.'), emptyList())),
            classLikeId = e.dri.parent.toKdElementId(),
            isMarkedNullable = false,
            annotations = emptyList(),
        ),
        typeParameters = emptyList(),
        extensionReceiverParameter = null,
        contextParameters = emptyList(),
        overridens = emptyList(),
        visibility = KdVisibility.PUBLIC,
        modality = KdModality.FINAL,
        since = e.extra.sinceIn(e, ss),
        deprecated = e.extra.deprecatedIn(e, ss),
        throws = e.extra.throwsIn(e, ss),
        annotations = e.extra.annotationsIn(ss),
        documentation = KdDocumentation.EMPTY,
    )

internal fun buildClassElement(
    node: DClasslike,
    ss: DokkaConfiguration.DokkaSourceSet,
    declarations: List<KdElementId>,
    isCompanion: Boolean,
): KdClass {
    val mods = node.extra.extraModifiersIn(ss)
    val annotations = node.extra.annotationsIn(ss)
    val since = node.extra.sinceIn(node, ss)
    val deprecated = node.extra.deprecatedIn(node, ss)
    val classKind = when (node) {
        is DClass -> KdClassKind.CLASS
        is DInterface -> KdClassKind.INTERFACE
        is DObject -> KdClassKind.OBJECT
        is DEnum -> KdClassKind.ENUM_CLASS
        is DAnnotation -> KdClassKind.ANNOTATION_CLASS
    }
    val modality = (node as? WithAbstraction)?.modifier?.get(ss)?.toKdModality() ?: KdModality.FINAL
    val generics = (node as? WithGenerics)?.generics.orEmpty()
    val superTypes = (node as? WithSupertypes)?.supertypes?.get(ss).orEmpty()
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
        typeParameters = generics.map { it.toKdTypeParameter(ss) },
        superTypes = superTypes.map { it.typeConstructor.toKdType(ss) },
        declarations = declarations,
        visibility = node.visibility[ss]?.toKdVisibility() ?: KdVisibility.PUBLIC,
        modality = modality,
        since = since,
        deprecated = deprecated,
        annotations = annotations,
        documentation = KdDocumentation.EMPTY,
    )
}

// endregion

// region Parameters / type parameters

private fun DTypeParameter.toKdTypeParameter(ss: DokkaConfiguration.DokkaSourceSet): KdTypeParameter {
    val variance = when (variantTypeParameter) {
        is Covariance -> KdTypeVariance.OUT
        is Contravariance -> KdTypeVariance.IN
        is Invariance -> null
    }
    return KdTypeParameter(
        name = name,
        isReified = extra.extraModifiersIn(ss).let { ExtraModifiers.KotlinOnlyModifiers.Reified in it },
        upperBounds = bounds.map { it.toKdType(ss) },
        variance = variance,
        annotations = extra.annotationsIn(ss),
        documentation = KdDocumentation.EMPTY,
    )
}

private fun DParameter.toKdValueParameter(ss: DokkaConfiguration.DokkaSourceSet): KdValueParameter {
    val mods = extra.extraModifiersIn(ss)
    val defaultValueExpression = extra[DefaultValue]?.expression?.get(ss)?.let {
        KdDefaultValueExpression.Defined(it.toKdExpression())
    }
    return KdValueParameter(
        name = name.orEmpty(),
        type = type.toKdType(ss),
        isNoinline = ExtraModifiers.KotlinOnlyModifiers.NoInline in mods,
        isCrossinline = ExtraModifiers.KotlinOnlyModifiers.CrossInline in mods,
        isVararg = ExtraModifiers.KotlinOnlyModifiers.VarArg in mods,
        defaultValueExpression = defaultValueExpression,
        annotations = extra.annotationsIn(ss),
        documentation = KdDocumentation.EMPTY,
    )
}

private fun DParameter.toKdExtensionReceiverParameter(ss: DokkaConfiguration.DokkaSourceSet): KdExtensionReceiverParameter =
    KdExtensionReceiverParameter(
        type = type.toKdType(ss),
        annotations = extra.annotationsIn(ss),
        documentation = KdDocumentation.EMPTY,
    )

private fun DParameter.toKdContextParameter(ss: DokkaConfiguration.DokkaSourceSet): KdContextParameter =
    KdContextParameter(
        name = name,
        type = type.toKdType(ss),
        annotations = extra.annotationsIn(ss),
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
