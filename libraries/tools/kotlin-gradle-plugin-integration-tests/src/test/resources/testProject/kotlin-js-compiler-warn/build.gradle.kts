import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.BOTH as BOTH_TYPE

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

@Suppress("DEPRECATION")
kotlin {
    js(BOTH)
    js(BOTH_TYPE) {
        useCommonJs()
        browser {
        }
    }
    js {

    }
}