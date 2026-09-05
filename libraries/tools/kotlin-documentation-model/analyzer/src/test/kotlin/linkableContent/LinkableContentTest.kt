/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package linkableContent

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.WithGenerics
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.doc.Text
import org.jsoup.Jsoup
import utils.assertNotNull
import java.net.URL
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class LinkableContentTest : BaseAbstractTest() {

    @Test
    fun `Include module and package documentation`() {

        val testDataDir = getTestDataDir("multiplatform/basicMultiplatformTest").toAbsolutePath()
        val includesDir = getTestDataDir("linkable/includes").toAbsolutePath()

        val configuration = dokkaConfiguration {
            moduleName = "example"
            sourceSets {
                val common = sourceSet {
                    name = "common"
                    displayName = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf(Paths.get("$testDataDir/commonMain/kotlin").toString())
                }
                val jvmAndJsSecondCommonMain = sourceSet {
                    name = "jvmAndJsSecondCommonMain"
                    displayName = "jvmAndJsSecondCommonMain"
                    analysisPlatform = "common"
                    dependentSourceSets = setOf(common.value.sourceSetID)
                    sourceRoots = listOf(Paths.get("$testDataDir/jvmAndJsSecondCommonMain/kotlin").toString())
                }
                sourceSet {
                    name = "js"
                    displayName = "js"
                    analysisPlatform = "js"
                    dependentSourceSets = setOf(common.value.sourceSetID, jvmAndJsSecondCommonMain.value.sourceSetID)
                    sourceRoots = listOf(Paths.get("$testDataDir/jsMain/kotlin").toString())
                    includes = listOf(Paths.get("$includesDir/include2.md").toString())
                }
                sourceSet {
                    name = "jvm"
                    displayName = "jvm"
                    analysisPlatform = "jvm"
                    dependentSourceSets = setOf(common.value.sourceSetID, jvmAndJsSecondCommonMain.value.sourceSetID)
                    sourceRoots = listOf(Paths.get("$testDataDir/jvmMain/kotlin").toString())
                    includes = listOf(Paths.get("$includesDir/include1.md").toString())
                }
            }
        }

        testFromData(configuration) {
            documentablesMergingStage = {
                assertEquals(2, it.documentation.size)
                assertEquals(2, it.packages.size)
                assertEquals(1, it.packages.first().documentation.size)
                assertEquals(0, it.packages.last().documentation.size)
            }
        }

    }

}


