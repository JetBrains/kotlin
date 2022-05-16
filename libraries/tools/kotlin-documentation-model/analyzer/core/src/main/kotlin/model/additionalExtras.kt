package org.jetbrains.dokka.model

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

class AdditionalModifiers(val content: SourceSetDependent<Set<ExtraModifiers>>) : ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, AdditionalModifiers> {
        override fun mergeStrategyFor(
            left: AdditionalModifiers,
            right: AdditionalModifiers
        ): MergeStrategy<Documentable> = MergeStrategy.Replace(AdditionalModifiers(left.content + right.content))
    }

    override fun equals(other: Any?): Boolean =
        if (other is AdditionalModifiers) other.content == content else false

    override fun hashCode() = content.hashCode()
    override val key: ExtraProperty.Key<Documentable, *> = AdditionalModifiers
}

fun SourceSetDependent<Set<ExtraModifiers>>.toAdditionalModifiers() = AdditionalModifiers(this)

data class Annotations(
    private val myContent: SourceSetDependent<List<Annotation>>
) : ExtraProperty<AnnotationTarget> {
    companion object : ExtraProperty.Key<AnnotationTarget, Annotations> {
        override fun mergeStrategyFor(left: Annotations, right: Annotations): MergeStrategy<AnnotationTarget> =
            MergeStrategy.Replace(Annotations(left.myContent + right.myContent))
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

fun SourceSetDependent<List<Annotations.Annotation>>.toAnnotations() = Annotations(this)

sealed class AnnotationParameterValue
data class AnnotationValue(val annotation: Annotations.Annotation) : AnnotationParameterValue()
data class ArrayValue(val value: List<AnnotationParameterValue>) : AnnotationParameterValue()
data class EnumValue(val enumName: String, val enumDri: DRI) : AnnotationParameterValue()
data class ClassValue(val className: String, val classDRI: DRI) : AnnotationParameterValue()
abstract class LiteralValue : AnnotationParameterValue() {
    abstract fun text() : String
}
data class IntValue(val value: Int) : LiteralValue() {
    override fun text(): String = value.toString()
}

data class LongValue(val value: Long) : LiteralValue() {
    override fun text(): String = value.toString()
}
data class FloatValue(val value: Float) : LiteralValue() {
    override fun text(): String = value.toString()
}
data class DoubleValue(val value: Double) : LiteralValue() {
    override fun text(): String = value.toString()
}
object NullValue : LiteralValue() {
    override fun text(): String = "null"
}
data class BooleanValue(val value: Boolean) : LiteralValue() {
    override fun text(): String = value.toString()
}
data class StringValue(val value: String) : LiteralValue() {
    override fun text(): String = value
    override fun toString(): String = value
}


object PrimaryConstructorExtra : ExtraProperty<DFunction>, ExtraProperty.Key<DFunction, PrimaryConstructorExtra> {
    override val key: ExtraProperty.Key<DFunction, *> = this
}

data class ActualTypealias(val underlyingType: SourceSetDependent<Bound>) : ExtraProperty<DClasslike> {
    companion object : ExtraProperty.Key<DClasslike, ActualTypealias> {
        override fun mergeStrategyFor(
            left: ActualTypealias,
            right: ActualTypealias
        ) =
            MergeStrategy.Replace(ActualTypealias(left.underlyingType + right.underlyingType))
    }

    override val key: ExtraProperty.Key<DClasslike, ActualTypealias> = ActualTypealias
}
