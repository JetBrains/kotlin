/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE
import org.gradle.api.attributes.Bundling.EXTERNAL
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.DOCUMENTATION
import org.gradle.api.attributes.Category.LIBRARY
import org.gradle.api.attributes.DocsType.DOCS_TYPE_ATTRIBUTE
import org.gradle.api.attributes.DocsType.SOURCES
import org.gradle.api.attributes.LibraryElements.JAR
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.Usage.JAVA_RUNTIME
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.attributes.java.TargetJvmEnvironment.STANDARD_JVM
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskInputFilePropertyBuilder
import org.gradle.api.tasks.TaskInputPropertyBuilder
import org.gradle.api.tasks.TaskOutputFilePropertyBuilder
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import javax.inject.Inject

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

    // This must be explicit so the full `java-psi` API takes precedence over
    // stripped copies that may be present in compiler-related artifacts.
    implementation("com.jetbrains.intellij.java:java-psi-impl:$intellijVersion") {
        exclude("org.jetbrains.intellij.deps", "log4j")
    }

    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-standalone"))
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

    // `com.intellij.util.xml.dom.StaxFactory` (used by plugin.xml parsing during Analysis API standalone
    // session setup) needs an actual StAX implementation on the runtime classpath, or its static initializer
    // fails with `NoClassDefFoundError: Could not initialize class com.intellij.util.xml.dom.StaxFactory`.
    // Same pattern as compiler/fir/analysis-tests/build.gradle.kts.
    runtimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    // `com.intellij.util.xml.dom.XmlElement` is `@Serializable` and needs `kotlinx-serialization-core`'s
    // `KSerializer` on the runtime classpath, or its static initializer fails with
    // `NoClassDefFoundError: Could not initialize class com.intellij.util.xml.dom.XmlElement`.
    // Same pattern as native/swift/swift-export-embeddable/build.gradle.kts.
    runtimeOnly(libs.kotlinx.serialization.core)

    testImplementation(kotlinTest("junit5"))
    testImplementation(libs.junit.jupiter.params)
}

tasks.test { maxHeapSize = "4G" }

// TODO use sources directly from
//region Download and unpack the latest kotlin-stdlib JVM sources, needed by tests that verify
// documentation generated for the standard library.
val kotlinStdlibSourcesDir = downloadLatestKotlinStdlibJvmSources(project)
tasks.withType<Test>().configureEach {
    systemProperty.inputDirectory("kotlinStdlibSourcesDir", kotlinStdlibSourcesDir).withPathSensitivity(RELATIVE)
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_1_8))

}
//endregion

//region Custom test targets that run subsets of tests filtered by JUnit tags (K2 symbols-based
// Java analysis vs. PSI-based Java analysis).
val symbolsTestImplementation: Configuration = configurations.create("symbolsTestImplementation") {
    description = "Dependencies for symbols tests (K2)"
    declarable()
}

val symbolsTestImplementationResolver: Configuration = configurations.create("symbolsTestImplementationResolver") {
    description = "Resolve dependencies for symbols tests (K2)"
    resolvable()
    extendsFrom(symbolsTestImplementation)
    attributes { jvmJar(objects) }
}

testing {
    suites {
        named<JvmTestSuite>("test").configure {

            // JUnit tags for Java analysis (PSI vs symbols) are defined with annotations in test classes.
            val onlyJavaPsiTags = listOf("onlyJavaPsi")
            val onlyJavaSymbolsTags = listOf("onlyJavaSymbols")

            // Create a new target for _only_ running test compatible with symbols-analysis (K2).
            val testSymbolsTarget = targets.register("testSymbols") {
                testTask.configure {
                    val excludedTags = onlyJavaSymbolsTags
                    description = "Runs tests using symbols-analysis (K2) (excluding tags: $excludedTags)"
                    useJUnitPlatform {
                        excludeTags.addAll(excludedTags)
                    }
                    // Analysis dependencies from `symbolsTestImplementation` should precede all other dependencies
                    // in order to use the shadowed stdlib from the analysis dependencies
                    classpath = symbolsTestImplementationResolver.incoming.files + classpath
                }
            }

            // Create a new target for running tests with enabled experimental symbols java analysis.
            val testJavaSymbolsTarget = targets.register("testJavaSymbols") {
                testTask.configure {
                    val excludedTags = onlyJavaPsiTags
                    description = "Runs tests using symbols-analysis (K2) for java (excluding tags: $excludedTags)"
                    useJUnitPlatform {
                        excludeTags.addAll(excludedTags)
                    }
                    // Analysis dependencies from `symbolsTestImplementation` should precede all other dependencies
                    // in order to use the shadowed stdlib from the analysis dependencies
                    classpath = symbolsTestImplementationResolver.incoming.files + classpath

                    // Enable experimental symbols java analysis
                    systemProperty("org.jetbrains.dokka.analysis.enableExperimentalSymbolsJavaAnalysis", "true")
                }
            }

            // Run all test targets when running :test;
            // don't run the task itself, as it's just an aggregate for the test targets.
            targets.named("test") {
                testTask.configure {
                    onlyIf { false }
                    dependsOn(testSymbolsTarget.map { it.testTask })
                    dependsOn(testJavaSymbolsTarget.map { it.testTask })
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

private fun AttributeContainer.jvmJar(objects: ObjectFactory) {
    attribute(USAGE_ATTRIBUTE, objects.named(JAVA_RUNTIME))
    attribute(CATEGORY_ATTRIBUTE, objects.named(LIBRARY))
    attribute(BUNDLING_ATTRIBUTE, objects.named(EXTERNAL))
    attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(STANDARD_JVM))
    attribute(LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(JAR))
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

/**
 * Utility for adding a System Property command line arguments to this [Test] task,
 * and correctly registering the values as task inputs (for Gradle up-to-date checks).
 */
// https://github.com/gradle/gradle/issues/11534
// https://github.com/gradle/gradle/issues/12247
private val Test.systemProperty: SystemPropertyAdder
    get() {
        val spa = extensions.findByType<SystemPropertyAdder>()
            ?: extensions.create<SystemPropertyAdder>("SystemPropertyAdder", this)
        return spa
    }

internal abstract class SystemPropertyAdder @Inject internal constructor(
    private val task: Test,
) {
    private val objects: ObjectFactory = task.project.objects

    @JvmName("inputDirectoryProvider")
    fun inputDirectory(
        key: String,
        value: Provider<out Directory>,
    ): TaskInputFilePropertyBuilder {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(key, value) {
                it.get().asFile.invariantSeparatorsPath
            }
        )
        return task.inputs.dir(value)
            .withPropertyName("SystemProperty input directory $key")
    }

    @JvmName("inputDirectoryFile")
    fun inputDirectory(
        key: String,
        value: Provider<File>,
    ): TaskInputFilePropertyBuilder =
        inputDirectory(key, objects.directoryProperty().fileProvider(value))

    fun inputDirectory(
        key: String,
        value: File,
    ): TaskInputFilePropertyBuilder =
        inputDirectory(key, objects.directoryProperty().fileValue(value))

    fun inputDirectory(
        key: String,
        value: Directory,
    ): TaskInputFilePropertyBuilder =
        inputDirectory(key, objects.directoryProperty().apply { set(value) })

    @JvmName("outputDirectoryProvider")
    fun outputDirectory(
        key: String,
        value: Provider<out Directory>,
    ): TaskOutputFilePropertyBuilder {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(key, value) {
                it.get().asFile.invariantSeparatorsPath
            }
        )
        return task.outputs.dir(value)
            .withPropertyName("SystemProperty input directory $key")
    }

    @JvmName("outputDirectoryFile")
    fun outputDirectory(
        key: String,
        value: Provider<File>,
    ): TaskOutputFilePropertyBuilder =
        outputDirectory(key, objects.directoryProperty().fileProvider(value))

    fun outputDirectory(
        key: String,
        value: File,
    ): TaskOutputFilePropertyBuilder =
        outputDirectory(key, objects.directoryProperty().fileValue(value))

    fun outputDirectory(
        key: String,
        value: Directory,
    ): TaskOutputFilePropertyBuilder =
        outputDirectory(key, objects.directoryProperty().apply { set(value) })

    fun inputFile(
        key: String,
        file: RegularFile,
    ): TaskInputFilePropertyBuilder {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(key, file) {
                it.asFile.invariantSeparatorsPath
            }
        )
        return task.inputs.file(file)
            .withPropertyName("SystemProperty input file $key")
    }

    fun inputFile(
        key: String,
        file: Provider<out RegularFile>,
    ): TaskInputFilePropertyBuilder {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(key, file) {
                it.orNull?.asFile?.invariantSeparatorsPath
            }
        )
        return task.inputs.file(file)
            .withPropertyName("SystemProperty input file $key")
    }

    fun inputFiles(
        key: String,
        files: Provider<out FileCollection>,
    ): TaskInputFilePropertyBuilder {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(key, files) { it.orNull?.asPath }
        )
        return task.inputs.files(files)
            .withPropertyName("SystemProperty input files $key")
    }

    fun inputProperty(
        key: String,
        value: Provider<out String>,
    ): TaskInputPropertyBuilder {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(key, value) { it.orNull }
        )
        return task.inputs.property("SystemProperty input property $key", value)
    }

    fun inputProperty(
        key: String,
        value: String,
    ): TaskInputPropertyBuilder {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(key, value) { it }
        )
        return task.inputs.property("SystemProperty input property $key", value)
    }

    @JvmName("inputBooleanProperty")
    fun inputProperty(
        key: String,
        value: Provider<out Boolean>,
    ): TaskInputPropertyBuilder = inputProperty(key, value.map { it.toString() })

    /**
     * Add a System Property (in the format `-D$key=$value`).
     *
     * [value] will be treated as if it were annotated with [org.gradle.api.tasks.Internal]
     * and will _not_ be registered as a Gradle [org.gradle.api.Task] input.
     */
    fun internalProperty(
        key: String,
        value: Provider<out String>,
    ) {
        task.jvmArgumentProviders.add(
            SystemPropertyArgumentProvider(
                key = key,
                value = value,
                transformer = { it.orNull },
            )
        )
    }
}

/**
 * Provide a Java system property.
 *
 * [value] is not registered as a Gradle Task input.
 * The value must be registered as a task input, using the [SystemPropertyAdder] utils.
 */
private class SystemPropertyArgumentProvider<T : Any>(
    @get:Input
    val key: String,
    private val value: T,
    private val transformer: (value: T) -> String?,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> {
        val value = transformer(value) ?: return emptyList()
        return listOf("-D$key=$value")
    }
}

//endregion
