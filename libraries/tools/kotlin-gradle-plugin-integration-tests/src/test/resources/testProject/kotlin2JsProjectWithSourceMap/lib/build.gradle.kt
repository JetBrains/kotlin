import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import kotlin.text.toBoolean

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

val useIrBackend = (findProperty("kotlin.js.useIrBackend") as? String?)?.toBoolean() ?: false

val backend = if (useIrBackend) {
    KotlinJsCompilerType.IR
} else {
    KotlinJsCompilerType.LEGACY
}

kotlin {
    js(backend) {
        browser()
    }
}

tasks.withType<KotlinJsCompile>() {
    kotlinOptions.freeCompilerArgs += "-Xforce-deprecated-legacy-compiler-usage"
}