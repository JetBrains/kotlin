/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.smoke

import com.intellij.openapi.util.Disposer
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.resolution.resolveSuccessfulCall
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds
import org.jetbrains.kotlin.analysis.api.types.classId
import org.jetbrains.kotlin.analysis.api.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.writeText

@OptIn(KaExperimentalApi::class)
class AnalysisApiSmokeTest {
    @TempDir
    lateinit var sourceRoot: Path

    @Test
    fun testKotlinAndJavaAnalysis() {
        sourceRoot.resolve("JavaGreeter.java").writeText(
            """
            package smoke;

            public class JavaGreeter {
                public static String greet(String name) {
                    return "Hello, " + name;
                }
            }
            """.trimIndent()
        )

        sourceRoot.resolve("main.kt").writeText(
            """
            package smoke

            class KotlinGreeter {
                fun greet(name: String): String = JavaGreeter.greet(name)
            }

            fun main() {
                println(KotlinGreeter().greet("world"))
            }
            """.trimIndent()
        )

        val disposable = Disposer.newDisposable("AnalysisApiSmokeTest")
        try {
            lateinit var sourceModule: KaSourceModule
            val session = buildStandaloneAnalysisAPISession(disposable) {
                buildKtModuleProvider {
                    platform = JvmPlatforms.defaultJvmPlatform

                    val jdkHome = Paths.get(System.getProperty("java.home"))
                    val sdk = addModule(
                        buildKtSdkModule {
                            addBinaryRootsFromJdkHome(jdkHome, isJre = true)
                            addBinaryRootsFromJdkHome(jdkHome, isJre = false)
                            platform = JvmPlatforms.defaultJvmPlatform
                            libraryName = "JDK"
                        }
                    )

                    sourceModule = addModule(
                        buildKtSourceModule {
                            addSourceRoot(sourceRoot)
                            addRegularDependency(sdk)
                            platform = JvmPlatforms.defaultJvmPlatform
                            moduleName = "source"
                        }
                    )
                }
            }

            val ktFile = session.modulesWithFiles.getValue(sourceModule).filterIsInstance<KtFile>().single()
            val calls = ktFile.collectDescendantsOfType<KtCallExpression>().associateBy { it.text }

            analyze(ktFile) {
                val javaMethod = calls.getValue("greet(name)").resolveSuccessfulCall()?.signature?.symbol
                    ?: fail("The call to the Java method is unresolved")

                assertEquals(CallableId(ClassId.fromString("smoke/JavaGreeter"), Name.identifier("greet")), javaMethod.callableId)
                assertEquals(KaSymbolOrigin.JAVA_SOURCE, javaMethod.origin)
                assertEquals(KaStandardTypeClassIds.STRING, javaMethod.returnType.lowerBoundIfFlexible().classId)

                val kotlinFunction = calls.getValue("greet(\"world\")").resolveSuccessfulCall()?.signature?.symbol
                    ?: fail("The call to the Kotlin function is unresolved")

                assertEquals(CallableId(ClassId.fromString("smoke/KotlinGreeter"), Name.identifier("greet")), kotlinFunction.callableId)
                assertEquals(KaSymbolOrigin.SOURCE, kotlinFunction.origin)
            }

            val lightClass = JavaPsiFacade.getInstance(session.project)
                .findClass("smoke.KotlinGreeter", GlobalSearchScope.allScope(session.project))
                ?: fail("No light class for the Kotlin class")

            assertEquals(listOf("greet"), lightClass.methods.filterNot { it.isConstructor }.map { it.name })
        } finally {
            Disposer.dispose(disposable)
        }
    }
}
