/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js

import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.emptyLoggingContext
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsMangler
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.SymbolTable

class ScriptDependencyCompiler(
    val environment: KotlinCoreEnvironment,
    private val nameTables: NameTables = NameTables(emptyList())
) {
    fun compile(dependencies: List<ModuleDescriptor>): Pair<String, NameTables> {
        val compiler = CoreScriptingJsCompiler(
            environment,
            nameTables,
            dependencies,
            ::createDeserializer
        )
        val compileResult = compiler.compile(makeReplCodeLine(0, ""))
        return (compileResult as ReplCompileResult.CompiledClasses).data as String to nameTables
    }

    private fun createDeserializer(m: ModuleDescriptor, symbolTable: SymbolTable, irBuiltIns: IrBuiltIns): DeserializerWithDependencies {
        val deserializer = JsIrLinker(m, JsMangler, emptyLoggingContext, irBuiltIns, symbolTable)
        val deserializedModuleFragments = m.allDependencyModules.map {
            deserializer.deserializeFullModule(it)
        }

        return DeserializerWithDependencies(deserializer, deserializedModuleFragments)
    }
}
