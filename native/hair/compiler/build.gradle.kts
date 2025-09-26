plugins {
    kotlin("jvm")
}

val main by sourceSets.getting {
    dependencies {
        api(kotlinStdlib())
        implementation(project(":native:hair:utils"))
        implementation(project(":native:hair:sym"))
        implementation(project(":native:hair:ir:core"))
        implementation(project(":native:hair:ir:generated"))
    }
}

val test by sourceSets.getting {
    dependencies {
        implementation(kotlin("test"))
    }
}

kotlin {
    compilerOptions {
        //languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0) // or 2.1
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}