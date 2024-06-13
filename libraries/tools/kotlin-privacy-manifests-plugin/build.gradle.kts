import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
}

if (kotlinBuildProperties.isApplePrivacyManifestsPluginEnabled) {
    apply(plugin = "com.gradle.plugin-publish")
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
    dependsOnKotlinGradlePluginInstall()
    systemProperty("kotlinVersion", rootProject.extra["kotlinVersion"] as String)
    useJUnitPlatform()
}

if (kotlinBuildProperties.isApplePrivacyManifestsPluginEnabled) {
    gradlePlugin {
        website.set("https://kotlinlang.org/")
        vcsUrl.set("https://github.com/jetbrains/kotlin")
        plugins.configureEach {
            tags.add("kotlin")
        }
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

    configureDefaultPublishing()

    publishing {
        repositories {
            maven(layout.buildDirectory.dir("repo")) {
                name = "BuildDirectory"
            }
        }

        publications.withType(MavenPublication::class.java).all {
            configureKotlinPomAttributes(project)
        }

        publications {
            withType<MavenPublication>().configureEach {
                if (name.endsWith("PluginMarkerMaven")) {
                    pom {
                        // https://github.com/gradle/gradle/issues/8754
                        // and https://github.com/gradle/gradle/issues/6155
                        packaging = "pom"
                    }
                }
            }
        }
    }
}