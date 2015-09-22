package templates

import generators.SourceFile
import templates.Family.*
import templates.Family.Collections
import java.io.StringReader
import java.util.*
import kotlin.properties.Delegates

enum class Family {
    Sequences,
    Iterables,
    Collections,
    Lists,
    Maps,
    Sets,
    ArraysOfObjects,
    ArraysOfPrimitives,
    InvariantArraysOfObjects,
    Strings,
    Ranges,
    RangesOfPrimitives,
    ProgressionsOfPrimitives,
    Primitives,
    Generic;

    val isPrimitiveSpecialization: Boolean by lazy { this in primitiveSpecializations }

    companion object {
        val primitiveSpecializations = setOf(ArraysOfPrimitives, RangesOfPrimitives, ProgressionsOfPrimitives, Primitives)
        val defaultFamilies = setOf(Iterables, Sequences, ArraysOfObjects, ArraysOfPrimitives, Strings)
    }
}

enum class PrimitiveType {
    Boolean,
    Byte,
    Char,
    Short,
    Int,
    Long,
    Float,
    Double;

    val name: String get() = this.name()

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

class ConcreteFunction(val textBuilder: (Appendable) -> Unit, val sourceFile: SourceFile)

class GenericFunction(val signature: String, val keyword: String = "fun") {

    open class SpecializedProperty<TKey: Any, TValue : Any>() {
        private val values = HashMap<TKey?, TValue>()

        fun get(key: TKey): TValue? = values.getOrElse(key, { values.getOrElse(null, { null }) })

        fun set(keys: Collection<TKey>, value: TValue) {
            if (keys.isEmpty())
                values[null] = value;
            else
                for (key in keys) {
                    values[key] = value
                    onKeySet(key)
                }
        }

        fun invoke(vararg keys: TKey, valueBuilder: ()-> TValue) = set(keys.asList(), valueBuilder())
        fun invoke(value: TValue, vararg keys: TKey) = set(keys.asList(), value)

        protected open fun onKeySet(key: TKey) {}
    }

    open class FamilyProperty<TValue: Any>() : SpecializedProperty<Family, TValue>()
    open class PrimitiveProperty<TValue: Any>() : SpecializedProperty<PrimitiveType, TValue>()


    val defaultFamilies = Family.defaultFamilies
    val defaultPrimitives = PrimitiveType.defaultPrimitives
    val numericPrimitives = PrimitiveType.numericPrimitives

    var toNullableT: Boolean = false

    var receiverAsterisk = false

    val buildFamilies = LinkedHashSet(defaultFamilies)
    val buildPrimitives = LinkedHashSet(defaultPrimitives)
    val buildFamilyPrimitives = FamilyProperty<Set<PrimitiveType>>()

    val customSignature = FamilyProperty<String>()
    val deprecate = FamilyProperty<String>()
    val deprecateReplacement = FamilyProperty<String>()
    val doc = FamilyProperty<String>()
    val platformName = PrimitiveProperty<String>()
    val inline = FamilyProperty<Boolean>()
    val jvmOnly = FamilyProperty<Boolean>()
    val typeParams = ArrayList<String>()
    val returns = FamilyProperty<String>()
    val operator = FamilyProperty<Boolean>()
    val body = object : FamilyProperty<String>() {
        override fun onKeySet(key: Family) = include(key)
    }
    val customPrimitiveBodies = HashMap<Pair<Family, PrimitiveType>, String>()
    val annotations = FamilyProperty<String>()
    val sourceFile = FamilyProperty<SourceFile>()

    fun bodyForTypes(family: Family, vararg primitiveTypes: PrimitiveType, b: () -> String) {
        include(family)
        for (primitive in primitiveTypes) {
            customPrimitiveBodies.put(family to primitive, b())
        }
    }

    fun typeParam(t: String) {
        typeParams.add(t)
    }

    fun receiverAsterisk(v: Boolean) {
        receiverAsterisk = v
    }

    fun exclude(vararg families: Family) {
        buildFamilies.removeAll(families.toList())
    }

    fun only(vararg families: Family) {
        buildFamilies.clear()
        buildFamilies.addAll(families.toList())
    }

    fun only(vararg primitives: PrimitiveType) {
        only(primitives.asList())
    }

    fun only(primitives: Collection<PrimitiveType>) {
        buildPrimitives.clear()
        buildPrimitives.addAll(primitives)
    }

    fun onlyPrimitives(family: Family, vararg primitives: PrimitiveType) {
        buildFamilyPrimitives(family) { primitives.toSet() }
    }

    fun onlyPrimitives(family: Family, primitives: Set<PrimitiveType>) {
        buildFamilyPrimitives(family) { primitives }
    }

    fun include(vararg families: Family) {
        buildFamilies.addAll(families.toList())
    }

    fun exclude(vararg p: PrimitiveType) {
        buildPrimitives.removeAll(p.toList())
    }

    fun include(vararg p: PrimitiveType) {
        buildPrimitives.addAll(p.toList())
    }

    fun instantiate(vararg families: Family = Family.values()): List<ConcreteFunction> {
        return families
                .sortedBy { it.name() }
                .filter { buildFamilies.contains(it) }
                .flatMap { family -> instantiate(family) }
    }

    fun instantiate(f: Family): List<ConcreteFunction> {
        val onlyPrimitives = buildFamilyPrimitives[f]

        if (f.isPrimitiveSpecialization || onlyPrimitives != null) {
            return (onlyPrimitives ?: buildPrimitives).sortedBy { it.name() }
                    .map { primitive -> ConcreteFunction( { build(it, f, primitive) }, sourceFileFor(f) ) }
        } else {
            return listOf(ConcreteFunction( { build(it, f, null) }, sourceFileFor(f) ))
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
        Primitives, Generic -> SourceFile.Misc
    }

    fun build(vararg families: Family = Family.values()): String {
        val builder = StringBuilder()
        for (family in families.sortedBy { it.name() }) {
            if (buildFamilies.contains(family))
                build(builder, family)
        }
        return builder.toString()
    }

    fun build(builder: StringBuilder, f: Family) {
        val onlyPrimitives = buildFamilyPrimitives[f]
        if (f.isPrimitiveSpecialization || onlyPrimitives != null) {
            for (primitive in (onlyPrimitives ?: buildPrimitives).sortedBy { it.name() })
                build(builder, f, primitive)
        } else {
            build(builder, f, null)
        }
    }

    fun build(builder: Appendable, f: Family, primitive: PrimitiveType?) {
        val returnType = returns[f] ?: throw RuntimeException("No return type specified for $signature")

        fun renderType(expression: String, receiver: String): String {
            val t = StringTokenizer(expression, " \t\n,:()<>?.", true)
            val answer = StringBuilder()

            while (t.hasMoreTokens()) {
                val token = t.nextToken()
                answer.append(when (token) {
                                  "SELF" -> receiver
                                  "PRIMITIVE" -> primitive?.name() ?: token
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
                                          Strings -> "Appendable"
                                          else -> renderType("MutableCollection<in T>", receiver)
                                      }
                                  }
                                  "T" -> {
                                      when (f) {
                                          Generic -> "T"
                                          Strings -> "Char"
                                          Maps -> "Map.Entry<K, V>"
                                          else -> primitive?.name() ?: token
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

        val isAsteriskOrT = if (receiverAsterisk) "*" else "T"
        val receiver = when (f) {
            Iterables -> "Iterable<$isAsteriskOrT>"
            Collections -> "Collection<$isAsteriskOrT>"
            Lists -> "List<$isAsteriskOrT>"
            Maps -> "Map<K, V>"
            Sets -> "Set<$isAsteriskOrT>"
            Sequences -> "Sequence<$isAsteriskOrT>"
            InvariantArraysOfObjects -> "Array<T>"
            ArraysOfObjects -> "Array<${isAsteriskOrT.replace("T", "out T")}>"
            Strings -> "String"
            Ranges -> "Range<$isAsteriskOrT>"
            ArraysOfPrimitives -> primitive?.let { it.name() + "Array" } ?: throw IllegalArgumentException("Primitive array should specify primitive type")
            RangesOfPrimitives -> primitive?.let { it.name() + "Range" } ?: throw IllegalArgumentException("Primitive range should specify primitive type")
            ProgressionsOfPrimitives -> primitive?.let { it.name() + "Progression" } ?: throw IllegalArgumentException("Primitive progression should specify primitive type")
            Primitives -> primitive?.let { it.name } ?: throw IllegalArgumentException("Primitive should specify primitive type")
            Generic -> "T"
        }.let { renderType(it, it) }

        fun String.renderType(): String = renderType(this, receiver)

        fun effectiveTypeParams(): List<String> {
            fun getGenericTypeParameters(genericType: String) =
                genericType
                    .dropWhile { it != '<' }
                    .drop(1)
                    .takeWhile { it != '>' }
                    .split(",")
                    .map { it.removePrefix("out").removePrefix("in").trim() }

            // TODO: Model for type parameter
            val types = ArrayList(typeParams)
            if (f == Generic) {
                // ensure type parameter T, if it's not added to typeParams before
                if (!types.any { it == "T" || it.startsWith("T:")}) {
                    types.add("T")
                }
                return types
            }
            else if (primitive == null && f != Strings) {
                val implicitTypeParameters = getGenericTypeParameters(receiver) + types.flatMap { getGenericTypeParameters(it) }
                for (implicit in implicitTypeParameters.reversed()) {
                    if (implicit != "*" && !types.any { it.startsWith(implicit) || it.startsWith("reified " + implicit) }) {
                        types.add(0, implicit)
                    }
                }

                return types
            } else {
                // remove T as parameter
                // TODO: Substitute primitive or String instead of T in other parameters from effective types not from original typeParams
                return typeParams.filter { !it.startsWith("T") }
            }
        }

        doc[f]?.let { methodDoc ->
            builder.append("/**\n")
            StringReader(methodDoc).forEachLine {
                val line = it.trim()
                if (!line.isEmpty()) {
                    builder.append(" * ").append(line).append("\n")
                }
            }
            builder.append(" */\n")
        }

        deprecate[f]?.let { deprecated ->
            val replacement = deprecateReplacement[f]?.let { ", ReplaceWith(\"$it\")" } ?: ""
            builder.append("@Deprecated(\"$deprecated\"$replacement)\n")
        }

        if (!f.isPrimitiveSpecialization && primitive != null) {
            platformName[primitive]
                    ?.replace("<T>", primitive.name)
                    ?.let { platformName -> builder.append("@kotlin.jvm.JvmName(\"${platformName}\")\n")}
        }

        if (jvmOnly[f] ?: false) {
            builder.append("@kotlin.jvm.JvmVersion\n")
        }

        annotations[f]?.let { builder.append(it).append('\n') }

        builder.append("public ")
        if (inline[f] == true)
            builder.append("inline ")
        if (operator[f] == true)
            builder.append("operator ")

        builder.append("$keyword ")

        val types = effectiveTypeParams()
        if (!types.isEmpty()) {
            builder.append(types.join(separator = ", ", prefix = "<", postfix = "> ").renderType())
        }

        val receiverType = (if (toNullableT) receiver.replace("T>", "T?>") else receiver).renderType()

        builder.append(receiverType)
        builder.append(".${(customSignature[f] ?: signature).renderType()}: ${returnType.renderType()}")
        if (keyword == "fun") builder.append(" {")

        val body = (customPrimitiveBodies[f to primitive] ?: body[f] ?: throw RuntimeException("No body specified for $signature for ${f to primitive}")).trim('\n')
        val indent: Int = body.takeWhile { it == ' ' }.length()

        builder.append('\n')
        StringReader(body).forEachLine {
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

fun f(signature: String, init: GenericFunction.() -> Unit): GenericFunction {
    val gf = GenericFunction(signature)
    gf.init()
    return gf
}

fun pval(signature: String, init: GenericFunction.() -> Unit): GenericFunction {
    val gf = GenericFunction(signature, "val")
    gf.init()
    return gf
}

fun pvar(signature: String, init: GenericFunction.() -> Unit): GenericFunction {
    val gf = GenericFunction(signature, "var")
    gf.init()
    return gf
}
