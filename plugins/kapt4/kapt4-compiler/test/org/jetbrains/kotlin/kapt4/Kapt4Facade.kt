/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoots
import org.jetbrains.kotlin.kapt3.test.KaptMessageCollectorProvider
import org.jetbrains.kotlin.kapt3.test.kaptOptionsProvider
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import java.io.File

class Kapt4Facade(private val testServices: TestServices) :
    AbstractTestFacade<ResultingArtifact.Source, Kapt4ContextBinaryArtifact>() {
    override val inputKind: TestArtifactKind<ResultingArtifact.Source>
        get() = SourcesKind
    override val outputKind: TestArtifactKind<Kapt4ContextBinaryArtifact>
        get() = Kapt4ContextBinaryArtifact.Kind

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::KaptMessageCollectorProvider))

    override fun transform(module: TestModule, inputArtifact: ResultingArtifact.Source): Kapt4ContextBinaryArtifact {
        val configurationProvider = testServices.compilerConfigurationProvider
//        val project = configurationProvider.getProject(module)

        val configuration = configurationProvider.getCompilerConfiguration(module)
        configuration.addKotlinSourceRoots(module.files.filter { it.isKtFile }.map { it.realFile().absolutePath })
        configuration.addJavaSourceRoots(module.files.filter { it.isKtFile }.map { it.realFile() })
        val options = testServices.kaptOptionsProvider[module]
        val (context, stubMap) = Kapt4Main.run(
            configuration,
            options,
            testServices.applicationDisposableProvider.getApplicationRootDisposable(),
            configurationProvider.testRootDisposable
        )
        return Kapt4ContextBinaryArtifact(context, stubMap.values.filterNotNull())
    }

    private fun TestFile.realFile(): File {
        return testServices.sourceFileProvider.getRealFileForSourceFile(this)
    }

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return true // TODO
    }
}

data class Kapt4ContextBinaryArtifact(
    val kaptContext: Kapt4ContextForStubGeneration,
    val kaptStubs: List<Kapt4StubGenerator.KaptStub>
) : ResultingArtifact.Binary<Kapt4ContextBinaryArtifact>() {
    object Kind : BinaryKind<Kapt4ContextBinaryArtifact>("KaptArtifact")

    override val kind: BinaryKind<Kapt4ContextBinaryArtifact>
        get() = Kind
}

