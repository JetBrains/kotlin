/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.inference

import java.lang.NumberFormatException

/**
 * A part of a [Scheme].
 */
sealed class Item {
    internal abstract val isAnonymous: Boolean
    internal open val isUnspecified: Boolean get() = false
    internal abstract fun toBinding(bindings: Bindings, context: MutableList<Binding>): Binding
    internal abstract fun serializeTo(writer: SchemeStringSerializationWriter)
}

/**
 * A bound part of a [Scheme] that is bound to [value].
 */
class Token(val value: String) : Item() {
    override val isAnonymous: Boolean get() = false
    override fun toBinding(bindings: Bindings, context: MutableList<Binding>): Binding =
        bindings.closed(value)
    override fun toString() = value
    override fun equals(other: Any?) = other is Token && other.value == value
    override fun hashCode(): Int = value.hashCode() * 31
    override fun serializeTo(writer: SchemeStringSerializationWriter) { writer.writeToken(value) }
}

/**
 * An open part of a [Scheme]. All [Open] items with the same non-negative index should be bound
 * together to the  same applier. [Open] items with a negative index are considered anonymous and
 * are treated as independent.
 */
class Open(val index: Int, override val isUnspecified: Boolean = false) : Item() {
    override val isAnonymous: Boolean get() = index < 0
    override fun toBinding(bindings: Bindings, context: MutableList<Binding>): Binding {
        if (index < 0) return bindings.open()
        while (index >= context.size) {
            context.add(bindings.open())
        }
        return context[index]
    }

    override fun toString() = if (index < 0) "_" else "$index"
    override fun equals(other: Any?) =
        other is Open && (other.index == index || (other.index < 0 && index < 0))
    override fun hashCode(): Int = if (index < 0) -31 else index * 31
    override fun serializeTo(writer: SchemeStringSerializationWriter) { writer.writeNumber(index) }
}

/**
 * A [Scheme] declares the applier the type expects and which appliers are expected of the
 * lambda parameters of a function or bound callable types of a generic type. The applier can be
 * open but all open appliers of the same non-negative index must be bound to the same applier.
 *
 * All non-composable lambda parameters of a function type are ignored and skipped when producing
 * or interpreting a scheme. Also if the result is not a composable lambda the result is `null`.
 * This is because inference is only inferring what applier is being used by the `$composer`
 * passed as a parameter of a function. Note that a lambda that captures a `$composer` in context
 * (such as the lambda passed to [forEach] in a composable function) has a scheme as if the
 * `$composer` captured was passed in as a parameter just like it was a composable lambda.
 */
class Scheme(
    val target: Item,
    val parameters: List<Scheme> = emptyList(),
    val result: Scheme? = null,
    val anyParameters: Boolean = false
) {
    init {
        check(!anyParameters || parameters.isEmpty()) {
            "`anyParameters` == true must have empty parameters"
        }
    }

    /**
     * Produce a string serialization of the scheme. This is not necessarily readable, use
     * [toString] for debugging instead.
     */
    fun serialize(): String = buildString { serializeTo(SchemeStringSerializationWriter(this)) }

    override fun toString(): String = "[$target$parametersStr$resultStr]"

    private val parametersStr get() =
        if (parameters.isEmpty()) ""
        else ", ${parameters.joinToString(", ") { it.toString() }}"

    private val resultStr get() = result?.let { ": $it" } ?: ""

    /**
     * Compare to [Scheme] instances for equality. Two [Scheme]s are considered equal if they are
     * [alpha equivalent][https://en.wikipedia.org/wiki/Lambda_calculus#%CE%B1-conversion]. This
     * is accomplished by normalizing both schemes and then comparing them simply for equality.
     * See [alphaRename] for details.
     */
    override fun equals(other: Any?): Boolean {
        val o = other as? Scheme ?: return false
        return this.alphaRename().simpleEquals(o.alphaRename())
    }

    fun canOverride(other: Scheme): Boolean = alphaRename().simpleCanOverride(other.alphaRename())

    override fun hashCode(): Int = alphaRename().simpleHashCode()

    private fun simpleCanOverride(other: Scheme): Boolean {
        return if (other.target is Open) {
            target is Open && other.target.index == target.index
        } else {
            target.isUnspecified || target == other.target
        } && parameters.zip(other.parameters).all { (a, b) -> a.simpleCanOverride(b) } &&
            (
                result == other.result ||
                    (other.result != null && result != null && result.canOverride((other.result)))
            )
    }

    private fun simpleEquals(other: Scheme) =
        target == other.target && parameters.zip(other.parameters).all { (a, b) -> a == b } &&
            result == result

    private fun simpleHashCode(): Int =
        target.hashCode() * 31 + parameters.hashOfElements() + (result?.hashCode() ?: 0)

    private fun List<Scheme>.hashOfElements() = if (isEmpty()) 0 else
        map { it.simpleHashCode() }.reduceRight { h, acc -> h + acc * 31 }

    private fun serializeTo(writer: SchemeStringSerializationWriter) {
        writer.writeOpen()
        target.serializeTo(writer)
        if (anyParameters) {
            writer.writeAnyParameters()
        } else {
            parameters.forEach { it.serializeTo(writer) }
        }
        if (result != null) {
            writer.writeResultPrefix()
            result.serializeTo(writer)
        }
        writer.writeClose()
    }

    /**
     * Both hashCode and equals are in terms of alpha rename equivalents. That means that the scheme
     * [0, [0]] and [2, [2]] should be treated as equal even though they have different indexes
     * because they are alpha rename equivalent. This method will rename all variables
     * consistently so that if they are alpha equivalent then they will have the same open
     * indexes in the same location. If the scheme is already alpha rename consistent then this is
     * returned.
     */
    private fun alphaRename(): Scheme {
        // Special case where the scheme would always be renamed to itself.
        if ((target !is Open || target.index in -1..0) && parameters.isEmpty()) return this

        // Calculate what the renames should be
        val alphaRenameMap = mutableMapOf<Int, Int>()
        var next = 0
        fun scan(scheme: Scheme) {
            val target = scheme.target
            val parameters = scheme.parameters
            val result = scheme.result
            if (target is Open) {
                val index = target.index
                if (index in alphaRenameMap) {
                    if (index >= 0 && alphaRenameMap[index] == -1)
                        alphaRenameMap[index] = next++
                } else alphaRenameMap[index] = -1
            }
            parameters.forEach { scan(it) }
            result?.let { scan(it) }
        }
        scan(this)

        // if no renames found, this is an entirely bound scheme, just return it
        if (alphaRenameMap.isEmpty()) return this

        fun rename(scheme: Scheme): Scheme {
            val target = scheme.target
            val parameters = scheme.parameters
            val result = scheme.result
            val newTarget = if (target is Open && target.index != alphaRenameMap[target.index])
                Open(alphaRenameMap[target.index]!!)
            else target
            val newParameters = parameters.map { rename(it) }
            val newResult = result?.let { rename(it) }
            return if (
                target !== newTarget || newParameters.zip(parameters).any { (a, b) ->
                    a !== b
                } || newResult != result
            ) Scheme(newTarget, newParameters, newResult)
            else scheme
        }
        return rename(this)
    }
}

private class SchemeParseError : Exception("Internal scheme parse error")

private fun schemeParseError(): Nothing { throw SchemeParseError() }

/**
 * Given a string produce a [Scheme] if the string is a valid serialization of a [Scheme] or null
 * otherwise.
 */
fun deserializeScheme(value: String): Scheme? {
    val reader = SchemeStringSerializationReader(value)

    fun item(): Item = when (reader.kind) {
        ItemKind.Token -> Token(reader.token())
        ItemKind.Number -> Open(reader.number())
        else -> schemeParseError()
    }

    fun <T> list(content: () -> T): List<T> {
        if (reader.kind != ItemKind.Open) return emptyList()
        val result = mutableListOf<T>()
        while (reader.kind == ItemKind.Open) {
            result.add(content())
        }
        return result
    }

    fun <T> delimited(
        prefix: ItemKind,
        postfix: ItemKind,
        content: () -> T
    ) = run {
        reader.expect(prefix)
        content().also {
            reader.expect(postfix)
        }
    }

    fun <T> optional(
        prefix: ItemKind,
        postfix: ItemKind = ItemKind.Invalid,
        content: () -> T
    ): T? =
        if (reader.kind == prefix) {
            delimited(prefix, postfix, content)
        } else null

    fun isItem(kind: ItemKind): Boolean =
        if (reader.kind == kind) {
            reader.expect(kind)
            true
        } else false

    fun scheme(): Scheme =
        delimited(ItemKind.Open, ItemKind.Close) {
            val target = item()
            val anyParameters = isItem(ItemKind.AnyParameters)
            val parameters = if (anyParameters) emptyList() else list { scheme() }
            val result = optional(ItemKind.ResultPrefix) { scheme() }
            Scheme(target, parameters, result, anyParameters)
        }

    return try {
        scheme().also { reader.end() }
    } catch (_: SchemeParseError) {
        null
    }
}

internal class SchemeStringSerializationWriter(private val builder: StringBuilder) {

    fun writeToken(token: String) {
        if (isNormal(token)) {
            builder.append(token)
        } else {
            builder.append('"')
            builder.append(token.replace("\\", "\\\\").replace("\"", "\\\""))
            builder.append('"')
        }
    }

    fun writeNumber(number: Int) {
        if (number < 0) {
            builder.append('_')
        } else {
            builder.append(number)
        }
    }

    fun writeOpen() { builder.append('[') }
    fun writeClose() { builder.append(']') }
    fun writeResultPrefix() { builder.append(':') }
    fun writeAnyParameters() { builder.append('*') }

    override fun toString(): String = builder.toString()

    private fun isNormal(value: String) = value.all { it == '.' || it.isLetter() }
}

private enum class ItemKind {
    Open,
    Close,
    ResultPrefix,
    AnyParameters,
    Token,
    Number,
    End,
    Invalid
}

private const val eos = '\u0000'

private class SchemeStringSerializationReader(private val value: String) {
    private var current = 0

    val kind: ItemKind get() =
        when (val ch = ch) {
            '_' -> ItemKind.Number
            '[' -> ItemKind.Open
            ']' -> ItemKind.Close
            ':' -> ItemKind.ResultPrefix
            '*' -> ItemKind.AnyParameters
            '"' -> ItemKind.Token
            else -> {
                when {
                    ch.isLetter() -> ItemKind.Token
                    ch.isDigit() -> ItemKind.Number
                    ch == eos -> ItemKind.End
                    else -> ItemKind.Invalid
                }
            }
        }

    fun end() {
        if (kind != ItemKind.End)
            schemeParseError()
    }

    fun number(): Int {
        if (ch == '_') {
            current++
            return -1
        }
        val start = current
        while (ch.isDigit())
            current++
        return try {
            Integer.parseUnsignedInt(value, start, current, 10)
        } catch (_: NumberFormatException) {
            schemeParseError()
        }
    }

    fun token(): String {
        var start = current
        val end: Int
        var prefix = ""
        if (ch == '"') {
            current++
            start = current
            while (ch != '"' && ch != eos) {
                if (ch == '\\') {
                    prefix += value.subSequence(start, current).toString()
                    current++
                    start = current
                    if (ch == '\"' || ch == '\\') {
                        current++
                    } else {
                        schemeParseError()
                    }
                } else {
                    current++
                }
            }
            end = current
            current++
        } else {
            while (run { val ch = ch; ch == '.' || ch.isLetter() }) current++
            end = current
        }
        return prefix + value.subSequence(start, end).toString()
    }

    fun expect(kind: ItemKind) {
        if (kind != ItemKind.Invalid) {
            if (this.kind != kind) {
                schemeParseError()
            }
            when (this.kind) {
                ItemKind.Open -> expect('[')
                ItemKind.Close -> expect(']')
                ItemKind.ResultPrefix -> expect(':')
                ItemKind.AnyParameters -> expect('*')
                ItemKind.Token -> token()
                ItemKind.Number -> number()
                ItemKind.End -> end()
                else -> schemeParseError()
            }
        }
    }

    private val ch: Char get() = if (current < value.length) value[current] else eos

    private fun expect(ch: Char) {
        if (current < value.length && value[current] == ch) {
            current++
        } else {
            schemeParseError()
        }
    }
}