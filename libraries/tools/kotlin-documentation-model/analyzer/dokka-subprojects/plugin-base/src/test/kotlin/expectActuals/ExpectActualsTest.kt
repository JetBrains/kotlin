/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package expectActuals

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.ExperimentalDokkaApi
import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.driOrNull
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.PointingToContextParameters
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.transformers.documentation.ClashingDriIdentifier
import org.junit.jupiter.api.Nested
import translators.findClasslike
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue


class ExpectActualsTest : BaseAbstractTest() {

    private val multiplatformConfiguration = dokkaConfiguration {
        sourceSets {
            val commonId = sourceSet {
                sourceRoots = listOf("src/common/")
                analysisPlatform = "common"
                name = "common"
                displayName = "common"
            }.value.sourceSetID
            sourceSet {
                sourceRoots = listOf("src/jvm/")
                analysisPlatform = "jvm"
                name = "jvm"
                displayName = "jvm"
                dependentSourceSets = setOf(commonId)
            }
            sourceSet {
                sourceRoots = listOf("src/native/")
                analysisPlatform = "native"
                name = "native"
                displayName = "native"
                dependentSourceSets = setOf(commonId)
            }
        }
    }
    private val commonSourceSetId =
        multiplatformConfiguration.sourceSets.single { it.displayName == "common" }.sourceSetID

    @Test
    fun `property inside expect class should be marked as expect`() = testInline(
        """
        /src/common/test.kt
        expect class ExpectActualClass {
          val property: String?
        }
        
        /src/jvm/test.kt
        actual class ExpectActualClass {
          actual val property: String? = null
        }
        
        /src/native/test.kt
        actual class ExpectActualClass {
          actual val property: String? = null
        }
        """.trimMargin(),
        multiplatformConfiguration
    ) {
        documentablesTransformationStage = { module ->
            val cls = module.packages.single().classlikes.single { it.name == "ExpectActualClass" }
            assertTrue(cls.isExpectActual)
            assertEquals(commonSourceSetId, cls.expectPresentInSet?.sourceSetID)
            val property = cls.properties.single { it.name == "property" }
            assertTrue(property.isExpectActual)
            assertEquals(commonSourceSetId, property.expectPresentInSet?.sourceSetID)
        }
    }

    @Test
    fun `function inside expect class should be marked as expect`() = testInline(
        """
        /src/common/test.kt
        expect class ExpectActualClass {
          fun function(): String?
        }
        
        /src/jvm/test.kt
        actual class ExpectActualClass {
          actual fun function(): String? = null
        }
        
        /src/native/test.kt
        actual class ExpectActualClass {
          actual fun function(): String? = null
        }
        """.trimMargin(),
        multiplatformConfiguration
    ) {
        documentablesTransformationStage = { module ->
            val cls = module.packages.single().classlikes.single { it.name == "ExpectActualClass" }
            assertTrue(cls.isExpectActual)
            assertEquals(commonSourceSetId, cls.expectPresentInSet?.sourceSetID)
            val function = cls.functions.single { it.name == "function" }
            assertTrue(function.isExpectActual)
            assertEquals(commonSourceSetId, function.expectPresentInSet?.sourceSetID)
        }
    }

    @Test
    fun `top level expect property should be marked as expect`() = testInline(
        """
        /src/common/test.kt
        expect val property: String?
        
        /src/jvm/test.kt
        actual val property: String? = null
        
        /src/native/test.kt
        actual val property: String? = null
        """.trimMargin(),
        multiplatformConfiguration
    ) {
        documentablesTransformationStage = { module ->
            val property = module.packages.single().properties.single { it.name == "property" }
            assertTrue(property.isExpectActual)
            assertEquals(commonSourceSetId, property.expectPresentInSet?.sourceSetID)
        }
    }

    @Test
    fun `top level expect function should be marked as expect`() = testInline(
        """
        /src/common/test.kt
        expect fun function(): String?
        
        /src/jvm/test.kt
        actual fun function(): String? = null
        
        /src/native/test.kt
        actual fun function(): String? = null
        """.trimMargin(),
        multiplatformConfiguration
    ) {
        documentablesTransformationStage = { module ->
            val function = module.packages.single().functions.single { it.name == "function" }
            assertTrue(function.isExpectActual)
            assertEquals(commonSourceSetId, function.expectPresentInSet?.sourceSetID)
        }
    }

    @Test
    fun `three same named expect actual classes`() {

        val configuration = dokkaConfiguration {
            moduleName = "example"
            sourceSets {
                val common = sourceSet {
                    name = "common"
                    displayName = "common"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/commonMain/kotlin/pageMerger/Test.kt")
                }
                val commonJ = sourceSet {
                    name = "commonJ"
                    displayName = "commonJ"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/commonJMain/kotlin/pageMerger/Test.kt")
                    dependentSourceSets = setOf(common.value.sourceSetID)
                }
                val commonN1 = sourceSet {
                    name = "commonN1"
                    displayName = "commonN1"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/commonN1Main/kotlin/pageMerger/Test.kt")
                    dependentSourceSets = setOf(common.value.sourceSetID)
                }
                val commonN2 = sourceSet {
                    name = "commonN2"
                    displayName = "commonN2"
                    analysisPlatform = "common"
                    sourceRoots = listOf("src/commonN2Main/kotlin/pageMerger/Test.kt")
                    dependentSourceSets = setOf(common.value.sourceSetID)
                }
                sourceSet {
                    name = "js"
                    displayName = "js"
                    analysisPlatform = "js"
                    dependentSourceSets = setOf(commonJ.value.sourceSetID)
                    sourceRoots = listOf("src/jsMain/kotlin/pageMerger/Test.kt")
                }
                sourceSet {
                    name = "jvm"
                    displayName = "jvm"
                    analysisPlatform = "jvm"
                    dependentSourceSets = setOf(commonJ.value.sourceSetID)
                    sourceRoots = listOf("src/jvmMain/kotlin/pageMerger/Test.kt")
                }
                sourceSet {
                    name = "linuxX64"
                    displayName = "linuxX64"
                    analysisPlatform = "native"
                    dependentSourceSets = setOf(commonN1.value.sourceSetID)
                    sourceRoots = listOf("src/linuxX64Main/kotlin/pageMerger/Test.kt")
                }
                sourceSet {
                    name = "mingwX64"
                    displayName = "mingwX64"
                    analysisPlatform = "native"
                    dependentSourceSets = setOf(commonN1.value.sourceSetID)
                    sourceRoots = listOf("src/mingwX64Main/kotlin/pageMerger/Test.kt")
                }
                sourceSet {
                    name = "iosArm64"
                    displayName = "iosArm64"
                    analysisPlatform = "native"
                    dependentSourceSets = setOf(commonN2.value.sourceSetID)
                    sourceRoots = listOf("src/iosArm64Main/kotlin/pageMerger/Test.kt")
                }
                sourceSet {
                    name = "iosX64"
                    displayName = "iosX64"
                    analysisPlatform = "native"
                    dependentSourceSets = setOf(commonN2.value.sourceSetID)
                    sourceRoots = listOf("src/iosX64Main/kotlin/pageMerger/Test.kt")
                }
            }
        }

        testInline(
            """
                |/src/commonMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |/src/commonJMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |expect class A
                |
                |/src/commonN1Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |expect class A
                |
                |/src/commonN2Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |expect class A
                |
                |/src/jsMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual class A
                |
                |/src/jvmMain/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual class A
                |
                |/src/linuxX64Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual class A
                |
                |/src/mingwX64Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual class A
                |
                |/src/iosArm64Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual class A
                |
                |/src/iosX64Main/kotlin/pageMerger/Test.kt
                |package pageMerger
                |
                |actual class A
                |
            """.trimMargin(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val classes = module.packages.single().classlikes.filter { it.name == "A" }.map { it as DClass }
                assertEquals(3, classes.size, "There should be three merged 'A' classes, one per expect/actual group")

                // each expect/actual group is merged into a separate classlike with its own source sets
                val sourceSetGroups = classes.map { cls -> cls.sourceSets.map { it.displayName }.toSet() }.toSet()
                assertEquals(
                    setOf(
                        setOf("commonJ", "js", "jvm"),
                        setOf("commonN1", "linuxX64", "mingwX64"),
                        setOf("commonN2", "iosArm64", "iosX64"),
                    ),
                    sourceSetGroups
                )

                // all three are marked as clashing DRIs, so there is no leftover ungrouped 'A'
                assertTrue(
                    classes.all { it.extra[ClashingDriIdentifier] != null },
                    "Each 'A' should be marked with a ClashingDriIdentifier"
                )
            }
        }
    }

    @Test
    fun `public actual function should be present when expect is internal`() = testInline(
        """
        /src/common/test.kt
        internal expect fun function(): String?
        
        /src/jvm/test.kt
        public actual fun function(): String? = null
        
        /src/native/test.kt
        public actual fun function(): String? = null
        """.trimMargin(),
        multiplatformConfiguration
    ) {
        documentablesTransformationStage = { module ->
            val function = module.packages.single().functions.single { it.name == "function" }
            assertTrue(function.isExpectActual)
            // no `common` is present
            assertEquals(
                setOf("jvm", "native"),
                function.sourceSets.map { it.sourceSetID.sourceSetName }.toSet()
            )
        }
    }

    @Test
    fun `public actual function should be present when expect is internal and other actual is internal`() = testInline(
        """
        /src/common/test.kt
        internal expect fun function(): String?
        
        /src/jvm/test.kt
        public actual fun function(): String? = null
        
        /src/native/test.kt
        internal actual fun function(): String? = null
        """.trimMargin(),
        multiplatformConfiguration
    ) {
        documentablesTransformationStage = { module ->
            val function = module.packages.single().functions.single { it.name == "function" }
            assertTrue(function.isExpectActual)
            // `common` - internal, `native` - internal, so only `jvm` is present
            assertEquals(
                setOf("jvm"),
                function.sourceSets.map { it.sourceSetID.sourceSetName }.toSet()
            )
        }
    }

    @Test
    fun `public actual classe should be present when expect is internal`() = testInline(
        """
        /src/common/test.kt
        internal expect class Class
        
        /src/jvm/test.kt
        public actual class Class
        
        /src/native/test.kt
        public actual class Class
        """.trimMargin(),
        multiplatformConfiguration
    ) {
        documentablesTransformationStage = { module ->
            val classlike = module.packages.single().classlikes.single { it.name == "Class" }
            assertTrue(classlike.isExpectActual)
            // no `common` is present
            assertEquals(
                setOf("jvm", "native"),
                classlike.sourceSets.map { it.sourceSetID.sourceSetName }.toSet()
            )
        }
    }


    @Test
    fun `should work with the reversed order of source sets #3798`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                val commonId = DokkaSourceSetID("root", "common")
                sourceSet {
                    sourceRoots = listOf("src/jvm/")
                    analysisPlatform = "jvm"
                    name = "jvm"
                    displayName = "jvm"
                    dependentSourceSets = setOf(commonId)
                }
                sourceSet {
                    sourceRoots = listOf("src/native/")
                    analysisPlatform = "native"
                    name = "native"
                    displayName = "native"
                    dependentSourceSets = setOf(commonId)
                }
                sourceSet {
                    sourceRoots = listOf("src/common/")
                    analysisPlatform = "common"
                    name = "common"
                    displayName = "common"
                }
            }
        }

        testInline(
            """
        /src/common/test.kt
        expect fun shared()
        
        /src/native/test.kt
        actual fun shared(){}
        
        /src/jvm/test.kt
        actual fun shared(){}
        """.trimMargin(), configuration
        ) {
            documentablesTransformationStage = { module ->
                val function = module.packages.single().functions.single { it.name == "shared" }
                assertTrue(function.isExpectActual)
                assertEquals(
                    "common", function.expectPresentInSet?.sourceSetID?.sourceSetName
                )
                assertEquals(
                    setOf("common", "jvm", "native"), function.sourceSets.map { it.sourceSetID.sourceSetName }.toSet()
                )
            }
        }
    }



    @Test
    @OptIn(ExperimentalDokkaApi::class)
    fun `expect-actual overloads with context parameters`() {
        testInline(
            """
        /src/common/test.kt
        context(_: Int)
        expect fun f(i: Int) = i
        
        context(_: String)
        expect fun f(i: Int) = i
        
        /src/jvm/test.kt
        context(_: Int)
        actual fun f(i: Int) = i
        
        context(_: String)
        actual fun f(i: Int) = i
        
        /src/native/test.kt
        context(_: Int)
        actual fun f(i: Int) = i
        
        context(_: String)
        actual fun f(i: Int) = i
        """.trimMargin(),
            multiplatformConfiguration
        ) {
            documentablesTransformationStage = { module ->
                val functions = module.packages.single().functions
                assertEquals(2, functions.size)
                val first = functions[0]
                assertEquals("f", first.name)
                with(first.contextParameters.single()) {
                    assertEquals(DRI("kotlin", "Int"), type.driOrNull)
                    assertEquals("_", name)
                    assertEquals(0, (dri.target as? PointingToContextParameters)?.parameterIndex)
                    assertEquals(
                        setOf("common", "jvm", "native"), this.sourceSets.map { it.sourceSetID.sourceSetName }.toSet()
                    )
                }
                assertEquals(
                    setOf("common", "jvm", "native"), first.sourceSets.map { it.sourceSetID.sourceSetName }.toSet()
                )

                val second = functions[1]
                assertEquals("f", second.name)
                with(second.contextParameters.single()) {
                    assertEquals(DRI("kotlin", "String"), type.driOrNull)
                    assertEquals("_", name)
                    assertEquals(0, (dri.target as? PointingToContextParameters)?.parameterIndex)
                    assertEquals(
                        setOf("common", "jvm", "native"), this.sourceSets.map { it.sourceSetID.sourceSetName }.toSet()
                    )
                }
                assertEquals(
                    setOf("common", "jvm", "native"), second.sourceSets.map { it.sourceSetID.sourceSetName }.toSet()
                )
            }
        }
    }

    @Test
    @OptIn(ExperimentalDokkaApi::class)
    fun `expect-actual properties with context parameters`() {
        testInline(
            """
        /src/common/test.kt
        context(_: String)
        expect val i = 42
        
        /src/jvm/test.kt
        context(_: String)
        actual val i = 42
        
        /src/native/test.kt
        context(_: String)
        actual val i = 42
        """.trimMargin(),
            multiplatformConfiguration
        ) {
            documentablesTransformationStage = { module ->
                val property = module.packages.single().properties.single()
                assertEquals("i", property.name)
                with(property.contextParameters.single()) {
                    assertEquals(DRI("kotlin", "String"), type.driOrNull)
                    assertEquals(0, (dri.target as? PointingToContextParameters)?.parameterIndex)
                    assertEquals("_", name)
                    assertEquals(
                        setOf("common", "jvm", "native"), this.sourceSets.map { it.sourceSetID.sourceSetName }.toSet()
                    )
                }
                assertEquals(
                    setOf("common", "jvm", "native"), property.sourceSets.map { it.sourceSetID.sourceSetName }.toSet()
                )
            }
        }
    }

    @Test
    fun `kdoc on expect-actual`() {
        testInline(
            """
        /src/common/test.kt
        /**
         * @constructor Common Description
         */
        expect class A() {
            /**
             * Common Description
             */
            fun foo()
            
            /**
             * Common Description
             */
            fun bar()
        }
        
        /src/jvm/test.kt
        actual class A actual constructor() {
            actual fun foo() {}
            
            /**
             * Platform Description
             */
            actual fun bar() {}
        }
        """.trimMargin(),
            multiplatformConfiguration
        ) {
            documentablesTransformationStage = { module ->
                val a = module.findClasslike("", "A") as DClass
                val constructor = a.constructors.single()
                val foo = a.functions.first { it.name == "foo" }
                val bar = a.functions.first { it.name == "bar" }

                val common = multiplatformConfiguration.sourceSets.first { it.displayName == "common" }
                val jvm = multiplatformConfiguration.sourceSets.first { it.displayName == "jvm" }

                assertEquals(2, constructor.documentation.size)
                assertEquals(constructor.documentation[common], constructor.documentation[jvm])
                constructor.documentation.values.forEach {
                    assertEquals(
                        "DocumentationNode(children=[Description(root=CustomDocTag(children=[P(children=[Text(body=Common Description, children=[], params={})], params={})], params={}, name=MARKDOWN_FILE))])",
                        it.toString()
                    )
                }

                assertEquals(2, foo.documentation.size)
                assertEquals(foo.documentation[common], foo.documentation[jvm])
                foo.documentation.values.forEach {
                    assertEquals(
                        "DocumentationNode(children=[Description(root=CustomDocTag(children=[P(children=[Text(body=Common Description, children=[], params={})], params={})], params={}, name=MARKDOWN_FILE))])",
                        it.toString()
                    )
                }

                assertEquals(2, bar.documentation.size)
                assertNotEquals(bar.documentation[common], bar.documentation[jvm])
                assertEquals(
                    "DocumentationNode(children=[Description(root=CustomDocTag(children=[P(children=[Text(body=Common Description, children=[], params={})], params={})], params={}, name=MARKDOWN_FILE))])",
                    bar.documentation[common].toString()
                )
                assertEquals(
                    "DocumentationNode(children=[Description(root=CustomDocTag(children=[P(children=[Text(body=Platform Description, children=[], params={})], params={})], params={}, name=MARKDOWN_FILE))])",
                    bar.documentation[jvm].toString()
                )
            }
        }
    }
}
