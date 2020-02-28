package org.jetbrains.kotlin.mainKts.test

import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.junit.Assert
import org.junit.Test
import javax.script.ScriptEngineManager

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class MainKtsJsr223Test {

    init {
        setIdeaIoUseFallback()
    }

    @Test
    fun testSimpleEval() {
        val engine = ScriptEngineManager().getEngineByExtension("main.kts")!!
        val res1 = engine.eval("val x = 3")
        Assert.assertNull(res1)
        val res2 = engine.eval("x + 2")
        Assert.assertEquals(5, res2)
    }

    @Test
    fun testWithDirectBindings() {
        val engine = ScriptEngineManager().getEngineByExtension("main.kts")!!
        engine.put("z", 6)
        val res1 = engine.eval("val x = 7")
        Assert.assertNull(res1)
        val res2 = engine.eval("z * x")
        Assert.assertEquals(42, res2)
    }

    @Test
    fun testWithImport() {
        val engine = ScriptEngineManager().getEngineByExtension("main.kts")!!
        val out = captureOut {
            val res1 = engine.eval("""
                @file:Import("$TEST_DATA_ROOT/import-common.main.kts")
                @file:Import("$TEST_DATA_ROOT/import-middle.main.kts")
                sharedVar = sharedVar + 1
                println(sharedVar)
            """.trimIndent())
            Assert.assertNull(res1)
        }.lines()
        Assert.assertEquals(listOf("Hi from common", "Hi from middle", "5"), out)
    }
}

