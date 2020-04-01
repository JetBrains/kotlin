plugins {
    kotlin("js") version "<pluginMarkerVersion>"
}

repositories {
    jcenter()
    mavenLocal()
    mavenCentral()
}

kotlin.js {
    binaries.executable()
    nodejs()
}

rootProject.tasks
    .withType(org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask::class.java)
    .named("kotlinNpmInstall")
    .configure {
        args.addAll(
            listOf(
                "--network-concurrency",
                "1",
                "--mutex",
                "network"
            )
        )
    }

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("com.example:lib2")
    implementation(npm("async", "3.2.0"))
    testImplementation(kotlin("test-js"))
}