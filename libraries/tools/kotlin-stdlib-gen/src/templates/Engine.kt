package templates

import java.util.ArrayList
import templates.Family.*
import java.util.HashSet
import java.util.HashMap
import java.io.StringReader
import java.util.StringTokenizer

enum class Family {
    Streams
    Iterables
    Collections
    Lists
    Maps
    ArraysOfObjects
    ArraysOfPrimitives
    Strings
}

enum class PrimitiveType(val name: String) {
    Boolean: PrimitiveType("Boolean")
    Byte: PrimitiveType("Byte")
    Char: PrimitiveType("Char")
    Short: PrimitiveType("Short")
    Int: PrimitiveType("Int")
    Long: PrimitiveType("Long")
    Float: PrimitiveType("Float")
    Double: PrimitiveType("Double")
}


class GenericFunction(val signature: String) : Comparable<GenericFunction> {
    val defaultFamilies = array(Iterables, Streams, ArraysOfObjects, ArraysOfPrimitives, Strings)

    var toNullableT: Boolean = false

    var defaultInline = false
    var receiverAsterisk = false
    val inlineFamilies = HashMap<Family, Boolean>()

    val buildFamilies = HashSet<Family>(defaultFamilies.toList())
    private val buildPrimitives = HashSet<PrimitiveType>(PrimitiveType.values().toList())

    var deprecate: String = ""
    val deprecates = HashMap<Family, String>()

    var doc: String = ""
    val docs = HashMap<Family, String>()

    var defaultBody: String = ""
    val bodies = HashMap<Family, String>()

    var defaultReturnType = ""
    val returnTypes = HashMap<Family, String>()

    val typeParams = ArrayList<String>()

    fun body(vararg families: Family, b: () -> String) {
        if (families.isEmpty())
            defaultBody = b()
        else {
            for (f in families) {
                include(f)
                bodies[f] = b()
            }
        }
    }

    fun doc(vararg families: Family, b: () -> String) {
        if (families.isEmpty())
            doc = b()
        else {
            for (f in families) {
                docs[f] = b()
            }
        }
    }

    fun deprecate(vararg families: Family, b: () -> String) {
        if (families.isEmpty())
            deprecate = b()
        else {
            for (f in families) {
                deprecates[f] = b()
            }
        }
    }

    fun returns(vararg families: Family, b: () -> String) {
        if (families.isEmpty())
            defaultReturnType = b()
        else {
            for (f in families) {
                returnTypes[f] = b()
            }
        }
    }

    fun returns(r: String) {
        defaultReturnType = r
    }

    fun typeParam(t: String) {
        typeParams.add(t)
    }

    fun receiverAsterisk(v: Boolean) {
        receiverAsterisk = true
    }

    fun inline(value: Boolean, vararg families: Family) {
        if (families.isEmpty())
            defaultInline = value
        else
            for (f in families)
                inlineFamilies.put(f, value)
    }

    fun exclude(vararg families: Family) {
        buildFamilies.removeAll(families.toList())
    }

    fun only(vararg families: Family) {
        buildFamilies.clear()
        buildFamilies.addAll(families.toList())
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
        if (f == ArraysOfPrimitives) {
            for (primitive in buildPrimitives.sortBy { it.name() })
                build(builder, f, primitive)
        } else {
            build(builder, f, null)
        }
    }

    fun build(builder: StringBuilder, f: Family, primitive: PrimitiveType?) {
        val returnType = returnTypes[f] ?: defaultReturnType
        if (returnType.isEmpty())
            throw RuntimeException("No return type specified for $signature")

        val isAsteriskOrT = if (receiverAsterisk) "*" else "T"
        val receiver = when (f) {
            Iterables -> "Iterable<$isAsteriskOrT>"
            Collections -> "Collection<$isAsteriskOrT>"
            Lists -> "List<$isAsteriskOrT>"
            Maps -> "Map<K, V>"
            Streams -> "Stream<$isAsteriskOrT>"
            ArraysOfObjects -> "Array<$isAsteriskOrT>"
            Strings -> "String"
            ArraysOfPrimitives -> primitive?.let { it.name() + "Array" } ?: throw IllegalArgumentException("Primitive array should specify primitive type")
            else -> throw IllegalStateException("Invalid family")
        }


        fun String.renderType(): String {
            val t = StringTokenizer(this, " \t\n,:()<>?.", true)
            val answer = StringBuilder()

            while (t.hasMoreTokens()) {
                val token = t.nextToken()
                answer.append(when (token) {
                                  "SELF" -> receiver
                                  "PRIMITIVE" -> primitive?.name() ?: token
                                  "SUM" -> {
                                      when (primitive) {
                                          PrimitiveType.Byte, PrimitiveType.Short -> "Int"
                                          else -> primitive
                                      }
                                  }
                                  "ZERO" -> when (primitive) {
                                      PrimitiveType.Double -> "0.0"
                                      PrimitiveType.Float -> "0.0f"
                                      else -> "0"
                                  }
                                  "TCollection" -> {
                                      when (f) {
                                          Strings -> "Appendable"
                                          else -> "MutableCollection<in T>".renderType()
                                      }
                                  }
                                  "T" -> {
                                      when (f) {
                                          Strings -> "Char"
                                          Maps -> "Map.Entry<K, V>"
                                          else -> primitive?.name() ?: token
                                      }
                                  }
                                  else -> token
                              })
            }

            return answer.toString()
        }

        fun effectiveTypeParams(): List<String> {
            val types = ArrayList(typeParams)
            if (primitive == null && f != Strings) {
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

        val methodDoc = docs[f] ?: doc
        if (methodDoc != "") {
            builder.append("/**\n")
            StringReader(methodDoc).forEachLine {
                val line = it.trim()
                if (!line.isEmpty()) {
                    builder.append(" * ").append(line).append("\n")
                }
            }
            builder.append(" */\n")
        }
        val deprecated = deprecates[f] ?: deprecate
        if (deprecated != "") {
            builder.append("deprecated(\"$deprecated\")\n")
        }

        builder.append("public ")
        if (inlineFamilies[f] ?: defaultInline)
            builder.append("inline ")

        builder.append("fun ")

        val types = effectiveTypeParams()
        if (!types.isEmpty()) {
            builder.append(types.makeString(separator = ", ", prefix = "<", postfix = "> ").renderType())
        }

        val receiverType = (
        if (toNullableT) {
            receiver.replace("T>", "T?>")
        } else {
            if (receiver == "Array<T>")
                "Array<out T>"
            else
                receiver
        }).renderType()


        builder.append(receiverType)
        builder.append(".${signature.renderType()}: ${returnType.renderType()} {")

        val body = (bodies[f] ?: defaultBody).trim("\n")
        val indent: Int = body.takeWhile { it == ' ' }.length

        builder.append('\n')
        StringReader(body).forEachLine {
            var count = indent
            val line = it.dropWhile { count-- > 0 && it == ' ' } .renderType()
            if (line.isNotEmpty()) {
                builder.append("    ").append(line)
                builder.append("\n")
            }
        }
        builder.append("}\n\n")
    }

    public override fun compareTo(other: GenericFunction): Int = this.signature.compareTo(other.signature)
}

fun String.trimTrailingSpaces(): String {
    var answer = this;
    while (answer.endsWith(' ') || answer.endsWith('\n')) answer = answer.substring(0, answer.length() - 1)
    return answer
}

fun f(signature: String, init: GenericFunction.() -> Unit): GenericFunction {
    val gf = GenericFunction(signature)
    gf.init()
    return gf
}
