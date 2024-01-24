/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package multiplatform

import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.driOrNull
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.dfs
import utils.OnlySymbols
import kotlin.test.Test
import kotlin.test.assertEquals

class BasicMultiplatformTest : BaseAbstractTest() {

    @Test
    fun dataTestExample() {
        val testDataDir = getTestDataDir("multiplatform/basicMultiplatformTest").toAbsolutePath()

        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("$testDataDir/jvmMain/")
                }
            }
        }

        testFromData(configuration) {
            pagesTransformationStage = {
                assertEquals(7, it.children.firstOrNull()?.children?.count() ?: 0)
            }
        }
    }

    @Test
    fun inlineTestExample() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/multiplatform/Test.kt")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/multiplatform/Test.kt
            |package multiplatform
            |
            |object Test {
            |   fun test2(str: String): Unit {println(str)}
            |}
        """.trimMargin(),
            configuration
        ) {
            pagesGenerationStage = {
                assertEquals(3, it.parentMap.size)
            }
        }
    }


    @OnlySymbols("#3377 - types from transitive source sets are unresolved in K1")
    @Test
    fun `should resolve types from transitive source sets`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                val common = sourceSet {
                    name = "common"
                    displayName = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/main/kotlin/common/Test.kt")
                }

                val shared = sourceSet {
                    name = "shared"
                    displayName = "shared"
                    analysisPlatform = "common"
                    dependentSourceSets = setOf(common.value.sourceSetID)
                }
                sourceSet {
                    name = "jvm"
                    displayName = "jvm"
                    analysisPlatform = "jvm"
                    sourceRoots = listOf("src/main/kotlin/jvm/Test.kt")
                    dependentSourceSets = setOf(shared.value.sourceSetID)
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/common/Test.kt
            |package multiplatform
            |
            |class A
            |
            |/src/main/kotlin/jvm/Test.kt
            |package multiplatform
            |
            |fun fn(a: A) {}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = {
                val fn = it.dfs { it is DFunction && it.name == "fn" } as DFunction
                assertEquals(DRI("multiplatform", "A"), fn.parameters.firstOrNull()?.type?.driOrNull)
            }
        }
    }
}
