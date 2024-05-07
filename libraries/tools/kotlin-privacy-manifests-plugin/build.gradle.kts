import plugins.configureDefaultPublishing

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
}

group = "org.jetbrains.kotlin"
version = findProperty("privacyManifestsPluginDeployVersion") as String? ?: "test"

standardPublicJars()

dependencies {
    compileOnly(project(":kotlin-gradle-plugin"))
    testApi(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(gradleTestKit())
}

val functionalTest by sourceSets.creating

gradlePlugin {
    plugins {
        create("apple-privacy-manifests") {
            id = "org.jetbrains.kotlin.apple-privacy-manifests"
            displayName = "Apple privacy manifests copying plugin"
            description = "Plugin for copying privacy manifests to Kotlin Multiplatform frameworks"
            implementationClass = "org.jetbrains.kotlin.PrivacyManifestsPlugin"
        }
    }
    testSourceSets(functionalTest)
}

configurations[functionalTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())

val cleanFunctionalTest = tasks.register<Delete>("cleanFunctionalTest") {
    setDelete(layout.buildDirectory.dir("functionalTest"))
}

tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTest.output.classesDirs
    classpath = configurations[functionalTest.runtimeClasspathConfigurationName] + functionalTest.output
    dependsOn(
        tasks.named("publishAllPublicationsToBuildDirectoryRepository"),
        cleanFunctionalTest,
    )
    useJUnitPlatform()
}

configureDefaultPublishing()

publishing {
    repositories {
        maven(layout.buildDirectory.dir("repo")) {
            name = "BuildDirectory"
        }
    }
}