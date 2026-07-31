/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyJvm
import org.jetbrains.kotlin.testFederation.AffectedByCompilerPlugins
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

@DisplayName("Kapt with compiler plugins")
@OtherGradlePluginTests
@AffectedByCompilerPlugins
class KaptCompilerPluginsIT : KaptBaseIT() {
    override val defaultBuildOptions: BuildOptions =
        super.defaultBuildOptions.copyEnsuringK2()

    @DisplayName("K2 kapt stubs use kotlin.jvm.functions.Function0 instead of compiler plugin function kinds")
    @GradleTest
    fun testFunctionTypeKindCompilerPluginInKapt(gradleVersion: GradleVersion) {
        val buildOptions = defaultBuildOptions.copy(
            // KT-76289 KAPT does not support isolated projects
            isolatedProjects = BuildOptions.IsolatedProjectsMode.DISABLED
        )
        val kotlinVersion = buildOptions.kotlinVersion

        project(
            "empty",
            gradleVersion,
            buildOptions = buildOptions,
        ) {
            createCompilerPluginFunctionKindProject()

            transferPluginRepositoriesIntoBuildScript()

            buildScriptBuildscriptBlockInjection {
                buildscript.configurations.getByName("classpath").dependencies.add(
                    buildscript.dependencies.create("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
                )
            }

            buildScriptInjection {
                val pluginProject = project.project(":plugin")
                pluginProject.applyJvm {
                    jvmToolchain(8)
                    compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
                    compilerOptions.optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
                }
                pluginProject.dependencies.add(
                    "implementation",
                    "org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlinVersion",
                )
                pluginProject.dependencies.add(
                    "compileOnly",
                    "org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion",
                )

                val annotationProcessorProject = project.project(":annotation-processor")
                annotationProcessorProject.applyJvm {
                    jvmToolchain(8)
                }
                annotationProcessorProject.dependencies.add(
                    "implementation",
                    "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion",
                )

                val exampleProject = project.project(":example")
                exampleProject.applyJvm {
                    jvmToolchain(8)
                }
                exampleProject.plugins.apply("org.jetbrains.kotlin.kapt")

                exampleProject.dependencies.add("implementation", "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
                exampleProject.dependencies.add(
                    "implementation",
                    exampleProject.dependencies.project(mapOf("path" to ":annotation-processor")),
                )
                exampleProject.dependencies.add(
                    "kapt",
                    exampleProject.dependencies.project(mapOf("path" to ":annotation-processor")),
                )
                exampleProject.dependencies.add(
                    "kotlinCompilerPluginClasspath",
                    exampleProject.dependencies.project(mapOf("path" to ":plugin")),
                )
            }

            build(":example:kaptGenerateStubsKotlin") {
                assertTasksExecuted(":example:kaptGenerateStubsKotlin")

                val interfaceStub = "example/build/tmp/kapt3/stubs/main/repro/TestInterface.java"
                assertFileInProjectDoesNotContain(interfaceStub, "repro.internal.PluginFunction0<kotlin.Unit>")
                assertFileInProjectContains(interfaceStub, "kotlin.jvm.functions.Function0<kotlin.Unit> block")
                assertFileInProjectContains(
                    interfaceStub,
                    "java.util.List<? extends kotlin.jvm.functions.Function0<kotlin.Unit>> blocks",
                )
                assertFileInProjectContains(interfaceStub, "kotlin.jvm.functions.Function0<kotlin.Unit> testReturn()")
                assertFileInProjectContains(
                    interfaceStub,
                    "java.util.List<kotlin.jvm.functions.Function0<kotlin.Unit>> testListReturn()",
                )

                val classStub = "example/build/tmp/kapt3/stubs/main/repro/TestClass.java"
                assertFileInProjectDoesNotContain(classStub, "repro.internal.PluginFunction0<kotlin.Unit>")
                assertFileInProjectContains(classStub, "private final kotlin.jvm.functions.Function0<kotlin.Unit> directProperty")
                assertFileInProjectContains(
                    classStub,
                    "private final java.util.List<kotlin.jvm.functions.Function0<kotlin.Unit>> listProperty",
                )
                assertFileInProjectContains(classStub, "private kotlin.jvm.functions.Function0<kotlin.Unit> mutableDirectProperty")
                assertFileInProjectContains(
                    classStub,
                    "private java.util.List<? extends kotlin.jvm.functions.Function0<kotlin.Unit>> mutableListProperty",
                )
                assertFileInProjectContains(classStub, "kotlin.jvm.functions.Function0<kotlin.Unit> getDirectProperty()")
                assertFileInProjectContains(
                    classStub,
                    "java.util.List<kotlin.jvm.functions.Function0<kotlin.Unit>> getListProperty()",
                )
                assertFileInProjectContains(classStub, "kotlin.jvm.functions.Function0<kotlin.Unit> p0")
                assertFileInProjectContains(
                    classStub,
                    "java.util.List<? extends kotlin.jvm.functions.Function0<kotlin.Unit>> p0",
                )
            }
        }
    }

    private fun TestProject.createCompilerPluginFunctionKindProject() {
        settingsGradle.appendText("\ninclude ':annotation-processor', ':example', ':plugin'\n")

        subProject("plugin").apply {
            kotlinSourcesDir().source("FunctionKindPlugin.kt") { functionKindPluginSource }
            projectPath.source(
                "src/main/resources/META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar"
            ) {
                "repro.plugin.FunctionKindPluginRegistrar"
            }
        }

        subProject("annotation-processor").apply {
            kotlinSourcesDir().source("InspectTypesProcessor.kt") { inspectTypesProcessorSource }
            projectPath.source("src/main/resources/META-INF/services/javax.annotation.processing.Processor") {
                "repro.processor.InspectTypesProcessor"
            }
        }

        subProject("example").kotlinSourcesDir().source("Reproducer.kt") { reproducerSource }
    }

    private companion object {
        private val inspectTypesProcessorSource = """
            package repro.processor

            import java.io.IOException
            import javax.annotation.processing.AbstractProcessor
            import javax.annotation.processing.RoundEnvironment
            import javax.annotation.processing.SupportedAnnotationTypes
            import javax.annotation.processing.SupportedSourceVersion
            import javax.lang.model.SourceVersion
            import javax.lang.model.element.TypeElement
            import javax.lang.model.type.TypeKind
            import javax.lang.model.util.ElementFilter
            import javax.tools.Diagnostic
            import javax.tools.StandardLocation

            @Target(AnnotationTarget.CLASS)
            annotation class Trigger

            @SupportedAnnotationTypes("repro.processor.Trigger")
            @SupportedSourceVersion(SourceVersion.RELEASE_8)
            class InspectTypesProcessor : AbstractProcessor() {
                private var wroteMarker = false

                override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
                    if (roundEnv.processingOver() || wroteMarker) return false

                    val typeElement = roundEnv.getElementsAnnotatedWith(Trigger::class.java).singleOrNull() as? TypeElement ?: return false
                    val testMethod = ElementFilter.methodsIn(typeElement.enclosedElements).single { it.simpleName.contentEquals("test") }
                    val parameter = testMethod.parameters.single()
                    val parameterType = parameter.asType()

                    if (parameterType.kind == TypeKind.ERROR) {
                        processingEnv.messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            "Detected error type from compiler plugin: ${'$'}parameterType",
                            parameter,
                        )
                        return false
                    }

                    try {
                        processingEnv.filer.createResource(StandardLocation.CLASS_OUTPUT, "", "type.txt").openWriter().use {
                            it.write(parameterType.toString())
                        }
                        wroteMarker = true
                    } catch (_: IOException) {
                        // Another round may try to recreate the marker; ignore it.
                    }

                    return false
                }
            }
        """.trimIndent()

        private val reproducerSource = """
            package repro

            import repro.processor.Trigger

            @Target(AnnotationTarget.TYPE)
            annotation class PluginFunction

            @Trigger
            interface TestInterface {
                fun test(block: @PluginFunction () -> Unit)

                fun testList(blocks: List<@PluginFunction () -> Unit>)

                fun testReturn(): @PluginFunction () -> Unit

                fun testListReturn(): List<@PluginFunction () -> Unit>
            }

            class TestClass {
                val directProperty: @PluginFunction () -> Unit = {}

                val listProperty: List<@PluginFunction () -> Unit> = emptyList()

                var mutableDirectProperty: @PluginFunction () -> Unit = {}

                var mutableListProperty: List<@PluginFunction () -> Unit> = emptyList()
            }
        """.trimIndent()

        private val functionKindPluginSource = """
            package repro.plugin

            import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
            import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
            import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
            import org.jetbrains.kotlin.compiler.plugin.registerExtension
            import org.jetbrains.kotlin.config.CompilerConfiguration
            import org.jetbrains.kotlin.config.LanguageVersion
            import org.jetbrains.kotlin.fir.FirSession
            import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
            import org.jetbrains.kotlin.fir.extensions.FirFunctionTypeKindExtension
            import org.jetbrains.kotlin.name.ClassId
            import org.jetbrains.kotlin.name.FqName

            private val pluginFunctionAnnotationId = ClassId.topLevel(FqName("repro.PluginFunction"))

            private val useLegacyCustomFunctionTypeSerializationUntil: String
                get() {
                    return LanguageVersion.values().last().versionString
                }

            @OptIn(ExperimentalCompilerApi::class)
            class FunctionKindPluginRegistrar : CompilerPluginRegistrar() {
                override val pluginId: String
                    get() = "repro.function-kind"

                override val supportsK2: Boolean
                    get() = true

                override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
                    FirExtensionRegistrar.registerExtension(FunctionKindExtensionRegistrar())
                }
            }

            private class FunctionKindExtensionRegistrar : FirExtensionRegistrar() {
                override fun ExtensionRegistrarContext.configurePlugin() {
                    +::FunctionKindExtension
                }
            }

            private class FunctionKindExtension(session: FirSession) : FirFunctionTypeKindExtension(session) {
                override fun FunctionTypeKindRegistrar.registerKinds() {
                    registerKind(PluginFunctionKind, KPluginFunctionKind)
                }
            }

            private object PluginFunctionKind : FunctionTypeKind(
                FqName("repro.internal"),
                "PluginFunction",
                pluginFunctionAnnotationId,
                isReflectType = false,
                isInlineable = true,
            ) {
                override val prefixForTypeRender: String
                    get() = "@PluginFunction"

                override val serializeAsFunctionWithAnnotationUntil: String
                    get() = useLegacyCustomFunctionTypeSerializationUntil

                override fun reflectKind(): FunctionTypeKind = KPluginFunctionKind
            }

            private object KPluginFunctionKind : FunctionTypeKind(
                FqName("repro.internal"),
                "KPluginFunction",
                pluginFunctionAnnotationId,
                isReflectType = true,
                isInlineable = false,
            ) {
                override val prefixForTypeRender: String
                    get() = "@PluginFunction"

                override val serializeAsFunctionWithAnnotationUntil: String
                    get() = useLegacyCustomFunctionTypeSerializationUntil

                override fun nonReflectKind(): FunctionTypeKind = PluginFunctionKind
            }
        """.trimIndent()
    }
}
