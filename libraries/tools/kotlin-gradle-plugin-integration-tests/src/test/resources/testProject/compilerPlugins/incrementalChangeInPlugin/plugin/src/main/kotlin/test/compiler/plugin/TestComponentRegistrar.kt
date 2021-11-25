/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.compiler.plugin

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.AnalysisResult.RetryWithAdditionalRoots
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import java.io.File

class TestComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        val kotlinSourceRoots = configuration.kotlinSourceRoots.ifEmpty { return }
        val sourceGenFolder = createSourceGenFolder(kotlinSourceRoots)

        var counter = 0

        AnalysisHandlerExtension.registerExtension(project, object : AnalysisHandlerExtension {
            private var didRecompile = false

            override fun analysisCompleted(
                project: Project,
                module: ModuleDescriptor,
                bindingTrace: BindingTrace,
                files: Collection<KtFile>
            ): AnalysisResult? {
                if (didRecompile) return null
                didRecompile = true

                val classes = files
                    .flatMap { ktFile ->
                        val packageName = ktFile.packageFqName.asString()
                        ktFile.findChildrenByClass(KtClassOrObject::class.java)
                            .map { packageName + "." + it.nameAsSafeName.asString() }
                    }
                    .mapNotNull {
                        module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName(it)))
                    }

                // Nothing to do.
                if (classes.isEmpty()) return null

                classes.forEach { classDescriptor ->
                    val className = classDescriptor.name.asString()

                    val directory = File(sourceGenFolder, "plugin/test/gen")
                    val file = File(directory, "$className.kt")
                        .also {
                            check(it.parentFile.exists() || it.parentFile.mkdirs()) {
                                "Could not generate package directory: $this"
                            }
                        }

                    file.writeText(
                        """
                  package plugin.test.gen
                  
                  val hello${counter++} = "world."
              """.trimIndent()
                    )
                }

                // This restarts the analysis phase and will include our file.
                return RetryWithAdditionalRoots(
                    bindingTrace.bindingContext, module, emptyList(), listOf(sourceGenFolder), addToEnvironment = true
                )
            }
        })
    }

    private fun createSourceGenFolder(kotlinSourceRoots: List<KotlinSourceRoot>): File {
        fun sampleDir(parent: File): File = File(parent, "sample-dir").also {
            check(it.exists() || it.mkdirs()) {
                "Could not create source generation directory: $it"
            }
        }

        val oneSourceFile = kotlinSourceRoots.first().path.let { File(it) }
        val parentSequence = generateSequence(oneSourceFile) { it.parentFile }

        // Try to find the src dir.
        parentSequence.firstOrNull { it.name == "src" }
            ?.let { return sampleDir(File(it.parentFile, "build")) }

        // If the src dir is not part of the input (incremental build), look for the build dir
        // directly.
        parentSequence.firstOrNull { it.name == "build" }
            ?.let { return sampleDir(it) }

        throw IllegalStateException(
            "Could not create source generation directory: $oneSourceFile"
        )
    }
}
