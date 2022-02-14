plugins {
    kotlin("jvm")
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
    /* Setup for backwards compatibility test */
    val minimalBackwardsCompatibleVersion = "1.7.0-dev-1825"
    val minimalBackwardsCompatibleVersionTestClasspath by configurations.creating
    dependencies {
        minimalBackwardsCompatibleVersionTestClasspath("org.jetbrains.kotlin:kotlin-gradle-plugin-idea:$minimalBackwardsCompatibleVersion")
    }

    dependsOn(minimalBackwardsCompatibleVersionTestClasspath)

    doFirst {
        systemProperty(
            "backwardsCompatibilityClasspath",
            minimalBackwardsCompatibleVersionTestClasspath.files.joinToString(";") { it.absolutePath }
        )
    }
}
