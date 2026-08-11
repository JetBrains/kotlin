plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Same published GAV as android-lib — control path that indexes correctly.
            implementation("ru.quickresto:kkm-contract:3.0.0")
        }
    }
}
