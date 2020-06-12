package org.jetbrains.kotlin.idea.core

import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.jvmPhases
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.DefaultCodegenFactory
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.idea.core.util.analyzeInlinedFunctions
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.*
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class KotlinCompilerIde(
    private val file: KtFile,
    private val configuration: CompilerConfiguration = getDefaultCompilerConfiguration(file),
    private val factory: ClassBuilderFactory = ClassBuilderFactories.BINARIES,
    private val resolutionFacadeProvider: (KtFile) -> ResolutionFacade? = ::getDefaultResolutionFacade,
    private val classFilesOnly: Boolean = false
) {
    companion object {
        private fun getDefaultCompilerConfiguration(file: KtFile): CompilerConfiguration {
            return CompilerConfiguration().apply {
                languageVersionSettings = file.languageVersionSettings
            }
        }

        private fun getDefaultResolutionFacade(file: KtFile): ResolutionFacade? {
            return KotlinCacheService.getInstance(file.project)
                .getResolutionFacadeByFile(file, JvmPlatforms.unspecifiedJvmPlatform)
        }
    }

    class CompiledFile(val path: String, val bytecode: ByteArray)

    fun compileToDirectory(destination: File) {
        destination.mkdirs()

        val state = compile() ?: return

        try {
            for (outputFile in getFiles(state)) {
                val target = File(destination, outputFile.relativePath)
                (target.parentFile ?: error("Can't find parent for file $target")).mkdirs()
                target.writeBytes(outputFile.asByteArray())
            }
        } finally {
            state.destroy()
        }
    }

    fun compileToJar(destination: File) {
        destination.outputStream().buffered().use { os ->
            ZipOutputStream(os).use { zos ->
                val state = compile()

                if (state != null) {
                    try {
                        for (outputFile in getFiles(state)) {
                            zos.putNextEntry(ZipEntry(outputFile.relativePath))
                            zos.write(outputFile.asByteArray())
                            zos.closeEntry()
                        }
                    } finally {
                        state.destroy()
                    }
                }
            }
        }
    }

    fun compileToBytecode(): List<CompiledFile> {
        val state = compile() ?: return emptyList()

        try {
            return getFiles(state).map { CompiledFile(it.relativePath, it.asByteArray()) }
        } finally {
            state.destroy()
        }
    }

    fun compile(): GenerationState? {
        val project = file.project
        val platform = file.platform

        if (!platform.isCommon() && !platform.isJvm()) return null

        val resolutionFacade = resolutionFacadeProvider(file) ?: return null
        val bindingContextForFiles = resolutionFacade.analyzeWithAllCompilerChecks(listOf(file)).bindingContext

        val (bindingContext, toProcess) = analyzeInlinedFunctions(
            resolutionFacade, file, configuration.getBoolean(CommonConfigurationKeys.DISABLE_INLINE),
            bindingContextForFiles
        )

        val generateClassFilter = object : GenerationState.GenerateClassFilter() {
            override fun shouldGeneratePackagePart(ktFile: KtFile): Boolean {
                return file === ktFile
            }

            override fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject): Boolean {
                return true
            }

            override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject): Boolean {
                return processingClassOrObject.containingKtFile === file
            }

            override fun shouldGenerateScript(script: KtScript): Boolean {
                return script.containingKtFile === file
            }

            override fun shouldGenerateCodeFragment(script: KtCodeFragment) = false
        }

        val codegenFactory = when {
            configuration.getBoolean(JVMConfigurationKeys.IR) -> JvmIrCodegenFactory(PhaseConfig(jvmPhases))
            else -> DefaultCodegenFactory
        }

        val state = GenerationState.Builder(project, factory, resolutionFacade.moduleDescriptor, bindingContext, toProcess, configuration)
            .generateDeclaredClassFilter(generateClassFilter)
            .codegenFactory(codegenFactory)
            .build()

        KotlinCodegenFacade.compileCorrectFiles(state)
        return state
    }

    private fun getFiles(state: GenerationState): List<OutputFile> {
        val allFiles = state.factory.asList()
        if (classFilesOnly) {
            return allFiles.filter { it.relativePath.endsWith(".class") }
        }
        return allFiles
    }
}