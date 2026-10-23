/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedHostTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.callCompiler
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.provisionedXcodeCompilerArgs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.File

/**
 * Debugging tests for `FactsTracker` (see `org.jetbrains.kotlin.backend.konan.facts`).
 *
 * Each test compiles the signature examples of the "signature facts" proposal into a klib, builds a per-file cache for it
 * with `-Xbinary=dumpFactsTo=<file>`, prints everything that was tracked, and checks the provided (`defs`) and used (`uses`) facts.
 *
 * Dump format: CSV with the header `relation,path,kind,symbolKey,hash,representation` (the representation is for debugging only).
 */
@Tag("caches")
@EnforcedHostTarget
class NativeFactsTrackerTest : AbstractNativeSimpleTest() {

    @BeforeEach
    fun setUp() {
        assumeTrue(HostManager.host.family == Family.OSX || HostManager.host.family == Family.LINUX)
    }

    // Example: `A.kt` and `C.kt` call `b` from `B.kt`.
    @Test
    fun ordinaryCalls() {
        val facts = trackFacts("ordinaryCalls", ORIGINAL)

        assertEquals(1, facts.defs("A.kt").size)
        assertEquals(2, facts.defs("B.kt").size)
        assertEquals(1, facts.defs("C.kt").size)

        // A and C use the same fact of `b`, with the hash `B.kt` provides.
        val usedFromB = facts.usedFrom("A.kt", "B.kt")
        assertEquals(1, usedFromB.size, "Facts of B.kt used by A.kt: $usedFromB")
        assertEquals(usedFromB, facts.usedFrom("C.kt", "B.kt"))
        assertEquals(facts.defs("B.kt").filterKeys { it in usedFromB }, usedFromB)
    }

    // An ordinary body changes, signature facts stay the same.
    @Test
    fun ordinaryBodyChange() {
        val before = trackFacts("ordinaryBodyChange/before", ORIGINAL)
        val after = trackFacts(
            "ordinaryBodyChange/after",
            ORIGINAL + ("B.kt" to "fun b(x: Int): Int = x + 2\nfun help(): String = \"Hello\"\n")
        )

        for (file in ORIGINAL.keys) {
            assertEquals(before.defs(file), after.defs(file), "Facts provided by $file")
            assertFalse(isDirty(file, before, after), "$file must not be dirty")
        }
    }

    // The signature of `b` changes (callers are fixed), so the old key of `b` disappears.
    @Test
    fun signatureChange() {
        val before = trackFacts("signatureChange/before", ORIGINAL)
        val after = trackFacts(
            "signatureChange/after", mapOf(
                "A.kt" to "fun a(x: Int): Int = b(x.toString()).length\n",
                "B.kt" to "fun b(msg: String): String = \"Hello \$msg!\"\nfun help(): String = \"Hello\"\n",
                "C.kt" to "fun c(): Int = b(\"0\").length\n",
            )
        )

        // `help` keeps its key and hash, the key of `b` is replaced.
        val oldB = before.defs("B.kt") - after.defs("B.kt").keys
        val newB = after.defs("B.kt") - before.defs("B.kt").keys
        assertEquals(1, oldB.size, "Old facts of b: $oldB")
        assertEquals(1, newB.size, "New facts of b: $newB")
        assertEquals(before.defs("B.kt") - oldB.keys, after.defs("B.kt") - newB.keys)

        assertEquals(before.defs("A.kt"), after.defs("A.kt"))
        assertEquals(before.defs("C.kt"), after.defs("C.kt"))
        assertEquals(oldB, before.usedFrom("A.kt", "B.kt"))
        assertEquals(newB, after.usedFrom("A.kt", "B.kt"))
        assertTrue(isDirty("A.kt", before, after)) { "A.kt must be dirty" }
        assertTrue(isDirty("C.kt", before, after)) { "C.kt must be dirty" }
    }

    // The return type of `b` changes: the key of `b` stays the same, only its hash changes.
    @Test
    fun returnTypeChange() {
        val before = trackFacts("returnTypeChange/before", ORIGINAL)
        val after = trackFacts(
            "returnTypeChange/after", mapOf(
                "A.kt" to "fun a(x: Int): Int = b(x).toInt()\n",
                "B.kt" to "fun b(x: Int): Long = x + 1L\nfun help(): String = \"Hello\"\n",
                "C.kt" to "fun c(): Int = b(0).toInt()\n",
            )
        )

        assertEquals(before.defs("B.kt").keys, after.defs("B.kt").keys)
        val changed = before.defs("B.kt").filter { [key, hash] -> after.defs("B.kt")[key] != hash }
        assertEquals(before.usedFrom("A.kt", "B.kt"), changed, "Only the hash of b must change")
        assertTrue(isDirty("A.kt", before, after)) { "A.kt must be dirty" }
        assertTrue(isDirty("C.kt", before, after)) { "C.kt must be dirty" }
    }

    // The type of `D.p` changes from String to Int: the keys stay the same, only the hash of its getter changes.
    @Test
    fun propertyTypeChange() {
        val before = trackFacts(
            "propertyTypeChange/before", mapOf(
                "D.kt" to "class D {\n    val p: String = \"42\"\n}\n",
                "E.kt" to "fun e(): Any = D().p\n",
            )
        )
        val after = trackFacts(
            "propertyTypeChange/after", mapOf(
                "D.kt" to "open class D {\n    val p: Int = 42\n}\nclass F : D() { val x: String = \"\" }\n",
                "E.kt" to "fun e(): Any = D().p\n",
            )
        )

        assertEquals(before.defs("D.kt").keys, after.defs("D.kt").keys)
        val changed = before.defs("D.kt").filter { [key, hash] -> after.defs("D.kt")[key] != hash }
        assertTrue(changed.isNotEmpty()) { "The hash of the getter of p must change" }
        assertTrue(changed.keys.all { it in before.usedFrom("E.kt", "D.kt") }) { "Changed: $changed" }
        assertEquals(before.defs("E.kt"), after.defs("E.kt"))
        assertTrue(isDirty("E.kt", before, after)) { "E.kt must be dirty" }
    }

    // `Base` gains a field: the key of its layout stays the same, only its hash changes.
    @Test
    fun baseClassLayoutChange() {
        val derived = "class Derived : Base() {\n    val y: Int = 2\n}\n"
        val before = trackFacts(
            "baseClassLayoutChange/before", mapOf(
                "Base.kt" to "open class Base {\n    val x: Int = 1\n}\n",
                "Derived.kt" to derived,
            )
        )
        val after = trackFacts(
            "baseClassLayoutChange/after", mapOf(
                "Base.kt" to "open class Base {\n    val x: Int = 1\n    val z: Long = 3L\n}\n",
                "Derived.kt" to derived,
            )
        )

        val oldLayout = before.defs("Base.kt").layouts()
        val newLayout = after.defs("Base.kt").layouts()
        assertEquals(1, oldLayout.size, "Layout facts of Base: $oldLayout")
        assertEquals(oldLayout.keys, newLayout.keys)
        assertNotEquals(oldLayout, newLayout)
        assertEquals(oldLayout, before.usedFrom("Derived.kt", "Base.kt").layouts())
        assertEquals(before.defs("Derived.kt"), after.defs("Derived.kt"))
        assertTrue(isDirty("Derived.kt", before, after)) { "Derived.kt must be dirty" }
    }

    // `B.kt` is deleted (and `a` no longer calls `b`), so `b` and `help` disappear.
    @Test
    fun providerFileDeleted() {
        val before = trackFacts("providerFileDeleted/before", ORIGINAL)
        val after = trackFacts(
            "providerFileDeleted/after", mapOf(
                "A.kt" to "fun a(x: Int): Int = x\n",
                "C.kt" to "fun c(): Int = a(0)\n",
            )
        )

        assertEquals(2, before.defs("B.kt").size)
        assertEquals(emptyMap<FactKey, String>(), after.defs("B.kt"))
        assertTrue(before.defs("B.kt").keys.none { it in after.allDefs }) { "b and help must not be defined anymore" }
        assertEquals(before.defs("A.kt"), after.defs("A.kt"))
        assertTrue(isDirty("A.kt", before, after)) { "A.kt must be dirty" }
        assertTrue(isDirty("C.kt", before, after)) { "C.kt must be dirty" }
    }

    // `b` and `help` move from `B.kt` to `M.kt`, keys stay the same, the owning file changes.
    @Test
    fun moveKeepsIdentity() {
        val before = trackFacts("moveKeepsIdentity/before", ORIGINAL)
        val after = trackFacts("moveKeepsIdentity/after", (ORIGINAL - "B.kt") + ("M.kt" to ORIGINAL.getValue("B.kt")))

        assertEquals(before.defs("B.kt"), after.defs("M.kt"))
        assertEquals(emptyMap<FactKey, String>(), after.defs("B.kt"))
        assertFalse(isDirty("A.kt", before, after), "A.kt must not be dirty")
        assertFalse(isDirty("C.kt", before, after), "C.kt must not be dirty")
    }

    // The facts computed from the klib alone are the ones the compiler records.
    @Test
    fun klibFactsMatchCompiler() {
        val scenario = "klibFactsMatchCompiler"
        val compiled = trackFacts(
            scenario, ORIGINAL + mapOf(
                "Base.kt" to "open class Base {\n    val x: Int = 1\n    open fun f(): String = \"\"\n}\n",
                "Derived.kt" to "class Derived : Base() {\n    val y: Long = 2L\n    override fun f(): String = \"d\"\n}\n",
                "Box.kt" to "class Box<T>(val value: T) {\n    fun get(): List<T?> = listOf(value)\n}\nprivate fun secret(): Box<String> = Box(\"\")\nobject O {\n    var counter: Int = 0\n}\n",
            )
        )
        val fromKlib = klibFacts(buildDir.resolve(scenario))

        assertTrue(compiled.allDefs.isNotEmpty())
        assertEquals(emptyMap<FactKey, String>(), fromKlib.allUses)
        assertEquals(compiled.allDefs, fromKlib.allDefs)
    }

    private data class FactKey(val kind: String, val symbolKey: String)

    private fun Map<FactKey, String>.layouts(): Map<FactKey, String> = filterKeys { it.kind == "BASE_CLASS_LAYOUT" }

    private data class TrackedFact(
        val relation: String,
        val fileName: String,
        val key: FactKey,
        val hash: String,
        val representation: String,
    )

    private class TrackedFacts(val facts: List<TrackedFact>) {
        private fun of(relation: String, fileName: String?): Map<FactKey, String> =
            facts.filter { it.relation == relation && (fileName == null || it.fileName == fileName) }.associate { it.key to it.hash }

        fun defs(fileName: String): Map<FactKey, String> = of("defines", fileName)
        fun uses(fileName: String): Map<FactKey, String> = of("uses", fileName)
        val allDefs: Map<FactKey, String> get() = of("defines", null)
        val allUses: Map<FactKey, String> get() = of("uses", null)

        /** The facts [user] used that are provided by [provider]. */
        fun usedFrom(user: String, provider: String): Map<FactKey, String> = uses(user).filterKeys { it in defs(provider) }
    }

    /**
     * [fileName] is dirty if a fact of this module it used in [before] is no longer provided in [after], or has a different hash.
     * Facts from other modules (e.g. stdlib) are ignored.
     */
    private fun isDirty(fileName: String, before: TrackedFacts, after: TrackedFacts): Boolean {
        val moduleDefs = before.allDefs
        val newDefs = after.allDefs
        return before.uses(fileName).any { [key, hash] -> key in moduleDefs && newDefs[key] != hash }
    }

    private fun trackFacts(scenario: String, sources: Map<String, String>): TrackedFacts {
        val workDir = buildDir.resolve(scenario).apply { deleteRecursively(); mkdirs() }
        val cacheDir = workDir.resolve("cache").apply { mkdirs() }
        val klibFile = workDir.resolve("lib.klib")
        val dumpFile = workDir.resolve("facts.csv")

        val sourcePaths = sources.map { entry -> workDir.resolve(entry.key).apply { writeText(entry.value) }.absolutePath }
        callNativeCompiler(*sourcePaths.toTypedArray(), "-p", "library", "-no-default-libs", "-o", klibFile.absolutePath)
        callNativeCompiler(
            "-g",
            "-p", "static_cache",
            "-no-default-libs",
            "-Xmake-per-file-cache",
            "-Xcache-directory=${cacheDir.absolutePath}",
            "-Xcache-directory=${systemCacheDir.absolutePath}",
            "-Xadd-cache=${klibFile.absolutePath}",
            "-Xbinary=dumpFactsTo=${dumpFile.absolutePath}",
        )

        return parseFacts(scenario, dumpFile)
    }

    private fun klibFacts(workDir: File): TrackedFacts {
        val dumpFile = workDir.resolve("klib-facts.csv")
        Class.forName("org.jetbrains.kotlin.backend.konan.facts.KlibFactsGeneratorKt", true, testRunSettings.get<KotlinNativeClassLoader>().classLoader)
            .getMethod("dumpKlibFacts", String::class.java, String::class.java)
            .invoke(null, workDir.resolve("lib.klib").absolutePath, dumpFile.absolutePath)
        return parseFacts("${workDir.name} (klib)", dumpFile)
    }

    private fun parseFacts(scenario: String, dumpFile: File): TrackedFacts {
        val text = if (dumpFile.exists()) dumpFile.readText() else ""
        println(buildString {
            appendLine("=== Facts tracked for '$scenario' (${dumpFile.absolutePath}) ===")
            val lines = text.lines().filter { it.isNotBlank() }
            lines.take(1).forEach { appendLine(it) }
            lines.drop(1).sorted().forEach { appendLine(it) }
        })

        val result = parseFactsCsv(text).map { parts ->
            TrackedFact(parts[0], File(parts[1]).name, FactKey(parts[2], parts[3]), parts[4], parts[5])
        }

        return TrackedFacts(result)
    }

    private val systemCacheDir: File by lazy {
        testRunSettings.get<KotlinNativeHome>().librariesDir.resolve("cache").listFiles().orEmpty().filter {
            it.isDirectory &&
                    it.name.startsWith(HostManager.host.name) &&
                    "-g" in it.name &&
                    "STATIC" in it.name
        }.minByOrNull { it.name.length } ?: fail { "System cache directory not found" }
    }

    private fun callNativeCompiler(vararg args: String) {
        val compilerArgs = (args.toList() + provisionedXcodeCompilerArgs).toTypedArray()
        (val exitCode, val toolOutput) = callCompiler(
            compilerArgs = compilerArgs,
            kotlinNativeClassLoader = testRunSettings.get<KotlinNativeClassLoader>().classLoader,
        )

        assertEquals(ExitCode.OK, exitCode) {
            buildString {
                appendLine("Compilation failed with exit code = $exitCode")
                appendLine("Command-line arguments: ${compilerArgs.joinToString(" ")}")
                appendLine("Compiler output:")
                appendLine(toolOutput)
            }
        }
    }

    companion object {
        private val ORIGINAL = mapOf(
            "A.kt" to "fun a(x: Int): Int = b(x)\n",
            "B.kt" to "fun b(x: Int): Int = x + 1\nfun help(): String = \"Hello\"\n",
            "C.kt" to "fun c(): Int = b(0)\n",
        )
    }
}
