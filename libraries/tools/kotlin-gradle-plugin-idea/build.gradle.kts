plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

object BackwardsCompatibilityTestConfiguration {
    const val minimalBackwardsCompatibleVersion = "1.7.0-dev-2018"
}

kotlin.sourceSets.configureEach {
    languageSettings.apiVersion = "1.4"
    languageSettings.languageVersion = "1.4"
    languageSettings.optIn("org.jetbrains.kotlin.gradle.kpm.idea.InternalKotlinGradlePluginApi")
}

dependencies {
    implementation(kotlinStdlib())
    testImplementation(gradleApi())
    testImplementation(gradleKotlinDsl())
    testImplementation(project(":kotlin-gradle-plugin"))
    testImplementation(project(":kotlin-test:kotlin-test-junit"))

    testImplementation("org.reflections:reflections:0.10.2") {
        because("Tests on the object graph are performed. This library will find implementations of interfaces at runtime")
    }

    testFixturesImplementation(gradleApi())
    testFixturesImplementation(gradleKotlinDsl())
    testFixturesImplementation(project(":kotlin-test:kotlin-test-junit"))
}

publish()
javadocJar()
sourcesJar()

/* Setup Backwards Compatibility Test */
run {
    /* When -Pkgp-idea-snapshot is set, then the test runs against a locally installed snapshot version (./gradlew install) */
    val isSnapshotTest = project.providers.gradleProperty("kotlin-gradle-plugin-idea.snapshot").isPresent

    repositories {
        if (isSnapshotTest) mavenLocal()
    }

    val version = if (isSnapshotTest) properties["defaultSnapshotVersion"].toString()
    else BackwardsCompatibilityTestConfiguration.minimalBackwardsCompatibleVersion

    val minimalBackwardsCompatibleVersionTestClasspath by configurations.creating {
        isCanBeResolved = true
        isCanBeConsumed = false
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        }
    }

    dependencies {
        minimalBackwardsCompatibleVersionTestClasspath("org.jetbrains.kotlin:kotlin-gradle-plugin-idea:$version")
    }

    tasks.test {
        inputs.files(minimalBackwardsCompatibleVersionTestClasspath)
        doFirst {
            if (isSnapshotTest) logger.quiet("Running test against snapshot: $version")
            else logger.quiet("Running test against $version")

            val resolvedClasspath = minimalBackwardsCompatibleVersionTestClasspath.files
            if (resolvedClasspath.none { "kotlin-gradle-plugin-idea-$version.jar" in it.path }) {
                throw IllegalStateException(buildString {
                    appendLine("Bad backwardsCompatibilityClasspath: $resolvedClasspath")
                    appendLine("Dependencies:${minimalBackwardsCompatibleVersionTestClasspath.allDependencies.joinToString()}")
                })
            }

            systemProperty(
                "backwardsCompatibilityClasspath",
                resolvedClasspath.joinToString(";") { it.absolutePath }
            )
        }
    }
}
