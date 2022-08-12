/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.intellij.openapi.Disposable
import com.sun.tools.javac.tree.TreeMaker
import com.sun.tools.javac.util.Context
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.components.KtMetadataCalculator
import org.jetbrains.kotlin.analysis.api.lifetime.KtAlwaysAccessibleLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.findFacadeClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.base.kapt3.KaptOptions
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.kapt3.base.KaptContext
import org.jetbrains.kotlin.kapt3.base.util.KaptLogger
import org.jetbrains.kotlin.kapt3.base.util.WriterBackedKaptLogger
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.org.objectweb.asm.Opcodes

object Kapt4Main {
    @OptIn(KtAnalysisApiInternals::class)
    fun run(
        configuration: CompilerConfiguration,
        options: KaptOptions,
        applicationDisposable: Disposable,
        projectDisposable: Disposable,
    ): Pair<Kapt4ContextForStubGeneration, Map<KtLightClass, Kapt4StubGenerator.KaptStub?>> {
        val module: KtSourceModule

        val analysisSession = buildStandaloneAnalysisAPISession(
            applicationDisposable,
            projectDisposable
        ) {
            // I have files: List<File>
            buildKtModuleProviderByCompilerConfiguration(configuration) {
                module = it
            }
        }
        val ktAnalysisSession = KtAnalysisSessionProvider.getInstance(analysisSession.project)
            .getAnalysisSessionByUseSiteKtModule(module, KtAlwaysAccessibleLifetimeTokenFactory)
        val ktFiles = module.ktFiles

        val lightClasses = buildList {
            ktFiles.flatMapTo(this) { file ->
                file.children.filterIsInstance<KtClassOrObject>().mapNotNull {
                    it.toLightClass()?.let { it to file }
                }
            }
            ktFiles.mapNotNullTo(this) { ktFile -> ktFile.findFacadeClass()?.let { it to ktFile } }
        }.toMap()

        val context = Kapt4ContextForStubGeneration(
            options,
            withJdk = false,
            WriterBackedKaptLogger(isVerbose = false),
            lightClasses.keys.toList(),
            lightClasses,
            ktAnalysisSession.ktMetadataCalculator
        )

        val generator = with(context) { Kapt4StubGenerator(ktAnalysisSession) }
        return context to generator.generateStubs()
    }
}

class Kapt4ContextForStubGeneration(
    options: KaptOptions,
    withJdk: Boolean,
    logger: KaptLogger,
    val classes: List<KtLightClass>,
    val origins: Map<KtLightClass, KtFile>,
    val metadataCalculator: KtMetadataCalculator
) : KaptContext(options, withJdk, logger) {
    val treeMaker = TreeMaker.instance(context) as Kapt4TreeMaker

    override fun preregisterTreeMaker(context: Context) {
        Kapt4TreeMaker.preRegister(context, this)
    }

    override fun close() {
        TODO()
    }
}

val NO_NAME_PROVIDED: String
    get() {
        return "<no name provided>"
    }

private const val LONG_DEPRECATED = Opcodes.ACC_DEPRECATED.toLong()
internal fun isDeprecated(access: Long) = (access and LONG_DEPRECATED) != 0L
internal fun isEnum(access: Int) = (access and Opcodes.ACC_ENUM) != 0
internal fun isPublic(access: Int) = (access and Opcodes.ACC_PUBLIC) != 0
internal fun isSynthetic(access: Int) = (access and Opcodes.ACC_SYNTHETIC) != 0
internal fun isFinal(access: Int) = (access and Opcodes.ACC_FINAL) != 0
internal fun isStatic(access: Int) = (access and Opcodes.ACC_STATIC) != 0
internal fun isAbstract(access: Int) = (access and Opcodes.ACC_ABSTRACT) != 0
