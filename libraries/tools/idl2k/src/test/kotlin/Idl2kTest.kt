/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */


import org.jetbrains.idl2k.BuildWebIdl
import org.jetbrains.idl2k.render
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.io.StringWriter
import java.io.Writer


class Idl2kTests {


    private fun convertIdlToWriter(file: File): Writer {
        val nonExistentCache = File.createTempFile("mdnCache", System.nanoTime().toString())
        val buildWebIdl = BuildWebIdl(nonExistentCache, file)

        val stringWriter = StringWriter()
        stringWriter.render(
            "",
            buildWebIdl.definitions,
            buildWebIdl.unions,
            buildWebIdl.repository.enums.values.toList(),
            buildWebIdl.mdnCache
        )

        nonExistentCache.delete()
        return stringWriter
    }

    private fun assertIdlCompiledTo(fileName: String, output: String) {
        assertEquals(
            output.trimIndent(),
            convertIdlToWriter(File(fileName)).toString()
        )
    }

    @Test
    fun basicTest() {
        assertIdlCompiledTo(
            "src/test/resources/SomethingNotInCache.idl", """public external open class SomethingNotInCache {
    open val someReadOnlyParam: dynamic
    var someWriteableParam: dynamic
    fun someEmptyMethod(): String
    fun someMethod(root: dynamic): String
    fun optionalUsvStringFetcher(name: String): String?
}


"""
        )
    }
}
