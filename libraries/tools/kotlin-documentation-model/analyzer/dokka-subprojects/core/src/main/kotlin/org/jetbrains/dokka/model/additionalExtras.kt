/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

public class AdditionalModifiers(
    public val content: SourceSetDependent<Set<ExtraModifiers>>
) : ExtraProperty<Documentable> {

    public companion object : ExtraProperty.Key<Documentable, AdditionalModifiers> {
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

public fun SourceSetDependent<Set<ExtraModifiers>>.toAdditionalModifiers(): AdditionalModifiers = AdditionalModifiers(this)

public data class Annotations(
    private val myContent: SourceSetDependent<List<Annotation>>
) : ExtraProperty<AnnotationTarget> {
    public companion object : ExtraProperty.Key<AnnotationTarget, Annotations> {
        override fun mergeStrategyFor(left: Annotations, right: Annotations): MergeStrategy<AnnotationTarget> =
            MergeStrategy.Replace(Annotations(left.myContent + right.myContent))
    }

    override val key: ExtraProperty.Key<AnnotationTarget, *> = Annotations

    public data class Annotation(
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

    public enum class AnnotationScope {
        DIRECT, FILE, GETTER, SETTER
    }
}

public fun SourceSetDependent<List<Annotations.Annotation>>.toAnnotations(): Annotations = Annotations(this)

public sealed class AnnotationParameterValue

public data class AnnotationValue(val annotation: Annotations.Annotation) : AnnotationParameterValue()

public data class ArrayValue(val value: List<AnnotationParameterValue>) : AnnotationParameterValue()

public data class EnumValue(val enumName: String, val enumDri: DRI) : AnnotationParameterValue()

public data class ClassValue(val className: String, val classDRI: DRI) : AnnotationParameterValue()

public abstract class LiteralValue : AnnotationParameterValue() {
    public abstract fun text() : String
}
public data class IntValue(val value: Int) : LiteralValue() {
    override fun text(): String = value.toString()
}

public data class LongValue(val value: Long) : LiteralValue() {
    override fun text(): String = value.toString()
}

public data class FloatValue(val value: Float) : LiteralValue() {
    override fun text(): String = value.toString()
}

public data class DoubleValue(val value: Double) : LiteralValue() {
    override fun text(): String = value.toString()
}

public object NullValue : LiteralValue() {
    override fun text(): String = "null"
}

public data class BooleanValue(val value: Boolean) : LiteralValue() {
    override fun text(): String = value.toString()
}

public data class StringValue(val value: String) : LiteralValue() {
    override fun text(): String = value
    override fun toString(): String = value
}

public object PrimaryConstructorExtra : ExtraProperty<DFunction>, ExtraProperty.Key<DFunction, PrimaryConstructorExtra> {
    override val key: ExtraProperty.Key<DFunction, *> = this
}

public data class ActualTypealias(
    val typeAlias: DTypeAlias
) : ExtraProperty<DClasslike> {

    @Suppress("unused")
    @Deprecated(message = "It can be removed soon. Use [typeAlias.underlyingType]", ReplaceWith("this.typeAlias.underlyingType"))
    val underlyingType: SourceSetDependent<Bound>
        get() = typeAlias.underlyingType

    public companion object : ExtraProperty.Key<DClasslike, ActualTypealias> {
        override fun mergeStrategyFor(
            left: ActualTypealias,
            right: ActualTypealias
        ): MergeStrategy<DClasslike> = MergeStrategy.Fail {
            throw IllegalStateException("Adding [ActualTypealias] should be after merging all documentables")
        }
    }

    override val key: ExtraProperty.Key<DClasslike, ActualTypealias> = ActualTypealias
}
