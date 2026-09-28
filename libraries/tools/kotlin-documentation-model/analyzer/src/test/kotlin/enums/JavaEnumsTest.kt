/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package enums

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DEnum
import kotlin.test.Test
import kotlin.test.assertEquals

class JavaEnumsTest : BaseAbstractTest() {

    private val basicConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
            }
        }
    }

    // The enum declaration should resolve its source line even in the presence of synthetic
    // methods (values, valueOf), see https://github.com/Kotlin/dokka/issues/2544
    @Test
    fun `java enum should resolve its source line`() {
        testInline(
            """
            |/src/main/java/testpackage/JavaEnum.java
            |package testpackage
            |
            |/**
            |* doc
            |*/
            |public enum JavaEnum {
            |    ONE, TWO, THREE
            |}
        """.trimMargin(),
            basicConfiguration
        ) {
            documentablesMergingStage = { module ->
                val enum = module.packages.single()
                    .classlikes.single() as DEnum

                assertEquals(2, enum.functions.count { it.name == "values" || it.name == "valueOf" })

                val enumSource = enum.sources.values.single()
                assertEquals("JavaEnum.java", enumSource.path.substringAfterLast('/'))
                assertEquals(6, enumSource.computeLineNumber())
            }
        }
    }
}
