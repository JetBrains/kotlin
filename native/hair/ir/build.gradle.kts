plugins {
    kotlin("multiplatform")
    id("common-configuration")
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
