import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask // for possible replacements

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js() {
        nodejs()
    }
    <SingleNativeTarget>("native")

    sourceSets {
        val commonTest = getByName("commonTest")
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }

    //insertable_at_kotlin_lvl
}
