import com.gradle.publish.*
import kotlinx.validation.build.*
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") apply false
    signing
    `maven-publish`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

sourceSets {
    test {
        java.srcDir("src/test/kotlin")
    }
}

sourceSets {
    create("functionalTest") {
        withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
        }
        compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath
        runtimeClasspath += output + compileClasspath
    }
}

tasks.register<Test>("functionalTest") {
    testClassesDirs = sourceSets["functionalTest"].output.classesDirs
    classpath = sourceSets["functionalTest"].runtimeClasspath
}
tasks.check { dependsOn(tasks["functionalTest"]) }

// While gradle testkit supports injection of the plugin classpath it doesn't allow using dependency notation
// to determine the actual runtime classpath for the plugin. It uses isolation, so plugins applied by the build
// script are not visible in the plugin classloader. This means optional dependencies (dependent on applied plugins -
// for example kotlin multiplatform) are not visible even if they are in regular gradle use. This hack will allow
// extending the classpath. It is based upon: https://docs.gradle.org/6.0/userguide/test_kit.html#sub:test-kit-classpath-injection

// Create a configuration to register the dependencies against
val testPluginRuntimeConfiguration = configurations.register("testPluginRuntime")

// The task that will create a file that stores the classpath needed for the plugin to have additional runtime dependencies
// This file is then used in to tell TestKit which classpath to use.
val createClasspathManifest = tasks.register("createClasspathManifest") {
    val outputDir = buildDir.resolve("cpManifests")
    inputs.files(testPluginRuntimeConfiguration)
        .withPropertyName("runtimeClasspath")
        .withNormalizer(ClasspathNormalizer::class)

    outputs.dir(outputDir)
        .withPropertyName("outputDir")

    doLast {
        outputDir.mkdirs()
        file(outputDir.resolve("plugin-classpath.txt")).writeText(testPluginRuntimeConfiguration.get().joinToString("\n"))
    }
}

val kotlinVersion: String by project

configurations.implementation {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.5.0")
    implementation("org.ow2.asm:asm:9.2")
    implementation("org.ow2.asm:asm-tree:9.2")
    implementation("com.googlecode.java-diff-utils:diffutils:1.3.0")
    compileOnly("org.jetbrains.kotlin.multiplatform:org.jetbrains.kotlin.multiplatform.gradle.plugin:1.6.0")

    // The test needs the full kotlin multiplatform plugin loaded as it has no visibility of previously loaded plugins,
    // unlike the regular way gradle loads plugins.
    add(testPluginRuntimeConfiguration.name, "org.jetbrains.kotlin.multiplatform:org.jetbrains.kotlin.multiplatform.gradle.plugin:$kotlinVersion")

    testImplementation(kotlin("test-junit"))
    "functionalTestImplementation"(files(createClasspathManifest))

    "functionalTestImplementation"("org.assertj:assertj-core:3.18.1")
    "functionalTestImplementation"(gradleTestKit())
    "functionalTestImplementation"(kotlin("test-junit"))
}

tasks.compileKotlin {
    kotlinOptions.apply {
        languageVersion = "1.4"
        apiVersion = "1.4"
        jvmTarget = "1.8"
        // TODO revert that when updating Kotlin. This flag also affects kts files and prevents
        // the project from build due to "w: Language version 1.4 is deprecated and its support will be removed"
//        allWarningsAsErrors = true
        // Suppress the warning about kotlin-reflect 1.3 and kotlin-stdlib 1.4 in the classpath.
        // It's incorrect in this case because we're limiting API version to 1.3 anyway.
        freeCompilerArgs += "-Xskip-runtime-version-check"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    compileTestKotlin {
        kotlinOptions {
            languageVersion = "1.6"
        }
    }
    test {
        systemProperty("overwrite.output", System.getProperty("overwrite.output", "false"))
        systemProperty("testCasesClassesDirs", sourceSets.test.get().output.classesDirs.asPath)
        jvmArgs("-ea")
    }
}

properties["DeployVersion"]?.let { version = it }

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            mavenCentralMetadata()
            mavenCentralArtifacts(project, project.sourceSets.main.get().allSource)
        }

        mavenRepositoryPublishing(project)
        mavenCentralMetadata()
    }

    publications.withType(MavenPublication::class).all {
        signPublicationIfKeyPresent(this)
    }
}

apply(plugin = "org.gradle.java-gradle-plugin")
apply(plugin = "com.gradle.plugin-publish")

extensions.getByType(PluginBundleExtension::class).apply {
    website = "https://github.com/Kotlin/binary-compatibility-validator"
    vcsUrl = "https://github.com/Kotlin/binary-compatibility-validator"
    tags = listOf("kotlin", "api-management", "binary-compatibility")
}

gradlePlugin {
    testSourceSets(sourceSets["functionalTest"])

    plugins {
        create("binary-compatibility-validator") {
            id = "org.jetbrains.kotlinx.binary-compatibility-validator"
            implementationClass = "kotlinx.validation.BinaryCompatibilityValidatorPlugin"
            displayName = "Binary compatibility validator"
            description = "Produces binary API dumps and compares them in order to verify that binary API is preserved"
        }
    }
}
