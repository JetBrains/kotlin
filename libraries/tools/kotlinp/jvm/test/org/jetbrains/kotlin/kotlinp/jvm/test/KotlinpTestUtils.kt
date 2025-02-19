/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp.jvm.test

import com.intellij.openapi.Disposable
import junit.framework.TestCase.assertEquals
import org.jetbrains.kotlin.checkers.setupLanguageVersionSettingsForCompilerTests
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.jvm.compiler.AbstractLoadJavaTest
import org.jetbrains.kotlin.kotlinp.Settings
import org.jetbrains.kotlin.kotlinp.jvm.JvmKotlinp
import org.jetbrains.kotlin.kotlinp.jvm.readClassFile
import org.jetbrains.kotlin.kotlinp.jvm.readModuleFile
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingCompilerConfigurationComponentRegistrar
import org.jetbrains.kotlin.scripting.compiler.plugin.ScriptingK2CompilerPluginRegistrar
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.KotlinModuleMetadata
import kotlin.metadata.jvm.UnstableMetadataApi
import kotlin.test.fail

fun compareAllFiles(
    file: File,
    disposable: Disposable,
    tmpdir: File,
    compareWithTxt: Boolean,
    readWriteAndCompare: Boolean,
    useK2: Boolean,
) {
    if (!useK2) {
        val ignored = InTextDirectivesUtils.findListWithPrefixes(file.readText(), "// IGNORE_BACKEND_K1: ")
        if (listOf(TargetBackend.JVM_IR, TargetBackend.JVM, TargetBackend.ANY).any { it.name in ignored }) return
    }

    val main = StringBuilder()
    val afterNodes = StringBuilder()

    val kotlinp = JvmKotlinp(Settings(isVerbose = true, sortDeclarations = true))

    @OptIn(UnstableMetadataApi::class)
    compile(file, disposable, tmpdir, useK2) { outputFile ->
        when (outputFile.extension) {
            "kotlin_module" -> {
                val moduleFile = readModuleFile(outputFile)!!
                val transformedWithNodes = KotlinModuleMetadata.read(moduleFile.write())

                for ((sb, moduleFileToRender) in listOf(
                    main to moduleFile, afterNodes to transformedWithNodes
                )) {
                    sb.appendFileName(outputFile.relativeTo(tmpdir))
                    sb.append(kotlinp.printModuleFile(moduleFileToRender))
                }
            }
            "class" -> {
                val metadata = readClassFile(outputFile)
                val classFile = KotlinClassMetadata.readStrict(metadata)
                val classFile2 = KotlinClassMetadata.readStrict(classFile.write())

                for ((sb, classFileToRender) in listOf(
                    main to classFile, afterNodes to classFile2
                )) {
                    sb.appendFileName(outputFile.relativeTo(tmpdir))
                    sb.append(kotlinp.printClassFile(classFileToRender))
                }
            }
            else -> fail("Unknown file: $outputFile")
        }
    }

    if (compareWithTxt) {
        val defaultTxtFile = File(file.path.replace(".kts?".toRegex(), ".txt"))
        val firTxtFile = File(file.path.replace(".kts?".toRegex(), ".fir.txt"))
        val txtFile = if (useK2 && firTxtFile.exists()) firTxtFile else defaultTxtFile
        KotlinTestUtils.assertEqualsToFile(txtFile, main.toString())
    }

    if (readWriteAndCompare && InTextDirectivesUtils.findStringWithPrefixes(file.readText(), "// NO_READ_WRITE_COMPARE") == null) {
        assertEquals("Metadata is different after transformation with nodes.", main.toString(), afterNodes.toString())
    }
}

@OptIn(ExperimentalCompilerApi::class)
private fun compile(file: File, disposable: Disposable, tmpdir: File, useK2: Boolean, forEachOutputFile: (File) -> Unit) {
    val content = file.readText()
    val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK)
    configuration.put(CommonConfigurationKeys.USE_FIR, useK2)
    @Suppress("DEPRECATION")
    configuration.add(ComponentRegistrar.PLUGIN_COMPONENT_REGISTRARS, ScriptingCompilerConfigurationComponentRegistrar())
    configuration.add(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS, ScriptingK2CompilerPluginRegistrar())
    AbstractLoadJavaTest.updateConfigurationWithDirectives(content, configuration)
    val environment = KotlinCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    setupLanguageVersionSettingsForCompilerTests(content, environment)
    val ktFile = KtTestUtil.createFile(file.name, content, environment.project)
    GenerationUtils.compileFileTo(ktFile, environment, tmpdir)

    for (outputFile in tmpdir.walkTopDown().sortedBy { it.nameWithoutExtension }) {
        if (outputFile.isFile) {
            forEachOutputFile(outputFile)
        }
    }
}

private fun StringBuilder.appendFileName(file: File) {
    appendLine("// ${file.invariantSeparatorsPath}")
    appendLine("// ------------------------------------------")
}
