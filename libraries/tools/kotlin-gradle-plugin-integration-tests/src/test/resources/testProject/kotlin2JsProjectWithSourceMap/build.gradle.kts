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

subprojects {
    apply(plugin = "org.jetbrains.kotlin.js")

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

kotlin {
    js()
}

project("lib") {
    kotlin {
        js {
            browser()
        }
    }
}

project("app") {
    kotlin {
        js {
            browser()
            binaries.executable()
        }
        dependencies {
            implementation(project(":lib"))
        }
    }
}
