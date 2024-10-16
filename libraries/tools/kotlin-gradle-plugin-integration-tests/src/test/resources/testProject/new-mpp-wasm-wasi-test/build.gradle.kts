plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {

    wasmWasi {
        nodejs {}
        binaries.executable()
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
    }
}

rootProject.plugins.apply(org.jetbrains.kotlin.gradle.targets.js.d8.D8Plugin::class.java)
rootProject.the<org.jetbrains.kotlin.gradle.targets.js.d8.D8RootExtension>().apply {
    // Test that we can set the version and it is a String.
    // But use the default version since update this place every time anyway.
    version = (version as String)
}

tasks.named<org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask>("kotlinStorePackageLock") {
    //A little hacky way to make yarn results
    inputFile.fileValue(projectDir.resolve("packageLockStub"))
}
