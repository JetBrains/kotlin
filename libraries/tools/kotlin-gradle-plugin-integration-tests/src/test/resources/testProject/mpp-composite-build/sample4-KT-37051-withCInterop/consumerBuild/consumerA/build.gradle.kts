@file:Suppress("OPT_IN_USAGE")

plugins {
    kotlin("multiplatform")
}

kotlin {
    applyDefaultHierarchyTemplate()
    jvm()
    linuxX64()
    linuxArm64()

    sourceSets.commonMain.get().dependencies {
        implementation("org.jetbrains.sample:producerA:1.0.0-SNAPSHOT")
    }

    targets.all {
        compilations.all {
            compilerOptions.configure {
                allWarningsAsErrors.set(true)
            }
        }
    }
}