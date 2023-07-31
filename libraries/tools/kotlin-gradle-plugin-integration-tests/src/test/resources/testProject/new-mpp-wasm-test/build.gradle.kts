plugins {
    kotlin("multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    mavenCentral()
}

with(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.apply(rootProject)) {
    nodeVersion = "20.2.0"
}

with(org.jetbrains.kotlin.gradle.targets.js.d8.D8RootPlugin.apply(rootProject)) {
    // Test that we can set the version and it is a String.
    // But use the default version since update this place every time anyway.
    version = (version as String)
}

tasks.named<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockCopyTask>("kotlinStoreYarnLock") {
    //A little hacky way to make yarn results
    inputFile.fileValue(projectDir.resolve("yarnLockStub"))
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
        <ApplyBinaryen>
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
    }
}
