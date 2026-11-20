/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

internal class AdditionalModifiers(
    val content: SourceSetDependent<Set<ExtraModifiers>>
) : ExtraProperty<Documentable> {

    companion object : ExtraProperty.Key<Documentable, AdditionalModifiers> {
        override fun mergeStrategyFor(
            left: AdditionalModifiers,
            right: AdditionalModifiers
        ): MergeStrategy<Documentable> = MergeStrategy.Replace(AdditionalModifiers(left.content + right.content))
    }

    override fun equals(other: Any?): Boolean =
        if (other is AdditionalModifiers) other.content == content else false

    override fun hashCode(): Int = content.hashCode()
    override val key: ExtraProperty.Key<Documentable, *> = AdditionalModifiers
}

internal fun SourceSetDependent<Set<ExtraModifiers>>.toAdditionalModifiers(): AdditionalModifiers = AdditionalModifiers(this)

internal data class Annotations(
    private val myContent: SourceSetDependent<List<Annotation>>
) : ExtraProperty<AnnotationTarget> {
    companion object : ExtraProperty.Key<AnnotationTarget, Annotations> {
        override fun mergeStrategyFor(left: Annotations, right: Annotations): MergeStrategy<AnnotationTarget> {
            val sourceSets = left.myContent.keys + right.myContent.keys
            // For each source set, merge the annotations from the `left` and `right`, if they exist.
            return MergeStrategy.Replace(
                Annotations(
                    sourceSets.associateWith { sourceSet ->
                        left.myContent.getOrDefault(sourceSet, emptyList()) +
                                right.myContent.getOrDefault(sourceSet, emptyList())
                    }
                )
            )
        }
    }

    override val key: ExtraProperty.Key<AnnotationTarget, *> = Annotations

    data class Annotation(
        val dri: DRI,
        val params: Map<String, AnnotationParameterValue>,
        val mustBeDocumented: Boolean = false,
        val scope: AnnotationScope = AnnotationScope.DIRECT
    ) {
        override fun equals(other: Any?): Boolean = when (other) {
            is Annotation -> dri == other.dri
            else -> false
        }

        override fun hashCode(): Int = dri.hashCode()
    }

    @Deprecated("Use directAnnotations or fileLevelAnnotations")
    val content: SourceSetDependent<List<Annotation>>
        get() = myContent

    val directAnnotations: SourceSetDependent<List<Annotation>> = annotationsByScope(AnnotationScope.DIRECT)

    val fileLevelAnnotations: SourceSetDependent<List<Annotation>> = annotationsByScope(AnnotationScope.FILE)

    private fun annotationsByScope(scope: AnnotationScope): SourceSetDependent<List<Annotation>> =
        myContent.entries.mapNotNull { (key, value) ->
            val withoutFileLevel = value.filter { it.scope == scope }
            if (withoutFileLevel.isEmpty()) null
            else Pair(key, withoutFileLevel)
        }.toMap()

    enum class AnnotationScope {
        DIRECT, FILE, GETTER, SETTER
    }
}

internal fun SourceSetDependent<List<Annotations.Annotation>>.toAnnotations(): Annotations = Annotations(this)

internal sealed class AnnotationParameterValue

internal data class AnnotationValue(val annotation: Annotations.Annotation) : AnnotationParameterValue()

internal data class ArrayValue(val value: List<AnnotationParameterValue>) : AnnotationParameterValue()

internal data class EnumValue(val enumName: String, val enumDri: DRI) : AnnotationParameterValue()

internal data class ClassValue(val className: String, val classDRI: DRI) : AnnotationParameterValue()

internal abstract class LiteralValue : AnnotationParameterValue() {
    abstract fun text() : String
}
internal data class IntValue(val value: Int) : LiteralValue() {
    override fun text(): String = value.toString()
}

internal data class LongValue(val value: Long) : LiteralValue() {
    override fun text(): String = value.toString()
}

internal data class FloatValue(val value: Float) : LiteralValue() {
    override fun text(): String = value.toString()
}

internal data class DoubleValue(val value: Double) : LiteralValue() {
    override fun text(): String = value.toString()
}

internal object NullValue : LiteralValue() {
    override fun text(): String = "null"
}

internal data class BooleanValue(val value: Boolean) : LiteralValue() {
    override fun text(): String = value.toString()
}

internal data class StringValue(val value: String) : LiteralValue() {
    override fun text(): String = value
    override fun toString(): String = value
}

internal object PrimaryConstructorExtra : ExtraProperty<DFunction>, ExtraProperty.Key<DFunction, PrimaryConstructorExtra> {
    override val key: ExtraProperty.Key<DFunction, *> = this
}

internal data class ActualTypealias(
    val typeAlias: DTypeAlias
) : ExtraProperty<DClasslike> {

    @Suppress("unused")
    @Deprecated(message = "It can be removed soon. Use [typeAlias.underlyingType]", ReplaceWith("this.typeAlias.underlyingType"))
    val underlyingType: SourceSetDependent<Bound>
        get() = typeAlias.underlyingType

    companion object : ExtraProperty.Key<DClasslike, ActualTypealias> {
        override fun mergeStrategyFor(
            left: ActualTypealias,
            right: ActualTypealias
        ): MergeStrategy<DClasslike> = MergeStrategy.Fail {
            throw IllegalStateException("Adding [ActualTypealias] should be after merging all documentables")
        }
    }

    override val key: ExtraProperty.Key<DClasslike, ActualTypealias> = ActualTypealias
}
