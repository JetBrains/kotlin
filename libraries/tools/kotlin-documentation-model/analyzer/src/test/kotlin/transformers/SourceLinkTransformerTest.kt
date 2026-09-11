/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package transformers

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DEnum
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.WithSources
import org.jetbrains.dokka.model.withDescendants
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceLinkTransformerTest : BaseAbstractTest() {

    @Test
    fun `source should point to the declaration line, not to kdoc or annotations`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Deprecated.kt
            |package testpackage
            |
            |/**
            |* Marks the annotated declaration as deprecated. ...
            |*/
            |@Target(CLASS, FUNCTION, PROPERTY, ANNOTATION_CLASS, CONSTRUCTOR, PROPERTY_SETTER, PROPERTY_GETTER, TYPEALIAS)
            |@MustBeDocumented
            |public annotation class Deprecated(
            |    val message: String,
            |    val replaceWith: ReplaceWith = ReplaceWith(""),
            |    val level: DeprecationLevel = DeprecationLevel.WARNING
            |)
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val deprecated = module.packages.single().classlikes.single() as WithSources
                val source = deprecated.sources.values.single()

                assertEquals("Deprecated.kt", source.path.substringAfterLast('/'))
                assertEquals(8, source.computeLineNumber())
            }
        }
    }

    @Test
    fun `source should be for actual typealias`() {
        val mppConfiguration = dokkaConfiguration {
            moduleName = "test"
            sourceSets {
                sourceSet {
                    name = "common"
                    sourceRoots = listOf("src/main/kotlin/common/Test.kt")
                    classpath = listOf(commonStdlibPath!!)
                }
                sourceSet {
                    name = "jvm"
                    dependentSourceSets = setOf(DokkaSourceSetID("test", "common"))
                    sourceRoots = listOf("src/main/kotlin/jvm/Test.kt")
                    classpath = listOf(commonStdlibPath!!)
                }
            }
        }

        testInline(
            """
                |/src/main/kotlin/common/Test.kt
                |package example
                |
                |expect class Foo
                |
                |/src/main/kotlin/jvm/Test.kt
                |package example
                |
                |class Bar
                |actual typealias Foo = Bar
                |
            """.trimMargin(),
            mppConfiguration
        ) {
            documentablesMergingStage = { module ->
                val jvmSource = module.withDescendants()
                    .filterIsInstance<WithSources>()
                    .filter { (it as Documentable).name == "Foo" }
                    .flatMap { it.sources.entries }
                    .single { it.key.sourceSetID.sourceSetName == "jvm" }
                    .value

                assertEquals("Test.kt", jvmSource.path.substringAfterLast('/'))
                assertEquals(4, jvmSource.computeLineNumber())
            }
        }
    }

    @Test
    fun `property and function with the same name should have their own source lines #4338`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Deprecated.kt
            |package testpackage
            |
            |val ff = 0 // #4338
            |fun ff() = 0
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val pkg = module.packages.single()

                val property = pkg.properties.single { it.name == "ff" }
                assertEquals(3, property.sources.values.single().computeLineNumber())

                val function = pkg.functions.single { it.name == "ff" }
                assertEquals(4, function.sources.values.single().computeLineNumber())
            }
        }
    }

    @Test
    fun `overloaded functions should have their own source lines #4049`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Deprecated.kt
            |package testpackage
            |fun overloadWithVararg(novararg: String){}  // #4049
            |fun overloadWithVararg(vararg elements: String){}
        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val overloads = module.packages.single().functions.filter { it.name == "overloadWithVararg" }

                assertEquals(2, overloads.size)
                assertEquals(
                    setOf(2, 3),
                    overloads.map { it.sources.values.single().computeLineNumber() }.toSet()
                )
            }
        }
    }

    @Test
    fun `synthetic enum values should have a source line`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/")
                }
            }
        }

        testInline(
            """
            |/src/main/kotlin/basic/Deprecated.kt
            |package testpackage
            |
            |enum class Deprecated {
            |    A;
            |}

        """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val enum = module.packages.single().classlikes.single() as DEnum
                val values = enum.functions.single { it.name == "values" }

                // synthetic `values` falls back to the enum declaration line
                assertEquals(3, values.sources.values.single().computeLineNumber())
            }
        }
    }
}
