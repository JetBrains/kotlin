plugins {
    kotlin("jvm")
}

dependencies {
    implementation("io.ktor:ktor-client-cio:1.4.0")
    implementation("com.google.code.gson:gson:${rootProject.extra["versions.jar.gson"]}")
    implementation("org.apache.velocity:velocity:1.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC")
}

val generateNpmVersions by generator(
    "org.jetbrains.kotlin.generators.gradle.targets.js.MainKt",
    sourceSets["main"]
)

listOf(generateNpmVersions).forEach {
    it.systemProperty(
        "org.jetbrains.kotlin.generators.gradle.targets.js.outputSourceRoot",
        project(":kotlin-gradle-plugin").projectDir.resolve("src/main/kotlin").absolutePath
    )
}