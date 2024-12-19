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

rootProject.plugins.apply(org.jetbrains.kotlin.gradle.targets.wasm.d8.D8Plugin::class.java)

tasks.named<org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask>("kotlinStorePackageLock") {
    //A little hacky way to make yarn results
    inputFile.fileValue(projectDir.resolve("packageLockStub"))
}
