import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = embeddedKotlinVersion
    coreLibrariesVersion = embeddedKotlinVersion
    jvmToolchain(17)

    compilerOptions {
        allWarningsAsErrors = true
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

gradlePlugin {
    plugins {
        register("kotlin-build-publishing") {
            id = "kotlin-build-publishing"
            implementationClass = "plugins.KotlinBuildPublishingPlugin"
        }
    }
}

dependencies {
    api(project(":utilities"))
    implementation(kotlinBuildHelpers())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
    implementation(libs.gradle.pluginPublish.gradlePlugin)

    // Shadow plugin has some interaction with spdx plugin leading to:
    // java.lang.ExceptionInInitializerError: No XmlService implementation found
    // as a workaround we provide maven-xml-impl to the classpath ourselves
    runtimeOnly(libs.maven.xml.impl)
    implementation(libs.spdx.gradlePlugin)
    implementation(libs.shadow.gradlePlugin)

    // Bump a transitive slf4j version to a version in verification-metadata.xml
    implementation("org.slf4j:slf4j-api:2.0.17")

    compileOnly(gradleApi())

    testImplementation(kotlin("test"))
    testImplementation(gradleKotlinDsl())
    testImplementation(gradleApi())
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.dokka.gradlePlugin)
    testImplementation(project(":gradle-plugins-documentation"))
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.jupiter.engine)

    constraints {
        api(libs.apache.commons.lang)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.register("checkBuild") {
    dependsOn("test")
}

// Tests are using Dokka v1 which triggers deprecation warnings
tasks.named<KotlinJvmCompile>("compileTestKotlin").configure {
    compilerOptions.allWarningsAsErrors.set(false)
}

listOf(
    org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME + "Main",
    org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME + "Test",
    "compilePluginsBlocksPluginClasspathElements",
).forEach { confName ->
    project.configurations.named(confName) {
        resolutionStrategy {
            eachDependency {
                if (this.requested.group == "org.jetbrains.kotlin") useVersion(embeddedKotlinVersion)
            }
        }
    }
}
