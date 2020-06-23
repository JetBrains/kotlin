/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.Family.*
import java.lang.IllegalArgumentException
import java.util.*

@TemplateDsl
class TestBuilder(
    allowedPlatforms: Set<Platform>,
    target: KotlinTarget,
    family: Family,
    primitive: PrimitiveType? = null
) : TemplateBuilderBase(allowedPlatforms, target, family, primitive) {

    override val hasPlatformSpecializations: Boolean get() = false

    var body: String? = null; private set

    fun body(valueBuilder: TestExtension.() -> String) {
        body = valueBuilder(test)
    }
    fun body(f: Family, valueBuilder: TestExtension.() -> String) {
        if (family == f) {
            body(valueBuilder)
        }
    }
    fun body(p: PrimitiveType, valueBuilder: TestExtension.() -> String) {
        if (primitive == p) {
            body(valueBuilder)
        }
    }
    fun body(vararg families: Family, valueBuilder: TestExtension.() -> String) {
        if (family in families) {
            body(valueBuilder)
        }
    }
    fun body(vararg primitives: PrimitiveType, valueBuilder: TestExtension.() -> String) {
        if (primitive in primitives) {
            body(valueBuilder)
        }
    }


    fun bodyAppend(valueBuilder: TestExtension.() -> String) {
        body += "\n" + valueBuilder(test)
    }
    fun bodyAppend(f: Family, valueBuilder: TestExtension.() -> String) {
        if (family == f) {
            bodyAppend(valueBuilder)
        }
    }
    fun bodyAppend(p: PrimitiveType, valueBuilder: TestExtension.() -> String) {
        if (primitive == p) {
            bodyAppend(valueBuilder)
        }
    }
    fun bodyAppend(vararg families: Family, valueBuilder: TestExtension.() -> String) {
        if (family in families) {
            bodyAppend(valueBuilder)
        }
    }
    fun bodyAppend(vararg primitives: PrimitiveType, valueBuilder: TestExtension.() -> String) {
        if (primitive in primitives) {
            bodyAppend(valueBuilder)
        }
    }

    fun on(platform: Platform, action: () -> Unit) {
        require(platform in allowedPlatforms) { "Platform $platform is not in the list of allowed platforms $allowedPlatforms" }
        if (target.platform == platform)
            action()
    }

    // toPrimitiveArray
    // runningFold
    // reverseRange
    // sortedTests
    // fill

    override fun build(builder: Appendable) {

        fun Appendable.appendIndented(csq: CharSequence): Appendable = append("    ").append(csq)

        annotations.forEach { builder.appendIndented(it.trimIndent()).append('\n') }

        if (suppressions.isNotEmpty()) {
            suppressions.joinTo(builder, separator = ", ", prefix = "@Suppress(", postfix = ")\n") {
                """"$it""""
            }
        }

        builder.appendIndented("@Test\n")

        val testNameSuffix = when (f) {
            ArraysOfPrimitives, ArraysOfUnsigned, ArraysOfObjects -> (primitive?.name ?: "") + "Array"
            Primitives, Unsigned -> primitive!!.name
            InvariantArraysOfObjects -> throw IllegalArgumentException("$signature test: invariant arrays are not supported yet")
            else -> f.name.let { if (it.last() == 's') it.dropLast(1) else it }
        }

        val fullSignature = signature.indexOf('(').let { "${signature.substring(0 until it)}_$testNameSuffix${signature.substring(it)}" }

        builder.appendIndented("fun $fullSignature {")

        val body = (body ?:
        """TODO("Body is not provided")""".also { System.err.println("ERROR: $fullSignature for ${target.fullName}: no body specified for ${family to primitive}") }
                ).trim('\n')
        val indent: Int = body.takeWhile { it == ' ' }.length

        builder.append('\n')
        body.lineSequence().forEach {
            var count = indent
            val line = it.dropWhile { count-- > 0 && it == ' ' }.replaceKeywords()
            if (line.isNotEmpty()) {
                builder.appendIndented("    ").append(line).append("\n")
            }
        }

        builder.appendIndented("}\n\n")
    }

    private fun String.replaceKeywords(): String {
        val t = StringTokenizer(this, " \t\n,:()<>?.", true)
        val answer = StringBuilder()

        while (t.hasMoreTokens()) {
            val token = t.nextToken()
            answer.append(
                when (token) {
                    "PRIMITIVE" -> primitive?.name ?: token
                    "ZERO" -> test.literal(0)
                    "-ZERO" -> "-" + test.literal(0)
                    "ONE" -> test.literal(1)
                    "-ONE" -> test.literal(-1)
                    "TWO" -> test.literal(2)
                    "THREE" -> test.literal(3)
                    "MAX_VALUE" -> test.P + ".MAX_VALUE"
                    "MIN_VALUE" -> test.P + ".MIN_VALUE"
                    "-MAX_VALUE" -> "-" + test.P + ".MAX_VALUE"
                    "-MIN_VALUE" -> "-" + test.P + ".MIN_VALUE"
                    "POSITIVE_INFINITY" -> test.P + ".POSITIVE_INFINITY"
                    "NEGATIVE_INFINITY" -> test.P + ".NEGATIVE_INFINITY"
                    else -> token
                }
            )
        }

        return answer.toString()
    }

    val test = TestExtension()

    inner class TestExtension {
        fun Family.of(vararg values: Int): String = when (this) {
            ArraysOfPrimitives, ArraysOfUnsigned -> "$of(${values.joinToString { literal(it) }})"
            else -> "$of<$P>(${values.joinToString { literal(it) }})"
        }

        fun of(vararg values: Int): String = family.of(*values)

        val Family.of: String
            get() = when (this) {
                ArraysOfPrimitives, ArraysOfUnsigned -> "${primitive!!.name.toLowerCase()}ArrayOf"
                ArraysOfObjects -> "arrayOf"
                Iterables, Lists -> "listOf"
                Sequences -> "sequenceOf"
                else -> throw IllegalArgumentException(this.toString())
            }

        val of: String
            get() = family.of

        val mutableOf: String
            get() = when (family) {
                Lists, Iterables -> "mutableListOf"
                ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned -> of
                else -> throw IllegalArgumentException(this.toString())
            }

        val P: String
            get() = primitive?.name ?: "Int"

        val Family.assertEquals: String
            get() = when (this) {
                ArraysOfPrimitives, ArraysOfUnsigned, ArraysOfObjects -> "assertArrayContentEquals"
                else -> "assertEquals"
            }

        val assertEquals: String
            get() = family.assertEquals

        fun literal(value: Int): String {
            return when (primitive) {
                PrimitiveType.Byte, PrimitiveType.Short, PrimitiveType.Int -> "$value"
                PrimitiveType.Long -> "${value}L"
                PrimitiveType.UByte, PrimitiveType.UShort, PrimitiveType.UInt -> "${value}u"
                PrimitiveType.ULong -> "${value}uL"
                PrimitiveType.Float -> "$value.0f"
                PrimitiveType.Double -> "$value.0"
                else -> "$value"
            }
        }

        fun toP(value: Int): String {
            return when (primitive) {
                PrimitiveType.Byte, PrimitiveType.Short, PrimitiveType.UByte, PrimitiveType.UShort -> "$value.to$P()"
                else -> literal(value)
            }
        }

        fun toString(value: Int): String {
            return when (primitive) {
                PrimitiveType.Float, PrimitiveType.Double -> "$value.0"
                else -> "$value"
            }
        }


        @UseExperimental(ExperimentalUnsignedTypes::class)
        val PrimitiveType.SIZE_BITS: Int
            get() = when (this) {
                PrimitiveType.Byte -> Byte.SIZE_BITS
                PrimitiveType.Short -> Short.SIZE_BITS
                PrimitiveType.Int -> Int.SIZE_BITS
                PrimitiveType.Long -> Long.SIZE_BITS
                PrimitiveType.Float -> Float.SIZE_BITS
                PrimitiveType.Double -> Double.SIZE_BITS
                PrimitiveType.Boolean -> throw IllegalArgumentException(this.toString())
                PrimitiveType.Char -> Char.SIZE_BITS
                PrimitiveType.UByte -> UByte.SIZE_BITS
                PrimitiveType.UShort -> UShort.SIZE_BITS
                PrimitiveType.UInt -> UInt.SIZE_BITS
                PrimitiveType.ULong -> ULong.SIZE_BITS
            }

        fun PrimitiveType.randomNext(): String = randomNext(range = "")

        fun PrimitiveType.randomNextFrom(from: Int): String =
            if (this == PrimitiveType.Boolean)
                throw IllegalArgumentException(this.toString())
            else if (name.endsWith("Int") || name.endsWith("Long"))
                randomNext(range = "${literal(from)}..MAX_VALUE")
            else if (this == PrimitiveType.Double)
                randomNext(range = "$from.0, MAX_VALUE")
            else if (this == PrimitiveType.Float)
                randomNext(range = "$from.0, MAX_VALUE.toDouble()")
            else
                randomNext(range = "$from..MAX_VALUE.toInt()")

        fun PrimitiveType.randomNextUntil(until: Int): String =
            if (this == PrimitiveType.Boolean)
                throw IllegalArgumentException(this.toString())
            else if (name.endsWith("Int") || name.endsWith("Long"))
                randomNext(range = "MIN_VALUE, ${literal(until)}")
            else if (this == PrimitiveType.Double)
                randomNext(range = "MIN_VALUE, $until.0")
            else if (this == PrimitiveType.Float)
                randomNext(range = "MIN_VALUE.toDouble(), $until.0")
            else
                randomNext(range = "MIN_VALUE.toInt(), $until")

        private fun PrimitiveType.randomNext(range: String): String =
            "Random." + if (name.endsWith("Int") || name.endsWith("Long") || this == PrimitiveType.Double)
                "next$name($range)"
            else if (this == PrimitiveType.Boolean) {
                check(range.isEmpty())
                "nextBoolean()"
            } else if (this == PrimitiveType.Float)
                if (range.isEmpty()) "nextFloat()" else "nextDouble($range).toFloat()"
            else
                "nextInt($range).to$name()"
    }
}


fun Family.isArray(): Boolean = when (this) {
    ArraysOfObjects, InvariantArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned -> true
    else -> false
}

