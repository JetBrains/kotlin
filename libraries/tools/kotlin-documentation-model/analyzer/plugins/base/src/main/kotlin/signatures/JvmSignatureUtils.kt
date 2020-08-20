package org.jetbrains.dokka.base.signatures

import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.driOrNull
import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.drisOfAllNestedBounds

interface JvmSignatureUtils {

    fun PageContentBuilder.DocumentableContentBuilder.annotationsBlock(d: Documentable)

    fun PageContentBuilder.DocumentableContentBuilder.annotationsInline(d: Documentable)

    fun <T : Documentable> WithExtraProperties<T>.modifiers(): SourceSetDependent<Set<ExtraModifiers>>

    fun Collection<ExtraModifiers>.toSignatureString(): String =
        joinToString("") { it.name.toLowerCase() + " " }

    fun <T : Documentable> WithExtraProperties<T>.annotations(): SourceSetDependent<List<Annotations.Annotation>> =
        extra[Annotations]?.content ?: emptyMap()

    private fun PageContentBuilder.DocumentableContentBuilder.annotations(
        d: Documentable,
        ignored: Set<Annotations.Annotation>,
        styles: Set<Style>,
        operation: PageContentBuilder.DocumentableContentBuilder.(Annotations.Annotation) -> Unit
    ): Unit = when (d) {
        is DFunction -> d.annotations()
        is DProperty -> d.annotations()
        is DClass -> d.annotations()
        is DInterface -> d.annotations()
        is DObject -> d.annotations()
        is DEnum -> d.annotations()
        is DAnnotation -> d.annotations()
        is DTypeParameter -> d.annotations()
        is DEnumEntry -> d.annotations()
        is DTypeAlias -> d.annotations()
        is DParameter -> d.annotations()
        else -> null
    }?.let {
        it.entries.forEach {
            it.value.filter { it !in ignored && it.mustBeDocumented }.takeIf { it.isNotEmpty() }?.let { annotations ->
                group(sourceSets = setOf(it.key), styles = styles, kind = ContentKind.Annotations) {
                    annotations.forEach {
                        operation(it)
                    }
                }
            }
        }
    } ?: Unit

    fun PageContentBuilder.DocumentableContentBuilder.toSignatureString(
        a: Annotations.Annotation,
        renderAtStrategy: AtStrategy,
        listBrackets: Pair<Char, Char>,
        classExtension: String
    ) {

        when (renderAtStrategy) {
            is All, is OnlyOnce -> text("@")
            is Never -> Unit
        }
        link(a.dri.classNames!!, a.dri)
        text("(")
        a.params.entries.forEachIndexed { i, it ->
            group(styles = setOf(TextStyle.BreakableAfter)) {
                text(it.key + " = ")
                when (renderAtStrategy) {
                    is All -> All
                    is Never, is OnlyOnce -> Never
                }.let { strategy ->
                    valueToSignature(it.value, strategy, listBrackets, classExtension)
                }
                if (i != a.params.entries.size - 1) text(", ")
            }
        }
        text(")")
    }

    private fun PageContentBuilder.DocumentableContentBuilder.valueToSignature(
        a: AnnotationParameterValue,
        renderAtStrategy: AtStrategy,
        listBrackets: Pair<Char, Char>,
        classExtension: String
    ): Unit = when (a) {
        is AnnotationValue -> toSignatureString(a.annotation, renderAtStrategy, listBrackets, classExtension)
        is ArrayValue -> {
            text(listBrackets.first.toString())
            a.value.forEachIndexed { i, it ->
                group(styles = setOf(TextStyle.BreakableAfter)) {
                    valueToSignature(it, renderAtStrategy, listBrackets, classExtension)
                    if (i != a.value.size - 1) text(", ")
                }
            }
            text(listBrackets.second.toString())
        }
        is EnumValue -> link(a.enumName, a.enumDri)
        is ClassValue -> link(a.className + classExtension, a.classDRI)
        is StringValue -> group(styles = setOf(TextStyle.Breakable)) { text(a.value) }
    }

    fun PageContentBuilder.DocumentableContentBuilder.annotationsBlockWithIgnored(
        d: Documentable,
        ignored: Set<Annotations.Annotation>,
        renderAtStrategy: AtStrategy,
        listBrackets: Pair<Char, Char>,
        classExtension: String
    ) {
        annotations(d, ignored, setOf(TextStyle.Block)) {
            group {
                toSignatureString(it, renderAtStrategy, listBrackets, classExtension)
            }
        }
    }

    fun PageContentBuilder.DocumentableContentBuilder.annotationsInlineWithIgnored(
        d: Documentable,
        ignored: Set<Annotations.Annotation>,
        renderAtStrategy: AtStrategy,
        listBrackets: Pair<Char, Char>,
        classExtension: String
    ) {
        annotations(d, ignored, setOf(TextStyle.Span)) {
            toSignatureString(it, renderAtStrategy, listBrackets, classExtension)
            text(Typography.nbsp.toString())
        }
    }

    fun <T : Documentable> WithExtraProperties<T>.stylesIfDeprecated(sourceSetData: DokkaSourceSet): Set<TextStyle> =
        if (extra[Annotations]?.content?.get(sourceSetData)?.any {
                it.dri == DRI("kotlin", "Deprecated")
                        || it.dri == DRI("java.lang", "Deprecated")
            } == true) setOf(TextStyle.Strikethrough) else emptySet()

    infix fun DFunction.uses(t: DTypeParameter): Boolean {
        val allDris: List<DRI> = (listOfNotNull(receiver?.dri, *receiver?.type?.drisOfAllNestedBounds?.toTypedArray() ?: emptyArray()) +
                parameters.flatMap { listOf(it.dri) + it.type.drisOfAllNestedBounds })
        return t.dri in allDris
    }
}

sealed class AtStrategy
object All : AtStrategy()
object OnlyOnce : AtStrategy()
object Never : AtStrategy()
