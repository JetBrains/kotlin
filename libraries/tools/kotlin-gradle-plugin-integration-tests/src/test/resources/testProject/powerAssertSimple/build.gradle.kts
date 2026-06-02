plugins {
    kotlin("multiplatform")
    kotlin("plugin.power-assert")
}

kotlin {
    jvm()
    js()
    linuxX64()

    sourceSets {
        val commonTest = getByName("commonTest") {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmTest = getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val jsTest = getByName("jsTest") {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

powerAssert {
    functions.addAll(
        "kotlin.test.assertTrue",
        "kotlin.require",
        "sample.AssertScope.assert",
        "sample.assert",
        "sample.dbg"
    )
}
