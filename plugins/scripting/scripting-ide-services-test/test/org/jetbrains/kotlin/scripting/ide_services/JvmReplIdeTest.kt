/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services

import com.intellij.mock.MockApplication
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.indexing.FileContentImpl
import junit.framework.TestCase
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinClassFileDecompiler
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsKotlinBinaryClassCache
import org.jetbrains.kotlin.analysis.decompiler.stub.file.DummyFileAttributeService
import org.jetbrains.kotlin.analysis.decompiler.stub.file.FileAttributeService
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.KJvmCompiledModuleInMemoryImpl
import org.jetbrains.kotlin.scripting.ide_services.test_util.JvmTestRepl
import org.jetbrains.kotlin.scripting.ide_services.test_util.checkCompile
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.script.experimental.jvm.impl.KJvmCompiledModuleInMemory
import kotlin.script.experimental.util.get

// This test checks the functionality that works only in IDE
// and doesn't run with embeddableTest configuration
class JvmReplIdeTest : TestCase() {
    fun testReplScriptClassFileDecompilation() {
        JvmTestRepl()
            .use { repl ->
                val compiledSnippet = checkCompile(repl, "10 + 10")
                val snippetValue = compiledSnippet.get()!!

                val compiledModule = snippetValue.getCompiledModule() as KJvmCompiledModuleInMemoryImpl
                val folder = saveCompiledOutput("repl-script-decompilation", compiledModule)

                val scriptClassName = "Line_0_simplescript"
                val fileUrl = "file://" + folder.resolve("$scriptClassName.class").invariantSeparatorsPath
                val vFile = VirtualFileManager.getInstance().findFileByUrl(fileUrl)!!
                val fileContent = FileContentImpl.createByContent(vFile, vFile.contentsToByteArray(false))

                val application = ApplicationManager.getApplication() as MockApplication
                KotlinCoreEnvironment.underApplicationLock {
                    registerDecompilerServices(application)
                }

                val fileStub = KotlinClassFileDecompiler().stubBuilder.buildFileStub(fileContent)!!
                val childrenStubs = fileStub.childrenStubs
                assertTrue(childrenStubs.any { it is KotlinClassStub && it.name == scriptClassName })
            }
    }

    @OptIn(ExperimentalPathApi::class)
    companion object {
        private val outputJarDir = createTempDirectory("temp-ide-services-ide-test")

        private fun saveCompiledOutput(subfolder: String, module: KJvmCompiledModuleInMemory): File {
            val folder = outputJarDir.resolve(subfolder).toFile()
            module.compilerOutputFiles.forEach { (name, contents) ->
                val file = folder.resolve(name)
                file.parentFile.mkdirs()
                file.writeBytes(contents)
            }
            return folder
        }

        private fun registerDecompilerServices(application: MockApplication) {
            application.registerService(FileAttributeService::class.java, DummyFileAttributeService())
            application.registerService(ClsKotlinBinaryClassCache::class.java, ClsKotlinBinaryClassCache())
        }
    }
}