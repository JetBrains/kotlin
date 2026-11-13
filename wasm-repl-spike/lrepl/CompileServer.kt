@file:OptIn(
    org.jetbrains.kotlin.CoreEnvironmentDeprecation::class,
    org.jetbrains.kotlin.config.CompilerConfiguration.Internals::class,
)

import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.js.KotlinWasmCompiler
import org.jetbrains.kotlin.cli.registerExtensionStorage
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.Base64

/**
 * Resident compile/parse server for lrepl.
 *
 * Mode "compile" (default): reads compile requests — arguments joined with
 * the unit separator char () — invokes the wasm compiler in-process
 * (no JVM restart per line), answers `RESULT <exitCode> <base64-diagnostics>`.
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
 *       fun:      { "kind":"fun", "name", "ret", "paramsRaw" (parens stripped),
 *                   "paramList", "params":[{"name","type","vararg","default"}],
 *                   "slotEligible", "body" (rewritten), "text" (whole decl,
 *                   rewritten — registry-only fallback), "used":[bindings] }
 *       property: { "kind":"property", "name", "isVar", "declaredType",
 *                   "initializer" (rewritten), "used":[…] }
 *       class:    { "kind":"class"|"typealias", "name", "text" (rewritten),
 *                   "isInterface", "isEnum", "used":[…] }
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
    val start = d.textRange.startOffset
    val block = d.bodyBlockExpression
    val body = d.bodyExpression
    val bodyText: String?
    val bodyBaseAbs: Int
    val bodyEl: KtExpression?
    if (block != null) {
        val t = block.text
        bodyText = if (t.length <= 2) "" else t.substring(1, t.length - 1)
        bodyBaseAbs = block.textRange.startOffset + 1
        bodyEl = block
    } else if (body != null) {
        bodyText = body.text
        bodyBaseAbs = body.textRange.startOffset
        bodyEl = body
    } else {
        bodyText = null
        bodyBaseAbs = start
        bodyEl = null
    }
    val exclude = collectLocalNames(d) + setOfNotNull(d.name)

    var hasUnlabeledReturn = false
    walk(d) { n -> if (n is KtReturnExpression && !n.text.startsWith("return@")) hasUnlabeledReturn = true }
    val suspend = d.modifierList?.hasModifier(KtTokens.SUSPEND_KEYWORD) == true
    val generic = d.typeParameterList != null
    // NB: receiverTypeReference is statically non-null in this PSI build — a
    // plain `== null` gets constant-folded by kotlinc; force a nullable view.
    val receiver: KtTypeReference? = d.receiverTypeReference
    val vararg = d.valueParameters.any { it.isVarArg }
    val eligible = !suspend && !generic && receiver == null && !vararg && !hasUnlabeledReturn && d.name != null

    val (bodyRew, bodyUsed) = if (bodyEl != null && bodyText != null)
        rewriteText(bodyText, collectRefs(bodyEl, bodyBaseAbs), collectAssigns(bodyEl, bodyBaseAbs), bindings, exclude, true)
    else "" to emptySet<String>()
    val (declRew, declUsed) = rewriteDeclText(d, bindings, exclude)
    val used = bodyUsed + declUsed

    val params = d.valueParameters.joinToString(",", "[", "]") { p ->
        "{\"name\":${jstr(p.name)},\"type\":${jstr(p.typeReference?.text)}" +
            ",\"vararg\":${p.isVarArg},\"default\":${p.defaultValue != null}}"
    }
    val paramList = d.valueParameters.joinToString(", ") { p -> "${p.name}: ${p.typeReference?.text ?: ""}" }
    // rewrite default values in the parameter list too (the slot wrapper
    // re-declares them, so a default like `n: Int = gold` must become
    // `gold__get()` for the wrapper to resolve)
    var paramsRaw = d.valueParameterList?.text?.trim() ?: ""
    val pl = d.valueParameterList
    if (pl != null) {
        val (rew, _) = rewriteText(paramsRaw, collectRefs(pl, pl.textRange.startOffset), collectAssigns(pl, pl.textRange.startOffset), bindings, exclude, true)
        paramsRaw = rew
    }
    if (paramsRaw.startsWith("(") && paramsRaw.endsWith(")")) paramsRaw = paramsRaw.substring(1, paramsRaw.length - 1)

    return ("{\"kind\":\"fun\",\"name\":${jstr(d.name)},\"text\":${jstr(declRew)}" +
        ",\"ret\":${jstr(d.typeReference?.text)},\"paramsRaw\":${jstr(paramsRaw)}" +
        ",\"paramList\":${jstr(paramList)},\"params\":$params,\"slotEligible\":$eligible" +
        ",\"body\":${jstr(bodyRew)},\"used\":${usedJson(used)}}")
}

private fun propertyJson(d: KtProperty, bindings: Map<String, Bnd>): String {
    val start = d.textRange.startOffset
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
        ",\"isInterface\":${d.isInterface()},\"isEnum\":${d.isEnum()}" +
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
    val stdin = System.`in`.bufferedReader()
    val stdout = System.out
    while (true) {
        val line = stdin.readLine() ?: break
        if (line == "EXIT") break
        if (line.isBlank()) continue
        val args = line.split('').toTypedArray()

        val oldErr = System.err
        val buffer = ByteArrayOutputStream()
        System.setErr(PrintStream(buffer, true))
        val code = try {
            CLICompiler.doMainNoExit(KotlinWasmCompiler(), args)
        } catch (e: Throwable) {
            oldErr.println("COMPILE CRASH: $e")
            org.jetbrains.kotlin.cli.common.ExitCode.INTERNAL_ERROR
        } finally {
            System.setErr(oldErr)
        }
        val text = Base64.getEncoder().encodeToString(buffer.toByteArray())
        stdout.println("RESULT ${code.code} $text")
        stdout.flush()
    }
}

fun main(args: Array<String>) {
    if (args.isNotEmpty() && args[0] == "parse") serveParse() else serveCompile()
}
