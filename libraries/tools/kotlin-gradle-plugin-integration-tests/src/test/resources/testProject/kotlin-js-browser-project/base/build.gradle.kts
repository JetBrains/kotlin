import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.BOTH as BOTH_TYPE

plugins {
    kotlin("js")
}

dependencies {
    implementation(kotlin("stdlib-js"))
}

@Suppress("DEPRECATION")
kotlin {
    js("both")
    js(BOTH)
    js(BOTH_TYPE) {
        useCommonJs()
        browser {
        }
    }
}

tasks.named<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile>("compileKotlinJsLegacy") {
    kotlinOptions.freeCompilerArgs += "-Xforce-deprecated-legacy-compiler-usage"
}
tasks.named<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile>("compileTestKotlinJsLegacy") {
    kotlinOptions.freeCompilerArgs += "-Xforce-deprecated-legacy-compiler-usage"
}
