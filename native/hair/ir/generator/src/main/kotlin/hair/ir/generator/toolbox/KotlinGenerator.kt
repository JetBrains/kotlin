package hair.ir.generator.toolbox

import kotlin.collections.isNotEmpty
import kotlin.text.*

fun StringBuilder.appendIndented(indent: String, string: String) {
    string.lines().forEach { appendLine("$indent$it") }
}

sealed class ScopeBuilder {
    protected val members = mutableListOf<String>()

    fun member(str: String)  {
        members += str.trim()
    }

    fun member(builderAction: StringBuilder.() -> Unit)  {
        members += StringBuilder().apply(builderAction).toString().trim()
    }

    fun blankLine() {
        member("")
    }

    abstract fun appendTo(stringBuilder: StringBuilder)

    fun build(): String = buildString {
        appendTo(this)
    }
}

class TopLevelBuilder : ScopeBuilder() {
    override fun appendTo(stringBuilder: StringBuilder) {
        with (stringBuilder) {
            if (members.isNotEmpty()) {
                for (member in members) {
                    appendIndented("", member)
                    appendLine()
                }
            } else {
                appendLine()
            }
        }
    }
}

fun topLevel(annotations: List<String>? = null, pkg: String, imports: List<String>? = null, body: TopLevelBuilder.() -> Unit = {}): String = buildString {
    annotations?.let {
        for (annotation in annotations) {
            appendLine(annotation)
        }
        appendLine()
    }
    append("package $pkg")
    appendLine()
    imports?.let {
        for (import in imports) {
            appendLine("import $import")
        }
        appendLine()
    }
    TopLevelBuilder().apply(body).appendTo(this)
}

class MembersBuilder : ScopeBuilder() {
    override fun appendTo(stringBuilder: StringBuilder) {
        with (stringBuilder) {
            if (members.isNotEmpty()) {
                appendLine(" {")
                for (member in members) {
                    appendIndented("    ", member)
                    appendLine()
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

fun ScopeBuilder.iface(name: String, superInterfaces: List<String>? = null, contentsBuilder: MembersBuilder.() -> Unit = {}) = member {
    append("interface $name")
    superInterfaces?.let { append(" : ${it.joinToString()}") }
    MembersBuilder().apply(contentsBuilder).appendTo(this)
}

fun renderInterface(name: String, superInterfaces: List<String>? = null, contentsBuilder: MembersBuilder.() -> Unit = {}): String =
    TopLevelBuilder().apply { iface(name, superInterfaces, contentsBuilder) }.build()

fun ScopeBuilder.cls(modifiers: List<String>? = null, name: String, constr: String? = null, superClass: String? = null, superInterfaces: List<String>? = null, contentsBuilder: MembersBuilder.() -> Unit = {}) = member {
    modifiers?.let { append(it.joinToString(" ", postfix = " ")) }
    append("class $name")
    constr?.let { append(it) }
    superClass?.let { append(": $it") }
    superInterfaces?.let {
        if (superClass == null) append(": ") else append(", ")
        append(it.joinToString())
    }
    MembersBuilder().apply(contentsBuilder).appendTo(this)
}

fun renderClass(modifiers: List<String>? = null, name: String, constr: String? = null, superClass: String? = null, superInterfaces: List<String>? = null, contentsBuilder: MembersBuilder.() -> Unit = {}): String =
    TopLevelBuilder().apply { cls(modifiers, name, constr, superClass, superInterfaces, contentsBuilder) }.build()

fun ScopeBuilder.method(
    name: String,
    modifiers: List<String>? = null,
    params: List<Pair<String, String>> = emptyList(),
    returnType: String? = null,
    value: String? = null,
    body: String? = null,
    extension: String? = null,
    context: List<Pair<String, String>>? = null,
) {
    member {
        context?.let {
            appendLine("context(${context.joinToString { (param, type) -> "$param: $type" }})")
        }
        modifiers?.let {
            for (modifier in modifiers) {
                append("$modifier ")
            }
        }
        append("fun ")
        extension?.let { append("$it.") }
        append("$name(")
        append(params.joinToString { "${it.first}: ${it.second}" })
        append(")")
        returnType?.let { append(": $it") }
        value?.let { append(" = $it") }
        require(body == null) // TODO
    }

}

fun ScopeBuilder.property(
    name: String,
    modifiers: List<String>? = null,
    type: String? = null,
    initial: String? = null,
    getter: String? = null,
    setter: String? = null,
    settable: Boolean = setter != null,
    context: List<Pair<String, String>>? = null,
) {
    val kind = if (settable) "var" else "val"
    member {
        context?.let {
            appendLine("context(${context.joinToString { (param, type) -> "$param: $type" }})")
        }
        modifiers?.let { append(it.joinToString(" ", postfix = " ")) }
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

