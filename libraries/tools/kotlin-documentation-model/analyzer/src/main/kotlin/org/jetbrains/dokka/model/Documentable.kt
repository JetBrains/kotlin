/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

import org.jetbrains.dokka.ExperimentalDokkaApi
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties

internal interface AnnotationTarget

internal abstract class Documentable : WithChildren<Documentable>,
    AnnotationTarget {
    abstract val name: String?
    abstract val dri: DRI
    abstract val documentation: SourceSetDependent<DocumentationNode>
    abstract val sourceSets: Set<DokkaSourceSet>
    abstract val expectPresentInSet: DokkaSourceSet?
    abstract override val children: List<Documentable>

    override fun toString(): String =
        "${javaClass.simpleName}($dri)"

    override fun equals(other: Any?): Boolean =
        other is Documentable && this.dri == other.dri // TODO: https://github.com/Kotlin/dokka/pull/667#discussion_r382555806

    override fun hashCode(): Int = dri.hashCode()
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
    override val dri: DRI = DRI.topLevel
    override val children: List<Documentable>
        get() = packages

    override fun withNewExtras(newExtras: PropertyContainer<DModule>): DModule = copy(extra = newExtras)
}

internal data class DPackage(
    override val dri: DRI,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val typealiases: List<DTypeAlias>,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet? = null,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DPackage> = PropertyContainer.empty()
) : Documentable(), WithScope, WithTypealiases, WithExtraProperties<DPackage> {

    val packageName: String = dri.packageName.orEmpty()

    /**
     * !!! WARNING !!!
     * This name is not guaranteed to be a be a canonical/real package name.
     * e.g. this will return a human readable version for root packages.
     * Use [packageName] or `dri.packageName` instead to obtain the real packageName
     */
    override val name: String = packageName.ifBlank { "[root]" }

    override val children: List<Documentable> = properties + functions + classlikes + typealiases

    override fun withNewExtras(newExtras: PropertyContainer<DPackage>): DPackage = copy(extra = newExtras)
}

internal data class DClass(
    override val dri: DRI,
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

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    constructor(
        dri: DRI,
        name: String,
        constructors: List<DFunction>,
        functions: List<DFunction>,
        properties: List<DProperty>,
        classlikes: List<DClasslike>,
        sources: SourceSetDependent<DocumentableSource>,
        visibility: SourceSetDependent<Visibility>,
        companion: DObject?,
        generics: List<DTypeParameter>,
        supertypes: SourceSetDependent<List<TypeConstructorWithKind>>,
        documentation: SourceSetDependent<DocumentationNode>,
        expectPresentInSet: DokkaSourceSet?,
        modifier: SourceSetDependent<Modifier>,
        sourceSets: Set<DokkaSourceSet>,
        isExpectActual: Boolean,
        extra: PropertyContainer<DClass> = PropertyContainer.empty(),
    ) : this(
        dri = dri,
        name = name,
        constructors = constructors,
        functions = functions,
        properties = properties,
        classlikes = classlikes,
        sources = sources,
        visibility = visibility,
        companion = companion,
        generics = generics,
        supertypes = supertypes,
        documentation = documentation,
        expectPresentInSet = expectPresentInSet,
        modifier = modifier,
        sourceSets = sourceSets,
        isExpectActual = isExpectActual,
        extra = extra,
        typealiases = emptyList(),
    )

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    fun copy(
        dri: DRI,
        name: String,
        constructors: List<DFunction>,
        functions: List<DFunction>,
        properties: List<DProperty>,
        classlikes: List<DClasslike>,
        sources: SourceSetDependent<DocumentableSource>,
        visibility: SourceSetDependent<Visibility>,
        companion: DObject?,
        generics: List<DTypeParameter>,
        supertypes: SourceSetDependent<List<TypeConstructorWithKind>>,
        documentation: SourceSetDependent<DocumentationNode>,
        expectPresentInSet: DokkaSourceSet?,
        modifier: SourceSetDependent<Modifier>,
        sourceSets: Set<DokkaSourceSet>,
        isExpectActual: Boolean,
        extra: PropertyContainer<DClass> = PropertyContainer.empty(),
    ): DClass = DClass(
        dri = dri,
        name = name,
        constructors = constructors,
        functions = functions,
        properties = properties,
        classlikes = classlikes,
        sources = sources,
        visibility = visibility,
        companion = companion,
        generics = generics,
        supertypes = supertypes,
        documentation = documentation,
        expectPresentInSet = expectPresentInSet,
        modifier = modifier,
        sourceSets = sourceSets,
        isExpectActual = isExpectActual,
        extra = extra,
        typealiases = emptyList(),
    )

    override val children: List<Documentable>
        get() = (functions + properties + classlikes + constructors + typealiases)

    override fun withNewExtras(newExtras: PropertyContainer<DClass>): DClass = copy(extra = newExtras)
}

internal data class DEnum(
    override val dri: DRI,
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

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    constructor(
        dri: DRI,
        name: String,
        entries: List<DEnumEntry>,
        documentation: SourceSetDependent<DocumentationNode>,
        expectPresentInSet: DokkaSourceSet?,
        sources: SourceSetDependent<DocumentableSource>,
        functions: List<DFunction>,
        properties: List<DProperty>,
        classlikes: List<DClasslike>,
        visibility: SourceSetDependent<Visibility>,
        companion: DObject?,
        constructors: List<DFunction>,
        supertypes: SourceSetDependent<List<TypeConstructorWithKind>>,
        sourceSets: Set<DokkaSourceSet>,
        isExpectActual: Boolean,
        extra: PropertyContainer<DEnum> = PropertyContainer.empty(),
    ) : this(
        dri = dri,
        name = name,
        entries = entries,
        documentation = documentation,
        expectPresentInSet = expectPresentInSet,
        sources = sources,
        functions = functions,
        properties = properties,
        classlikes = classlikes,
        visibility = visibility,
        companion = companion,
        constructors = constructors,
        supertypes = supertypes,
        sourceSets = sourceSets,
        isExpectActual = isExpectActual,
        extra = extra,
        typealiases = emptyList(),
    )

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    fun copy(
        dri: DRI,
        name: String,
        entries: List<DEnumEntry>,
        documentation: SourceSetDependent<DocumentationNode>,
        expectPresentInSet: DokkaSourceSet?,
        sources: SourceSetDependent<DocumentableSource>,
        functions: List<DFunction>,
        properties: List<DProperty>,
        classlikes: List<DClasslike>,
        visibility: SourceSetDependent<Visibility>,
        companion: DObject?,
        constructors: List<DFunction>,
        supertypes: SourceSetDependent<List<TypeConstructorWithKind>>,
        sourceSets: Set<DokkaSourceSet>,
        isExpectActual: Boolean,
        extra: PropertyContainer<DEnum> = PropertyContainer.empty(),
    ): DEnum = DEnum(
        dri = dri,
        name = name,
        entries = entries,
        documentation = documentation,
        expectPresentInSet = expectPresentInSet,
        sources = sources,
        functions = functions,
        properties = properties,
        classlikes = classlikes,
        visibility = visibility,
        companion = companion,
        constructors = constructors,
        supertypes = supertypes,
        sourceSets = sourceSets,
        isExpectActual = isExpectActual,
        extra = extra,
        typealiases = emptyList(),
    )

    override fun withNewExtras(newExtras: PropertyContainer<DEnum>): DEnum = copy(extra = newExtras)
}

internal data class DEnumEntry(
    override val dri: DRI,
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
    override val dri: DRI,
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

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    constructor(
        dri: DRI,
        name: String,
        isConstructor: Boolean,
        parameters: List<DParameter>,
        documentation: SourceSetDependent<DocumentationNode>,
        expectPresentInSet: DokkaSourceSet?,
        sources: SourceSetDependent<DocumentableSource>,
        visibility: SourceSetDependent<Visibility>,
        type: Bound,
        generics: List<DTypeParameter>,
        receiver: DParameter?,
        modifier: SourceSetDependent<Modifier>,
        sourceSets: Set<DokkaSourceSet>,
        isExpectActual: Boolean,
        extra: PropertyContainer<DFunction> = PropertyContainer.empty()
    ) : this(
        dri = dri,
        name = name,
        isConstructor = isConstructor,
        parameters = parameters,
        documentation = documentation,
        expectPresentInSet = expectPresentInSet,
        sources = sources,
        visibility = visibility,
        type = type,
        generics = generics,
        receiver = receiver,
        modifier = modifier,
        sourceSets = sourceSets,
        isExpectActual = isExpectActual,
        extra = extra,
        contextParameters = emptyList()
    )

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    fun copy(
        dri: DRI,
        name: String,
        isConstructor: Boolean,
        parameters: List<DParameter>,
        documentation: SourceSetDependent<DocumentationNode>,
        expectPresentInSet: DokkaSourceSet?,
        sources: SourceSetDependent<DocumentableSource>,
        visibility: SourceSetDependent<Visibility>,
        type: Bound,
        generics: List<DTypeParameter>,
        receiver: DParameter?,
        modifier: SourceSetDependent<Modifier>,
        sourceSets: Set<DokkaSourceSet>,
        isExpectActual: Boolean,
        extra: PropertyContainer<DFunction> = PropertyContainer.empty()
    ): DFunction = DFunction(
        dri = dri,
        name = name,
        isConstructor = isConstructor,
        parameters = parameters,
        documentation = documentation,
        expectPresentInSet = expectPresentInSet,
        sources = sources,
        visibility = visibility,
        type = type,
        generics = generics,
        receiver = receiver,
        modifier = modifier,
        sourceSets = sourceSets,
        isExpectActual = isExpectActual,
        extra = extra,
        contextParameters = emptyList()
    )

    override val children: List<Documentable>
        get() = parameters + @OptIn(ExperimentalDokkaApi::class) contextParameters

    override fun withNewExtras(newExtras: PropertyContainer<DFunction>): DFunction = copy(extra = newExtras)
}

internal data class DInterface(
    override val dri: DRI,
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

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    constructor(
        dri: DRI,
        name: String,
        documentation: SourceSetDependent<DocumentationNode>,
        expectPresentInSet: DokkaSourceSet?,
        sources: SourceSetDependent<DocumentableSource>,
        functions: List<DFunction>,
        properties: List<DProperty>,
        classlikes: List<DClasslike>,
        visibility: SourceSetDependent<Visibility>,
        companion: DObject?,
        generics: List<DTypeParameter>,
        supertypes: SourceSetDependent<List<TypeConstructorWithKind>>,
        modifier: SourceSetDependent<Modifier>,
        sourceSets: Set<DokkaSourceSet>,
        isExpectActual: Boolean,
        extra: PropertyContainer<DInterface> = PropertyContainer.empty(),
    ) : this(
        dri = dri,
        name = name,
        documentation = documentation,
        expectPresentInSet = expectPresentInSet,
        sources = sources,
        functions = functions,
        properties = properties,
        classlikes = classlikes,
        visibility = visibility,
        companion = companion,
        generics = generics,
        supertypes = supertypes,
        modifier = modifier,
        sourceSets = sourceSets,
        isExpectActual = isExpectActual,
        extra = extra,
        typealiases = emptyList(),
    )

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    fun copy(
        dri: DRI,
        name: String,
        documentation: SourceSetDependent<DocumentationNode>,
        expectPresentInSet: DokkaSourceSet?,
        sources: SourceSetDependent<DocumentableSource>,
        functions: List<DFunction>,
        properties: List<DProperty>,
        classlikes: List<DClasslike>,
        visibility: SourceSetDependent<Visibility>,
        companion: DObject?,
        generics: List<DTypeParameter>,
        supertypes: SourceSetDependent<List<TypeConstructorWithKind>>,
        modifier: SourceSetDependent<Modifier>,
        sourceSets: Set<DokkaSourceSet>,
        isExpectActual: Boolean,
        extra: PropertyContainer<DInterface> = PropertyContainer.empty(),
    ): DInterface = DInterface(
        dri = dri,
        name = name,
        documentation = documentation,
        expectPresentInSet = expectPresentInSet,
        sources = sources,
        functions = functions,
        properties = properties,
        classlikes = classlikes,
        visibility = visibility,
        companion = companion,
        generics = generics,
        supertypes = supertypes,
        modifier = modifier,
        sourceSets = sourceSets,
        isExpectActual = isExpectActual,
        extra = extra,
        typealiases = emptyList(),
    )

    override fun withNewExtras(newExtras: PropertyContainer<DInterface>): DInterface = copy(extra = newExtras)
}

internal data class DObject(
    override val name: String?,
    override val dri: DRI,
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

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    constructor(
        name: String?,
        dri: DRI,
        documentation: SourceSetDependent<DocumentationNode>,
        expectPresentInSet: DokkaSourceSet?,
        sources: SourceSetDependent<DocumentableSource>,
        functions: List<DFunction>,
        properties: List<DProperty>,
        classlikes: List<DClasslike>,
        visibility: SourceSetDependent<Visibility>,
        supertypes: SourceSetDependent<List<TypeConstructorWithKind>>,
        sourceSets: Set<DokkaSourceSet>,
        isExpectActual: Boolean,
        extra: PropertyContainer<DObject> = PropertyContainer.empty(),
    ) : this(
        name = name,
        dri = dri,
        documentation = documentation,
        expectPresentInSet = expectPresentInSet,
        sources = sources,
        functions = functions,
        properties = properties,
        classlikes = classlikes,
        visibility = visibility,
        supertypes = supertypes,
        sourceSets = sourceSets,
        isExpectActual = isExpectActual,
        extra = extra,
        typealiases = emptyList(),
    )

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    fun copy(
        name: String?,
        dri: DRI,
        documentation: SourceSetDependent<DocumentationNode>,
        expectPresentInSet: DokkaSourceSet?,
        sources: SourceSetDependent<DocumentableSource>,
        functions: List<DFunction>,
        properties: List<DProperty>,
        classlikes: List<DClasslike>,
        visibility: SourceSetDependent<Visibility>,
        supertypes: SourceSetDependent<List<TypeConstructorWithKind>>,
        sourceSets: Set<DokkaSourceSet>,
        isExpectActual: Boolean,
        extra: PropertyContainer<DObject> = PropertyContainer.empty(),
    ): DObject = DObject(
        name = name,
        dri = dri,
        documentation = documentation,
        expectPresentInSet = expectPresentInSet,
        sources = sources,
        functions = functions,
        properties = properties,
        classlikes = classlikes,
        visibility = visibility,
        supertypes = supertypes,
        sourceSets = sourceSets,
        isExpectActual = isExpectActual,
        extra = extra,
        typealiases = emptyList(),
    )

    override fun withNewExtras(newExtras: PropertyContainer<DObject>): DObject = copy(extra = newExtras)
}

internal data class DAnnotation(
    override val name: String,
    override val dri: DRI,
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
    override val dri: DRI,
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

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    constructor(
        dri: DRI,
        name: String,
        documentation: SourceSetDependent<DocumentationNode>,
        expectPresentInSet: DokkaSourceSet?,
        sources: SourceSetDependent<DocumentableSource>,
        visibility: SourceSetDependent<Visibility>,
        type: Bound,
        receiver: DParameter?,
        setter: DFunction?,
        getter: DFunction?,
        modifier: SourceSetDependent<Modifier>,
        sourceSets: Set<DokkaSourceSet>,
        generics: List<DTypeParameter>,
        isExpectActual: Boolean,
        extra: PropertyContainer<DProperty> = PropertyContainer.empty()
    ) : this(
        dri = dri,
        name = name,
        documentation = documentation,
        expectPresentInSet = expectPresentInSet,
        sources = sources,
        visibility = visibility,
        type = type,
        receiver = receiver,
        setter = setter,
        getter = getter,
        modifier = modifier,
        sourceSets = sourceSets,
        generics = generics,
        isExpectActual = isExpectActual,
        extra = extra,
        contextParameters = emptyList()
    )

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    fun copy(
        dri: DRI,
        name: String,
        documentation: SourceSetDependent<DocumentationNode>,
        expectPresentInSet: DokkaSourceSet?,
        sources: SourceSetDependent<DocumentableSource>,
        visibility: SourceSetDependent<Visibility>,
        type: Bound,
        receiver: DParameter?,
        setter: DFunction?,
        getter: DFunction?,
        modifier: SourceSetDependent<Modifier>,
        sourceSets: Set<DokkaSourceSet>,
        generics: List<DTypeParameter>,
        isExpectActual: Boolean,
        extra: PropertyContainer<DProperty> = PropertyContainer.empty()
    ): DProperty = DProperty(
        dri = dri,
        name = name,
        documentation = documentation,
        expectPresentInSet = expectPresentInSet,
        sources = sources,
        visibility = visibility,
        type = type,
        receiver = receiver,
        setter = setter,
        getter = getter,
        modifier = modifier,
        sourceSets = sourceSets,
        generics = generics,
        isExpectActual = isExpectActual,
        extra = extra,
        contextParameters = emptyList()
    )

    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<DProperty>): DProperty = copy(extra = newExtras)
}

// TODO: treat named Parameters and receivers differently
internal data class DParameter(
    override val dri: DRI,
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
    val variantTypeParameter: Variance<TypeParameter>,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    val bounds: List<Bound>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DTypeParameter> = PropertyContainer.empty()
) : Documentable(), WithExtraProperties<DTypeParameter> {

    constructor(
        dri: DRI,
        name: String,
        presentableName: String?,
        documentation: SourceSetDependent<DocumentationNode>,
        expectPresentInSet: DokkaSourceSet?,
        bounds: List<Bound>,
        sourceSets: Set<DokkaSourceSet>,
        extra: PropertyContainer<DTypeParameter> = PropertyContainer.empty()
    ) : this(
        Invariance(TypeParameter(dri, name, presentableName)),
        documentation,
        expectPresentInSet,
        bounds,
        sourceSets,
        extra
    )

    override val dri: DRI by variantTypeParameter.inner::dri
    override val name: String by variantTypeParameter.inner::name

    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<DTypeParameter>): DTypeParameter = copy(extra = newExtras)
}

internal data class DTypeAlias(
    override val dri: DRI,
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
    val dri: DRI,
    val name: String,
    val presentableName: String? = null,
    override val extra: PropertyContainer<TypeParameter> = PropertyContainer.empty()
) : Bound(), AnnotationTarget, WithExtraProperties<TypeParameter> {
    override fun withNewExtras(newExtras: PropertyContainer<TypeParameter>): TypeParameter =
        copy(extra = extra)
}

internal sealed class TypeConstructor : Bound(), AnnotationTarget {
    abstract val dri: DRI
    abstract val projections: List<Projection>
    abstract val presentableName: String?
}

internal data class GenericTypeConstructor(
    override val dri: DRI,
    override val projections: List<Projection>,
    override val presentableName: String? = null,
    override val extra: PropertyContainer<GenericTypeConstructor> = PropertyContainer.empty()
) : TypeConstructor(), WithExtraProperties<GenericTypeConstructor> {
    override fun withNewExtras(newExtras: PropertyContainer<GenericTypeConstructor>): GenericTypeConstructor =
        copy(extra = newExtras)
}

internal data class FunctionalTypeConstructor(
    override val dri: DRI,
    override val projections: List<Projection>,
    val isExtensionFunction: Boolean = false,
    val isSuspendable: Boolean = false,
    override val presentableName: String? = null,
    override val extra: PropertyContainer<FunctionalTypeConstructor> = PropertyContainer.empty(),
    @property:ExperimentalDokkaApi
    val contextParametersCount: Int = 0
) : TypeConstructor(), WithExtraProperties<FunctionalTypeConstructor> {

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    constructor(
        dri: DRI,
        projections: List<Projection>,
        isExtensionFunction: Boolean = false,
        isSuspendable: Boolean = false,
        presentableName: String? = null,
        extra: PropertyContainer<FunctionalTypeConstructor> = PropertyContainer.empty()
    ) : this(
        dri = dri,
        projections = projections,
        isExtensionFunction = isExtensionFunction,
        isSuspendable = isSuspendable,
        presentableName = presentableName,
        extra = extra
    )

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    fun copy(
        dri: DRI,
        projections: List<Projection>,
        isExtensionFunction: Boolean = false,
        isSuspendable: Boolean = false,
        presentableName: String? = null,
        extra: PropertyContainer<FunctionalTypeConstructor> = PropertyContainer.empty()
    ): FunctionalTypeConstructor = FunctionalTypeConstructor(
        dri = dri,
        projections = projections,
        isExtensionFunction = isExtensionFunction,
        isSuspendable = isSuspendable,
        presentableName = presentableName,
        extra = extra,
        contextParametersCount = 0
    )

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

@Suppress("unused")
internal fun Variance<TypeParameter>.withDri(dri: DRI): Variance<TypeParameter> = when (this) {
    is Contravariance -> Contravariance(TypeParameter(dri, inner.name, inner.presentableName))
    is Covariance -> Covariance(TypeParameter(dri, inner.name, inner.presentableName))
    is Invariance -> Invariance(TypeParameter(dri, inner.name, inner.presentableName))
}

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
