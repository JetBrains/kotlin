package templates

import java.util.ArrayList
import templates.Family.*
import java.util.HashSet
import java.util.HashMap
import java.io.StringReader
import java.util.StringTokenizer

enum class Family {
    Iterators
    Iterables
    Collections
    Arrays
    PrimitiveArrays
}

class GenericFunction(val signature : String) {
    var doc : String = ""
    var toNullableT : Boolean = false
    val isInline : Boolean = true;
    val blockedFor = HashSet<Family>()
    val bodies = HashMap<Family, String>()
    val returnTypes = HashMap<Family, String>()
    val typeParams = ArrayList<String>()

    fun body(b : () -> String) {
        for (f in Family.values()) {
            if (bodies[f] == null) f.body(b)
        }
    }

    fun Family.body(b : () -> String) {
        bodies[this] = b()
    }

    fun returns(r : String) {
        for (f in Family.values()) {
            if (returnTypes[f] == null) f.returns(r)
        }
    }

    fun Family.returns(r:String) {
        returnTypes[this] = r
    }

    fun typeParam(t:String) {
        typeParams.add(t)
    }

    fun absentFor(vararg f : Family) {
        blockedFor.addAll(f.toCollection())
    }

    private fun effectiveTypeParams(f : Family) : List<String> {
        val types = ArrayList(typeParams)
        if (typeParams.find { it.startsWith("T") } == null) {
            types.add(0, "T")
        }

        if (f == PrimitiveArrays) {
            types.remove(types.find { it.startsWith("T") })
        }

        return types
    }



    fun buildFor(f: Family, arrName : String = "") : String {
        if (blockedFor.contains(f)) return ""

        if (returnTypes[f] == null) throw RuntimeException("No return type specified for $signature")
        val retType = returnTypes[f]!!

        val selftype = when (f) {
            Iterables -> "Iterable<T>"
            Collections -> "Collection<T>"
            Iterators -> "Iterator<T>"
            Arrays -> "Array<out T>"
            PrimitiveArrays -> "${arrName}Array"
        }

        fun String.renderType() : String {
            val t = StringTokenizer(this, " \t\n,:()<>?.", true)
            val answer = StringBuilder()

            while (t.hasMoreTokens()) {
                val token = t.nextToken()
                answer.append(when (token) {
                    "SELF" -> selftype
                    "T" -> if (f == Family.PrimitiveArrays) arrName else token
                    else -> token
                })
            }

            return answer.toString()
        }

        val builder = StringBuilder()
        if (doc != "") {
            builder.append("/**\n")
            StringReader(doc).forEachLine {
                val line = it.trim()
                if (!line.isEmpty()) {
                    builder.append(" * ").append(line).append("\n")
                }
            }
            builder.append(" */\n")
        }

        builder.append("public ")
        if (isInline) builder.append("inline ")

        builder.append("fun ")

        val types = effectiveTypeParams(f)

        if (!types.isEmpty()) {
            builder.append(types.makeString(separator = ", ", prefix = "<", postfix = "> ").renderType())
        }

        builder.append((
        if (toNullableT) {
            selftype.replace("T>", "T?>")
        }
        else {
            selftype
        }).renderType())

        builder.append(".${signature.renderType()} : ${retType.renderType()} {")

        val body = bodies[f]!!.trim("\n")
        val prefix : Int = body.takeWhile { it == ' ' }.length

        StringReader(body).forEachLine {
            builder.append('\n')
            var count = prefix
            builder.append("    ").append(it.dropWhile {count-- > 0 && it == ' '} .renderType())
        }

        return builder.toString().trimTrailingSpaces() + "\n}\n\n"
    }
}

fun String.trimTrailingSpaces() : String {
    var answer = this;
    while (answer.endsWith(' ') || answer.endsWith('\n')) answer = answer.substring(0, answer.length() - 1)
    return answer
}

val templates = ArrayList<GenericFunction>()

fun f(signature : String, init : GenericFunction.() -> Unit) {
    val gf = GenericFunction(signature)
    gf.init()
    templates.add(gf)
}

fun main(args : Array<String>) {
    collections()
    for (t in templates) {
        print(t.buildFor(PrimitiveArrays, "Byte"))
    }
}
