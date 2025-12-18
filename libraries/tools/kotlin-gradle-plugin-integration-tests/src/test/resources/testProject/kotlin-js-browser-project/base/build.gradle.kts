import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.IR as IR_TYPE

plugins {
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        jsMain {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
    }
}

kotlin {
    js {
        useCommonJs()
        browser {
        }
    }
}