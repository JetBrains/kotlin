plugins {
    kotlin("multiplatform")
    `maven-publish`
    // id("com.android.library") // AGP
}

/* Begin AGP
android {
    compileSdk = 31
    defaultConfig {
        minSdk = 31
    }
    namespace = "org.jetbrains.kotlin.sample"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    flavorDimensions("myFlavor")
    productFlavors {
        create("flavor1") { dimension = "myFlavor" }
    }
}
kotlin {
    androidTarget {
        publishAllLibraryVariants()
        compilations.all {
            // for compatibility between 1.7.21 and 2.0+
            compileTaskProvider.configure {
               compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
            }
        }
    }
}
End AGP */

kotlin {
    // jvm() // JVM

    linuxX64()
    linuxArm64()
}

publishing {
    repositories {
        maven("<localRepo>")
    }
}

version = "1.0"