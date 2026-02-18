import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary

plugins {
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        jsMain {
            dependencies {
                implementation(project(":lib"))
            }
        }

        jsTest {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

kotlin {
    js {
        browser {
        }
        binaries.executable()
    }
}
