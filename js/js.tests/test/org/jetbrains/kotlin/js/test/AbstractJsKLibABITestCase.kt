/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformerTmp
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.testOld.V8IrJsTestChecker
import org.jetbrains.kotlin.klib.AbstractKlibABITestCase
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.ModuleKind
import java.io.File

abstract class AbstractJsKLibABITestCase : AbstractKlibABITestCase() {
    override fun stdlibFile(): File = File("libraries/stdlib/js-ir/build/classes/kotlin/js/main").absoluteFile

    override fun buildKlib(moduleName: String, moduleSourceDir: File, dependencies: Dependencies, klibFile: File) {
        val ktFiles = environment.createPsiFiles(moduleSourceDir)

        val config = environment.configuration.copy()
        config.put(CommonConfigurationKeys.MODULE_NAME, moduleName)

        val sourceModule = prepareAnalyzedSourceModule(
            environment.project,
            ktFiles,
            config,
            dependencies.regularDependencies.map { it.path },
            dependencies.friendDependencies.map { it.path },
            AnalyzerWithCompilerReport(config)
        )

        generateKLib(sourceModule, IrFactoryImpl, klibFile.path, nopack = false, jsOutputName = moduleName)
    }

    override fun buildBinaryAndRun(mainModuleKlibFile: File, dependencies: Dependencies) {
        val project = environment.project
        val configuration = environment.configuration

        configuration.put(JSConfigurationKeys.PARTIAL_LINKAGE, true)
        configuration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)
        configuration.put(JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION, true)
        configuration.put(CommonConfigurationKeys.MODULE_NAME, MAIN_MODULE_NAME)

        val kLib = MainModule.Klib(mainModuleKlibFile.path)

        val moduleStructure = ModulesStructure(
            project,
            kLib,
            configuration,
            dependencies.regularDependencies.map { it.path },
            dependencies.friendDependencies.map { it.path }
        )

        val ir = compile(
            moduleStructure,
            PhaseConfig(jsPhases),
            IrFactoryImplForJsIC(WholeWorldStageController()),
            exportedDeclarations = setOf(FqName("box")),
            granularity = JsGenerationGranularity.PER_MODULE,
            icCompatibleIr2Js = true
        )

        val transformer = IrModuleToJsTransformerTmp(ir.context, emptyList())

        val compiledResult = transformer.generateModule(ir.allModules, setOf(TranslationMode.FULL_DCE_MINIMIZED_NAMES), false)

        val dceOutput = compiledResult.outputs[TranslationMode.FULL_DCE_MINIMIZED_NAMES] ?: error("No DCE output")

        val binariesDir = File(buildDir, BIN_DIR_NAME).also { it.mkdirs() }

        val binaries = ArrayList<File>(dependencies.regularDependencies.size)

        for ((name, code) in dceOutput.dependencies) {
            val depBinary = binariesDir.binJsFile(name)
            depBinary.parentFile?.let { if (!it.exists()) it.mkdirs() }
            depBinary.writeText(code.jsCode)
            binaries.add(depBinary)
        }

        val mainBinary = binariesDir.binJsFile(MAIN_MODULE_NAME)
        mainBinary.writeText(dceOutput.jsCode)
        binaries.add(mainBinary)

        executeAndCheckBinaries(MAIN_MODULE_NAME, binaries)
    }

    private fun KotlinCoreEnvironment.createPsiFiles(sourceDir: File): List<KtFile> {
        val psiManager = PsiManager.getInstance(project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL) as CoreLocalFileSystem

        return sourceDir.walkTopDown().filter { file ->
            file.isFile && file.extension == "kt"
        }.flatMap { file ->
            val virtualFile = fileSystem.findFileByIoFile(file) ?: error("VirtualFile for $file not found")
            SingleRootFileViewProvider(psiManager, virtualFile).allFiles
        }.filterIsInstance<KtFile>().toList()
    }

    private fun File.binJsFile(name: String): File = File(this, "$name.js")

    private fun executeAndCheckBinaries(mainModuleName: String, dependencies: Collection<File>) {
        val checker = V8IrJsTestChecker

        val filePaths = dependencies.map { it.canonicalPath }
        checker.check(filePaths, mainModuleName, null, "box", "OK", withModuleSystem = false)
    }

    companion object {
        private const val BIN_DIR_NAME = "_bins_js"
    }
}
