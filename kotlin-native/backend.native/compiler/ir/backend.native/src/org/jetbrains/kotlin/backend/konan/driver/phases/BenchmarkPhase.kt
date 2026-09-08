/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Temporary benchmarking / analysis code.
//
// Runs TWO passes over the same set of IR functions and writes ONE combined
// report so that "before hair" and "after hair" numbers stand next to each
// other for every function:
//
//   * BeforeHairBenchmarkPhase (file-level, runs pre-codegen after every IR
//     lowering) counts IR array `get`/`set` and `*WithoutBoundCheck` calls per
//     loop-nesting depth. This is what would reach codegen if HaIR fell back.
//
//   * runAfterHairBenchmark(...) runs right after GenerateHairPhase. For every
//     IR function it looks up the corresponding HaIR `FunctionCompilation` in
//     the map produced by GenerateHairPhase and, when present, counts
//     surviving `ArrayIndexCheck` nodes (= BC) and total `LoadArrayElement +
//     StoreArrayElement` nodes (so NoBC = total - BC) — i.e. exactly what
//     HaIR's BCE actually removed. When absent (hair failed for that
//     function), the entry is marked "hair failed" instead.
//
// Both passes write into the same singleton dumper, so a single file appears
// at `<dumpHairTo>/arrayAccessesBenchmark.txt` with one summary block and one
// per-file / per-function breakdown listing before + after side by side.
//
// Enabled automatically whenever `-Xbinary=dumpHairTo=/path/to/dir` is set; a
// no-op otherwise so normal builds are unaffected. All temporary code lives
// in this one file plus one hook line in `TopLevelPhases.runCodegen` and one
// phase declaration in `NativeLoweringPhases.kt`, so cleanup is a delete of
// those three edits.

package org.jetbrains.kotlin.backend.konan.driver.phases

import hair.compilation.FunctionCompilation
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.NativeBackendContext
import org.jetbrains.kotlin.backend.konan.hair.shouldGenerateBody
import org.jetbrains.kotlin.backend.konan.ir.KonanNameConventions
import org.jetbrains.kotlin.backend.konan.llvm.computeFullName
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.util.isPrimitiveArray
import org.jetbrains.kotlin.ir.util.isUnsignedArray
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.io.File

private const val OUTPUT_FILE_NAME = "arrayAccessesBenchmark.txt"

// ============================================================================
// Phase 1: BEFORE HAIR
// ============================================================================

internal class BeforeHairBenchmarkPhase(val context: NativeBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val dumpDir = context.config.dumpHairTo ?: return
        Dumper.ensureOutput(File(dumpDir, OUTPUT_FILE_NAME))
        Dumper.lowerBefore(irFile)
    }
}

// ============================================================================
// Phase 2: AFTER HAIR (called directly, not a NamedCompilerPhase, because it
// needs the per-function HaIR compilation map)
// ============================================================================

internal fun runAfterHairBenchmark(
        context: NativeBackendContext,
        irFile: IrFile,
        hair: Map<IrFunction, FunctionCompilation>,
) {
    val dumpDir = context.config.dumpHairTo ?: return
    Dumper.ensureOutput(File(dumpDir, OUTPUT_FILE_NAME))
    Dumper.lowerAfter(irFile, hair)
}

// ============================================================================
// Shared singleton dumper
// ============================================================================

/**
 * Snapshot of array-access counters taken from a HaIR [hair.compilation.Session]
 * right after [hair.opt.eliminateBoundsChecks] runs — i.e. before `optimize()`
 * / `lower()` rewrite the graph further and delete the remaining
 * [hair.ir.nodes.ArrayIndexCheck] nodes as part of code lowering. Reading these
 * numbers off the live session at the end of the HaIR pipeline gives 0 checks
 * for every function, which is why we have to snapshot here instead.
 *
 * The three lists are indexed by IR loop-nesting depth (0 = top level), same
 * convention as the before-hair pass. Each list is trimmed to whatever the
 * max observed depth is for that function; the reader must be prepared for
 * three lists of possibly different lengths (they are aligned per-depth via
 * `getOrElse(i) { 0 }`).
 */
internal data class HairArrayAccessSnapshot(
        val checksAfterBcePerDepth: List<Int>,
        val loadsPerDepth: List<Int>,
        val storesPerDepth: List<Int>,
)

internal object Dumper {
    @Volatile private var _file: File? = null
    // Preserve first-seen order for stable output; index by function full name
    // so that the after-hair pass can find the entry the before-hair pass
    // added for the same function.
    private val data = LinkedHashMap<String, AnalysisData>()

    // Populated from IrToHair right after eliminateBoundsChecks(); read from
    // the after-hair pass instead of the live session (see doc on
    // HairArrayAccessSnapshot).
    private val snapshots = mutableMapOf<FunctionCompilation, HairArrayAccessSnapshot>()

    fun recordHairSnapshot(fc: FunctionCompilation, snapshot: HairArrayAccessSnapshot) {
        synchronized(this) { snapshots[fc] = snapshot }
    }

    internal fun getHairSnapshot(fc: FunctionCompilation): HairArrayAccessSnapshot? =
            synchronized(this) { snapshots[fc] }

    fun ensureOutput(f: File) {
        if (_file != null) return
        synchronized(this) {
            if (_file == null) {
                f.parentFile?.mkdirs()
                f.writeText("")
                _file = f
            }
        }
    }

    fun lowerBefore(irFile: IrFile) {
        val out = _file ?: return
        synchronized(this) {
            val fileName = irFile.fileEntry.name
            val packageName = irFile.packageFqName.asString()
            val visitor = BeforeHairVisitor(fileName, data)
            for (declaration in irFile.declarations) {
                declaration.accept(visitor, PathContext(packageName, null))
            }
            dump(out)
        }
    }

    fun lowerAfter(irFile: IrFile, hair: Map<IrFunction, FunctionCompilation>) {
        val out = _file ?: return
        synchronized(this) {
            val fileName = irFile.fileEntry.name
            val visitor = AfterHairVisitor(fileName, data, hair)
            for (declaration in irFile.declarations) {
                declaration.accept(visitor, Unit)
            }
            dump(out)
        }
    }

    /**
     * Rewrites the output file with the full accumulated state on every call,
     * so the summary appears exactly once and no per-file repetition sneaks in.
     */
    private fun dump(out: File) {
        val entries = data.values.filter { it.hasAnyArrayAccesses() }
        if (entries.isEmpty()) {
            out.writeText("")
            return
        }

        // ---------- summaries ----------

        fun sumPerDepth(selector: (AnalysisData) -> List<Int>): IntArray {
            val maxDepth = entries.maxOfOrNull { selector(it).size } ?: 0
            val results = IntArray(maxDepth)
            for (analysisData in entries) {
                for ([depth, count] in selector(analysisData).withIndex()) {
                    results[depth] += count
                }
            }
            return results
        }

        // Top-of-file summary as a table: one row per IR loop depth, with
        // "before" and "after" halves each split into BC / NoBC / total
        // columns. Much easier to scan than the previous single-line-per-
        // direction compact format.
        fun formatSummaryTable(
                bcBefore: IntArray, noBcBefore: IntArray,
                bcAfter: IntArray, noBcAfter: IntArray,
        ): String {
            val maxDepth = maxOf(bcBefore.size, noBcBefore.size, bcAfter.size, noBcAfter.size)
            if (maxDepth == 0) return "Summary: (no array accesses)\n"
            data class Row(
                    val depth: Int,
                    val bB: Int, val nB: Int, val tB: Int,
                    val bA: Int, val nA: Int, val tA: Int,
            )
            val rows = (0 until maxDepth).map { d ->
                val bB = bcBefore.getOrElse(d) { 0 }
                val nB = noBcBefore.getOrElse(d) { 0 }
                val bA = bcAfter.getOrElse(d) { 0 }
                val nA = noBcAfter.getOrElse(d) { 0 }
                Row(d, bB, nB, bB + nB, bA, nA, bA + nA)
            }
            // Totals row across all depths.
            val totals = Row(
                    -1,
                    rows.sumOf { it.bB }, rows.sumOf { it.nB }, rows.sumOf { it.tB },
                    rows.sumOf { it.bA }, rows.sumOf { it.nA }, rows.sumOf { it.tA },
            )
            val allRows = rows + totals

            fun colW(header: String, sel: (Row) -> Int): Int =
                    maxOf(header.length, allRows.maxOf { sel(it).toString().length })

            val depthW = maxOf("depth".length, "total".length, (maxDepth - 1).toString().length)
            val bBW = colW("BC", Row::bB);   val nBW = colW("NoBC", Row::nB);   val tBW = colW("total", Row::tB)
            val bAW = colW("BC", Row::bA);   val nAW = colW("NoBC", Row::nA);   val tAW = colW("total", Row::tA)

            val beforeGroupW = bBW + nBW + tBW + 4 // 3 cols + 2 separator spaces + 2 padding
            val afterGroupW  = bAW + nAW + tAW + 4

            fun String.padCenter(w: Int): String {
                if (length >= w) return this
                val total = w - length
                val left = total / 2
                val right = total - left
                return " ".repeat(left) + this + " ".repeat(right)
            }

            return buildString {
                // Group header: "before hair" / "after hair"
                append("  ")
                append(" ".repeat(depthW))
                append("  ")
                append("before hair".padCenter(beforeGroupW))
                append("  ")
                append("after hair".padCenter(afterGroupW))
                append("\n")
                // Column header
                append("  ")
                append("depth".padStart(depthW))
                append("  ")
                append("BC".padStart(bBW)); append("  ")
                append("NoBC".padStart(nBW)); append("  ")
                append("total".padStart(tBW))
                append("  ")
                append("BC".padStart(bAW)); append("  ")
                append("NoBC".padStart(nAW)); append("  ")
                append("total".padStart(tAW))
                append("\n")
                // Data rows
                for (r in allRows) {
                    append("  ")
                    val depthLabel = if (r.depth < 0) "total" else r.depth.toString()
                    append(depthLabel.padStart(depthW))
                    append("  ")
                    append(r.bB.toString().padStart(bBW)); append("  ")
                    append(r.nB.toString().padStart(nBW)); append("  ")
                    append(r.tB.toString().padStart(tBW))
                    append("  ")
                    append(r.bA.toString().padStart(bAW)); append("  ")
                    append(r.nA.toString().padStart(nAW)); append("  ")
                    append(r.tA.toString().padStart(tAW))
                    append("\n")
                }
            }
        }

        val beforeBcSums = sumPerDepth { it.beforeBC }
        val beforeNoBcSums = sumPerDepth { it.beforeNoBC }
        val beforeMaxDepth = maxOf(beforeBcSums.size, beforeNoBcSums.size)

        // Per-depth after-hair sums. Compiled entries contribute their
        // HaIR-derived per-depth counts; failed entries fall back to the IR
        // (before) per-depth numbers (codegen fallback path), matching the
        // per-entry policy in `AnalysisData.toString()`; not-run entries
        // contribute nothing.
        fun sumAfterPerDepth(pick: (AnalysisData) -> List<Int>, fallback: (AnalysisData) -> List<Int>): IntArray {
            val relevant = entries.filter { it.hairStatus != HairStatus.NOT_RUN }
            val maxDepth = relevant.maxOfOrNull {
                if (it.hairStatus == HairStatus.COMPILED) pick(it).size else fallback(it).size
            } ?: 0
            val out = IntArray(maxDepth)
            for (e in relevant) {
                val src = if (e.hairStatus == HairStatus.COMPILED) pick(e) else fallback(e)
                for ([depth, count] in src.withIndex()) out[depth] += count
            }
            return out
        }
        val afterBcSums = sumAfterPerDepth({ it.afterBC }, { it.beforeBC })
        val afterNoBcSums = sumAfterPerDepth({ it.afterNoBC }, { it.beforeNoBC })

        val compiledCount = entries.count { it.hairStatus == HairStatus.COMPILED }
        val failedCount = entries.count { it.hairStatus == HairStatus.FAILED }
        val notRunCount = entries.count { it.hairStatus == HairStatus.NOT_RUN }
        val totalFunctions = compiledCount + failedCount + notRunCount

        // ---------- comparison / effectiveness ----------
        //
        // Three views, all derived from the same numbers we already have:
        //   1. Overall: BC before → BC after across ALL entries (compiled +
        //      failed IR-fallback + not-run treated as 0-contribution). This
        //      is the "what actually reaches codegen" story.
        //   2. Compiled-only: BC before → BC after restricted to functions
        //      HaIR successfully compiled. This is the "HaIR effectiveness"
        //      story — the number to look at when comparing HaIR versions.
        //   3. Coverage: how much of the codebase HaIR handled, both by
        //      function count and by array-access count. Puts (2) in
        //      perspective: 100% elimination on 5% of accesses is not the
        //      same as 20% elimination on 90% of accesses.

        fun pct(num: Int, den: Int): String =
                if (den == 0) "  n/a" else "%5.1f%%".format(num * 100.0 / den)

        val compiledEntries = entries.filter { it.hairStatus == HairStatus.COMPILED }
        fun Iterable<AnalysisData>.sumBeforeBC() = sumOf { it.beforeBC.sum() }
        fun Iterable<AnalysisData>.sumBeforeNoBC() = sumOf { it.beforeNoBC.sum() }
        fun Iterable<AnalysisData>.sumAfterBC() = sumOf { it.afterBC.sum() }
        fun Iterable<AnalysisData>.sumAfterNoBC() = sumOf { it.afterNoBC.sum() }

        val overallBcBefore = beforeBcSums.sum()
        val overallNoBcBefore = beforeNoBcSums.sum()
        val overallTotalBefore = overallBcBefore + overallNoBcBefore
        val overallBcAfter = afterBcSums.sum()
        val overallNoBcAfter = afterNoBcSums.sum()
        val overallTotalAfter = overallBcAfter + overallNoBcAfter
        val overallBcRemoved = overallBcBefore - overallBcAfter

        val cBcBefore = compiledEntries.sumBeforeBC()
        val cNoBcBefore = compiledEntries.sumBeforeNoBC()
        val cTotalBefore = cBcBefore + cNoBcBefore
        val cBcAfter = compiledEntries.sumAfterBC()
        val cNoBcAfter = compiledEntries.sumAfterNoBC()
        val cTotalAfter = cBcAfter + cNoBcAfter
        val cBcRemoved = cBcBefore - cBcAfter

        // Per-depth relative BC reduction (overall).
        val perDepthReduction = buildString {
            append("  BC reduction by depth:\n")
            for (i in 0 until beforeMaxDepth) {
                val b = beforeBcSums.getOrElse(i) { 0 }
                val a = afterBcSums.getOrElse(i) { 0 }
                val removed = b - a
                append("    depth $i: %5d -> %5d  (removed %5d, %s)\n".format(b, a, removed, pct(removed, b)))
            }
        }

        val comparison = buildString {
            append("Comparison:\n")
            append("  Overall (compiled + IR fallback for failed):\n")
            append("    accesses before: $overallTotalBefore  (BC=$overallBcBefore, NoBC=$overallNoBcBefore)\n")
            append("    accesses after : $overallTotalAfter  (BC=$overallBcAfter, NoBC=$overallNoBcAfter)\n")
            append("    BC rate before : ${pct(overallBcBefore, overallTotalBefore)}\n")
            append("    BC rate after  : ${pct(overallBcAfter, overallTotalAfter)}\n")
            append("    BCs removed    : $overallBcRemoved  (${pct(overallBcRemoved, overallBcBefore)} of before-BC)\n")
            append("  HaIR effectiveness (compiled functions only):\n")
            append("    compiled       : $compiledCount / $totalFunctions  (${pct(compiledCount, totalFunctions)})\n")
            append("    accesses seen  : $cTotalBefore -> $cTotalAfter  (BC=$cBcBefore->$cBcAfter, NoBC=$cNoBcBefore->$cNoBcAfter)\n")
            append("    BC rate before : ${pct(cBcBefore, cTotalBefore)}\n")
            append("    BC rate after  : ${pct(cBcAfter, cTotalAfter)}\n")
            append("    BCs eliminated : $cBcRemoved  (${pct(cBcRemoved, cBcBefore)} of compiled before-BC)\n")
            append("  Coverage:\n")
            append("    HaIR-compiled functions : $compiledCount / $totalFunctions  (${pct(compiledCount, totalFunctions)})\n")
            append("    HaIR-compiled BCs       : $cBcBefore / $overallBcBefore  (${pct(cBcBefore, overallBcBefore)} of all before-BC)\n")
            append("    HaIR-compiled accesses  : $cTotalBefore / $overallTotalBefore  (${pct(cTotalBefore, overallTotalBefore)} of all before-accesses)\n")
            append(perDepthReduction)
        }

        // ---------- top by depth ----------

        fun AnalysisData.beforeTotalAt(depth: Int): Int =
                beforeBC.getOrElse(depth) { 0 } + beforeNoBC.getOrElse(depth) { 0 }

        fun AnalysisData.beforeTotalOverAllDepths(): Int =
                beforeBC.sum() + beforeNoBC.sum()

        fun formatTopEntry(label: String, entry: AnalysisData?): String = buildString {
            if (entry == null) {
                append("  $label: -\n")
                return@buildString
            }
            // Reuse AnalysisData.toString() so the top-by-depth entries carry the
            // same before/after BC/NoBC/total block as the per-file breakdown.
            // toString() starts with the function name on its own line; we
            // prepend "  $label: " to that first line and indent the rest by
            // two extra spaces so it visually nests under the label.
            val lines = entry.toString().lines()
            append("  $label: ${lines.first()}\n")
            for (line in lines.drop(1)) {
                append("  $line\n")
            }
        }

        val topByDepth = buildString {
            append("Top by depth (before hair):\n")
            for (depth in 0 until beforeMaxDepth) {
                val top = entries.maxByOrNull { it.beforeTotalAt(depth) }
                        ?.takeIf { it.beforeTotalAt(depth) > 0 }
                append(formatTopEntry(depth.toString(), top))
            }
            val topTotal = entries.maxByOrNull { it.beforeTotalOverAllDepths() }
                    ?.takeIf { it.beforeTotalOverAllDepths() > 0 }
            append(formatTopEntry("total", topTotal))
        }

        // ---------- per file / per function breakdown ----------

        val perFile = entries.groupBy { it.sourceFile }.entries.joinToString(
                separator = "", prefix = "\n", postfix = ""
        ) { [fileName, fileData] ->
            fileData.joinToString(separator = "\n  ", prefix = "$fileName\n  ", postfix = "\n")
        }

        // ---------- final ----------

        out.writeText(buildString {
            append("Summary (rows = IR loop depth, 0 = top level):\n")
            append(formatSummaryTable(beforeBcSums, beforeNoBcSums, afterBcSums, afterNoBcSums))
            append("\n")
            append("Hair status: compiled=$compiledCount, failed=$failedCount, not-run=$notRunCount\n")
            append("\n")
            append(comparison)
            append("\n")
            append(topByDepth)
            append(perFile)
        })
    }
}

// ============================================================================
// AnalysisData: one entry per IR simple function seen; carries both before-
// and after-hair numbers so they can be printed side by side.
// ============================================================================

enum class HairStatus {
    // Before-hair pass ran but after-hair pass hasn't been invoked yet for
    // this function (e.g. HaIR generation is disabled).
    NOT_RUN,
    // HaIR successfully compiled this function; afterBC / afterNoBC are meaningful.
    COMPILED,
    // HaIR bailed out on this function; afterBC / afterNoBC are meaningless.
    FAILED,
}

data class AnalysisData(
        val sourceFile: String,
        val currentContainerName: String,
) {
    // Before hair: per loop-nesting depth (IR walk).
    val beforeBC: MutableList<Int> = mutableListOf(0)
    val beforeNoBC: MutableList<Int> = mutableListOf(0)
    var depth: Int = 0

    // After hair: per IR loop-nesting depth. Populated from a snapshot taken
    // inside `IrToHair` right after `eliminateBoundsChecks()`, where each
    // surviving array-access node is bucketed by the IR loop depth it was
    // emitted at (tracked in the IR visitor). Empty when hair failed / didn't
    // run — in those cases the before-hair numbers are the effective after.
    val afterBC: MutableList<Int> = mutableListOf()
    val afterNoBC: MutableList<Int> = mutableListOf()
    var hairStatus: HairStatus = HairStatus.NOT_RUN

    fun ensureDepth(depth: Int) {
        while (beforeBC.size <= depth) beforeBC.add(0)
        while (beforeNoBC.size <= depth) beforeNoBC.add(0)
    }

    fun hasAnyArrayAccesses(): Boolean =
            beforeBC.any { it != 0 } ||
                    beforeNoBC.any { it != 0 } ||
                    afterBC.any { it != 0 } ||
                    afterNoBC.any { it != 0 }

    override fun toString(): String = buildString {
        append(currentContainerName)
        append("\n")
        // Compact per-depth format: one line per direction (before / after),
        // each cell rendered as `bc/nobc(total)`. Columns are per-cell padded
        // independently across the row so cells stay aligned.
        fun renderRow(bc: List<Int>, noBc: List<Int>, label: String, suffix: String = ""): String {
            val maxDepth = maxOf(bc.size, noBc.size)
            if (maxDepth == 0) return "$label (none)$suffix\n"
            val cells = (0 until maxDepth).map { d ->
                val b = bc.getOrElse(d) { 0 }
                val n = noBc.getOrElse(d) { 0 }
                Triple(b, n, b + n)
            }
            val bcW = cells.maxOf { it.first.toString().length }
            val nW = cells.maxOf { it.second.toString().length }
            val tW = cells.maxOf { it.third.toString().length }
            return cells.joinToString(separator = " ", prefix = label, postfix = "$suffix\n") { [b, n, t] ->
                "${b.toString().padStart(bcW)}/${n.toString().padStart(nW)}(${t.toString().padStart(tW)})"
            }
        }
        append(renderRow(beforeBC, beforeNoBC, "    before: "))
        when (hairStatus) {
            HairStatus.NOT_RUN -> append("    after : (hair not run)\n")
            HairStatus.FAILED -> append(renderRow(beforeBC, beforeNoBC, "    after : ", "    (hair failed, IR fallback)"))
            HairStatus.COMPILED -> append(renderRow(afterBC, afterNoBC, "    after : "))
        }
    }
}

// ============================================================================
// Before-hair visitor: walks IR, counts array `get`/`set` and `*WithoutBoundCheck`
// per IR loop-nesting depth for each simple function.
// ============================================================================

private fun IrType.isBasicArray() = isPrimitiveArray() || isArray() || isUnsignedArray()

class PathContext(val path: String, val counting: AnalysisData?)

private class BeforeHairVisitor(
        private val sourceFile: String,
        private val results: MutableMap<String, AnalysisData>,
) : IrVisitor<Unit, PathContext>() {

    override fun visitElement(element: IrElement, data: PathContext) {
        element.acceptChildren(this, data)
    }

    override fun visitClass(declaration: IrClass, data: PathContext) {
        val classPath = "${data.path}.${declaration.name.asString()}"
        declaration.acceptChildren(this, PathContext(classPath, null))
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: PathContext) {
        val funcPath = declaration.computeFullName()
        val analysisData = results.getOrPut(funcPath) {
            AnalysisData(sourceFile, funcPath)
        }
        declaration.acceptChildren(this, PathContext(funcPath, analysisData))
    }

    override fun visitLoop(loop: IrLoop, data: PathContext) {
        val counting = data.counting ?: return super.visitLoop(loop, data)
        counting.depth++
        counting.ensureDepth(counting.depth)
        loop.acceptChildren(this, data)
        counting.depth--
    }

    override fun visitCall(expression: IrCall, data: PathContext) {
        fun incrementBC(counting: AnalysisData) {
            counting.ensureDepth(counting.depth)
            counting.beforeBC[counting.depth]++
        }

        fun incrementNoBC(counting: AnalysisData) {
            counting.ensureDepth(counting.depth)
            counting.beforeNoBC[counting.depth]++
        }

        val counting = data.counting ?: return super.visitCall(expression, data)
        val receiverType = expression.dispatchReceiver?.type
        if (receiverType?.isBasicArray() != true) return super.visitCall(expression, data)
        val function = expression.symbol.owner

        when (function.name) {
            OperatorNameConventions.SET -> incrementBC(counting)
            OperatorNameConventions.GET -> incrementBC(counting)
            KonanNameConventions.setWithoutBoundCheck -> incrementNoBC(counting)
            KonanNameConventions.getWithoutBoundCheck -> incrementNoBC(counting)
        }

        return super.visitCall(expression, data)
    }
}

// ============================================================================
// After-hair visitor: for each IR simple function that has a body, either
// pulls counts from the HaIR compilation (BC = surviving `ArrayIndexCheck`,
// NoBC = LoadArrayElement + StoreArrayElement - BC), or marks the function
// "hair failed" when it's absent from the map.
// ============================================================================

private class AfterHairVisitor(
        private val sourceFile: String,
        private val results: MutableMap<String, AnalysisData>,
        private val hair: Map<IrFunction, FunctionCompilation>,
) : IrVisitor<Unit, Unit>() {

    override fun visitElement(element: IrElement, data: Unit) {
        element.acceptChildren(this, data)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: Unit) {
        val funcPath = declaration.computeFullName()
        val analysisData = results.getOrPut(funcPath) { AnalysisData(sourceFile, funcPath) }

        val funCompilation = hair[declaration]
        when {
            funCompilation != null -> {
                // NOTE: read counts from the snapshot recorded by IrToHair
                // right after `eliminateBoundsChecks()` — reading from the
                // live session at this point would give 0 checks for every
                // function because HaIR's `lower()` pass deletes the
                // `ArrayIndexCheck` nodes as part of code lowering.
                val snapshot = Dumper.getHairSnapshot(funCompilation)
                if (snapshot != null) {
                    // Align the three per-depth lists: at any depth `d`,
                    // total = loads[d] + stores[d]; BC = checksAfterBce[d];
                    // NoBC = max(0, total - BC). Depth is the IR loop-nesting
                    // depth captured at HaIR emission time.
                    val maxDepth = maxOf(
                            snapshot.checksAfterBcePerDepth.size,
                            snapshot.loadsPerDepth.size,
                            snapshot.storesPerDepth.size,
                    )
                    val bcOut = MutableList(maxDepth) { 0 }
                    val noBcOut = MutableList(maxDepth) { 0 }
                    for (d in 0 until maxDepth) {
                        val bc = snapshot.checksAfterBcePerDepth.getOrElse(d) { 0 }
                        val total = snapshot.loadsPerDepth.getOrElse(d) { 0 } +
                                snapshot.storesPerDepth.getOrElse(d) { 0 }
                        bcOut[d] = bc
                        noBcOut[d] = (total - bc).coerceAtLeast(0)
                    }
                    analysisData.afterBC.clear(); analysisData.afterBC.addAll(bcOut)
                    analysisData.afterNoBC.clear(); analysisData.afterNoBC.addAll(noBcOut)
                    analysisData.hairStatus = HairStatus.COMPILED
                } else {
                    // FunctionCompilation exists but no snapshot was recorded
                    // — treat as failed (shouldn't normally happen).
                    analysisData.hairStatus = HairStatus.FAILED
                }
            }
            declaration.shouldGenerateBody() -> {
                // Function was a candidate for HaIR but is absent from the map,
                // so HaIR bailed out on it (see IrToHair.HairGenerator.lower).
                analysisData.hairStatus = HairStatus.FAILED
            }
            // else: not a HaIR candidate (abstract / external / ...), leave NOT_RUN.
        }

        declaration.acceptChildren(this, data)
    }
}
