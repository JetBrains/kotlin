@file:Suppress("HasPlatformType")

/**
 * Version of kotlin-gradle-plugin-idea module that should be resolved for compatibility tests
 * This version can be treated as 'minimal guaranteed backwards compatible version' of the module.
 */
val testedVersion = "1.8.20-dev-4242"

val isSnapshotTest = properties.contains("kgp-idea.snapshot_test")
val resolvedTestedVersion = if (isSnapshotTest) properties["defaultSnapshotVersion"].toString() else testedVersion

//region Download and prepare classpath for specified tested version

repositories {
    if (isSnapshotTest) {
        mavenLocal()
        mavenCentral()
    }

    maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
    maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
}

val classpathDestination = layout.buildDirectory.dir("classpath")

val incomingClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
}

dependencies {
    incomingClasspath(kotlin("gradle-plugin-idea", resolvedTestedVersion))
    incomingClasspath(testFixtures(kotlin("gradle-plugin-idea", resolvedTestedVersion)))
    incomingClasspath(kotlin("gradle-plugin-idea-proto", resolvedTestedVersion))

    incomingClasspath.resolutionStrategy {
        force(kotlin("stdlib", bootstrapKotlinVersion))
        force(kotlin("test-junit", bootstrapKotlinVersion))
    }
}

val syncClasspath by tasks.register<Sync>("syncClasspath") {
    if (isSnapshotTest) dependsOnKotlinGradlePluginInstall()

    from(incomingClasspath)
    into(classpathDestination)

    val testedVersionLocal = resolvedTestedVersion
    /* Test if the correct version was resolved */
    doLast {
        val expectedJar = destinationDir.resolve("kotlin-gradle-plugin-idea-$testedVersionLocal.jar")
        check(expectedJar.exists()) { "Expected $expectedJar in classpath. Found ${destinationDir.listFiles().orEmpty()}" }
    }
}

val outgoingClasspath by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    outgoing.artifact(classpathDestination) { builtBy(syncClasspath) }
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}

//endregion
