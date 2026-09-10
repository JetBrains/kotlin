/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.DOCUMENTATION
import org.gradle.api.attributes.DocsType.DOCS_TYPE_ATTRIBUTE
import org.gradle.api.attributes.DocsType.SOURCES
import org.gradle.api.attributes.Usage.JAVA_RUNTIME
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    `java-test-fixtures`
    id("test-inputs-check")
}

kotlin {
    explicitApi()
    compilerOptions {
        optIn.addAll(
            "kotlin.RequiresOptIn",
            "org.jetbrains.dokka.InternalDokkaApi",
        )
    }
}

// This module was written against a stable Kotlin release and relies on plain component1()/component2()
// destructuring semantics. Override the repo-wide "-Xname-based-destructuring=complete" (set in
// common-configuration.gradle.kts) back down to "only-syntax" so short-form destructuring by variable
// name doesn't change behavior here. Registered after the `plugins {}` block applies common-configuration,
// so this action runs after (and therefore overrides) the one added there.
//
// It also disables "ErrorAboutDataClassCopyVisibilityChange", another language-version-2.5 deprecation-phase-2
// diagnostic that turns pre-existing data classes with non-public constructors (copied as-is from this
// module's original codebase) from a warning into a compile error.
//
// Finally, this module's vendored source carries pre-existing warnings (deprecated stdlib calls, etc.)
// that predate this migration and are out of scope to fix here, so all-warnings-as-errors is turned off
// for this module only (common-configuration otherwise enables it repo-wide).
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xname-based-destructuring=only-syntax")
        freeCompilerArgs.add("-XXLanguage:-ErrorAboutDataClassCopyVisibilityChange")
        allWarningsAsErrors.set(false)
    }
}

val intellijVersion = kotlinBuildProperties.versionsProperty("intellijSdk").get()

dependencies {
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jsoup)
    implementation(libs.jetbrains.markdown)

    runtimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    runtimeOnly(libs.intellij.fastutil)
    runtimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    runtimeOnly(commonDependency("one.util:streamex"))
    runtimeOnly("com.jetbrains.intellij.platform:util-jdom:$intellijVersion") { isTransitive = false }
    runtimeOnly(libs.kotlinx.serialization.core) { isTransitive = false }

    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-standalone"))
    runtimeOnly(project(":analysis:analysis-api-platform-interface"))
    runtimeOnly(project(":analysis:analysis-api-fir"))
    runtimeOnly(project(":analysis:low-level-api-fir"))
    runtimeOnly(project(":analysis:symbol-light-classes"))
    runtimeOnly(project(":analysis:analysis-api-standalone:analysis-api-standalone-fir"))
    runtimeOnly(project(":analysis:analysis-api-impl-base"))

    implementation(project(":compiler:cli-base"))
    implementation(project(":core:language.targets.jvm"))
    implementation(project(":js:js.config"))
    implementation(project(":native:native.config"))
    implementation(project(":wasm:wasm.config"))

    testFixturesImplementation(kotlinStdlib())
    testImplementation(kotlinTest("junit5"))
    testImplementation(libs.junit.jupiter.params)
}

// TODO use sources directly
//region Download and unpack the latest kotlin-stdlib JVM sources, needed by tests that verify
// documentation generated for the standard library.
val kotlinStdlibSourcesDir = downloadLatestKotlinStdlibJvmSources(project)
tasks.withType<Test>().configureEach {
    addDirectoryProperty(kotlinStdlibSourcesDir.get(), "kotlinStdlibSourcesDir")
}
//endregion


projectTests {
    // Test code reads these resources directly by file path (not via the classpath), so they must be
    // declared explicitly or `test-inputs-check` flags them as undeclared inputs.
    testData(project.isolated, "src/test/resources")

    testing {
        suites {
            named<JvmTestSuite>("test").configure {

                // JUnit tags for Java analysis (PSI vs symbols) are defined with annotations in test classes.
                val onlyJavaPsiTags = listOf("onlyJavaPsi")
                val onlyJavaSymbolsTags = listOf("onlyJavaSymbols")

                // Create a new target for _only_ running test compatible with symbols-analysis (K2).
                val testSymbolsTarget = targets.register("testSymbols") {

                    testTask(
                        taskName = testTask.name,
                        javaLauncher = JdkMajorVersion.JDK_1_8,
                        maxHeapSize = testMaxHeapSizeLarge,
                        skipInLocalBuild = false,
                    ) {
                        val excludedTags = onlyJavaSymbolsTags
                        description = "Runs tests using symbols-analysis (K2) (excluding tags: $excludedTags)"
                        useJUnitPlatform {
                            excludeTags.addAll(excludedTags)
                        }
                    }
                }


                // Create a new target for running tests with enabled experimental symbols java analysis.
                val testJavaSymbolsTarget = targets.register("testJavaSymbols") {
                    testTask(
                        taskName = testTask.name,
                        javaLauncher = JdkMajorVersion.JDK_1_8,
                        maxHeapSize = testMaxHeapSizeLarge,
                        skipInLocalBuild = false,
                    ) {
                        val excludedTags = onlyJavaPsiTags
                        description = "Runs tests using symbols-analysis (K2) for java (excluding tags: $excludedTags)"
                        useJUnitPlatform {
                            excludeTags.addAll(excludedTags)
                        }
                        // Enable experimental symbols java analysis
                        systemProperty("org.jetbrains.dokka.analysis.enableExperimentalSymbolsJavaAnalysis", "true")
                    }
                }

                // Run all test targets when running :test;
                // don't run the task itself, as it's just an aggregate for the test targets.
                targets.named("test") {
                    projectTests {
                        testTask(
                            taskName = testTask.name,
                            javaLauncher = JdkMajorVersion.JDK_1_8,
                            skipInLocalBuild = false,
                        ) {
                            onlyIf { false }
                            dependsOn(testSymbolsTarget.map { it.testTask })
                            dependsOn(testJavaSymbolsTarget.map { it.testTask })
                        }
                    }
                }
            }
        }
    }
}
//endregion

//region Inlined build-logic utilities (previously provided by the standalone build's build-logic).

@Suppress("DEPRECATION")
private fun Configuration.declarable(visible: Boolean = false) {
    isCanBeResolved = false
    isCanBeConsumed = false
    isCanBeDeclared = true
    isVisible = visible
}

@Suppress("DEPRECATION")
private fun Configuration.resolvable(visible: Boolean = false) {
    isCanBeResolved = true
    isCanBeConsumed = false
    isCanBeDeclared = false
    isVisible = visible
}

/**
 * Download and unpack the latest Kotlin stdlib JVM source code.
 *
 * @returns the directory containing the unpacked sources.
 */
private fun downloadLatestKotlinStdlibJvmSources(project: Project): Provider<File> {
    val kotlinStdlibJvmSources: Configuration = project.configurations.create("kotlinStdlibJvmSources") {
        description = "kotlin-stdlib JVM source code."
        declarable()
        defaultDependencies {
            add(project.dependencies.create(project.kotlinStdlib()))
        }
    }

    val kotlinStdlibJvmSourcesResolver: Configuration = project.configurations.create("kotlinStdlibJvmSourcesResolver") {
        description = "Resolver for ${kotlinStdlibJvmSources.name}."
        resolvable()
        isTransitive = false
        extendsFrom(kotlinStdlibJvmSources)
        attributes {
            attribute(USAGE_ATTRIBUTE, project.objects.named(JAVA_RUNTIME))
            attribute(CATEGORY_ATTRIBUTE, project.objects.named(DOCUMENTATION))
            attribute(DOCS_TYPE_ATTRIBUTE, project.objects.named(SOURCES))
        }
    }

    val downloadKotlinStdlibSources = project.tasks.register<Sync>("downloadKotlinStdlibSources") {
        description = "Download and unpacks kotlin-stdlib JVM source code."
        val archives = project.serviceOf<ArchiveOperations>()
        val unpackedJvmSources = kotlinStdlibJvmSourcesResolver.incoming.artifacts.resolvedArtifacts.map { artifacts ->
            artifacts.map {
                archives.zipTree(it.file)
            }
        }
        from(unpackedJvmSources)
        into(temporaryDir)
    }

    return downloadKotlinStdlibSources.map { it.destinationDir }
}
