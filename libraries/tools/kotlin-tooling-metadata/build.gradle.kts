plugins {
    java
    kotlin("jvm")
    id("test-inputs-check")
}

publish()
sourcesJar()
javadocJar()
configureKotlinCompileTasksGradleCompatibility()

val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()

// Apply the serialization compiler plugin pinned to the same Kotlin version used for
// Gradle-compatible compilation (see configureKotlinCompileTasksGradleCompatibility).
// We cannot use kotlin("plugin.serialization") because it would pick up the current
// dev-version compiler plugin which is incompatible with the older language version
// used for Gradle compatibility compilation.
dependencies.add(
    "kotlinCompilerPluginClasspathMain",
    "org.jetbrains.kotlin:kotlin-serialization-compiler-plugin-embeddable:${coreDepsVersion}"
)

dependencies {
    implementation(kotlin("stdlib", coreDepsVersion))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(kotlin("test-junit", coreDepsVersion))
}
