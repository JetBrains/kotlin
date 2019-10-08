/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.stubBuilder

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.BinaryLightVirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.registerServiceInstance
import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.idea.caches.IDEKotlinBinaryClassCache
import org.jetbrains.kotlin.idea.decompiler.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.idea.decompiler.classFile.KotlinClsStubBuilder
import org.jetbrains.kotlin.idea.decompiler.classFile.KtClsFile
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil
import org.jetbrains.kotlin.psi.stubs.elements.KtFileStubBuilder
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils.getAnnotationsJar
import org.jetbrains.kotlin.test.KotlinTestUtils.newConfiguration
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.TestJdkKind
import org.junit.Assert
import java.io.File

abstract class AbstractLoadJavaClsStubTest : TestCaseWithTmpdir() {
    @Throws(Exception::class)
    protected fun doTestCompiledKotlin(ktFileName: String) {
        doTestCompiledKotlin(ktFileName, ConfigurationKind.JDK_ONLY, false)
    }

    @Throws(Exception::class)
    private fun doTestCompiledKotlin(ktFileName: String, configurationKind: ConfigurationKind, useTypeTableInSerializer: Boolean) {
        val ktFile = File(ktFileName)

        val configuration = newConfiguration(configurationKind, TestJdkKind.MOCK_JDK, getAnnotationsJar())
        if (useTypeTableInSerializer) {
            configuration.put(JVMConfigurationKeys.USE_TYPE_TABLE, true)
        }
        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        environment.projectEnvironment.environment.application.registerService(
            IDEKotlinBinaryClassCache::class.java,
            IDEKotlinBinaryClassCache()
        )

        LoadDescriptorUtil.compileKotlinToDirAndGetModule(listOf(ktFile), tmpdir, environment)

        val classFiles = tmpdir.walk().filter { it.extension == "class" }.toList()

        val testDir = File(tmpdir, "test")

        val fileChildren = HashMap<String, VirtualFile>()
        val parentDir = object : LightVirtualFile(testDir.absolutePath) {
            override fun findChild(name: String) = fileChildren[name]
        }

        classFiles.forEach { classFile ->
            Assert.assertTrue(classFile.parent == testDir.absolutePath)
            fileChildren[classFile.name] = object : BinaryLightVirtualFile(classFile.name, classFile.readBytes()) {
                override fun getParent(): VirtualFile = parentDir
            }
        }

        for (file in fileChildren.values) {
            val fileContent = FileContentImpl.createByFile(file)

            val stubTreeFromCls = KotlinClsStubBuilder().buildFileStub(fileContent)

            if (stubTreeFromCls != null) {
                val stubsFromDeserializedDescriptors = run {
                    val decompiledProvider = KotlinDecompiledFileViewProvider(PsiManager.getInstance(environment.project), file, true) { provider ->
                        KtClsFile(provider)
                    }

                    KtFileStubBuilder().buildStubTree(KtClsFile(decompiledProvider))
                }

                Assert.assertEquals("File: ${file.name}", stubsFromDeserializedDescriptors.serializeToString(), stubTreeFromCls.serializeToString())
            }
        }
    }
}