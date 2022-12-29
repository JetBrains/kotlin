plugins {
    kotlin("multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    mavenCentral()
}

with(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.apply(rootProject)) {
    //nightly nodejs that supports wasm M5
    nodeVersion = "19.0.0-nightly202206017ad5b420ae"
    nodeDownloadBaseUrl = "https://nodejs.org/download/nightly/"
}

with(org.jetbrains.kotlin.gradle.targets.js.d8.D8RootPlugin.apply(rootProject)) {
    // Test that we can set the version and it is a String.
    // But use the default version since update this place every time anyway.
    version = (version as String)
}

with(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin.apply(rootProject)) {
    //A little hacky way to disable yarn for unsupported nightly node version
    command = "echo"
    download = false
}

tasks.named<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockCopyTask>("kotlinStoreYarnLock") {
    //A little hacky way to make yarn results
    inputFile.fileValue(projectDir.resolve("yarnLockStub"))
}

kotlin {
    wasm {
        <JsEngine>()
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
