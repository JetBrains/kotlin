plugins {
    kotlin("multiplatform")
    kotlin("plugin.power-assert")
}

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    jvm()
    js()
    linuxX64()

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val jsTest by getting {
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
