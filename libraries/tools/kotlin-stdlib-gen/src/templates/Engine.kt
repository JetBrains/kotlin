package templates

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
    ArraysOfObjects,
    ArraysOfPrimitives,
    Strings,
    RangesOfPrimitives,
    ProgressionsOfPrimitives,
    Primitives,
    Generic;

    val isPrimitiveSpecialization: Boolean by Delegates.lazy { this in listOf(ArraysOfPrimitives, RangesOfPrimitives, ProgressionsOfPrimitives, Primitives) }
}

enum class PrimitiveType(val name: String) {
    Boolean("Boolean"),
    Byte("Byte"),
    Char("Char"),
    Short("Short"),
    Int("Int"),
    Long("Long"),
    Float("Float"),
    Double("Double")
}



class GenericFunction(val signature: String, val keyword: String = "fun") : Comparable<GenericFunction> {

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


    val defaultFamilies = array(Iterables, Sequences, ArraysOfObjects, ArraysOfPrimitives, Strings)
    val defaultPrimitives = PrimitiveType.values()
    val numericPrimitives = array(PrimitiveType.Int, PrimitiveType.Long, PrimitiveType.Byte, PrimitiveType.Short, PrimitiveType.Double, PrimitiveType.Float)

    var toNullableT: Boolean = false

    var receiverAsterisk = false

    val buildFamilies = LinkedHashSet(defaultFamilies.toList())
    val buildPrimitives = LinkedHashSet(defaultPrimitives.toList())

    val deprecate = FamilyProperty<String>()
    val doc = FamilyProperty<String>()
    val platformName = PrimitiveProperty<String>()
    val inline = FamilyProperty<Boolean>()
    val typeParams = ArrayList<String>()
    val returns = FamilyProperty<String>()
    val body = object : FamilyProperty<String>() {
        override fun onKeySet(key: Family) = include(key)
    }
    val customPrimitiveBodies = HashMap<Pair<Family, PrimitiveType>, String>()
    val annotations = FamilyProperty<String>()

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
        buildPrimitives.clear()
        buildPrimitives.addAll(primitives.toList())
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


    fun build(vararg families: Family = Family.values()): String {
        val builder = StringBuilder()
        for (family in families.sortBy { it.name() }) {
            if (buildFamilies.contains(family))
                build(builder, family)
        }
        return builder.toString()
    }

    fun build(builder: StringBuilder, f: Family) {
        if (f.isPrimitiveSpecialization) {
            for (primitive in buildPrimitives.sortBy { it.name() })
                build(builder, f, primitive)
        } else {
            build(builder, f, null)
        }
    }

    fun build(builder: StringBuilder, f: Family, primitive: PrimitiveType?) {
        if (f == Sequences) {
            val text = StringBuilder {
                doBuild(this, f, primitive)
            }.toString()
            builder.append(text)
            builder.appendln()
            if (deprecate[f] == null) // (deprecates[f] == null && deprecate.isEmpty())
                builder.appendln("deprecated(\"Migrate to using Sequence<T> and respective functions\")")
            val streamText = text
                    .replace("Sequence", "Stream")
                    .replace("sequence", "stream")
                    .replace("MultiStream", "Multistream")
            builder.append(streamText)
        } else
            doBuild(builder, f, primitive)
    }

    fun doBuild(builder: StringBuilder, f: Family, primitive: PrimitiveType?) {
        val returnType = returns[f] ?: throw RuntimeException("No return type specified for $signature")

        val isAsteriskOrT = if (receiverAsterisk) "*" else "T"
        val receiver = when (f) {
            Iterables -> "Iterable<$isAsteriskOrT>"
            Collections -> "Collection<$isAsteriskOrT>"
            Lists -> "List<$isAsteriskOrT>"
            Maps -> "Map<K, V>"
            Sequences -> "Sequence<$isAsteriskOrT>"
            ArraysOfObjects -> "Array<$isAsteriskOrT>"
            Strings -> "String"
            ArraysOfPrimitives -> primitive?.let { it.name() + "Array" } ?: throw IllegalArgumentException("Primitive array should specify primitive type")
            RangesOfPrimitives -> primitive?.let { it.name() + "Range" } ?: throw IllegalArgumentException("Primitive range should specify primitive type")
            ProgressionsOfPrimitives -> primitive?.let { it.name() + "Progression" } ?: throw IllegalArgumentException("Primitive progression should specify primitive type")
            Primitives -> primitive?.let { it.name } ?: throw IllegalArgumentException("Primitive should specify primitive type")
            Generic -> "T"
            else -> throw IllegalStateException("Invalid family")
        }


        fun String.renderType(): String {
            val t = StringTokenizer(this, " \t\n,:()<>?.", true)
            val answer = StringBuilder()

            while (t.hasMoreTokens()) {
                val token = t.nextToken()
                answer.append(when (token) {
                                  "SELF" -> if (receiver == "Array<T>") "Array<out T>" else receiver
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
                                      else -> "0"
                                  }
                                  "ONE" -> when (primitive) {
                                      PrimitiveType.Double -> "1.0"
                                      PrimitiveType.Float -> "1.0f"
                                      PrimitiveType.Long -> "1.toLong()"
                                      else -> "1"
                                  }
                                  "-ONE" -> when (primitive) {
                                      PrimitiveType.Double -> "-1.0"
                                      PrimitiveType.Float -> "-1.0f"
                                      PrimitiveType.Long -> "-1.toLong()"
                                      else -> "-1"
                                  }
                                  "TCollection" -> {
                                      when (f) {
                                          Strings -> "Appendable"
                                          else -> "MutableCollection<in T>".renderType()
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

        fun effectiveTypeParams(): List<String> {
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
                val implicitTypeParameters = receiver.dropWhile { it != '<' }.drop(1).filterNot { it == ' ' }.takeWhile { it != '>' }.split(",")
                for (implicit in implicitTypeParameters.reverse()) {
                    if (implicit != "*" && !types.any { it.startsWith(implicit) || it.startsWith("reified " + implicit) }) {
                        types.add(0, implicit)
                    }
                }

                return types
            } else {
                // primitive type arrays should drop constraints
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
            builder.append("deprecated(\"$deprecated\")\n")
        }

        if (!f.isPrimitiveSpecialization && primitive != null) {
            platformName[primitive]
                    ?.replace("<T>", primitive.name)
                    ?.let { platformName -> builder.append("platformName(\"${platformName}\")\n")}
        }

        annotations[f]?.let { builder.append(it).append('\n') }

        builder.append("public ")
        if (inline[f] == true)
            builder.append("inline ")

        builder.append("$keyword ")

        val types = effectiveTypeParams()
        if (!types.isEmpty()) {
            builder.append(types.join(separator = ", ", prefix = "<", postfix = "> ").renderType())
        }

        val receiverType = (when (receiver) {
            "Array<T>" -> if (toNullableT) "Array<out T?>" else "Array<out T>"
            else -> if (toNullableT) receiver.replace("T>", "T?>") else receiver
        }).renderType()


        builder.append(receiverType)
        builder.append(".${signature.renderType()}: ${returnType.renderType()}")
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

    public override fun compareTo(other: GenericFunction): Int = this.signature.compareTo(other.signature)
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
