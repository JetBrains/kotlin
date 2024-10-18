import kotlinx.kover.gradle.plugin.dsl.MetricType
import kotlinx.validation.build.mavenCentralMetadata
import kotlinx.validation.build.mavenRepositoryPublishing
import kotlinx.validation.build.signPublicationIfKeyPresent
import org.gradle.api.attributes.TestSuiteType.FUNCTIONAL_TEST
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.net.URL

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    id("com.gradle.plugin-publish")
    kotlinx.validation.build.conventions.`java-base`
    signing
    `maven-publish`
    `jvm-test-suite`
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
}

group = "org.jetbrains.kotlinx"
project.findProperty("DeployVersion")?.let {
    version = it
}

sourceSets {
    test {
        java.srcDir("src/test/kotlin")
    }
}

// While gradle testkit supports injection of the plugin classpath it doesn't allow using dependency notation
// to determine the actual runtime classpath for the plugin. It uses isolation, so plugins applied by the build
// script are not visible in the plugin classloader. This means optional dependencies (dependent on applied plugins -
// for example kotlin multiplatform) are not visible even if they are in regular gradle use. This hack will allow
// extending the classpath. It is based upon: https://docs.gradle.org/6.0/userguide/test_kit.html#sub:test-kit-classpath-injection

// Create a configuration to register the dependencies against
val testPluginRuntimeConfiguration = configurations.create("testPluginRuntime") {
    isCanBeConsumed = false
    isCanBeResolved = true
    isVisible = false
}

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
        file(outputDir.resolve("plugin-classpath.txt"))
            .writeText(testPluginRuntimeConfiguration.joinToString("\n"))
    }
}

dependencies {
    implementation(gradleApi())
    implementation(libs.kotlinx.metadata)
    compileOnly(libs.kotlin.compiler.embeddable)
    implementation(libs.ow2.asm)
    implementation(libs.ow2.asmTree)
    implementation(libs.javaDiffUtils)
    compileOnly(libs.gradlePlugin.kotlin)

    // Android support is not yet implemented https://github.com/Kotlin/binary-compatibility-validator/issues/94
    //compileOnly(libs.gradlePlugin.android)

    // The test needs the full kotlin multiplatform plugin loaded as it has no visibility of previously loaded plugins,
    // unlike the regular way gradle loads plugins.
    testPluginRuntimeConfiguration(libs.gradlePlugin.android)
    testPluginRuntimeConfiguration(libs.gradlePlugin.kotlin)
}

tasks.compileKotlin {
    compilerOptions {
        allWarningsAsErrors.set(true)
        @Suppress("DEPRECATION") // Compatibility with Gradle 7 requires Kotlin 1.4
        languageVersion.set(KotlinVersion.KOTLIN_1_4)
        apiVersion.set(languageVersion)
        jvmTarget.set(JvmTarget.JVM_1_8)
        // Suppressing "w: Language version 1.4 is deprecated and its support will be removed" message
        // because LV=1.4 in practice is mandatory as it is a default language version in Gradle 7.0+ for users' kts scripts.
        freeCompilerArgs.addAll(
            "-Xexplicit-api=strict",
            "-Xsuppress-version-warnings",
            "-Xopt-in=kotlin.RequiresOptIn"
        )
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

kotlin {
    jvmToolchain(8)
}

tasks.compileTestKotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_1_9)
        freeCompilerArgs.add("-Xjvm-default=all-compatibility")
    }
}

tasks.withType<Test>().configureEach {
    systemProperty("overwrite.output", System.getProperty("overwrite.output", "false"))
    systemProperty("testCasesClassesDirs", sourceSets.test.get().output.classesDirs.asPath)
    systemProperty("kover.enabled", project.findProperty("kover.enabled")?.toString().toBoolean())
    jvmArgs("-ea")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }

        mavenRepositoryPublishing(project)
        mavenCentralMetadata()
    }

    publications.withType<MavenPublication>().all {
        signPublicationIfKeyPresent(this)
    }

    tasks.withType<PublishToMavenRepository>().configureEach {
        dependsOn(tasks.withType<Sign>())
    }
    // a publication will be created automatically by com.gradle.plugin-publish
}

@Suppress("UnstableApiUsage")
gradlePlugin {
    website.set("https://github.com/Kotlin/binary-compatibility-validator")
    vcsUrl.set("https://github.com/Kotlin/binary-compatibility-validator")

    plugins.configureEach {
        tags.addAll("kotlin", "api-management", "binary-compatibility")
    }

    plugins {
        create("binary-compatibility-validator") {
            id = "org.jetbrains.kotlinx.binary-compatibility-validator"
            implementationClass = "kotlinx.validation.BinaryCompatibilityValidatorPlugin"
            displayName = "Binary compatibility validator"
            description =
                "Produces binary API dumps and compares them in order to verify that binary API is preserved"
        }
    }
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite>().configureEach {
            useJUnit()
            dependencies {
                implementation(project())
                implementation(libs.assertJ.core)
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.compiler.embeddable)
            }
        }

        val test by getting(JvmTestSuite::class) {
            description = "Regular unit tests"
        }

        val functionalTest by creating(JvmTestSuite::class) {
            testType.set(FUNCTIONAL_TEST)
            description = "Functional Plugin tests using Gradle TestKit"

            dependencies {
                implementation(files(createClasspathManifest))

                implementation(gradleApi())
                implementation(gradleTestKit())
            }

            targets.configureEach {
                testTask.configure {
                    shouldRunAfter(test)
                }
            }
        }

        gradlePlugin.testSourceSets(functionalTest.sources)

        tasks.check {
            dependsOn(functionalTest)
        }
    }
}

tasks.withType<Sign>().configureEach {
    onlyIf("only sign if signatory is present") { signatory?.keyId != null }
}

kover {
    koverReport {
        filters {
            excludes {
                packages("kotlinx.validation.test")
            }
        }
        verify {
            rule {
                minBound(80, MetricType.BRANCH)
                minBound(90, MetricType.LINE)
            }
        }
    }
    // Unfortunately, we can't test both configuration cache use and the test coverage
    // simultaneously, so the coverage collection should be enabled explicitly (and that
    // will disable configuration cache).
    if (!project.findProperty("kover.enabled")?.toString().toBoolean()) {
        disable()
    }
}


tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        includes.from("Module.md")

        sourceLink {
            localDirectory.set(rootDir)
            remoteUrl.set(URL("https://github.com/Kotlin/binary-compatibility-validator/tree/master"))
            remoteLineSuffix.set("#L")
        }
        samples.from("src/test/kotlin/samples/KlibDumpSamples.kt")
    }
}
