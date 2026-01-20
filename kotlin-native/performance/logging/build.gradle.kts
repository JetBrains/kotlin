/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

plugins {
    id("benchmarking")
}

kotlin {
    applyDefaultHierarchyTemplate() // due to custom posixMain source set

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
            }
            kotlin.srcDir("src/main/kotlin")
            kotlin.srcDir("../reports/src/main/kotlin/report")
            kotlin.srcDir("../shared/src/main/kotlin")
        }
        nativeMain {
            kotlin.srcDir("src/main/kotlin-native")
            kotlin.srcDir("../shared/src/main/kotlin-native/common")
        }
        mingwMain {
            kotlin.srcDir("../shared/src/main/kotlin-native/mingw")
        }
        val posixMain by creating {
            dependsOn(nativeMain.get())
            kotlin.srcDir("../shared/src/main/kotlin-native/posix")
        }
        linuxMain.get().dependsOn(posixMain)
        appleMain.get().dependsOn(posixMain)
    }
}

benchmark {
    applicationName = "Logging"
    compilerOpts = listOf("-Xruntime-logs=logging=debug")
}
