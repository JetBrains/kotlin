/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.js.klib.generateIrForKlibSerialization
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.ProjectInfo
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.ic.CacheUpdater
import org.jetbrains.kotlin.ir.backend.js.ic.JsExecutableProducer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputs
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.WebConfigurationKeys
import org.jetbrains.kotlin.js.testOld.V8IrJsTestChecker
import org.jetbrains.kotlin.klib.KlibABITestUtils
import org.jetbrains.kotlin.klib.KlibABITestUtils.MAIN_MODULE_NAME
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File
import kotlin.io.path.createTempDirectory

abstract class AbstractJsKLibABIWithICTestCase : AbstractJsKLibABITestCase() {
    override val useIncrementalCompiler get() = true
}

abstract class AbstractJsKLibABINoICTestCase : AbstractJsKLibABITestCase() {
    override val useIncrementalCompiler get() = false
}

abstract class AbstractJsKLibABITestCase : KtUsefulTestCase() {
    abstract val useIncrementalCompiler: Boolean

    private lateinit var buildDir: File
    private lateinit var environment: KotlinCoreEnvironment

    override fun setUp() {
        super.setUp()
        buildDir = createTempDirectory().toFile().also { it.mkdirs() }

        environment = KotlinCoreEnvironment.createForTests(
            testRootDisposable,
            CompilerConfiguration(),
            EnvironmentConfigFiles.JS_CONFIG_FILES
        )
    }

    override fun tearDown() {
        buildDir.deleteRecursively()
        super.tearDown()
    }

    private inner class JsTestConfiguration(testPath: String) : KlibABITestUtils.TestConfiguration {
        override val testDir: File = File(testPath).absoluteFile
        override val buildDir: File get() = this@AbstractJsKLibABITestCase.buildDir
        override val stdlibFile: File get() = File("libraries/stdlib/js-ir/build/classes/kotlin/js/main").absoluteFile

        override fun buildKlib(moduleName: String, moduleSourceDir: File, dependencies: KlibABITestUtils.Dependencies, klibFile: File) =
            this@AbstractJsKLibABITestCase.buildKlib(moduleName, moduleSourceDir, dependencies, klibFile)

        override fun buildBinaryAndRun(mainModuleKlibFile: File, dependencies: KlibABITestUtils.Dependencies) =
            this@AbstractJsKLibABITestCase.buildBinaryAndRun(mainModuleKlibFile, dependencies)

        override fun onNonEmptyBuildDirectory(directory: File) {
            directory.listFiles()?.forEach(File::deleteRecursively)
        }

        // TODO: Suppress the tests failing with ISE "Symbol for <signature> is unbound" until KT-54491 is fixed.
        //  Such failures are caused by references to unbound symbols still preserved in CacheUpdater in JS IR IC.
        override fun isIgnoredTest(projectInfo: ProjectInfo) = when {
            super.isIgnoredTest(projectInfo) -> true
            !useIncrementalCompiler -> false
            else -> projectInfo.name in setOf(
                "removeFunction",
                "removeProperty",
                "removeOpenFunction",
                "removeOpenProperty",
                "removeInlinedClass"
            )
        }

        override fun onIgnoredTest() {
            /* Do nothing specific. JUnit 3 does not support programmatic tests muting. */
        }
    }

    // The entry point to generated test classes.
    fun doTest(testPath: String) = KlibABITestUtils.runTest(JsTestConfiguration(testPath))

    private fun buildKlib(moduleName: String, moduleSourceDir: File, dependencies: KlibABITestUtils.Dependencies, klibFile: File) {
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

        val moduleSourceFiles = (sourceModule.mainModule as MainModule.SourceFiles).files
        val icData = sourceModule.compilerConfiguration.incrementalDataProvider?.getSerializedData(moduleSourceFiles) ?: emptyList()
        val expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()
        val (moduleFragment, _) = generateIrForKlibSerialization(
            environment.project,
            moduleSourceFiles,
            config,
            sourceModule.jsFrontEndResult.jsAnalysisResult,
            sortDependencies(sourceModule.moduleDependencies),
            icData,
            expectDescriptorToSymbol,
            IrFactoryImpl,
            verifySignatures = true
        ) {
            sourceModule.getModuleDescriptor(it)
        }

        val metadataSerializer =
            KlibMetadataIncrementalSerializer(config, sourceModule.project, sourceModule.jsFrontEndResult.hasErrors)

        generateKLib(
            sourceModule,
            klibFile.path,
            nopack = false,
            jsOutputName = moduleName,
            icData = icData,
            expectDescriptorToSymbol = expectDescriptorToSymbol,
            moduleFragment = moduleFragment
        ) { file ->
            metadataSerializer.serializeScope(file, sourceModule.jsFrontEndResult.bindingContext, moduleFragment.descriptor)
        }
    }

    private fun buildBinaryAndRun(mainModuleKlibFile: File, allDependencies: KlibABITestUtils.Dependencies) {
        val configuration = environment.configuration.copy()

        configuration.put(WebConfigurationKeys.PARTIAL_LINKAGE, true)
        configuration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.PLAIN)
        configuration.put(WebConfigurationKeys.PROPERTY_LAZY_INITIALIZATION, true)
        configuration.put(CommonConfigurationKeys.MODULE_NAME, MAIN_MODULE_NAME)

        val compilationOutputs = if (useIncrementalCompiler)
            buildBinaryWithIC(configuration, mainModuleKlibFile, allDependencies)
        else
            buildBinaryNoIC(configuration, mainModuleKlibFile, allDependencies)

        val binariesDir = File(buildDir, BIN_DIR_NAME).also { it.mkdirs() }
        val binaries = compilationOutputs.writeAll(binariesDir, MAIN_MODULE_NAME, false, MAIN_MODULE_NAME, ModuleKind.PLAIN).filter {
            it.extension == "js"
        }

        executeAndCheckBinaries(MAIN_MODULE_NAME, binaries)
    }

    private fun buildBinaryWithIC(
        configuration: CompilerConfiguration,
        mainModuleKlibFile: File,
        allDependencies: KlibABITestUtils.Dependencies
    ): CompilationOutputs {
        // TODO: what about friend dependencies?
        val cacheUpdater = CacheUpdater(
            mainModule = mainModuleKlibFile.absolutePath,
            allModules = allDependencies.regularDependencies.map { it.path },
            mainModuleFriends = emptyList(),
            cacheDir = buildDir.resolve("libs-cache").absolutePath,
            compilerConfiguration = configuration,
            irFactory = { IrFactoryImplForJsIC(WholeWorldStageController()) },
            mainArguments = null,
            compilerInterfaceFactory = { mainModule, cfg ->
                JsIrCompilerWithIC(mainModule, cfg, JsGenerationGranularity.PER_MODULE, PhaseConfig(jsPhases), setOf(BOX_FUN_FQN))
            }
        )
        val icCaches = cacheUpdater.actualizeCaches()

        val mainModuleName = icCaches.last().moduleExternalName
        val jsExecutableProducer = JsExecutableProducer(
            mainModuleName = mainModuleName,
            moduleKind = configuration[JSConfigurationKeys.MODULE_KIND]!!,
            sourceMapsInfo = SourceMapsInfo.from(configuration),
            caches = icCaches,
            relativeRequirePath = true
        )

        return jsExecutableProducer.buildExecutable(multiModule = true, outJsProgram = true).compilationOut
    }

    private fun buildBinaryNoIC(
        configuration: CompilerConfiguration,
        mainModuleKlibFile: File,
        allDependencies: KlibABITestUtils.Dependencies
    ): CompilationOutputs {
        val klib = MainModule.Klib(mainModuleKlibFile.path)
        val moduleStructure = ModulesStructure(
            environment.project,
            klib,
            configuration,
            allDependencies.regularDependencies.map { it.path },
            allDependencies.friendDependencies.map { it.path }
        )

        val ir = compile(
            moduleStructure,
            PhaseConfig(jsPhases),
            IrFactoryImplForJsIC(WholeWorldStageController()),
            exportedDeclarations = setOf(BOX_FUN_FQN),
            granularity = JsGenerationGranularity.PER_MODULE
        )

        val transformer = IrModuleToJsTransformer(
            backendContext = ir.context,
            mainArguments = emptyList()
        )

        val compiledResult = transformer.generateModule(
            modules = ir.allModules,
            modes = setOf(TranslationMode.PER_MODULE_DEV),
            relativeRequirePath = false
        )

        return compiledResult.outputs[TranslationMode.PER_MODULE_DEV] ?: error("No compiler output")
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
        checker.check(filePaths, mainModuleName, null, BOX_FUN_FQN.asString(), "OK", withModuleSystem = false)
    }

    companion object {
        private const val BIN_DIR_NAME = "_bins_js"
        private val BOX_FUN_FQN = FqName("box")
    }
}
