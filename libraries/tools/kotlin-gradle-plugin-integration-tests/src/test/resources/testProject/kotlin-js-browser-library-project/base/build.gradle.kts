import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.IR as IR_TYPE

plugins {
    kotlin("js")
}

dependencies {
    implementation(kotlin("stdlib-js"))
}

@Suppress("DEPRECATION")
kotlin {
    js("ir")
    js(IR)
    js(IR_TYPE) {
        useCommonJs()
        browser {
        }
    }
}