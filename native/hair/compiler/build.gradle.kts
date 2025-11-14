plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(kotlinStdlib())
                implementation(project(":native:hair:utils"))
                implementation(project(":native:hair:sym"))
                implementation(project(":native:hair:ir"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
    
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}