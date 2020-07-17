package org.jetbrains.dokka.model

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.DriWithKind
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties


abstract class Documentable : WithChildren<Documentable> {
    abstract val name: String?
    abstract val dri: DRI
    abstract val documentation: SourceSetDependent<DocumentationNode>
    abstract val sourceSets: Set<DokkaSourceSet>
    abstract val expectPresentInSet: DokkaSourceSet?

    override fun toString(): String =
        "${javaClass.simpleName}($dri)"

    override fun equals(other: Any?) =
        other is Documentable && this.dri == other.dri // TODO: https://github.com/Kotlin/dokka/pull/667#discussion_r382555806

    override fun hashCode() = dri.hashCode()
}

typealias SourceSetDependent<T> = Map<DokkaSourceSet, T>

interface WithExpectActual {
    val sources: SourceSetDependent<DocumentableSource>
}

interface WithScope {
    val functions: List<DFunction>
    val properties: List<DProperty>
    val classlikes: List<DClasslike>
}

interface WithVisibility {
    val visibility: SourceSetDependent<Visibility>
}

interface WithType {
    val type: Bound
}

interface WithAbstraction {
    val modifier: SourceSetDependent<Modifier>
}

sealed class Modifier(val name: String)
sealed class KotlinModifier(name: String) : Modifier(name) {
    object Abstract : KotlinModifier("abstract")
    object Open : KotlinModifier("open")
    object Final : KotlinModifier("final")
    object Sealed : KotlinModifier("sealed")
    object Empty : KotlinModifier("")
}

sealed class JavaModifier(name: String) : Modifier(name) {
    object Abstract : JavaModifier("abstract")
    object Final : JavaModifier("final")
    object Empty : JavaModifier("")
}

interface WithCompanion {
    val companion: DObject?
}

interface WithConstructors {
    val constructors: List<DFunction>
}

interface WithGenerics {
    val generics: List<DTypeParameter>
}

interface WithSupertypes {
    val supertypes: SourceSetDependent<List<DriWithKind>>
}

interface Callable : WithVisibility, WithType, WithAbstraction, WithExpectActual {
    val receiver: DParameter?
}

sealed class DClasslike : Documentable(), WithScope, WithVisibility, WithExpectActual

data class DModule(
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

    override fun withNewExtras(newExtras: PropertyContainer<DModule>) = copy(extra = newExtras)
}

data class DPackage(
    override val dri: DRI,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    val typealiases: List<DTypeAlias>,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet? = null,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DPackage> = PropertyContainer.empty()
) : Documentable(), WithScope, WithExtraProperties<DPackage> {
    override val name = dri.packageName.orEmpty()
    override val children: List<Documentable>
        get() = (properties + functions + classlikes)

    override fun withNewExtras(newExtras: PropertyContainer<DPackage>) = copy(extra = newExtras)
}

data class DClass(
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
    override val supertypes: SourceSetDependent<List<DriWithKind>>,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val modifier: SourceSetDependent<Modifier>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DClass> = PropertyContainer.empty()
) : DClasslike(), WithAbstraction, WithCompanion, WithConstructors, WithGenerics, WithSupertypes,
    WithExtraProperties<DClass> {

    override val children: List<Documentable>
        get() = (functions + properties + classlikes + constructors)

    override fun withNewExtras(newExtras: PropertyContainer<DClass>) = copy(extra = newExtras)
}

data class DEnum(
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
    override val supertypes: SourceSetDependent<List<DriWithKind>>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DEnum> = PropertyContainer.empty()
) : DClasslike(), WithCompanion, WithConstructors, WithSupertypes, WithExtraProperties<DEnum> {
    override val children: List<Documentable>
        get() = (entries + functions + properties + classlikes + constructors)

    override fun withNewExtras(newExtras: PropertyContainer<DEnum>) = copy(extra = newExtras)
}

data class DEnumEntry(
    override val dri: DRI,
    override val name: String,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DEnumEntry> = PropertyContainer.empty()
) : Documentable(), WithScope, WithExtraProperties<DEnumEntry> {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes)

    override fun withNewExtras(newExtras: PropertyContainer<DEnumEntry>) = copy(extra = newExtras)
}

data class DFunction(
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
    override val extra: PropertyContainer<DFunction> = PropertyContainer.empty()
) : Documentable(), Callable, WithGenerics, WithExtraProperties<DFunction> {
    override val children: List<Documentable>
        get() = parameters

    override fun withNewExtras(newExtras: PropertyContainer<DFunction>) = copy(extra = newExtras)
}

data class DInterface(
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
    override val supertypes: SourceSetDependent<List<DriWithKind>>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DInterface> = PropertyContainer.empty()
) : DClasslike(), WithCompanion, WithGenerics, WithSupertypes, WithExtraProperties<DInterface> {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes)

    override fun withNewExtras(newExtras: PropertyContainer<DInterface>) = copy(extra = newExtras)
}

data class DObject(
    override val name: String?,
    override val dri: DRI,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val visibility: SourceSetDependent<Visibility>,
    override val supertypes: SourceSetDependent<List<DriWithKind>>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DObject> = PropertyContainer.empty()
) : DClasslike(), WithSupertypes, WithExtraProperties<DObject> {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes) as List<Documentable>

    override fun withNewExtras(newExtras: PropertyContainer<DObject>) = copy(extra = newExtras)
}

data class DAnnotation(
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
    override val extra: PropertyContainer<DAnnotation> = PropertyContainer.empty()
) : DClasslike(), WithCompanion, WithConstructors, WithExtraProperties<DAnnotation>, WithGenerics {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes + constructors)

    override fun withNewExtras(newExtras: PropertyContainer<DAnnotation>) = copy(extra = newExtras)
}

data class DProperty(
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
    override val extra: PropertyContainer<DProperty> = PropertyContainer.empty()
) : Documentable(), Callable, WithExtraProperties<DProperty>, WithGenerics {
    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<DProperty>) = copy(extra = newExtras)
}

// TODO: treat named Parameters and receivers differently
data class DParameter(
    override val dri: DRI,
    override val name: String?,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    val type: Bound,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DParameter> = PropertyContainer.empty()
) : Documentable(), WithExtraProperties<DParameter> {
    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<DParameter>) = copy(extra = newExtras)
}

data class DTypeParameter(
    override val dri: DRI,
    override val name: String,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    val bounds: List<Bound>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DTypeParameter> = PropertyContainer.empty()
) : Documentable(), WithExtraProperties<DTypeParameter> {
    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<DTypeParameter>) = copy(extra = newExtras)
}

data class DTypeAlias(
    override val dri: DRI,
    override val name: String,
    override val type: Bound,
    val underlyingType: SourceSetDependent<Bound>,
    override val visibility: SourceSetDependent<Visibility>,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DTypeAlias> = PropertyContainer.empty()
) : Documentable(), WithType, WithVisibility, WithExtraProperties<DTypeAlias> {
    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<DTypeAlias>) = copy(extra = newExtras)
}

sealed class Projection
sealed class Bound : Projection()
data class OtherParameter(val declarationDRI: DRI, val name: String) : Bound()
object Star : Projection()
data class TypeConstructor(
    val dri: DRI,
    val projections: List<Projection>,
    val modifier: FunctionModifiers = FunctionModifiers.NONE
) : Bound()

data class Nullable(val inner: Bound) : Bound()
data class Variance(val kind: Kind, val inner: Bound) : Projection() {
    enum class Kind { In, Out }
}

data class PrimitiveJavaType(val name: String) : Bound()
object Void : Bound()
object JavaObject : Bound()
object Dynamic : Bound()
data class UnresolvedBound(val name: String) : Bound()

enum class FunctionModifiers {
    NONE, FUNCTION, EXTENSION
}

private fun String.shorten(maxLength: Int) = lineSequence().first().let {
    if (it.length != length || it.length > maxLength) it.take(maxLength - 3) + "..." else it
}

fun Documentable.dfs(predicate: (Documentable) -> Boolean): Documentable? =
    if (predicate(this)) {
        this
    } else {
        this.children.asSequence().mapNotNull { it.dfs(predicate) }.firstOrNull()
    }

sealed class Visibility(val name: String)
sealed class KotlinVisibility(name: String) : Visibility(name) {
    object Public : KotlinVisibility("public")
    object Private : KotlinVisibility("private")
    object Protected : KotlinVisibility("protected")
    object Internal : KotlinVisibility("internal")
}

sealed class JavaVisibility(name: String) : Visibility(name) {
    object Public : JavaVisibility("public")
    object Private : JavaVisibility("private")
    object Protected : JavaVisibility("protected")
    object Default : JavaVisibility("")
}

fun <T> SourceSetDependent<T>?.orEmpty(): SourceSetDependent<T> = this ?: emptyMap()

interface DocumentableSource {
    val path: String
}
