import plugins.KotlinBuildPublishingPlugin.Companion.DEFAULT_MAIN_PUBLICATION_NAME
import plugins.signLibraryPublication

description = "kotlin-gradle-statistics"

plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("jps-compatible")
    `maven-publish`
}

configureKotlinCompileTasksGradleCompatibility()
configureCommonPublicationSettingsForGradle(signLibraryPublication)
extensions.extraProperties["kotlin.stdlib.default.dependency"] = "false"

dependencies {
    compileOnly(kotlinStdlib())

    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(libs.junit4)
}

projectTest {
    workingDir = rootDir
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
