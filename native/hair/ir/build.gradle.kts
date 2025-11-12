plugins {
    kotlin("multiplatform")
    // TODO kotlin("plugin.serialization") version "2.2.20"
    idea
}

kotlin {
    jvm()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlinStdlib())
                implementation(project(":native:hair:sym"))
                implementation(project(":native:hair:utils"))
            }
            kotlin.srcDir("src/commonMain/src")
            kotlin.srcDir("src/commonMain/generated")
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

idea {
    module {
        generatedSourceDirs.add(file("src/commonMain/generated"))
    }
}
