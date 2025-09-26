package hair.ir.generator.toolbox

import kotlin.text.*

interface GenerationScope

interface GenerationScopeWithMembers : GenerationScope

interface ClassGeneration : GenerationScopeWithMembers

fun GenerationScope.generateClass(name: String, generation: ClassGeneration.() -> Unit = {}) {

}

/*
public sealed abstract class Foo private constructor(arg: Type = default, ...) : SuperClass(arg, ...), SuperInterface, ... {
    private public internal val name: Type = expression
    open overwrite fun name(arg: Type = default, ...): Result = expression
    fun name(arg: Type = default, ...): Result {
        statements...
    }
    fun <GENERIC : Bound> ...
    fun ExtansionReceiver.<...>
    class...
    companion object {
        members...
    }
}

+ object : Type {
}

///

fun params("arg" to "Type" withDefault expr(), ...)
 */

// first steps:
fun StringBuilder.appendIndented(indent: String, string: String) {
    string.lines().forEach { appendLine("$indent$it") }
}

class SimpleMembersBuilder {
    private val members = mutableListOf<String>()

    fun member(str: String)  {
        members += str.trim()
    }

    fun member(builderAction: StringBuilder.() -> Unit)  {
        members += StringBuilder().apply(builderAction).toString().trim()
    }

    fun blankLine() {
        member("")
    }

    fun appendTo(stringBuilder: StringBuilder) {
        with (stringBuilder) {
            if (members.isNotEmpty()) {
                appendLine(" {")
                for (member in members) {
                    appendIndented("    ", member)
                }
                appendLine("}")
            } else {
                appendLine()
            }
        }
    }

    // FIXME remove
    fun appendSimple(stringBuilder: StringBuilder) {
        with (stringBuilder) {
            if (members.isNotEmpty()) {
                for (member in members) {
                    appendIndented("", member)
                }
            } else {
                appendLine()
            }
        }
    }
}

fun renderInterface(name: String, superInterfaces: List<String>? = null, contentsBuilder: SimpleMembersBuilder.() -> Unit = {}): String = buildString {
    append("interface $name")
    superInterfaces?.let { append(" : ${it.joinToString()}") }
    SimpleMembersBuilder().apply(contentsBuilder).appendTo(this)
}

fun renderClass(modifiers: List<String>? = null, name: String, constr: String? = null, superClass: String? = null, superInterfaces: List<String>? = null, contentsBuilder: SimpleMembersBuilder.() -> Unit = {}): String = buildString {
    modifiers?.let { append(it.joinToString(" ", postfix = " ")) }
    append("class $name")
    constr?.let { append(it) }
    superClass?.let { append(": $it") }
    superInterfaces?.let {
        if (superClass == null) append(": ") else append(", ")
        append(it.joinToString())
    }
    SimpleMembersBuilder().apply(contentsBuilder).appendTo(this)
}

fun SimpleMembersBuilder.method(
    name: String,
    modifiers: List<String>? = null,
    params: List<Pair<String, String>> = emptyList(),
    returnType: String? = null,
    value: String? = null,
    body: String? = null,
) {
    member {
        modifiers?.let { append(it.joinToString(" ", postfix = " ")) }
        append("fun $name(")
        append(params.joinToString { "${it.first}: ${it.second}" })
        append(")")
        returnType?.let { append(": $it") }
        value?.let { append(" = $it") }
        require(body == null) // TODO
    }

}

fun SimpleMembersBuilder.property(name: String, type: String? = null, initial: String? = null, getter: String? = null, setter: String? = null, settable: Boolean = setter != null) {
    val kind = if (settable) "var" else "val"
    member {
        append("$kind $name")
        type?.let{ append(": $it") }
        initial?.let{ append(" = $it") }
        appendLine()
        getter?.let{
            appendLine("    get() = $it")
        }
        setter?.let{
            appendLine("    set(value) $it")
        }
    }
}

operator fun String.invoke(vararg args: String): String = this(args.toList())
operator fun String.invoke(args: List<String>? = null): String = "$this(${args?.joinToString() ?: ""})"

