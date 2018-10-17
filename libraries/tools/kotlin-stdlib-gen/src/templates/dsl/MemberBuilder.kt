/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.Family.*
import java.io.StringReader
import java.util.StringTokenizer

private fun getDefaultSourceFile(f: Family): SourceFile = when (f) {
    Iterables, Collections, Lists -> SourceFile.Collections
    Sequences -> SourceFile.Sequences
    Sets -> SourceFile.Sets
    Ranges, RangesOfPrimitives, ProgressionsOfPrimitives -> SourceFile.Ranges
    ArraysOfObjects, InvariantArraysOfObjects, ArraysOfPrimitives -> SourceFile.Arrays
    ArraysOfUnsigned -> SourceFile.UArrays
    Maps -> SourceFile.Maps
    Strings -> SourceFile.Strings
    CharSequences -> SourceFile.Strings
    Primitives, Generic, Unsigned -> SourceFile.Misc
}

@TemplateDsl
class MemberBuilder(
        val allowedPlatforms: Set<Platform>,
        val platform: Platform,
        var family: Family,
        var sourceFile: SourceFile = getDefaultSourceFile(family),
        var primitive: PrimitiveType? = null
) {
    lateinit var keyword: Keyword    // fun/val/var
    lateinit var signature: String   // name and params

    var sortingSignature: String? = null
        get() = field ?: signature
        private set

    val f get() = family

    private val legacyMode = false
    var hasPlatformSpecializations: Boolean = legacyMode
        private set

    var doc: String? = null; private set

    val samples = mutableListOf<String>()

    val sequenceClassification = mutableListOf<SequenceClass>()
    var deprecate: Deprecation? = null; private set
    var since: String? = null; private set
    var platformName: String? = null; private set

    var visibility: String? = null; private set
    var external: Boolean = false; private set
    var inline: Inline = Inline.No; private set
    var infix: Boolean = false; private set
    var operator: Boolean = false; private set
    val typeParams = mutableListOf<String>()
    var primaryTypeParameter: String? = null; private set
    var customReceiver: String? = null; private set
    var receiverAsterisk: Boolean = false // TODO: rename to genericStarProjection
    var toNullableT: Boolean = false

    var returns: String? = null; private set
    var body: String? = null; private set
    val annotations: MutableList<String> = mutableListOf()
    val suppressions: MutableList<String> = mutableListOf()

    fun sourceFile(file: SourceFile) { sourceFile = file }

    fun deprecate(value: Deprecation) { deprecate = value }
    fun deprecate(value: String) { deprecate = Deprecation(value) }
    fun since(value: String) { since = value }
    fun platformName(name: String) { platformName = name }

    fun visibility(value: String) { visibility = value }
    fun external(value: Boolean = true) { external = value }
    fun operator(value: Boolean = true) { operator = value }
    fun infix(value: Boolean = true) { infix = value }
    fun inline(value: Inline = Inline.Yes, suppressWarning: Boolean = false) {
        inline = value
        if (suppressWarning) {
            require(value == Inline.Yes)
            inline = Inline.YesSuppressWarning
        }
    }
    fun inlineOnly() { inline = Inline.Only }

    fun receiver(value: String) { customReceiver = value }
    @Deprecated("Use receiver()", ReplaceWith("receiver(value)"))
    fun customReceiver(value: String) = receiver(value)
    fun signature(value: String, notForSorting: Boolean = false) {
        if (notForSorting) sortingSignature = signature
        signature = value
    }
    fun returns(type: String) { returns = type }
    @Deprecated("Use specialFor", ReplaceWith("specialFor(*fs) { returns(run(valueBuilder)) }"))
    fun returns(vararg fs: Family, valueBuilder: () -> String) = specialFor(*fs) { returns(run(valueBuilder)) }

    fun typeParam(typeParameterName: String, primary: Boolean = false) {
        typeParams += typeParameterName
        if (primary) {
            check(primaryTypeParameter == null)
            primaryTypeParameter = typeParameterName
        }
    }

    fun annotation(annotation: String) {
        annotations += annotation
    }

    fun suppress(diagnostic: String) {
        suppressions += diagnostic
    }

    fun sequenceClassification(vararg sequenceClass: SequenceClass) {
        sequenceClassification += sequenceClass
    }

    fun doc(valueBuilder: DocExtensions.() -> String) {
        doc = valueBuilder(DocExtensions)
    }

    @Deprecated("Use specialFor", ReplaceWith("specialFor(*fs) { doc(valueBuilder) }"))
    fun doc(vararg fs: Family, valueBuilder: DocExtensions.() -> String) = specialFor(*fs) { doc(valueBuilder) }

    fun sample(sampleRef: String) {
        samples += sampleRef
    }

    fun body(valueBuilder: () -> String) {
        body = valueBuilder()
    }
    fun body(f: Family, valueBuilder: () -> String) {
        specialFor(f) { body(valueBuilder) }
    }
    fun body(vararg families: Family, valueBuilder: () -> String) {
        specialFor(*families) { body(valueBuilder) }
    }


    fun on(platform: Platform, action: () -> Unit) {
        require(platform in allowedPlatforms) { "Platform $platform is not in the list of allowed platforms $allowedPlatforms" }
        if (this.platform == platform)
            action()
        else {
            hasPlatformSpecializations = true
        }
    }

    fun specialFor(f: Family, action: () -> Unit) {
        if (family == f)
            action()
    }
    fun specialFor(vararg families: Family, action: () -> Unit) {
        require(families.isNotEmpty())
        if (family in families)
            action()
    }


    fun build(builder: Appendable) {
        val headerOnly: Boolean
        val isImpl: Boolean
        if (!legacyMode) {
            headerOnly = platform == Platform.Common && hasPlatformSpecializations
            isImpl = platform != Platform.Common && Platform.Common in allowedPlatforms
        }
        else {
            // legacy mode when all is headerOnly + no_impl
            // except functions with optional parameters - they are common + no_impl
            val hasOptionalParams = signature.contains("=")
            headerOnly =  platform == Platform.Common && !hasOptionalParams
            isImpl = false
        }

        val returnType = returns ?: throw RuntimeException("No return type specified for $signature")
        val primaryTypeParameter = this.primaryTypeParameter ?: "T"

        fun renderType(expression: String, receiver: String, self: String): String {
            val t = StringTokenizer(expression, " \t\n,:()<>?.", true)
            val answer = StringBuilder()

            while (t.hasMoreTokens()) {
                val token = t.nextToken()
                answer.append(when (token) {
                    "RECEIVER" -> receiver
                    "SELF" -> self
                    "PRIMITIVE" -> primitive?.name ?: token
                    "SUM" -> {
                        when (primitive) {
                            PrimitiveType.Byte, PrimitiveType.Short, PrimitiveType.Char -> "Int"
                            else -> primitive
                        }
                    }
                    "ZERO" -> when (primitive) {
                        PrimitiveType.Double -> "0.0"
                        PrimitiveType.Float -> "0.0f"
                        PrimitiveType.Long -> "0L"
                        else -> "0"
                    }
                    "ONE" -> when (primitive) {
                        PrimitiveType.Double -> "1.0"
                        PrimitiveType.Float -> "1.0f"
                        PrimitiveType.Long -> "1L"
                        else -> "1"
                    }
                    "-ONE" -> when (primitive) {
                        PrimitiveType.Double -> "-1.0"
                        PrimitiveType.Float -> "-1.0f"
                        PrimitiveType.Long -> "-1L"
                        else -> "-1"
                    }
                    "TCollection" -> {
                        when (family) {
                            CharSequences, Strings -> "Appendable"
                            else -> renderType("MutableCollection<in $primaryTypeParameter>", receiver, self)
                        }
                    }
                    primaryTypeParameter -> {
                        when (family) {
                            Generic -> primaryTypeParameter
                            CharSequences, Strings -> "Char"
                            Maps -> "Map.Entry<K, V>"
                            else -> primitive?.name ?: token
                        }
                    }
                    "TRange" -> {
                        when (family) {
                            Generic -> "Range<$primaryTypeParameter>"
                            else -> primitive!!.name + "Range"
                        }
                    }
                    "TProgression" -> {
                        when (family) {
                            Generic -> "Progression<out $primaryTypeParameter>"
                            else -> primitive!!.name + "Progression"
                        }
                    }
                    else -> token
                })
            }

            return answer.toString()
        }

        val isAsteriskOrT = if (receiverAsterisk) "*" else primaryTypeParameter
        val self = (when (family) {
            Iterables -> "Iterable<$isAsteriskOrT>"
            Collections -> "Collection<$isAsteriskOrT>"
            Lists -> "List<$isAsteriskOrT>"
            Maps -> "Map<out K, V>"
            Sets -> "Set<$isAsteriskOrT>"
            Sequences -> "Sequence<$isAsteriskOrT>"
            InvariantArraysOfObjects -> "Array<$primaryTypeParameter>"
            ArraysOfObjects -> "Array<${isAsteriskOrT.replace(primaryTypeParameter, "out $primaryTypeParameter")}>"
            Strings -> "String"
            CharSequences -> "CharSequence"
            Ranges -> "ClosedRange<$isAsteriskOrT>"
            ArraysOfPrimitives, ArraysOfUnsigned -> primitive?.let { it.name + "Array" } ?: throw IllegalArgumentException("Primitive array should specify primitive type")
            RangesOfPrimitives -> primitive?.let { it.name + "Range" } ?: throw IllegalArgumentException("Primitive range should specify primitive type")
            ProgressionsOfPrimitives -> primitive?.let { it.name + "Progression" } ?: throw IllegalArgumentException("Primitive progression should specify primitive type")
            Primitives, Unsigned -> primitive?.let { it.name } ?: throw IllegalArgumentException("Primitive should specify primitive type")
            Generic -> primaryTypeParameter
        })

        val receiver = (customReceiver ?: self).let { renderType(it, it, self) }

        fun String.renderType(): String = renderType(this, receiver, self)

        fun effectiveTypeParams(): List<TypeParameter> {
            val parameters = typeParams.mapTo(mutableListOf()) { parseTypeParameter(it.renderType()) }

            if (family == Generic) {
                if (parameters.none { it.name == primaryTypeParameter })
                    parameters.add(TypeParameter(primaryTypeParameter))
                return parameters
            } else if (primitive == null && family != Strings && family != CharSequences) {
                val mentionedTypes = parseTypeRef(receiver).mentionedTypes() + parameters.flatMap { it.mentionedTypeRefs() }
                val implicitTypeParameters = mentionedTypes.filter { it.name.all(Char::isUpperCase) }
                for (implicit in implicitTypeParameters.reversed()) {
                    if (implicit.name != "*" && parameters.none { it.name == implicit.name }) {
                        parameters.add(0, TypeParameter(implicit.name))
                    }
                }

                return parameters
            } else {
                // substituted T is no longer a parameter
                val renderedT = primaryTypeParameter.renderType()
                return parameters.filterNot { it.name == renderedT }
            }
        }


        doc?.let { methodDoc ->
            builder.append("/**\n")
            StringReader(methodDoc.trim()).forEachLine { line ->
                builder.append(" * ").append(line.trim()).append("\n")
            }
            if (family == Sequences && sequenceClassification.isNotEmpty()) {
                builder.append(" *\n")
                builder.append(" * The operation is ${sequenceClassification.joinToString(" and ") { "_${it}_" }}.\n")
            }
            if (samples.any()) {
                builder.append(" * \n")
                samples.forEach { builder.append(" * @sample $it\n")}
            }
            builder.append(" */\n")
        }



        deprecate?.let { deprecated ->
            val args = listOfNotNull(
                    "\"${deprecated.message}\"",
                    deprecated.replaceWith?.let { "ReplaceWith(\"$it\")" },
                    deprecated.level.let { if (it != DeprecationLevel.WARNING) "level = DeprecationLevel.$it" else null }
            )
            builder.append("@Deprecated(${args.joinToString(", ")})\n")
        }

        if (!f.isPrimitiveSpecialization && primitive != null) {
            platformName
                    ?.replace("<$primaryTypeParameter>", primitive!!.name)
                    ?.let { platformName -> builder.append("@kotlin.jvm.JvmName(\"${platformName}\")\n") }
        }

        since?.let { since ->
            builder.append("@SinceKotlin(\"$since\")\n")
        }

        annotations.forEach { builder.append(it.trimIndent()).append('\n') }

        when (inline) {
            Inline.Only -> builder.append("@kotlin.internal.InlineOnly").append('\n')
            Inline.YesSuppressWarning -> suppressions.add("NOTHING_TO_INLINE")
        }

        if (suppressions.isNotEmpty()) {
            suppressions.joinTo(builder, separator = ", ", prefix = "@Suppress(", postfix = ")\n") {
                """"$it""""
            }
        }

        listOfNotNull(
                visibility ?: "public",
                "expect".takeIf { headerOnly },
                "actual".takeIf { isImpl },
                "external".takeIf { external },
                "inline".takeIf { inline.isInline() },
                "infix".takeIf { infix },
                "operator".takeIf { operator },
                keyword.value
        ).forEach { builder.append(it).append(' ') }

        val types = effectiveTypeParams()
        if (!types.isEmpty()) {
            builder.append(types.joinToString(separator = ", ", prefix = "<", postfix = "> ", transform = { it.original }))
        }

        val receiverType = (if (toNullableT) receiver.replace("T>", "T?>") else receiver).renderType()

        builder.append(receiverType)
        if (receiverType.isNotEmpty()) builder.append('.')
        builder.append("${signature.renderType()}: ${returnType.renderType()}")

        if (headerOnly) {
            builder.append("\n\n")
            return
        }

        if (keyword == Keyword.Function) builder.append(" {")

        val body = (body ?:
                deprecate?.replaceWith?.let { "return $it" } ?:
                throw RuntimeException("$signature for ${platform.fullName}: no body specified for ${family to primitive}")
                ).trim('\n')
        val indent: Int = body.takeWhile { it == ' ' }.length

        builder.append('\n')
        body.lineSequence().forEach {
            var count = indent
            val line = it.dropWhile { count-- > 0 && it == ' ' }.renderType()
            if (!line.isEmpty()) {
                builder.append("    ").append(line)
                builder.append("\n")
            }
        }

        if (keyword == Keyword.Function) builder.append("}\n")
        builder.append("\n")
    }

}