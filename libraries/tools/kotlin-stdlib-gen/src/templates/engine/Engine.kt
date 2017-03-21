package templates

import templates.Family.*
import templates.Family.Collections
import java.io.StringReader
import java.util.*

enum class Family {
    Iterables,
    Collections,
    Lists,
    Sets,
    Maps,
    InvariantArraysOfObjects,
    ArraysOfObjects,
    ArraysOfPrimitives,
    Sequences,
    CharSequences,
    Strings,
    Ranges,
    RangesOfPrimitives,
    ProgressionsOfPrimitives,
    Generic,
    Primitives;

    val isPrimitiveSpecialization: Boolean by lazy { this in primitiveSpecializations }

    class DocExtension(val family: Family)
    class CodeExtension(val family: Family)
    val doc = DocExtension(this)
    val code = CodeExtension(this)

    companion object {
        val primitiveSpecializations = setOf(ArraysOfPrimitives, RangesOfPrimitives, ProgressionsOfPrimitives, Primitives)
        val defaultFamilies = setOf(Iterables, Sequences, ArraysOfObjects, ArraysOfPrimitives)
    }
}

enum class PrimitiveType {
    Byte,
    Short,
    Int,
    Long,
    Float,
    Double,
    Boolean,
    Char;

    val capacity by lazy { descendingByDomainCapacity.indexOf(this).let { if (it < 0) it else descendingByDomainCapacity.size - it } }

    companion object {
        val defaultPrimitives = PrimitiveType.values().toSet()
        val numericPrimitives = setOf(Int, Long, Byte, Short, Double, Float)
        val integralPrimitives = setOf(Int, Long, Byte, Short, Char)

        val descendingByDomainCapacity = listOf(Double, Float, Long, Int, Short, Char, Byte)

        fun maxByCapacity(fromType: PrimitiveType, toType: PrimitiveType): PrimitiveType = descendingByDomainCapacity.first { it == fromType || it == toType }
    }
}

fun PrimitiveType.isIntegral(): Boolean = this in PrimitiveType.integralPrimitives
fun PrimitiveType.isNumeric(): Boolean = this in PrimitiveType.numericPrimitives

enum class Inline {
    No,
    Yes,
    Only;

    fun isInline() = this != No
}

enum class Platform {
    Common,
    JVM,
    JS
}

enum class SequenceClass {
    terminal,
    intermediate,
    stateless,
    nearly_stateless,
    stateful
}

data class Deprecation(val message: String, val replaceWith: String? = null, val level: DeprecationLevel = DeprecationLevel.WARNING)
val forBinaryCompatibility = Deprecation("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)

open class BaseSpecializedProperty<TKey: Any, TValue : Any> {
    protected open fun onKeySet(key: TKey) {}

    var default: TValue? = null
        protected set
}

open class PlatformSpecializedProperty<TKey: Any, TValue : Any>() : BaseSpecializedProperty<TKey, TValue>() {
    private val valueBuilders = HashMap<Platform?, HashMap<TKey?, ((TKey) -> TValue)>>()

    operator fun get(platform: Platform, key: TKey): TValue? = run {
        valueBuilders[platform]?.get(key)
                ?: valueBuilders[null]?.get(key)
                ?: valueBuilders[platform]?.get(null)
                ?: valueBuilders[null]?.get(null)
    }?.let { it(key) }

    fun isSpecializedFor(platform: Platform, key: TKey): Boolean
            = valueBuilders[platform]?.contains(key) == true || valueBuilders[null]?.contains(key) == true

    operator fun set(platform: Platform?, keys: Collection<TKey>, value: TValue) {
        if (platform == null && keys.isEmpty()) default = value
        set(platform, keys, { value })
    }
    operator fun set(platform: Platform?, keys: Collection<TKey>, value: (TKey)->TValue) {
        val valueBuilders = valueBuilders.getOrPut(platform) { hashMapOf() }
        if (keys.isEmpty())
            valueBuilders[null] = value;
        else
            for (key in keys) {
                valueBuilders[key] = value
                onKeySet(key)
            }
    }

}

open class SpecializedProperty<TKey: Any, TValue : Any>() : BaseSpecializedProperty<TKey, TValue>() {
    private val valueBuilders = HashMap<TKey?, ((TKey) -> TValue)>()

    operator fun get(key: TKey): TValue? = (valueBuilders[key] ?: valueBuilders[null])?.let { it(key) }


    operator fun set(keys: Collection<TKey>, value: TValue) {
        if (keys.isEmpty()) default = value
        set(keys, { value })
    }
    operator fun set(keys: Collection<TKey>, value: (TKey)->TValue) {
        if (keys.isEmpty())
            valueBuilders[null] = value;
        else
            for (key in keys) {
                valueBuilders[key] = value
                onKeySet(key)
            }
    }

}

operator fun <TKey: Any, TValue : Any> SpecializedProperty<TKey, TValue>.invoke(vararg keys: TKey, valueBuilder: (TKey) -> TValue) = set(keys.asList(), valueBuilder)
operator fun <TKey: Any, TValue : Any> SpecializedProperty<TKey, TValue>.invoke(value: TValue, vararg keys: TKey) = set(keys.asList(), value)
operator fun <TKey: Any, TValue : Any> PlatformSpecializedProperty<TKey, TValue>.invoke(vararg keys: TKey, valueBuilder: (TKey) -> TValue) = set(null, keys.asList(), valueBuilder)
operator fun <TKey: Any, TValue : Any> PlatformSpecializedProperty<TKey, TValue>.invoke(value: TValue, vararg keys: TKey) = set(null, keys.asList(), value)
operator fun <TKey: Any, TValue : Any> PlatformSpecializedProperty<TKey, TValue>.invoke(platform: Platform, vararg keys: TKey, valueBuilder: (TKey) -> TValue) = set(platform, keys.asList(), valueBuilder)
operator fun <TKey: Any, TValue : Any> PlatformSpecializedProperty<TKey, TValue>.invoke(platform: Platform, value: TValue, vararg keys: TKey) = set(platform, keys.asList(), value)

typealias FamilyProperty<TValue> = SpecializedProperty<Family, TValue>
typealias PlatformProperty<TValue> = SpecializedProperty<Platform, TValue>
typealias PlatformFamilyProperty<TValue> = PlatformSpecializedProperty<Family, TValue>
typealias PlatformPrimitiveProperty<TValue> = PlatformSpecializedProperty<PrimitiveType, TValue>

class DeprecationProperty() : PlatformFamilyProperty<Deprecation>()
operator fun DeprecationProperty.invoke(value: String, vararg keys: Family) = set(null, keys.asList(), Deprecation(value))

class DocProperty() : PlatformFamilyProperty<String>()
operator fun DocProperty.invoke(vararg keys: Family, valueBuilder: DocExtensions.(Family) -> String) = set(null, keys.asList(), { f -> valueBuilder(DocExtensions, f) })

class InlineProperty : PlatformFamilyProperty<Inline>()
operator fun InlineProperty.invoke(vararg keys: Family) = set(null, keys.asList(), Inline.Yes)
operator fun InlineProperty.invoke(value: Boolean, vararg keys: Family) = set(null, keys.asList(), if (value) Inline.Yes else Inline.No)

class ConcreteFunction(val textBuilder: (Appendable) -> Unit, val sourceFile: SourceFile)

class GenericFunction(val signature: String, val keyword: String = "fun") {

    data class TypeParameter(val original: String, val name: String, val constraint: TypeRef? = null) {
        constructor(simpleName: String) : this(simpleName, simpleName)

        data class TypeRef(val name: String, val typeArguments: List<TypeArgument> = emptyList()) {
            fun mentionedTypes(): List<TypeRef> =
                    if (typeArguments.isEmpty()) listOf(this) else typeArguments.flatMap { it.type.mentionedTypes() }
        }

        data class TypeArgument(val type: TypeRef)

        fun mentionedTypeRefs(): List<TypeRef> = constraint?.mentionedTypes().orEmpty()
    }

    val defaultFamilies = Family.defaultFamilies
    val defaultPrimitives = PrimitiveType.defaultPrimitives
    val numericPrimitives = PrimitiveType.numericPrimitives

    var toNullableT: Boolean = false

    val receiverAsterisk = PlatformFamilyProperty<Boolean>()

    val buildFamilies = PlatformProperty<Set<Family>>().apply {
        invoke(defaultFamilies)
    }
    //val buildFamilies = LinkedHashSet(defaultFamilies)
    val buildFamilyPrimitives = PlatformFamilyProperty<Set<PrimitiveType>>().apply {
        invoke(defaultPrimitives)
    }

    val customReceiver = PlatformFamilyProperty<String>()
    val customSignature = PlatformFamilyProperty<String>()
    val deprecate = DeprecationProperty()
    val doc = DocProperty()
    val platformName = PlatformPrimitiveProperty<String>()
    val inline = InlineProperty()
    val jvmOnly = FamilyProperty<Boolean>()
    val since = PlatformFamilyProperty<String>()
    val typeParams = ArrayList<String>()
    val returns = PlatformFamilyProperty<String>()
    val visibility = FamilyProperty<String>()
    val operator = FamilyProperty<Boolean>()
    val infix = FamilyProperty<Boolean>()
    val external = PlatformFamilyProperty<Boolean>()
    val body = object : PlatformFamilyProperty<String>() {
        override fun onKeySet(key: Family) = include(key)
    }
    val customPrimitiveBodies = HashMap<Pair<Family, PrimitiveType>, String>()
    val annotations = PlatformFamilyProperty<String>()
    val sourceFile = FamilyProperty<SourceFile>()
    val sequenceClassification = mutableListOf<SequenceClass>()

    fun bodyForTypes(family: Family, vararg primitiveTypes: PrimitiveType, b: (PrimitiveType) -> String) {
        include(family)
        for (primitive in primitiveTypes) {
            customPrimitiveBodies.put(family to primitive, b(primitive))
        }
    }

    fun typeParam(t: String) {
        typeParams.add(t)
    }

    fun sequenceClassification(vararg sequenceClass: SequenceClass) {
        sequenceClassification.addAll(sequenceClass)
    }

    fun exclude(vararg families: Family) {
        buildFamilies(buildFamilies.default!! - families)
    }

    fun only(vararg families: Family) {
        buildFamilies(families.toSet())
    }

    fun only(platform: Platform, vararg families: Family) {
        require(families.isNotEmpty()) { "Need to specify families" }
        buildFamilies(families.toSet(), platform)
    }

    fun only(vararg primitives: PrimitiveType) {
        only(primitives.asList())
    }

    fun only(primitives: Collection<PrimitiveType>) {
        buildFamilyPrimitives(primitives.toSet())
    }

    fun onlyPrimitives(family: Family, vararg primitives: PrimitiveType) {
        buildFamilyPrimitives(family) { primitives.toSet() }
    }

    fun onlyPrimitives(family: Family, primitives: Set<PrimitiveType>) {
        buildFamilyPrimitives(family) { primitives }
    }

    fun include(vararg families: Family) {
        buildFamilies(buildFamilies.default!! + families)
    }

    fun exclude(vararg p: PrimitiveType) {
        buildFamilyPrimitives(buildFamilyPrimitives.default!! - p)
    }

    fun instantiate(platform: Platform, vararg families: Family = Family.values()): List<ConcreteFunction> {
        return families
                .sortedBy { it.ordinal }
                .filter { buildFamilies[platform]!!.contains(it) }
                .filter { platform == Platform.JVM || jvmOnly[it] != true  }
                .flatMap { family -> instantiate(family, platform) }
    }

    fun instantiate(f: Family, platform: Platform): List<ConcreteFunction> {
        val onlyPrimitives = buildFamilyPrimitives[platform, f]!!

        if (f.isPrimitiveSpecialization || buildFamilyPrimitives.isSpecializedFor(platform, f)) {
            return (onlyPrimitives).sortedBy { it.ordinal }
                    .map { primitive -> ConcreteFunction( { build(it, f, primitive, platform) }, sourceFileFor(f) ) }
        } else {
            return listOf(ConcreteFunction( { build(it, f, null, platform) }, sourceFileFor(f) ))
        }
    }

    private fun sourceFileFor(f: Family) = sourceFile[f] ?: getDefaultSourceFile(f)

    private fun getDefaultSourceFile(f: Family): SourceFile = when (f) {
        Iterables, Collections, Lists -> SourceFile.Collections
        Sequences -> SourceFile.Sequences
        Sets -> SourceFile.Sets
        Ranges, RangesOfPrimitives, ProgressionsOfPrimitives -> SourceFile.Ranges
        ArraysOfObjects, InvariantArraysOfObjects, ArraysOfPrimitives -> SourceFile.Arrays
        Maps -> SourceFile.Maps
        Strings -> SourceFile.Strings
        CharSequences -> SourceFile.Strings
        Primitives, Generic -> SourceFile.Misc
    }

/*
    fun build(vararg families: Family = Family.values()): String {
        val builder = StringBuilder()
        for (family in families.sortedBy { it.name }) {
            if (buildFamilies.contains(family))
                build(builder, family)
        }
        return builder.toString()
    }

    fun build(builder: StringBuilder, f: Family) {
        val onlyPrimitives = buildFamilyPrimitives[f]
        if (f.isPrimitiveSpecialization || onlyPrimitives != null) {
            for (primitive in (onlyPrimitives ?: buildPrimitives).sortedBy { it.name })
                build(builder, f, primitive)
        } else {
            build(builder, f, null)
        }
    }
*/

    fun build(builder: Appendable, f: Family, primitive: PrimitiveType?, platform: Platform) {
        val headerOnly: Boolean = platform == Platform.Common
        val hasOptionalParams = (customSignature[platform, f] ?: signature).contains("=")
        val returnType = returns[platform, f] ?: throw RuntimeException("No return type specified for $signature")

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
                                      when (f) {
                                          CharSequences, Strings -> "Appendable"
                                          else -> renderType("MutableCollection<in T>", receiver, self)
                                      }
                                  }
                                  "T" -> {
                                      when (f) {
                                          Generic -> "T"
                                          CharSequences, Strings -> "Char"
                                          Maps -> "Map.Entry<K, V>"
                                          else -> primitive?.name ?: token
                                      }
                                  }
                                  "TRange" -> {
                                      when (f) {
                                          Generic -> "Range<T>"
                                          else -> primitive!!.name + "Range"
                                      }
                                  }
                                  "TProgression" -> {
                                      when (f) {
                                          Generic -> "Progression<out T>"
                                          else -> primitive!!.name + "Progression"
                                      }
                                  }
                                  else -> token
                              })
            }

            return answer.toString()
        }

        val isAsteriskOrT = if (receiverAsterisk[platform, f] == true) "*" else "T"
        val self = (when (f) {
            Iterables -> "Iterable<$isAsteriskOrT>"
            Collections -> "Collection<$isAsteriskOrT>"
            Lists -> "List<$isAsteriskOrT>"
            Maps -> "Map<out K, V>"
            Sets -> "Set<$isAsteriskOrT>"
            Sequences -> "Sequence<$isAsteriskOrT>"
            InvariantArraysOfObjects -> "Array<T>"
            ArraysOfObjects -> "Array<${isAsteriskOrT.replace("T", "out T")}>"
            Strings -> "String"
            CharSequences -> "CharSequence"
            Ranges -> "ClosedRange<$isAsteriskOrT>"
            ArraysOfPrimitives -> primitive?.let { it.name + "Array" } ?: throw IllegalArgumentException("Primitive array should specify primitive type")
            RangesOfPrimitives -> primitive?.let { it.name + "Range" } ?: throw IllegalArgumentException("Primitive range should specify primitive type")
            ProgressionsOfPrimitives -> primitive?.let { it.name + "Progression" } ?: throw IllegalArgumentException("Primitive progression should specify primitive type")
            Primitives -> primitive?.let { it.name } ?: throw IllegalArgumentException("Primitive should specify primitive type")
            Generic -> "T"
        })

        val receiver = (customReceiver[platform, f] ?: self).let { renderType(it, it, self) }

        fun String.renderType(): String = renderType(this, receiver, self)

        fun effectiveTypeParams(): List<TypeParameter> {
            val parameters = typeParams.mapTo(mutableListOf()) { parseTypeParameter(it.renderType()) }

            if (f == Generic) {
                if (parameters.none { it.name == "T" })
                    parameters.add(TypeParameter("T"))
                return parameters
            }
            else if (primitive == null && f != Strings && f != CharSequences) {
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
                val renderedT = "T".renderType()
                return parameters.filterNot { it.name == renderedT }
            }
        }

        doc[platform, f]?.let { methodDoc ->
            builder.append("/**\n")
            StringReader(methodDoc.trim()).forEachLine { line ->
                builder.append(" * ").append(line.trim()).append("\n")
            }
            if (f == Sequences && sequenceClassification.isNotEmpty()) {
                builder.append(" *\n")
                builder.append(" * The operation is ${sequenceClassification.joinToString(" and ") { "_${it.toString().replace('_', ' ')}_" }}.\n")
            }
            builder.append(" */\n")
        }

        deprecate[platform, f]?.let { deprecated ->
            val args = listOfNotNull(
                "\"${deprecated.message}\"",
                deprecated.replaceWith?.let { "ReplaceWith(\"$it\")" },
                deprecated.level.let { if (it != DeprecationLevel.WARNING) "level = DeprecationLevel.$it" else null }
            )
            builder.append("@Deprecated(${args.joinToString(", ")})\n")
        }

        if (!f.isPrimitiveSpecialization && primitive != null) {
            platformName[platform, primitive]
                    ?.replace("<T>", primitive.name)
                    ?.let { platformName -> builder.append("@kotlin.jvm.JvmName(\"${platformName}\")\n")}
        }

        if (jvmOnly[f] ?: false) {
            builder.append("@kotlin.jvm.JvmVersion\n")
        }
        since[platform, f]?.let { since ->
            builder.append("@SinceKotlin(\"$since\")\n")
        }

        annotations[platform, f]?.let { builder.append(it).append('\n') }

        if (inline[platform, f] == Inline.Only) {
            builder.append("@kotlin.internal.InlineOnly").append('\n')
        }

        builder.append(visibility[f] ?: "public").append(' ')
        if (headerOnly && !hasOptionalParams) {
            builder.append("header ")
        }
        if (external[platform, f] == true)
            builder.append("external ")
        if (inline[platform, f]?.isInline() == true)
            builder.append("inline ")
        if (infix[f] == true)
            builder.append("infix ")
        if (operator[f] == true)
            builder.append("operator ")

        builder.append("$keyword ")

        val types = effectiveTypeParams()
        if (!types.isEmpty()) {
            builder.append(types.joinToString(separator = ", ", prefix = "<", postfix = "> ", transform = { it.original }))
        }

        val receiverType = (if (toNullableT) receiver.replace("T>", "T?>") else receiver).renderType()

        builder.append(receiverType)
        if (receiverType.isNotEmpty()) builder.append('.')
        builder.append("${(customSignature[platform, f] ?: signature).renderType()}: ${returnType.renderType()}")

        if (headerOnly && !hasOptionalParams) {
            builder.append("\n\n")
            return
        }

        if (keyword == "fun") builder.append(" {")

        val body = (
                primitive?.let { customPrimitiveBodies[f to primitive] } ?:
                body[platform, f] ?:
                deprecate[platform, f]?.replaceWith?.let { "return $it" } ?:
                throw RuntimeException("No body specified for $signature for ${f to primitive}")
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
        if (keyword == "fun") builder.append("}\n")
        builder.append("\n")
    }

}

infix fun MutableList<GenericFunction>.add(item: GenericFunction) = add(item)
infix fun MutableList<GenericFunction>.addAll(items: Iterable<GenericFunction>) = this.addAll(elements = items)

fun f(signature: String, init: GenericFunction.() -> Unit) = GenericFunction(signature).apply(init)

fun pval(signature: String, init: GenericFunction.() -> Unit) = GenericFunction(signature, "val").apply(init)

fun pvar(signature: String, init: GenericFunction.() -> Unit) = GenericFunction(signature, "var").apply(init)
