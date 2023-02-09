package model

import org.jetbrains.dokka.model.DPackage
import org.junit.jupiter.api.Test
import utils.AbstractModelTest

class PackagesTest : AbstractModelTest("/src/main/kotlin/packages/Test.kt", "packages") {

    @Test
    fun rootPackage() {
        inlineModelTest(
            """
                |
            """.trimIndent(),
            prependPackage = false,
            configuration = dokkaConfiguration {
                sourceSets {
                    sourceSet {
                        sourceRoots = listOf("src/main/kotlin")
                        displayName = "JVM"
                    }
                }
            }
        ) {
            with((this / "[root]").cast<DPackage>()) {
                packageName equals ""
                children counts 0
            }
        }
    }

    @Test
    fun simpleNamePackage() {
        inlineModelTest(
            """
                |package simple
            """.trimIndent(),
            prependPackage = false
        ) {
            with((this / "simple").cast<DPackage>()) {
                packageName equals "simple"
                children counts 0
            }
        }
    }

    @Test
    fun dottedNamePackage() {
        inlineModelTest(
            """
                |package dot.name
            """.trimIndent(),
            prependPackage = false
        ) {
            with((this / "dot.name").cast<DPackage>()) {
                packageName equals "dot.name"
                children counts 0
            }
        }

    }

    @Test
    fun multipleFiles() {
        inlineModelTest(
            """
                |package dot.name
                |/src/main/kotlin/packages/Test2.kt
                |package simple
            """.trimIndent(),
            prependPackage = false
        ) {
            children counts 2
            with((this / "dot.name").cast<DPackage>()) {
                packageName equals "dot.name"
                children counts 0
            }
            with((this / "simple").cast<DPackage>()) {
                packageName equals "simple"
                children counts 0
            }
        }
    }

    @Test
    fun multipleFilesSamePackage() {
        inlineModelTest(
            """
                |package simple
                |/src/main/kotlin/packages/Test2.kt
                |package simple
            """.trimIndent(),
            prependPackage = false
        ) {
            children counts 1
            with((this / "simple").cast<DPackage>()) {
                packageName equals "simple"
                children counts 0
            }
        }
    }

    @Test
    fun classAtPackageLevel() {
        inlineModelTest(
            """
                |package simple.name
                |
                |class Foo {}
            """.trimIndent(),
            prependPackage = false
        ) {
            with((this / "simple.name").cast<DPackage>()) {
                packageName equals "simple.name"
                children counts 1
            }
        }
    }
}
