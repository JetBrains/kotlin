plugins {
    kotlin("jvm")
}

object BackwardsCompatibilityTestConfiguration {
    const val minimalBackwardsCompatibleVersion = "1.7.0-dev-1868"
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
}

publish()

tasks.test {
    /* When -Psnapshot is set, then the test runs against a locally installed snapshot version (./gradlew install) */
    val isSnapshotTest = project.providers.gradleProperty("snapshot").isPresent

    repositories {
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        if (isSnapshotTest) mavenLocal()
    }

    /* Setup for backwards compatibility test */
    val version = if (isSnapshotTest) properties["defaultSnapshotVersion"].toString()
    else BackwardsCompatibilityTestConfiguration.minimalBackwardsCompatibleVersion

    val minimalBackwardsCompatibleVersionTestClasspath by configurations.creating

    dependencies {
        minimalBackwardsCompatibleVersionTestClasspath("org.jetbrains.kotlin:kotlin-gradle-plugin-idea:$version")
    }

    dependsOn(minimalBackwardsCompatibleVersionTestClasspath)

    doFirst {
        systemProperty(
            "backwardsCompatibilityClasspath",
            minimalBackwardsCompatibleVersionTestClasspath.files.joinToString(";") { it.absolutePath }
        )
    }
}
