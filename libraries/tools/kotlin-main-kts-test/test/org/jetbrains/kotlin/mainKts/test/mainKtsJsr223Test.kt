package org.jetbrains.kotlin.mainKts.test

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import javax.script.ScriptEngineManager

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class MainKtsJsr223Test {

    @Test
    fun testSimpleEval() {
        val engine = ScriptEngineManager().getEngineByExtension("main.kts")!!
        val res1 = engine.eval("val x = 3")
        Assertions.assertNull(res1)
        val res2 = engine.eval("x + 2")
        Assertions.assertEquals(5, res2)
    }

    @Test
    fun testWithDirectBindings() {
        val engine = ScriptEngineManager().getEngineByExtension("main.kts")!!
        engine.put("z", 6)
        val res1 = engine.eval("val x = 7")
        Assertions.assertNull(res1)
        val res2 = engine.eval("z * x")
        Assertions.assertEquals(42, res2)
    }

    @Test
    @Disabled(
        """ BLOCKED-DESIGN-G15: K2ReplCompiler's isReplSnippetSource predicate is session-wide 'true', so the @file:Import(...)ed scripts
            are compiled as REPL snippets (FirReplSnippet) instead of scripts (FirScript) since the light-tree snippet builder landed (KT-83498).
            Several snippets in one FIR session are not supported by the REPL resolution: the imports are collected correctly (K2 refinement),
            but the declarations of the imported "snippets" are not visible to each other nor to the root snippet, so it fails with
            "Unresolved reference 'sharedVar'" in import-middle.main.kts and in the root snippet.
            Fix: narrow the predicate to the root snippet source (see current/80-known-gotchas.md G15 / target/90-open-questions.md Q2).
            """
    )
    fun testWithImport() {
        val engine = ScriptEngineManager().getEngineByExtension("main.kts")!!
        val out = captureOut {
            val res1 = engine.eval("""
                @file:Import("$TEST_DATA_ROOT/import-common.main.kts")
                @file:Import("$TEST_DATA_ROOT/import-middle.main.kts")
                sharedVar = sharedVar + 1
                println(sharedVar)
            """.trimIndent())
            Assertions.assertNull(res1)
        }.lines()
        Assertions.assertEquals(listOf("Hi from common", "Hi from middle", "5"), out)
    }
}
