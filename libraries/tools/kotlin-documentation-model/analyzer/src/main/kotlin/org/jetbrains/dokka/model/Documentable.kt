/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

import org.jetbrains.dokka.ExperimentalDokkaApi
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.kotlin.documentation.analyzer.ids.MODULE_DECLARATION_ID
import org.jetbrains.kotlin.documentation.KdElementId
import org.jetbrains.kotlin.documentation.analyzer.ids.declarationIdToPackageName

internal interface AnnotationTarget

internal abstract class Documentable : WithChildren<Documentable>,
    AnnotationTarget {
    abstract val name: String?
    abstract val declarationId: KdElementId
    abstract val documentation: SourceSetDependent<DocumentationNode>
    abstract val sourceSets: Set<DokkaSourceSet>
    abstract val expectPresentInSet: DokkaSourceSet?
    abstract override val children: List<Documentable>

    override fun toString(): String =
        "${javaClass.simpleName}($declarationId)"

    override fun equals(other: Any?): Boolean =
        other is Documentable && this.declarationId == other.declarationId // TODO: https://github.com/Kotlin/dokka/pull/667#discussion_r382555806

    override fun hashCode(): Int = declarationId.hashCode()
}

internal typealias SourceSetDependent<T> = Map<DokkaSourceSet, T>

internal interface WithSources {
    val sources: SourceSetDependent<DocumentableSource>
}

internal interface WithScope {
    val functions: List<DFunction>
    val properties: List<DProperty>
    val classlikes: List<DClasslike>
}

internal interface WithVisibility {
    val visibility: SourceSetDependent<Visibility>
}

internal interface WithType {
    val type: Bound
}

internal interface WithAbstraction {
    val modifier: SourceSetDependent<Modifier>
}

internal sealed class Modifier(
    val name: String
)

internal sealed class KotlinModifier(name: String) : Modifier(name) {
    object Abstract : KotlinModifier("abstract")
    object Open : KotlinModifier("open")
    object Final : KotlinModifier("final")
    object Sealed : KotlinModifier("sealed")
    object Empty : KotlinModifier("")
}

internal sealed class JavaModifier(name: String) : Modifier(name) {
    object Abstract : JavaModifier("abstract")
    object Final : JavaModifier("final")
    object Empty : JavaModifier("")
}

internal interface WithCompanion {
    val companion: DObject?
}

internal interface WithConstructors {
    val constructors: List<DFunction>
}

internal interface WithGenerics {
    val generics: List<DTypeParameter>
}

@ExperimentalDokkaApi
internal interface WithContextParameters {
    val contextParameters: List<DParameter>
}

internal interface WithTypealiases {
    val typealiases: List<DTypeAlias>
}

internal interface WithSupertypes {
    val supertypes: SourceSetDependent<List<TypeConstructorWithKind>>
}

internal interface WithIsExpectActual {
    val isExpectActual: Boolean
}

@OptIn(ExperimentalDokkaApi::class)
internal interface Callable : WithVisibility, WithContextParameters, WithType, WithAbstraction, WithSources, WithIsExpectActual {
    val receiver: DParameter?
}

internal sealed class DClasslike : Documentable(), WithScope, WithVisibility, WithSources, WithIsExpectActual

internal data class DModule(
    override val name: String,
    val packages: List<DPackage>,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet? = null,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DModule> = PropertyContainer.empty()
) : Documentable(), WithExtraProperties<DModule> {
    override val declarationId: KdElementId = MODULE_DECLARATION_ID
    override val children: List<Documentable>
        get() = packages

    override fun withNewExtras(newExtras: PropertyContainer<DModule>): DModule = copy(extra = newExtras)
}

internal data class DPackage(
    override val declarationId: KdElementId,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val typealiases: List<DTypeAlias>,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet? = null,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DPackage> = PropertyContainer.empty()
) : Documentable(), WithScope, WithTypealiases, WithExtraProperties<DPackage> {

    val packageName: String = declarationIdToPackageName(declarationId)

    /**
     * !!! WARNING !!!
     * This name is not guaranteed to be a be a canonical/real package name.
     * e.g. this will return a human readable version for root packages.
     * Use [packageName] instead to obtain the real packageName
     */
    override val name: String = packageName.ifBlank { "[root]" }

    override val children: List<Documentable> = properties + functions + classlikes + typealiases

    override fun withNewExtras(newExtras: PropertyContainer<DPackage>): DPackage = copy(extra = newExtras)
}

internal data class DClass(
    override val declarationId: KdElementId,
    override val name: String,
    override val constructors: List<DFunction>,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val visibility: SourceSetDependent<Visibility>,
    override val companion: DObject?,
    override val generics: List<DTypeParameter>,
    override val supertypes: SourceSetDependent<List<TypeConstructorWithKind>>,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val modifier: SourceSetDependent<Modifier>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val isExpectActual: Boolean,
    override val extra: PropertyContainer<DClass> = PropertyContainer.empty(),
    override val typealiases: List<DTypeAlias> = emptyList(),
) : DClasslike(), WithAbstraction, WithCompanion, WithConstructors, WithGenerics, WithSupertypes,
    WithExtraProperties<DClass>, WithTypealiases {

    override val children: List<Documentable>
        get() = (functions + properties + classlikes + constructors + typealiases)

    override fun withNewExtras(newExtras: PropertyContainer<DClass>): DClass = copy(extra = newExtras)
}

internal data class DEnum(
    override val declarationId: KdElementId,
    override val name: String,
    val entries: List<DEnumEntry>,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val visibility: SourceSetDependent<Visibility>,
    override val companion: DObject?,
    override val constructors: List<DFunction>,
    override val supertypes: SourceSetDependent<List<TypeConstructorWithKind>>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val isExpectActual: Boolean,
    override val extra: PropertyContainer<DEnum> = PropertyContainer.empty(),
    override val typealiases: List<DTypeAlias> = emptyList(),
) : DClasslike(), WithCompanion, WithConstructors, WithSupertypes, WithExtraProperties<DEnum>, WithTypealiases {
    override val children: List<Documentable>
        get() = (entries + functions + properties + classlikes + constructors + typealiases)

    override fun withNewExtras(newExtras: PropertyContainer<DEnum>): DEnum = copy(extra = newExtras)
}

internal data class DEnumEntry(
    override val declarationId: KdElementId,
    override val name: String,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DEnumEntry> = PropertyContainer.empty(),
) : Documentable(), WithScope, WithExtraProperties<DEnumEntry> {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes)

    override fun withNewExtras(newExtras: PropertyContainer<DEnumEntry>): DEnumEntry = copy(extra = newExtras)
}

internal data class DFunction(
    override val declarationId: KdElementId,
    override val name: String,
    val isConstructor: Boolean,
    val parameters: List<DParameter>,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val visibility: SourceSetDependent<Visibility>,
    override val type: Bound,
    override val generics: List<DTypeParameter>,
    override val receiver: DParameter?,
    override val modifier: SourceSetDependent<Modifier>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val isExpectActual: Boolean,
    override val extra: PropertyContainer<DFunction> = PropertyContainer.empty(),
    @property:ExperimentalDokkaApi
    override val contextParameters: List<DParameter> = emptyList(),
) : Documentable(), Callable, WithGenerics, WithExtraProperties<DFunction> {

    override val children: List<Documentable>
        get() = parameters + @OptIn(ExperimentalDokkaApi::class) contextParameters

    override fun withNewExtras(newExtras: PropertyContainer<DFunction>): DFunction = copy(extra = newExtras)
}

internal data class DInterface(
    override val declarationId: KdElementId,
    override val name: String,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val visibility: SourceSetDependent<Visibility>,
    override val companion: DObject?,
    override val generics: List<DTypeParameter>,
    override val supertypes: SourceSetDependent<List<TypeConstructorWithKind>>,
    override val modifier: SourceSetDependent<Modifier>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val isExpectActual: Boolean,
    override val extra: PropertyContainer<DInterface> = PropertyContainer.empty(),
    override val typealiases: List<DTypeAlias> = emptyList(),
) : DClasslike(), WithAbstraction, WithCompanion, WithGenerics, WithSupertypes, WithExtraProperties<DInterface>,
    WithTypealiases {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes + typealiases)

    override fun withNewExtras(newExtras: PropertyContainer<DInterface>): DInterface = copy(extra = newExtras)
}

internal data class DObject(
    override val name: String?,
    override val declarationId: KdElementId,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val visibility: SourceSetDependent<Visibility>,
    override val supertypes: SourceSetDependent<List<TypeConstructorWithKind>>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val isExpectActual: Boolean,
    override val extra: PropertyContainer<DObject> = PropertyContainer.empty(),
    override val typealiases: List<DTypeAlias> = emptyList(),
) : DClasslike(), WithSupertypes, WithExtraProperties<DObject>, WithTypealiases {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes + typealiases)

    override fun withNewExtras(newExtras: PropertyContainer<DObject>): DObject = copy(extra = newExtras)
}

internal data class DAnnotation(
    override val name: String,
    override val declarationId: KdElementId,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val visibility: SourceSetDependent<Visibility>,
    override val companion: DObject?,
    override val constructors: List<DFunction>,
    override val generics: List<DTypeParameter>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val isExpectActual: Boolean,
    override val extra: PropertyContainer<DAnnotation> = PropertyContainer.empty()
) : DClasslike(), WithCompanion, WithConstructors, WithExtraProperties<DAnnotation>, WithGenerics {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes + constructors)

    override fun withNewExtras(newExtras: PropertyContainer<DAnnotation>): DAnnotation = copy(extra = newExtras)
}

internal data class DProperty(
    override val declarationId: KdElementId,
    override val name: String,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val visibility: SourceSetDependent<Visibility>,
    override val type: Bound,
    override val receiver: DParameter?,
    val setter: DFunction?,
    val getter: DFunction?,
    override val modifier: SourceSetDependent<Modifier>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val generics: List<DTypeParameter>,
    override val isExpectActual: Boolean,
    override val extra: PropertyContainer<DProperty> = PropertyContainer.empty(),
    @property:ExperimentalDokkaApi
    override val contextParameters: List<DParameter> = emptyList()
) : Documentable(), Callable, WithExtraProperties<DProperty>, WithGenerics {

    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<DProperty>): DProperty = copy(extra = newExtras)
}

// TODO: treat named Parameters and receivers differently
internal data class DParameter(
    override val declarationId: KdElementId,
    override val name: String?,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val type: Bound,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DParameter> = PropertyContainer.empty()
) : Documentable(), WithExtraProperties<DParameter>, WithType {
    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<DParameter>): DParameter = copy(extra = newExtras)
}

internal data class DTypeParameter(
    override val declarationId: KdElementId,
    val variantTypeParameter: Variance<TypeParameter>,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    val bounds: List<Bound>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DTypeParameter> = PropertyContainer.empty()
) : Documentable(), WithExtraProperties<DTypeParameter> {

    override val name: String by variantTypeParameter.inner::name

    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<DTypeParameter>): DTypeParameter = copy(extra = newExtras)
}

internal data class DTypeAlias(
    override val declarationId: KdElementId,
    override val name: String,
    override val type: Bound,
    val underlyingType: SourceSetDependent<Bound>,
    override val visibility: SourceSetDependent<Visibility>,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val sourceSets: Set<DokkaSourceSet>,
    override val generics: List<DTypeParameter>,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val extra: PropertyContainer<DTypeAlias> = PropertyContainer.empty()
) : Documentable(), WithType, WithVisibility, WithExtraProperties<DTypeAlias>, WithGenerics, WithSources {
    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<DTypeAlias>): DTypeAlias = copy(extra = newExtras)
}

internal sealed class Projection
internal sealed class Bound : Projection()
internal data class TypeParameter(
    /** The declaration ID of the declaration that declares this type parameter. */
    val containerDeclarationId: KdElementId,
    val name: String,
    val presentableName: String? = null,
    override val extra: PropertyContainer<TypeParameter> = PropertyContainer.empty()
) : Bound(), AnnotationTarget, WithExtraProperties<TypeParameter> {
    override fun withNewExtras(newExtras: PropertyContainer<TypeParameter>): TypeParameter =
        copy(extra = extra)
}

internal sealed class TypeConstructor : Bound(), AnnotationTarget {
    /** The classifier declaration ID of the type (see `generateDeclarationId`). */
    abstract val declarationId: KdElementId
    abstract val projections: List<Projection>
    abstract val presentableName: String?
}

internal data class GenericTypeConstructor(
    override val declarationId: KdElementId,
    override val projections: List<Projection>,
    override val presentableName: String? = null,
    override val extra: PropertyContainer<GenericTypeConstructor> = PropertyContainer.empty()
) : TypeConstructor(), WithExtraProperties<GenericTypeConstructor> {
    override fun withNewExtras(newExtras: PropertyContainer<GenericTypeConstructor>): GenericTypeConstructor =
        copy(extra = newExtras)
}

internal data class FunctionalTypeConstructor(
    override val declarationId: KdElementId,
    override val projections: List<Projection>,
    val isExtensionFunction: Boolean = false,
    val isSuspendable: Boolean = false,
    override val presentableName: String? = null,
    override val extra: PropertyContainer<FunctionalTypeConstructor> = PropertyContainer.empty(),
    @property:ExperimentalDokkaApi
    val contextParametersCount: Int = 0
) : TypeConstructor(), WithExtraProperties<FunctionalTypeConstructor> {

    override fun withNewExtras(newExtras: PropertyContainer<FunctionalTypeConstructor>): FunctionalTypeConstructor =
        copy(extra = newExtras)
}

// kotlin.annotation.AnnotationTarget.TYPEALIAS
internal data class TypeAliased(
    val typeAlias: Bound,
    val inner: Bound,
    override val extra: PropertyContainer<TypeAliased> = PropertyContainer.empty()
) : Bound(), AnnotationTarget, WithExtraProperties<TypeAliased> {
    override fun withNewExtras(newExtras: PropertyContainer<TypeAliased>): TypeAliased =
        copy(extra = newExtras)
}

internal data class PrimitiveJavaType(
    val name: String,
    override val extra: PropertyContainer<PrimitiveJavaType> = PropertyContainer.empty()
) : Bound(), AnnotationTarget, WithExtraProperties<PrimitiveJavaType> {
    override fun withNewExtras(newExtras: PropertyContainer<PrimitiveJavaType>): PrimitiveJavaType =
        copy(extra = newExtras)
}

internal data class JavaObject(override val extra: PropertyContainer<JavaObject> = PropertyContainer.empty()) :
    Bound(), AnnotationTarget, WithExtraProperties<JavaObject> {
    override fun withNewExtras(newExtras: PropertyContainer<JavaObject>): JavaObject =
        copy(extra = newExtras)
}

internal data class UnresolvedBound(
    val name: String,
    override val extra: PropertyContainer<UnresolvedBound> = PropertyContainer.empty()
) : Bound(), AnnotationTarget, WithExtraProperties<UnresolvedBound> {
    override fun withNewExtras(newExtras: PropertyContainer<UnresolvedBound>): UnresolvedBound =
        copy(extra = newExtras)
}

// The following Projections are not AnnotationTargets; they cannot be annotated.
internal data class Nullable(val inner: Bound) : Bound()

/**
 * It introduces [definitely non-nullable types](https://github.com/Kotlin/KEEP/blob/c72601cf35c1e95a541bb4b230edb474a6d1d1a8/proposals/definitely-non-nullable-types.md)
 */
internal data class DefinitelyNonNullable(val inner: Bound) : Bound()

internal sealed class Variance<out T : Bound> : Projection() {
    abstract val inner: T
}

internal data class Covariance<out T : Bound>(override val inner: T) : Variance<T>() {
    override fun toString(): String = "out"
}

internal data class Contravariance<out T : Bound>(override val inner: T) : Variance<T>() {
    override fun toString(): String = "in"
}

internal data class Invariance<out T : Bound>(override val inner: T) : Variance<T>() {
    override fun toString(): String = ""
}

internal object Star : Projection()

internal object Void : Bound()
internal object Dynamic : Bound()

internal fun Documentable.dfs(predicate: (Documentable) -> Boolean): Documentable? =
    if (predicate(this)) {
        this
    } else {
        this.children.asSequence().mapNotNull { it.dfs(predicate) }.firstOrNull()
    }

internal sealed class Visibility(val name: String)

internal sealed class KotlinVisibility(name: String) : Visibility(name) {
    object Public : KotlinVisibility("public")
    object Private : KotlinVisibility("private")
    object Protected : KotlinVisibility("protected")
    object Internal : KotlinVisibility("internal")
}

internal sealed class JavaVisibility(name: String) : Visibility(name) {
    object Public : JavaVisibility("public")
    object Private : JavaVisibility("private")
    object Protected : JavaVisibility("protected")
    object Default : JavaVisibility("")
}

internal fun <T> SourceSetDependent<T>?.orEmpty(): SourceSetDependent<T> = this ?: emptyMap()

internal interface DocumentableSource {
    val path: String

    /**
     * Computes the first line number of the documentable's declaration/signature/identifier.
     *
     * Numbering is always 1-based.
     *
     * May return null if the sources could not be found - for example, for synthetic/generated declarations.
     */
    fun computeLineNumber(): Int?
}

internal data class TypeConstructorWithKind(val typeConstructor: TypeConstructor, val kind: ClassKind)
