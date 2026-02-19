import plugins.KotlinBuildPublishingPlugin.Companion.DEFAULT_MAIN_PUBLICATION_NAME
import plugins.signLibraryPublication

description = "kotlin-gradle-statistics"

plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("project-tests-convention")
    `maven-publish`
}

configureKotlinCompileTasksGradleCompatibility()
configureCommonPublicationSettingsForGradle(signLibraryPublication)
extensions.extraProperties["kotlin.stdlib.default.dependency"] = "false"

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly(kotlin("stdlib", coreDepsVersion))

    testImplementation(libs.junit4)
    testImplementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(kotlin("test", coreDepsVersion))
    testRuntimeOnly(kotlin("test-junit", coreDepsVersion))
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit4) {
        workingDir = rootDir
    }
}

publishing {
    publications {
        register<MavenPublication>(DEFAULT_MAIN_PUBLICATION_NAME) {
            from(components["java"])
        }
    }
}
sourcesJar()
javadocJar()
