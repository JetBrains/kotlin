@file:OptIn(
    org.jetbrains.kotlin.CoreEnvironmentDeprecation::class,
    org.jetbrains.kotlin.config.CompilerConfiguration.Internals::class,
)

import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.registerExtensionStorage
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerId
import org.jetbrains.kotlin.daemon.common.configureDaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.configureDaemonOptions
import org.jetbrains.kotlin.daemon.common.runFilesPathOrDefault
import org.jetbrains.kotlin.psi.*
import java.io.File
import java.util.Base64
import kotlin.system.exitProcess

/**
 * Resident compile/parse server for lrepl.
 *
 * Mode "compile" (default): reads compile requests — arguments joined with
 * the unit separator char () — and forwards each to the official
 * Kotlin compile daemon (compiler/daemon — the same resident service
 * Gradle drives; its CompileServiceImpl has a first-class
 * TargetPlatform.WASM branch). The shim never loads the compiler: the
 * daemon is spawned with the dist's jars (its -cp IS the CompilerId), so
 * a dist rebuild is caught by the daemon's classpath watcher and it
 * restarts itself — server.jar no longer embeds or runs the compiler,
 * so it no longer needs deleting after `./gradlew dist` (the IC-cache
 * rule stays: that one is about cache encodings, not the server). If
 * the daemon can't be reached the shim exits nonzero and the drivers
 * fall back to spawning kotlinc.
 * Answers `RESULT <exitCode> <base64-diagnostics>`.
 * Terminate with EXIT.
 *
 * Mode "parse" (`CompileServerKt parse`): a separate process (the IntelliJ
 * Application is a JVM-wide singleton and must not mix with the compiler's)
 * that does the whole frontend job for one submission: parses it with the
 * real Kotlin PSI and REWRITES it against the current binding table, so the
 * Python driver only deals in final texts.
 *
 * Request:  <b64 source><b64 binding spec>          (spec: "name=fun|n2=val|…")
 * Response: RESULT 0 <b64 JSON> —
 *
 *   { "ok": <file parsed cleanly>, "errorMessage": …,
 *     "imports": ["kotlin.math.abs", …],
 *     "decls": [
 *       fun:      { "kind":"fun", "name", "ret", "paramList",
 *                   "params":[{"name","type","vararg","default"}],
 *                   "text" (whole decl, rewritten), "used":[bindings] }
 *       property: { "kind":"property", "name", "isVar", "declaredType",
 *                   "initializer" (rewritten), "used":[…] }
 *       class:    { "kind":"class"|"typealias", "name", "text" (rewritten),
 *                   "used":[…] }
 *       object:   { "kind":"object", "name", "classText" (object->class
 *                   transform applied, rewritten), "used":[…] } ],
 *     "asExpression": { "ok", "text" (rewritten), "used":[…] },
 *     "asBlock":      { "ok", "text" (rewritten incl. setter conversions), "used":[…] } }
 *
 * Rewriting rules (offset-precise on PSI ranges — string literals and
 * comments can never be touched):
 *   - references to val/obj bindings become `name__get()` calls;
 *     `$name` template entries become `${name__get()}`;
 *   - qualified selectors (`a.b`) are left alone;
 *   - locally declared names (params, locals, members) shadow bindings;
 *   - assignments to var bindings become `name__set(…)` calls, incl.
 *     compound ops (`k += 1` → `k__set(k__get() + 1)`);
 *   - fun bindings are referenced by name (the driver imports them).
 */

private val ASSIGN_OPS = setOf("=", "+=", "-=", "*=", "/", "%=")

private class Ref(val text: String, val start: Int, val end: Int, val qualified: Boolean, val template: Boolean)
private class Assign(
    val name: String, val op: String,
    val start: Int, val end: Int,
    val lhsStart: Int, val lhsEnd: Int, val rhsStart: Int, val rhsEnd: Int,
)

private class Bnd(val kind: String)

private fun parseBindings(spec: String): Map<String, Bnd> {
    val out = mutableMapOf<String, Bnd>()
    for (part in spec.split('|')) {
        if (part.isEmpty()) continue
        val i = part.indexOf('=')
        if (i > 0) out[part.substring(0, i)] = Bnd(part.substring(i + 1))
    }
    return out
}

private fun walk(el: PsiElement, f: (PsiElement) -> Unit) {
    f(el)
    var c = el.firstChild
    while (c != null) {
        walk(c, f)
        c = c.nextSibling
    }
}

private fun hasErrors(el: PsiElement): Boolean {
    var err = false
    walk(el) { if (it is PsiErrorElement) err = true }
    return err
}

private fun firstErrorMessage(el: PsiElement): String? {
    var msg: String? = null
    walk(el) {
        if (it is PsiErrorElement && msg == null) {
            msg = try {
                it.errorDescription
            } catch (e: Throwable) {
                "syntax error"
            }
        }
    }
    return msg
}

private fun isQualified(r: KtNameReferenceExpression): Boolean {
    val p = r.parent
    if (p is KtQualifiedExpression && p.selectorExpression == r) return true
    if (p is KtCallExpression) {
        val pp = p.parent
        if (pp is KtQualifiedExpression && pp.selectorExpression == p) return true
    }
    return false
}

private fun isTemplateRef(r: KtNameReferenceExpression): Boolean {
    var a = r.parent
    while (a != null) {
        if (a is KtSimpleNameStringTemplateEntry) return true
        if (a is KtBlockExpression || a is KtNamedFunction) return false
        a = a.parent
    }
    return false
}

private fun collectRefs(el: PsiElement, base: Int): List<Ref> {
    val out = mutableListOf<Ref>()
    walk(el) { n ->
        if (n is KtNameReferenceExpression) {
            out.add(
                Ref(
                    n.text, n.textRange.startOffset - base, n.textRange.endOffset - base,
                    isQualified(n), isTemplateRef(n),
                )
            )
        }
    }
    return out
}

private fun collectAssigns(el: PsiElement, base: Int): List<Assign> {
    val raw = mutableListOf<Assign>()
    walk(el) { n ->
        if (n is KtBinaryExpression) {
            val op = n.operationReference.text
            val lhs = n.left
            val rhs = n.right
            if (op in ASSIGN_OPS && lhs is KtNameReferenceExpression && rhs != null) {
                raw.add(
                    Assign(
                        lhs.text, op,
                        n.textRange.startOffset - base, n.textRange.endOffset - base,
                        lhs.textRange.startOffset - base, lhs.textRange.endOffset - base,
                        rhs.textRange.startOffset - base, rhs.textRange.endOffset - base,
                    )
                )
            }
        }
    }
    // drop assignments nested inside another assignment's span
    return raw.filter { a -> raw.none { b -> b !== a && b.start <= a.start && a.end <= b.end } }
}

private fun collectLocalNames(decl: KtDeclaration): Set<String> {
    val names = mutableSetOf<String>()
    walk(decl) { n ->
        when (n) {
            is KtParameter -> n.name?.let(names::add)
            is KtProperty -> n.name?.let(names::add)
            is KtNamedFunction -> n.name?.let(names::add)
            is KtClass -> n.name?.let(names::add)
            is KtObjectDeclaration -> n.name?.let(names::add)
            is KtTypeAlias -> n.name?.let(names::add)
            is KtTypeParameter -> n.name?.let(names::add)
            is KtDestructuringDeclarationEntry -> n.name?.let(names::add)
        }
    }
    return names
}

// ------------------------------------------------------------------ rewriting

/** The heart of the frontend: splice accessor/setter calls into `text` at
 *  the PSI-recorded reference ranges. Returns (rewritten, used bindings). */
private fun rewriteText(
    text: String,
    refs: List<Ref>,
    assigns: List<Assign>,
    bindings: Map<String, Bnd>,
    exclude: Set<String>,
    convertAssigns: Boolean,
): Pair<String, Set<String>> {
    data class Edit(val start: Int, val end: Int, val repl: String)

    val edits = mutableListOf<Edit>()
    val covered = mutableListOf<Pair<Int, Int>>()
    val used = mutableSetOf<String>()

    for (a in assigns) {
        val b = bindings[a.name]
        if (convertAssigns && b != null && b.kind == "var" && a.name !in exclude) {
            val prefix = if (a.op == "=") "${a.name}__set("
            else "${a.name}__set(${a.name}__get() ${a.op.dropLast(1)} "
            edits.add(Edit(a.start, a.rhsStart, prefix))
            edits.add(Edit(a.rhsEnd, a.rhsEnd, ")"))
            used.add(a.name)
            covered.add(a.start to a.lhsEnd)
        } else {
            covered.add(a.lhsStart to a.lhsEnd)
        }
    }
    for (r in refs) {
        if (r.qualified) continue
        val b = bindings[r.text] ?: continue
        if (r.text in exclude) continue
        used.add(r.text)
        if (b.kind == "fun") continue // imported under the same name
        if (covered.any { it.first <= r.start && r.start < it.second }) continue
        if (r.template) {
            // replace the whole $name entry (the '$' is one char before the ref)
            edits.add(Edit(r.start - 1, r.end, "\${${r.text}__get()}"))
        } else {
            edits.add(Edit(r.start, r.end, "${r.text}__get()"))
        }
    }
    if (edits.isEmpty()) return text to used

    val applied = mutableListOf<Pair<Int, Int>>()
    var out = text
    for ((s, e, repl) in edits.sortedWith(
        compareByDescending<Edit> { it.start }.thenByDescending { it.end - it.start }
    )) {
        if (applied.any { it.first < e && s < it.second }) continue
        out = out.substring(0, s) + repl + out.substring(e)
        applied.add(s to e)
    }
    return out to used
}

private fun rewriteDeclText(
    d: KtDeclaration, bindings: Map<String, Bnd>, exclude: Set<String>,
): Pair<String, Set<String>> {
    val start = d.textRange.startOffset
    return rewriteText(d.text, collectRefs(d, start), collectAssigns(d, start), bindings, exclude, true)
}

// ------------------------------------------------------------------ JSON

private fun jstr(s: String?): String {
    if (s == null) return "null"
    val b = StringBuilder("\"")
    for (c in s) {
        when {
            c == '"' -> b.append("\\\"")
            c == '\\' -> b.append("\\\\")
            c == '\n' -> b.append("\\n")
            c == '\r' -> b.append("\\r")
            c == '\t' -> b.append("\\t")
            c < ' ' -> b.append("\\u%04x".format(c.code))
            else -> b.append(c)
        }
    }
    b.append('"')
    return b.toString()
}

private fun usedJson(used: Set<String>): String =
    used.joinToString(",", "[", "]") { jstr(it) }

// ------------------------------------------------------------------ decls

private fun funJson(d: KtNamedFunction, bindings: Map<String, Bnd>): String {
    val exclude = collectLocalNames(d) + setOfNotNull(d.name)
    val (declRew, used) = rewriteDeclText(d, bindings, exclude)
    val params = d.valueParameters.joinToString(",", "[", "]") { p ->
        "{\"name\":${jstr(p.name)},\"type\":${jstr(p.typeReference?.text)}" +
            ",\"vararg\":${p.isVarArg},\"default\":${p.defaultValue != null}}"
    }
    val paramList = d.valueParameters.joinToString(", ") { p -> "${p.name}: ${p.typeReference?.text ?: ""}" }
    return ("{\"kind\":\"fun\",\"name\":${jstr(d.name)},\"text\":${jstr(declRew)}" +
        ",\"ret\":${jstr(d.typeReference?.text)}" +
        ",\"paramList\":${jstr(paramList)},\"params\":$params" +
        ",\"used\":${usedJson(used)}}")
}

private fun propertyJson(d: KtProperty, bindings: Map<String, Bnd>): String {
    val init = d.initializer
    val exclude = collectLocalNames(d) + setOfNotNull(d.name)
    val (initRew, used) = if (init != null)
        rewriteText(init.text, collectRefs(init, init.textRange.startOffset), collectAssigns(init, init.textRange.startOffset), bindings, exclude, true)
    else "" to emptySet<String>()
    return ("{\"kind\":\"property\",\"name\":${jstr(d.name)},\"isVar\":${d.isVar}" +
        ",\"declaredType\":${jstr(d.typeReference?.text)}" +
        ",\"initializer\":${jstr(if (init == null) null else initRew)}" +
        ",\"used\":${usedJson(used)}}")
}

private fun classJson(d: KtClass, bindings: Map<String, Bnd>): String {
    val (text, used) = rewriteDeclText(d, bindings, collectLocalNames(d) + setOfNotNull(d.name))
    return ("{\"kind\":\"class\",\"name\":${jstr(d.name)},\"text\":${jstr(text)}" +
        ",\"used\":${usedJson(used)}}")
}

private fun objectJson(d: KtObjectDeclaration, bindings: Map<String, Bnd>): String {
    val name = d.name ?: return "{\"kind\":\"object\",\"name\":null}"
    val (text, used) = rewriteDeclText(d, bindings, collectLocalNames(d) + setOf(name))
    // object Foo { … } -> class Foo__obj { … }, via PSI ranges (no regex)
    val kw = d.getObjectKeyword()
    val nameId = d.nameIdentifier
    var classText: String? = null
    if (kw != null && nameId != null) {
        val rel = d.textRange.startOffset
        val kwStart = kw.textRange.startOffset - rel
        val kwEnd = kw.textRange.endOffset - rel
        val nmStart = nameId.textRange.startOffset - rel
        val nmEnd = nameId.textRange.endOffset - rel
        classText = text.substring(0, kwStart) + "class " + text.substring(kwEnd, nmStart) +
            name + "__obj" + text.substring(nmEnd)
    }
    return ("{\"kind\":\"object\",\"name\":${jstr(name)},\"classText\":${jstr(classText)}" +
        ",\"used\":${usedJson(used)}}")
}

private fun typealiasJson(d: KtTypeAlias, bindings: Map<String, Bnd>): String {
    val (text, used) = rewriteDeclText(d, bindings, emptySet())
    return ("{\"kind\":\"typealias\",\"name\":${jstr(d.name)},\"text\":${jstr(text)}" +
        ",\"used\":${usedJson(used)}}")
}

// ------------------------------------------------------------------ parse

private fun parseSource(factory: KtPsiFactory, src: String, bindings: Map<String, Bnd>): String {
    val sb = StringBuilder("{")

    // ---- attempt 1: parse as a file (top-level declarations)
    val file = factory.createFile("f.kt", src)
    val fileOk = !hasErrors(file)
    sb.append("\"ok\":$fileOk")
    sb.append(",\"errorMessage\":${jstr(if (fileOk) null else firstErrorMessage(file))}")
    sb.append(",\"imports\":${file.importDirectives.joinToString(",", "[", "]") { jstr(it.text.removePrefix("import ")) }}")
    sb.append(",\"decls\":[")
    var first = true
    for (d in file.declarations) {
        val json = when (d) {
            is KtNamedFunction -> funJson(d, bindings)
            is KtProperty -> propertyJson(d, bindings)
            is KtClass -> classJson(d, bindings)
            is KtObjectDeclaration -> objectJson(d, bindings)
            is KtTypeAlias -> typealiasJson(d, bindings)
            else -> null
        }
        if (json != null) {
            if (!first) sb.append(",")
            sb.append(json)
            first = false
        }
    }
    sb.append("]")

    // ---- attempt 2: parse as a single expression ("val __w = <src>")
    run {
        val wrap = factory.createFile("e.kt", "val __w = $src")
        val prop = wrap.declarations.firstOrNull() as? KtProperty
        val init = prop?.initializer
        val ok = init != null && !hasErrors(wrap)
        val base = init?.textRange?.startOffset ?: 0
        val assigns = if (init != null) collectAssigns(init, base) else emptyList()
        val (rew, used) = if (init != null)
            rewriteText(src, collectRefs(init, base), assigns, bindings, emptySet(), false)
        else src to emptySet<String>()
        sb.append(",\"asExpression\":{\"ok\":$ok,\"text\":${jstr(rew)},\"used\":${usedJson(used)}" +
            ",\"assignCount\":${assigns.size}}")
    }

    // ---- attempt 3: parse as statements ("fun __w() {\n<src>\n}")
    run {
        val wrap = factory.createFile("b.kt", "fun __w() {\n$src\n}")
        val fn = wrap.declarations.firstOrNull() as? KtNamedFunction
        val block = fn?.bodyBlockExpression
        val ok = block != null && !hasErrors(wrap)
        val base = block?.textRange?.startOffset?.plus(2) ?: 0
        val (rew, used) = if (block != null)
            rewriteText(src, collectRefs(block, base), collectAssigns(block, base), bindings, emptySet(), true)
        else src to emptySet<String>()
        sb.append(",\"asBlock\":{\"ok\":$ok,\"text\":${jstr(rew)},\"used\":${usedJson(used)}}")
    }

    sb.append("}")
    return sb.toString()
}

private fun serveParse() {
    // standalone IntelliJ platform: it insists on an "installation home"
    if (System.getProperty("idea.home") == null) {
        System.setProperty("idea.home", System.getProperty("user.dir"))
    }
    val disposable = Disposer.newDisposable()
    val env = KotlinCoreEnvironment.createForProduction(
        disposable,
        CompilerConfiguration().apply { registerExtensionStorage() },
        EnvironmentConfigFiles.JVM_CONFIG_FILES,
    )
    val factory = KtPsiFactory(env.project, markGenerated = false)
    val stdin = System.`in`.bufferedReader()
    val stdout = System.out
    while (true) {
        val line = stdin.readLine() ?: break
        if (line == "EXIT") break
        if (line.isBlank()) continue
        val out: String = try {
            val parts = line.split('')
            val src = String(Base64.getDecoder().decode(parts[0].trim()), Charsets.UTF_8)
            val bindings = if (parts.size > 1) parseBindings(String(Base64.getDecoder().decode(parts[1].trim()), Charsets.UTF_8)) else emptyMap()
            "RESULT 0 " + Base64.getEncoder().encodeToString(parseSource(factory, src, bindings).toByteArray(Charsets.UTF_8))
        } catch (e: Throwable) {
            "RESULT 1 " + Base64.getEncoder().encodeToString(("parse process error: $e").toByteArray(Charsets.UTF_8))
        }
        stdout.println(out)
        stdout.flush()
    }
}

private fun serveCompile() {
    System.setProperty("java.rmi.server.hostname", "127.0.0.1")

    // The daemon is spawned with the dist's jars — located by side effect
    // of the drivers putting kotlin-compiler.jar on this shim's classpath.
    // Canonicalize: the CompilerId (and the run-file digest the next shim
    // discovers daemons by) is over the path STRINGS, and the drivers pass
    // differently-spelled paths (node's path.join normalizes "..", python's
    // os.path.join does not) — without this, each spelling gets its own
    // daemon.
    val libDir = System.getProperty("java.class.path")
        .split(File.pathSeparator).asSequence()
        .map(::File).firstOrNull { it.name == "kotlin-compiler.jar" }?.canonicalFile?.parentFile
    if (libDir == null) {
        System.err.println("hot shim: kotlin-compiler.jar is not on the classpath")
        exitProcess(3)
    }
    val daemonJars = listOf("kotlin-compiler.jar", "kotlin-daemon.jar", "kotlin-reflect.jar")
        .map { File(libDir, it) }.filter(File::isFile)
    if (daemonJars.none { it.name == "kotlin-daemon.jar" }) {
        System.err.println("hot shim: no kotlin-daemon.jar in $libDir")
        exitProcess(3)
    }
    val compilerId = CompilerId.makeCompilerId(daemonJars)

    // A pinned client-alive file (the default one autodeletes with this
    // JVM) lets the daemon outlive the shim: the next session's first
    // compile skips daemon startup. It still dies on the 2h idle timer,
    // and `KotlinCompilerClient --stop` (or deleting the file) reclaims it.
    val daemonOptions = configureDaemonOptions()
    val aliveFlag = File(daemonOptions.runFilesPathOrDefault, "kotlin-hot-shim-alive").apply {
        parentFile.mkdirs()
        if (!exists()) createNewFile()
    }
    System.setProperty("kotlin.daemon.client.alive.path", aliveFlag.absolutePath)
    val daemonJvmOptions = configureDaemonJVMOptions(
        inheritMemoryLimits = true, inheritOtherJvmOptions = false, inheritAdditionalProperties = true,
    )

    fun connect(): CompileService? = KotlinCompilerClient.connectToCompileService(
        compilerId, daemonJvmOptions, daemonOptions,
        DaemonReportingTargets(out = System.err),
    )

    var service: CompileService? = null
    val stdin = System.`in`.bufferedReader()
    val stdout = System.out
    while (true) {
        val line = stdin.readLine() ?: break
        if (line == "EXIT") break
        if (line.isBlank()) continue
        val args = line.split('').toTypedArray()

        if (service == null) service = connect()
        if (service == null) exitProcess(3) // drivers fall back to kotlinc

        val diagnostics = StringBuilder()
        fun run(): Int = KotlinCompilerClient.compile(
            service!!, CompileService.NO_SESSION, CompileService.TargetPlatform.WASM, args,
            object : MessageCollector {
                override fun clear() {}
                override fun hasErrors() = false
                override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
                    if (diagnostics.isNotEmpty()) diagnostics.append('\n')
                    diagnostics.append(
                        when {
                            severity.isError -> "error: "
                            severity.isWarning -> "warning: "
                            else -> ""
                        }
                    ).append(message)
                    if (location != null) diagnostics.append(" (${location.path}:${location.line}:${location.column})")
                }
            },
        )
        val code = try {
            run()
        } catch (e: Throwable) {
            // daemon died mid-request (dist rebuild watcher, idle timer):
            // reconnect once; if that fails, die so the driver falls back
            System.err.println("hot shim: daemon dropped: $e")
            service = connect()
            if (service == null) exitProcess(3)
            run()
        }
        val text = Base64.getEncoder().encodeToString(diagnostics.toString().toByteArray(Charsets.UTF_8))
        stdout.println("RESULT $code $text")
        stdout.flush()
    }
    // RMI's non-daemon threads would hold the JVM open past stdin EOF
    exitProcess(0)
}

fun main(args: Array<String>) {
    if (args.isNotEmpty() && args[0] == "parse") serveParse() else serveCompile()
}
