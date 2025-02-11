plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    wasmJs {
        <JsEngine> {
            testTask {
                filter.apply {
                    excludeTest("WasmTest", "testShouldBeExcluded")
                }
            }
        }
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
the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec>().apply {
    // Test that we can set the version and it is a String.
    // But use the default version since update this place every time anyway.
    version.set(version.get())
}

tasks.named<org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask>("kotlinStorePackageLock") {
    //A little hacky way to make yarn results
    inputFile.fileValue(projectDir.resolve("packageLockStub"))
}
