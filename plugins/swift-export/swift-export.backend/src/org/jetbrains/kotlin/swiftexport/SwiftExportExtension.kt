/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.KtAlwaysAccessibleLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.analysisapi.SirGenerator
import org.jetbrains.kotlin.sir.bridge.BridgeRequest
import org.jetbrains.kotlin.sir.bridge.createBridgeGenerator
import org.jetbrains.kotlin.sir.bridge.createCBridgePrinter
import org.jetbrains.kotlin.sir.bridge.createKotlinBridgePrinter
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.visitors.SirVisitor
import org.jetbrains.sir.passes.SirInflatePackagesPass
import org.jetbrains.sir.passes.SirModulePass
import org.jetbrains.sir.passes.SirPass
import org.jetbrains.sir.passes.run
import org.jetbrains.sir.passes.translation.ForeignIntoSwiftFunctionTranslationPass
import org.jetbrains.sir.printer.SirAsSwiftSourcesPrinter
import java.io.File

class SwiftExportExtension(
    private val destination: File,
    private val outputFileName: String,
) : FirAnalysisHandlerExtension() {
    override fun isApplicable(configuration: CompilerConfiguration): Boolean {
        return true
    }

    override fun doAnalysis(configuration: CompilerConfiguration): Boolean {
        buildSwiftModule(configuration)
            .transformToSwift()
            .dumpResultToFiles()
        return true
    }

    @OptIn(KtAnalysisApiInternals::class)
    private fun buildSwiftModule(configuration: CompilerConfiguration): SirModule {
        val standaloneAnalysisAPISession =
            buildStandaloneAnalysisAPISession(classLoader = SwiftExportExtension::class.java.classLoader) {
                @Suppress("DEPRECATION") // TODO: KT-61319 Kapt: remove usages of deprecated buildKtModuleProviderByCompilerConfiguration
                buildKtModuleProviderByCompilerConfiguration(configuration)

                registerProjectService(KtLifetimeTokenProvider::class.java, KtAlwaysAccessibleLifetimeTokenProvider())
            }

        val (sourceModule, rawFiles) = standaloneAnalysisAPISession.modulesWithFiles.entries.single()

        val ktFiles = rawFiles.filterIsInstance<KtFile>()

        return buildModule {
            name = sourceModule.moduleName
            val sirFactory = SirGenerator()
            ktFiles.forEach { file ->
                declarations += sirFactory.build(file)
            }
        }.apply {
            declarations.forEach { it.parent = this }
        }
    }

    private fun SirModule.transformToSwift(): SirModule {
        return SirPassesConfiguration.passes.fold(this) { module, pass ->
            pass.run(module)
        }
    }

    private fun SirModule.dumpResultToFiles() {
        val destinationPath = destination.absolutePath

        val cHeaderFile = File("${destinationPath}/${outputFileName}.h")
        val ktBridgeFile = File("${destinationPath}/${outputFileName}.kt")
        val swiftFile = File("${destinationPath}/${outputFileName}.swift")

        val bridges = generateBridgeSources()
        val swiftSrc = generateSwiftSrc()

        dumpTextAtFile(bridges.ktSrc, ktBridgeFile)
        dumpTextAtFile(bridges.cSrc, cHeaderFile)
        dumpTextAtFile(sequenceOf(swiftSrc), swiftFile)
    }

    private fun SirModule.generateSwiftSrc(): String {
        return SirAsSwiftSourcesPrinter().print(this)
    }

    private fun SirModule.generateBridgeSources(): BridgeSources {
        val requests = BridgeRequestsBuilder.build(this)

        val generator = createBridgeGenerator()
        val kotlinBridgePrinter = createKotlinBridgePrinter()
        val cBridgePrinter = createCBridgePrinter()

        requests.forEach { request ->
            val bridge = generator.generate(request)
            kotlinBridgePrinter.add(bridge)
            cBridgePrinter.add(bridge)
        }

        val actualKotlinSrc = kotlinBridgePrinter.print()
        val actualCHeader = cBridgePrinter.print()

        return BridgeSources(ktSrc = actualKotlinSrc, cSrc = actualCHeader)
    }

    private fun dumpTextAtFile(text: Sequence<String>, file: File) {
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        val writer = file.printWriter()
        for (t in text) {
            writer.println(t)
        }
        writer.close()
    }

    private data class BridgeSources(val ktSrc: Sequence<String>, val cSrc: Sequence<String>)

    private object SirPassesConfiguration {
        val passes: List<SirModulePass> = listOf(
            SirInflatePackagesPass(),
            WholeModuleTranslationByElementPass(ForeignIntoSwiftFunctionTranslationPass()),
        )

        class WholeModuleTranslationByElementPass(
            val pass: SirPass<SirElement, Nothing?, SirDeclaration>
        ) : SirModulePass {
            override fun run(element: SirModule, data: Nothing?): SirModule {
                return buildModule {
                    name = element.name
                    element.declarations.forEach {
                        val newDecl = pass.run(it)
                        declarations.add(newDecl)
                    }
                }.apply {
                    declarations.forEach { it.parent = this }
                }
            }
        }
    }

    private object BridgeRequestsBuilder : SirVisitor<Unit, MutableList<BridgeRequest>>() {
        fun build(from: SirModule): List<BridgeRequest> {
            val result = mutableListOf<BridgeRequest>()
            from.accept(this, result)
            return result
        }

        override fun visitFunction(function: SirFunction, data: MutableList<BridgeRequest>) {
            val fqName = (function.origin as? SirKotlinOrigin.Function)?.fqName ?: return

            data.add(
                BridgeRequest(
                    function,
                    fqName.joinToString("_"),
                    fqName
                )
            )
        }

        override fun visitElement(element: SirElement, data: MutableList<BridgeRequest>) {
            element.acceptChildren(this, data)
        }
    }
}
