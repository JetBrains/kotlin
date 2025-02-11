plugins {
    kotlin("multiplatform").apply(false)
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    tasks.named<org.jetbrains.kotlin.gradle.targets.js.npm.LockCopyTask>("kotlinStorePackageLock") {
        //A little hacky way to make yarn results
        inputFile.fileValue(projectDir.resolve("packageLockStub"))
    }
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
