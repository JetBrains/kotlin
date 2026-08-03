/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.ir.generator.toolbox

interface ScopeBuilder {
    fun line(text: String = "")
    fun comment(text: String)
    fun raw(text: String)

    fun cls(
        modifiers: List<String> = emptyList(),
        name: String,
        typeParams: List<String> = emptyList(),
        constructor: String? = null,
        superclass: String? = null,
        superInterfaces: List<String> = emptyList(),
        block: Block.() -> Unit,
    )

    fun iface(
        modifiers: List<String> = emptyList(),
        name: String,
        typeParams: List<String> = emptyList(),
        superInterfaces: List<String> = emptyList(),
        block: Block.() -> Unit,
    )

    fun method(
        contextReceivers: List<Pair<String, String>> = emptyList(),
        modifiers: List<String> = emptyList(),
        name: String,
        typeParams: List<String> = emptyList(),
        receiver: String? = null,
        params: List<Pair<String, String>> = emptyList(),
        returns: String? = null,
        expr: String? = null,
        block: (Block.() -> Unit)? = null,
    )

    fun property(
        contextReceivers: List<Pair<String, String>> = emptyList(),
        modifiers: List<String> = emptyList(),
        name: String,
        type: String? = null,
        receiver: String? = null,
        value: String? = null,
        delegate: String? = null,
        getter: String? = null,
        setter: String? = null,
    )

    fun companion(block: Block.() -> Unit)
}

class TopLevelBuilder(
    val pkg: String,
    val name: String,
    private val body: Block = Block(""),
) : ScopeBuilder by body {
    private val imports = mutableListOf<String>()
    private val fileAnnotations = mutableSetOf<String>()

    private val license = """
            |/*
            | * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
            | * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
            | */
            |
        """.trimMargin()

    fun imports(vararg fqns: String) {
        imports.addAll(fqns)
    }

    fun render(): String = buildString {
        append(license)
        line()
        append(fileAnnotations.joinToString("\n") { "@file:$it" })
        appendLine()
        appendLine("package $pkg")
        appendLine()
        if (imports.isNotEmpty()) {
            imports.forEach { appendLine("import $it") }
            appendLine()
        }
        append(body.target)
    }

    fun fileAnnotations(annotation: String) {
        fileAnnotations += annotation
    }

    fun writeTo(sink: FileSink) = sink.write(pkg, "$name.kt", render())
}

class Block internal constructor(
    private val indent: String,
    val target: StringBuilder = StringBuilder(),
) : ScopeBuilder {
    override fun line(text: String) {
        if (text.isEmpty()) {
            target.appendLine()
            return
        }
        text.lineSequence().forEach { l ->
            if (l.isEmpty()) target.appendLine()
            else target.appendLine("$indent$l")
        }
    }

    override fun raw(text: String) {
        target.append(text)
    }

    override fun comment(text: String) {
        line("// $text")
    }

    override fun cls(
        modifiers: List<String>,
        name: String,
        typeParams: List<String>,
        constructor: String?,
        superclass: String?,
        superInterfaces: List<String>,
        block: Block.() -> Unit,
    ) = braced(classHeader("class", name, modifiers, typeParams, constructor, superclass, superInterfaces), block)

    override fun iface(
        modifiers: List<String>,
        name: String,
        typeParams: List<String>,
        superInterfaces: List<String>,
        block: Block.() -> Unit,
    ) = braced(classHeader("interface", name, modifiers, typeParams, null, null, superInterfaces), block)

    private fun classHeader(
        keyword: String,
        name: String,
        modifiers: List<String>,
        typeParams: List<String>,
        constructor: String?,
        superclass: String?,
        superInterfaces: List<String>,
    ): String = buildString {
        appendModifiers(modifiers)
        append("$keyword $name")
        if (typeParams.isNotEmpty()) append("<${typeParams.joinToString(", ")}>")
        constructor?.let { append(it) }
        val supers = listOfNotNull(superclass) + superInterfaces
        if (supers.isNotEmpty()) append(" : ${supers.joinToString(", ")}")
    }

    override fun method(
        contextReceivers: List<Pair<String, String>>,
        modifiers: List<String>,
        name: String,
        typeParams: List<String>,
        receiver: String?,
        params: List<Pair<String, String>>,
        returns: String?,
        expr: String?,
        block: (Block.() -> Unit)?
    ) {
        require(expr == null || block == null) { "fn($name): expr and block are mutually exclusive." }
        val header = buildString {
            appendContextReceiver(contextReceivers)
            appendModifiers(modifiers)
            append("fun ")
            if (typeParams.isNotEmpty()) append("<${typeParams.joinToString(", ")}> ")
            receiver?.let { append("$it.") }
            append("$name(")
            append(params.joinToString(", ") { [n, t] -> "$n: $t" })
            append(")")
            returns?.let { append(": $it") }
        }
        when {
            block != null -> braced(header, block)
            expr != null -> line("$header = $expr")
            else -> line(header)
        }
    }

    override fun property(
        contextReceivers: List<Pair<String, String>>,
        modifiers: List<String>,
        name: String,
        type: String?,
        receiver: String?,
        value: String?,
        delegate: String?,
        getter: String?,
        setter: String?
    ) {
        val kind = if (setter != null) "var" else "val"
        val header = buildString {
            appendContextReceiver(contextReceivers)
            appendModifiers(modifiers)
            append("$kind ")
            receiver?.let { append("$it.") }
            append(name)
            type?.let { append(": $it") }
        }
        when {
            value != null -> line("$header = $value")
            delegate != null -> line("$header by $delegate")
            getter != null || setter != null -> {
                line(header)
                getter?.let { line("    get() = $it") }
                setter?.let { line("    set(value) $it") }
            }
            else -> line(header)
        }
    }

    private fun StringBuilder.appendModifiers(modifiers: List<String>) {
        if (modifiers.isNotEmpty()) append(modifiers.joinToString(" ")).append(' ')
    }

    private fun StringBuilder.appendContextReceiver(list: List<Pair<String, String>>) {
        if (list.isEmpty()) return
        append("context(")
        append(list.joinToString(", ") { [n, t] -> "$n: $t" })
        append(")\n$indent")
    }

    override fun companion(block: Block.() -> Unit) = braced("companion object") { block() }

    private fun braced(header: String, block: Block.() -> Unit) {
        val tempTarget = StringBuilder()
        Block("$indent    ", tempTarget).apply(block)
        if (tempTarget.isEmpty()) {
            target.append("$indent$header")
        } else {
            target.appendLine("$indent$header {")
            target.append(tempTarget)
            target.appendLine("$indent}")
        }
    }
}

inline fun writeKotlinFile(
    sink: FileSink,
    pkg: String,
    name: String,
    block: TopLevelBuilder.() -> Unit,
) {
    TopLevelBuilder(pkg, name).apply(block).writeTo(sink)
}
