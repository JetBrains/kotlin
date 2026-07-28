plugins {
    kotlin("multiplatform")
    id("common-configuration")
}

kotlin {
    jvm()
    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                api(kotlinStdlib())
                implementation(project(":native:hair:utils"))
                implementation(project(":native:hair:sym"))
                implementation(project(":native:hair:ir"))
            }
        }
        val commonTest = getByName("commonTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
    
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}
