import org.jetbrains.kotlin.benchmarkingTargets

plugins {
    id("custom-kotlin-native-home")
    kotlin("multiplatform")
}

kotlin {
    benchmarkingTargets()

    applyDefaultHierarchyTemplate() // due to custom posixMain source set

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
            }
            kotlin.srcDir("src/main/kotlin")
            kotlin.srcDir("../reports/src/main/kotlin/report")
        }
        nativeMain {
            kotlin.srcDir("src/main/kotlin-native/common")
        }
        mingwMain {
            kotlin.srcDir("src/main/kotlin-native/mingw")
        }
        val posixMain by creating {
            dependsOn(nativeMain.get())
            kotlin.srcDir("src/main/kotlin-native/posix")
        }
        linuxMain.get().dependsOn(posixMain)
        appleMain.get().dependsOn(posixMain)
    }
}