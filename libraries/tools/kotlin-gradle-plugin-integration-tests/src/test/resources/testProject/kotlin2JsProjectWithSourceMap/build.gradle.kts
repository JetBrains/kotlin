import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import kotlin.text.toBoolean

plugins {
    kotlin("js")
}

repositories {
    mavenLocal()
    mavenCentral()
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.js")

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

val useIrBackend = (findProperty("kotlin.js.useIrBackend") as? String?)?.toBoolean() ?: false

val backend = if (useIrBackend) {
    KotlinJsCompilerType.IR
} else {
    KotlinJsCompilerType.LEGACY
}

project("lib") {
    kotlin {
        js(backend) {
            browser()
        }
    }

    tasks.withType<KotlinJsCompile>() {
        kotlinOptions.freeCompilerArgs += "-Xforce-deprecated-legacy-compiler-usage"
    }
}

project("app") {
    kotlin {
        js(backend) {
            browser()
            binaries.executable()
        }
        dependencies {
            implementation(project(":lib"))
        }
    }

    tasks.withType<KotlinJsCompile>() {
        kotlinOptions {
            sourceMap = true
            freeCompilerArgs += "-Xforce-deprecated-legacy-compiler-usage"
        }
    }
}
