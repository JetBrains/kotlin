/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package translators

import org.jetbrains.dokka.model.*
import utils.AbstractModelTest
import utils.OnlySymbols
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KMPTest : AbstractModelTest("/src/main/kotlin/kmp/Test.kt", "kmp") {

    // copy-pasted org.jetbrains.dokka.analysis.test.api.util.getResourceAbsolutePath
    private fun getResourceAbsolutePath(resourcePath: String): String {
        val resource = object {}.javaClass.classLoader.getResource(resourcePath)?.file
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")

        return File(resource).absolutePath
    }

    val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/androidMain/kotlin")
                analysisPlatform = "jvm"
                name = "android-example"
                // contains only `class Firebase`
                classpath = listOf(getResourceAbsolutePath("jars/jvmAndroidLib-jvm-copy.jar"))
            }
            sourceSet {
                sourceRoots = listOf("src/jvmMain/kotlin")
                analysisPlatform = "jvm"
                name = "jvm-example"
                // contains only `class Firebase`
                classpath = listOf(getResourceAbsolutePath("jars/jvmAndroidLib-jvm.jar"))
            }

        }
    }

    @Test
    fun `should resolve Firebase from the same renamed jars #3702`() {
        inlineModelTest(
            """
                |/src/androidMain/kotlin/main.kt
                |package example
                |import Firebase
                |
                |fun android(f: Firebase){}   
                |             
                |/src/jvmMain/kotlin/main.kt
                |package example
                |import Firebase
                |
                |fun jvm(f: Firebase){}
            """,
            configuration = configuration
        ) {
            with((this / "example" / "android").cast<DFunction>()) {
                assertTrue(parameters[0].type is GenericTypeConstructor)
            }
            with((this / "example" / "jvm").cast<DFunction>()) {
                assertTrue(parameters[0].type is GenericTypeConstructor)
            }
        }
    }


    @Test
    @OnlySymbols("Wasm Wasi")
    fun `should resolve a default import of wasmWasi`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/wasmWasi/kotlin")
                    analysisPlatform = "wasmWasi"
                    classpath = listOf(getResourceAbsolutePath("stdlib/kotlin-stdlib-wasm-wasi-2.2.21.klib"))
                }

            }
        }
        inlineModelTest(
            """
                |/src/wasmWasi/kotlin/main.kt
                |package example
                |
                |@ExperimentalWasmInterop
                |external fun f(): Int
            """,
            configuration = configuration
        ) {
            with((this / "example" / "f").cast<DFunction>()) {
                val annotation = extra[Annotations]?.directAnnotations?.values?.first()?.first().notNull("annotation")
                assertEquals("ExperimentalWasmInterop", annotation.dri.classNames)
                assertEquals("kotlin.wasm", annotation.dri.packageName)
                assertEquals(0, logger.warnMessages.size)
            }
        }
    }

    @Test
    @OnlySymbols("Wasm Wasi")
    fun `should not resolve a default import of wasmWasi`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/wasmWasi/kotlin")
                    analysisPlatform = "wasmJs"
                    classpath = listOf(getResourceAbsolutePath("stdlib/kotlin-stdlib-wasm-wasi-2.2.21.klib"))
                }

            }
        }
        inlineModelTest(
            """
                |/src/wasmWasi/kotlin/main.kt
                |package example
                |
                |@ExperimentalWasmInterop
                |external fun f(): Int
            """,
            configuration = configuration
        ) {
            with((this / "example" / "f").cast<DFunction>()) {
                val annotation = extra[Annotations]?.directAnnotations?.values?.first()?.first().notNull("annotation")
                assertEquals("<ERROR CLASS>", annotation.dri.classNames)
                assertEquals(1, logger.warnMessages.size)
            }
        }
    }
}
