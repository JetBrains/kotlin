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

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.analysis.Stability
import androidx.compose.compiler.plugins.kotlin.analysis.StabilityInferencer
import androidx.compose.compiler.plugins.kotlin.analysis.knownStable
import androidx.compose.compiler.plugins.kotlin.analysis.knownUnstable
import androidx.compose.compiler.plugins.kotlin.lower.ComposableFunctionBodyTransformer
import androidx.compose.compiler.plugins.kotlin.lower.IrSourcePrinterVisitor
import androidx.compose.compiler.plugins.kotlin.lower.isUnitOrNullableUnit
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtParameter
import java.io.File

interface FunctionMetrics {
    val isEmpty: Boolean get() = false
    val packageName: FqName
    val name: String
    val composable: Boolean
    val skippable: Boolean
    val restartable: Boolean
    val readonly: Boolean
    val inline: Boolean
    val isLambda: Boolean
    val hasDefaults: Boolean
    val defaultsGroup: Boolean
    val groups: Int
    val calls: Int
    val scheme: String?
    fun recordGroup()
    fun recordComposableCall(
        expression: IrCall,
        paramMeta: List<ComposableFunctionBodyTransformer.ParamMeta>
    )
    fun recordParameter(
        declaration: IrValueParameter,
        type: IrType,
        stability: Stability,
        default: IrExpression?,
        defaultStatic: Boolean,
        used: Boolean,
    )
    fun recordFunction(
        composable: Boolean,
        restartable: Boolean,
        skippable: Boolean,
        isLambda: Boolean,
        inline: Boolean,
        hasDefaults: Boolean,
        readonly: Boolean
    )
    fun recordScheme(
        scheme: String
    )
    fun print(out: Appendable, src: IrSourcePrinterVisitor)
}

interface ModuleMetrics {
    val isEmpty get() = false

    fun recordFunction(
        function: FunctionMetrics,
    )
    fun recordClass(
        declaration: IrClass,
        marked: Boolean,
        stability: Stability,
    )
    fun recordLambda(
        composable: Boolean,
        memoized: Boolean,
        singleton: Boolean,
    )
    fun recordComposableCall(
        expression: IrCall,
        paramMeta: List<ComposableFunctionBodyTransformer.ParamMeta>
    )
    fun log(message: String)
    fun Appendable.appendModuleJson()
    fun Appendable.appendComposablesCsv()
    fun Appendable.appendComposablesTxt()
    fun Appendable.appendClassesTxt()
    fun saveMetricsTo(directory: String)
    fun saveReportsTo(directory: String)
    fun makeFunctionMetrics(function: IrFunction): FunctionMetrics
}

object EmptyModuleMetrics : ModuleMetrics {
    override val isEmpty: Boolean get() = true
    override fun recordFunction(function: FunctionMetrics) {}
    override fun recordClass(declaration: IrClass, marked: Boolean, stability: Stability) {}
    override fun recordLambda(composable: Boolean, memoized: Boolean, singleton: Boolean) {}
    override fun recordComposableCall(
        expression: IrCall,
        paramMeta: List<ComposableFunctionBodyTransformer.ParamMeta>
    ) {}
    override fun log(message: String) {
        println(message)
    }
    override fun Appendable.appendModuleJson() {}
    override fun Appendable.appendComposablesCsv() {}
    override fun Appendable.appendComposablesTxt() {}
    override fun Appendable.appendClassesTxt() {}
    override fun saveMetricsTo(directory: String) {}
    override fun saveReportsTo(directory: String) {}
    override fun makeFunctionMetrics(function: IrFunction): FunctionMetrics = EmptyFunctionMetrics
}

object EmptyFunctionMetrics : FunctionMetrics {
    private fun emptyMetricsAccessed(): Nothing = error("Empty metrics accessed")
    override val isEmpty: Boolean get() = true
    override val packageName: FqName
        get() = emptyMetricsAccessed()
    override val name: String
        get() = emptyMetricsAccessed()
    override val composable: Boolean
        get() = emptyMetricsAccessed()
    override val skippable: Boolean
        get() = emptyMetricsAccessed()
    override val restartable: Boolean
        get() = emptyMetricsAccessed()
    override val readonly: Boolean
        get() = emptyMetricsAccessed()
    override val inline: Boolean
        get() = emptyMetricsAccessed()
    override val isLambda: Boolean
        get() = emptyMetricsAccessed()
    override val hasDefaults: Boolean
        get() = emptyMetricsAccessed()
    override val defaultsGroup: Boolean
        get() = emptyMetricsAccessed()
    override val groups: Int
        get() = emptyMetricsAccessed()
    override val calls: Int
        get() = emptyMetricsAccessed()
    override val scheme: String
        get() = emptyMetricsAccessed()
    override fun recordGroup() {}
    override fun recordComposableCall(
        expression: IrCall,
        paramMeta: List<ComposableFunctionBodyTransformer.ParamMeta>
    ) {}
    override fun recordParameter(
        declaration: IrValueParameter,
        type: IrType,
        stability: Stability,
        default: IrExpression?,
        defaultStatic: Boolean,
        used: Boolean
    ) {}
    override fun recordFunction(
        composable: Boolean,
        restartable: Boolean,
        skippable: Boolean,
        isLambda: Boolean,
        inline: Boolean,
        hasDefaults: Boolean,
        readonly: Boolean
    ) {}
    override fun recordScheme(scheme: String) {}
    override fun print(out: Appendable, src: IrSourcePrinterVisitor) {}
}

class ModuleMetricsImpl(
    var name: String,
    context: IrPluginContext
) : ModuleMetrics {
    val stabilityInferencer = StabilityInferencer(context)
    private var skippableComposables = 0
    private var restartableComposables = 0
    private var readonlyComposables = 0
    private var totalComposables = 0
    private var restartGroups = 0
    private var totalGroups = 0
    private var staticArguments = 0
    private var certainArguments = 0
    private var knownStableArguments = 0
    private var knownUnstableArguments = 0
    private var unknownStableArguments = 0
    private var totalArguments = 0
    private var markedStableClasses = 0
    private var inferredStableClasses = 0
    private var inferredUnstableClasses = 0
    private var inferredUncertainClasses = 0
    private var effectivelyStableClasses = 0
    private var totalClasses = 0
    private var memoizedLambdas = 0
    private var singletonLambdas = 0
    private var singletonComposableLambdas = 0
    private var composableLambdas = 0
    private var totalLambdas = 0

    private val composables = mutableListOf<FunctionMetrics>()
    private val classes = mutableListOf<ClassMetrics>()
    private val logMessages = mutableListOf<String>()

    private inner class ClassMetrics(
        val declaration: IrClass,
        val marked: Boolean,
        val stability: Stability
    ) {

        private fun Stability.simpleHumanReadable() = when {
            knownStable() -> "stable"
            knownUnstable() -> "unstable"
            else -> "runtime"
        }
        fun print(out: Appendable, src: IrSourcePrinterVisitor) = with(out) {
            append("${stability.simpleHumanReadable()} ")
            append("class ")
            append(declaration.name.asString())
            appendLine(" {")
            for (decl in declaration.declarations) {
                val isVar = when (decl) {
                    is IrProperty -> decl.isVar
                    is IrField -> true
                    else -> false
                }
                val field = when (decl) {
                    is IrProperty -> decl.backingField ?: continue
                    is IrField -> decl
                    else -> continue
                }
                if (field.name == KtxNameConventions.STABILITY_FLAG) continue
                append("  ")
                val fieldStability = stabilityInferencer.stabilityOf(field.type)
                append("${fieldStability.simpleHumanReadable()} ")
                append(if (isVar) "var " else "val ")
                append(field.name.asString())
                append(": ")
                append(src.printType(field.type))
                appendLine()
            }
            if (!marked) {
                appendLine("  <runtime stability> = $stability")
            }
            appendLine("}")
        }
    }

    override fun recordFunction(function: FunctionMetrics) {
        if (!function.composable) return
        totalComposables++
        if (!function.isLambda) composables.add(function)
        if (function.readonly) readonlyComposables++
        if (function.skippable) skippableComposables++
        if (function.restartable) {
            restartableComposables++
            restartGroups++
        }
        totalGroups += function.groups
    }

    override fun recordClass(
        declaration: IrClass,
        marked: Boolean,
        stability: Stability
    ) {
        classes.add(
            ClassMetrics(
                declaration,
                marked,
                stability
            )
        )
        totalClasses++
        when {
            marked -> {
                markedStableClasses++
                effectivelyStableClasses++
            }
            stability.knownStable() -> {
                inferredStableClasses++
                effectivelyStableClasses++
            }
            stability.knownUnstable() -> {
                inferredUnstableClasses++
            }
            else -> {
                inferredUncertainClasses++
            }
        }
    }

    override fun recordLambda(
        composable: Boolean,
        memoized: Boolean,
        singleton: Boolean,
    ) {
        totalLambdas++
        if (composable) composableLambdas++
        if (memoized) memoizedLambdas++
        if (composable && singleton) singletonComposableLambdas++
        if (!composable && singleton) singletonLambdas++
    }

    override fun recordComposableCall(
        expression: IrCall,
        paramMeta: List<ComposableFunctionBodyTransformer.ParamMeta>
    ) {
        for (arg in paramMeta) {
            totalArguments++
            if (arg.isCertain) certainArguments++
            if (arg.isStatic) staticArguments++
            when {
                arg.stability.knownStable() -> knownStableArguments++
                arg.stability.knownUnstable() -> knownUnstableArguments++
                else -> unknownStableArguments++
            }
        }
    }

    override fun log(message: String) {
        logMessages.add(message)
    }

    override fun Appendable.appendModuleJson() = appendJson {
        entry("skippableComposables", skippableComposables)
        entry("restartableComposables", restartableComposables)
        entry("readonlyComposables", readonlyComposables)
        entry("totalComposables", totalComposables)
        entry("restartGroups", restartGroups)
        entry("totalGroups", totalGroups)
        entry("staticArguments", staticArguments)
        entry("certainArguments", certainArguments)
        entry("knownStableArguments", knownStableArguments)
        entry("knownUnstableArguments", knownUnstableArguments)
        entry("unknownStableArguments", unknownStableArguments)
        entry("totalArguments", totalArguments)
        entry("markedStableClasses", markedStableClasses)
        entry("inferredStableClasses", inferredStableClasses)
        entry("inferredUnstableClasses", inferredUnstableClasses)
        entry("inferredUncertainClasses", inferredUncertainClasses)
        entry("effectivelyStableClasses", effectivelyStableClasses)
        entry("totalClasses", totalClasses)
        entry("memoizedLambdas", memoizedLambdas)
        entry("singletonLambdas", singletonLambdas)
        entry("singletonComposableLambdas", singletonComposableLambdas)
        entry("composableLambdas", composableLambdas)
        entry("totalLambdas", totalLambdas)
    }

    override fun Appendable.appendComposablesCsv() = appendCsv {
        row {
            col("package")
            col("name")
            col("composable")
            col("skippable")
            col("restartable")
            col("readonly")
            col("inline")
            col("isLambda")
            col("hasDefaults")
            col("defaultsGroup")
            col("groups")
            col("calls")
        }
        for (fn in composables) {
            row {
                col(fn.packageName.asString())
                col(fn.name)
                col(fn.composable)
                col(fn.skippable)
                col(fn.restartable)
                col(fn.readonly)
                col(fn.inline)
                col(fn.isLambda)
                col(fn.hasDefaults)
                col(fn.defaultsGroup)
                col(fn.groups)
                col(fn.calls)
            }
        }
    }

    override fun Appendable.appendComposablesTxt() {
        val src = IrSourcePrinterVisitor(this)
        for (fn in composables) {
            fn.print(this, src)
        }
    }

    override fun Appendable.appendClassesTxt() {
        val src = IrSourcePrinterVisitor(this)
        for (declaration in classes) {
            declaration.print(this, src)
        }
    }

    override fun saveMetricsTo(directory: String) {
        val dir = File(directory)
        val prefix = name
            .replace('.', '_')
            .replace("<", "")
            .replace(">", "")
        File(dir, "$prefix-module.json").write {
            appendModuleJson()
        }
    }

    override fun saveReportsTo(directory: String) {
        val dir = File(directory)
        val prefix = name
            .replace('.', '_')
            .replace("<", "")
            .replace(">", "")
        File(dir, "$prefix-composables.csv").write {
            appendComposablesCsv()
        }

        File(dir, "$prefix-composables.txt").write {
            appendComposablesTxt()
        }

        if (logMessages.isNotEmpty()) {
            File(dir, "$prefix-composables.log").write {
                for (line in logMessages) appendLine(line)
            }
        }

        File(dir, "$prefix-classes.txt").write {
            appendClassesTxt()
        }
    }

    override fun makeFunctionMetrics(function: IrFunction): FunctionMetrics =
        FunctionMetricsImpl(function)
}

class FunctionMetricsImpl(
    val function: IrFunction
) : FunctionMetrics {
    override var packageName: FqName = function.fqNameForIrSerialization
    override var name: String = function.name.asString()
    override var composable: Boolean = false
    override var skippable: Boolean = false
    override var restartable: Boolean = false
    override var readonly: Boolean = false
    override var inline: Boolean = false
    override var isLambda: Boolean = false
    override var hasDefaults: Boolean = false
    override var defaultsGroup: Boolean = false
    override var groups: Int = 0
    override var calls: Int = 0
    override var scheme: String? = null
    private class Param(
        val declaration: IrValueParameter,
        val type: IrType,
        val stability: Stability,
        val default: IrExpression?,
        val defaultStatic: Boolean,
        val used: Boolean
    ) {
        @OptIn(ObsoleteDescriptorBasedAPI::class)
        fun print(out: Appendable, src: IrSourcePrinterVisitor) = with(out) {
            if (!used) append("unused ")
            when {
                stability.knownStable() -> append("stable ")
                stability.knownUnstable() -> append("unstable ")
            }
            append(declaration.name.asString())
            append(": ")
            append(src.printType(type))
            if (default != null) {
                append(" = ")
                if (defaultStatic) append("@static ")
                else append("@dynamic ")
                val psi = declaration.symbol.descriptor.findPsi() as? KtParameter
                val str = psi?.defaultValue?.text
                if (str != null) {
                    append(str)
                } else {
                    default.accept(src, null)
                }
            }
        }
    }

    private val parameters = mutableListOf<Param>()

    override fun recordGroup() {
        groups++
    }

    override fun recordComposableCall(
        expression: IrCall,
        paramMeta: List<ComposableFunctionBodyTransformer.ParamMeta>
    ) {
        calls++
    }

    override fun recordFunction(
        composable: Boolean,
        restartable: Boolean,
        skippable: Boolean,
        isLambda: Boolean,
        inline: Boolean,
        hasDefaults: Boolean,
        readonly: Boolean
    ) {
        this.composable = composable
        this.restartable = restartable
        this.skippable = skippable
        this.isLambda = isLambda
        this.inline = inline
        this.hasDefaults = hasDefaults
        this.readonly = readonly
    }

    override fun recordParameter(
        declaration: IrValueParameter,
        type: IrType,
        stability: Stability,
        default: IrExpression?,
        defaultStatic: Boolean,
        used: Boolean,
    ) {
        parameters.add(
            Param(
                declaration,
                type,
                stability,
                default,
                defaultStatic,
                used,
            )
        )
    }

    override fun recordScheme(scheme: String) {
        this.scheme = scheme
    }

    override fun print(out: Appendable, src: IrSourcePrinterVisitor): Unit = with(out) {
        if (restartable) append("restartable ")
        if (skippable) append("skippable ")
        if (readonly) append("readonly ")
        if (inline) append("inline ")
        scheme?.let { append("scheme(\"$it\") ") }
        append("fun ")
        append(name)
        if (parameters.isEmpty()) {
            appendLine("()")
        } else {
            appendLine("(")
            for (param in parameters) {
                append("  ")
                param.print(out, src)
                appendLine()
            }
            append(")")
            if (!function.returnType.isUnitOrNullableUnit()) {
                append(": ")
                append(src.printType(function.returnType))
            }
            appendLine()
        }
    }
}
