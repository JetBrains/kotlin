plugins {
    kotlin("multiplatform").apply(false)
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().nodeVersion = "20.2.0"

    tasks.named<org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask>("kotlinStorePackageLock") {
        //A little hacky way to make yarn results
        inputFile.fileValue(projectDir.resolve("packageLockStub"))
    }
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.d8.D8Plugin> {
    // Test that we can set the version and it is a String.
    // But use the default version since update this place every time anyway.
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.d8.D8Extension>().version = (version as String)
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
