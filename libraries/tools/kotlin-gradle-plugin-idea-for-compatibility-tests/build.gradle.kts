@file:Suppress("HasPlatformType")

/**
 * Version of kotlin-gradle-plugin-idea module that should be resolved for compatibility tests
 * This version can be treated as 'minimal guaranteed backwards compatible version' of the module.
 */
val testedVersion = "1.7.0-dev-2723"

//region Download and prepare classpath for specified tested version

val classpathDestination = layout.buildDirectory.dir("classpath")

val incomingClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    attributes.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
}

dependencies {
    incomingClasspath(kotlin("gradle-plugin-idea", testedVersion))
}

val syncClasspath by tasks.register<Sync>("syncClasspath") {
    from(incomingClasspath)
    into(classpathDestination)

    val testedVersionLocal = testedVersion
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
    delete(project.buildDir)
}

//endregion
