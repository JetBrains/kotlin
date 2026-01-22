import org.gradle.api.internal.file.collections.DefaultConfigurableFileCollection
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.config.MavenComparableVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

// Contains common configuration that should be applied to all projects

// Common Group and version
val kotlinVersion: String by rootProject.extra
group = "org.jetbrains.kotlin"
version = kotlinVersion

project.configureJvmDefaultToolchain()
project.addEmbeddedConfigurations()
project.addImplicitDependenciesConfiguration()
project.configureJavaCompile()
project.configureJavaBasePlugin()
project.configureKotlinCompilationOptions()
project.configureArtifacts()
project.configureTests()

// There are problems with common build dir:
//  - some tests (in particular js and binary-compatibility-validator depend on the fixed (default) location
//  - idea seems unable to exclude common buildDir from indexing
// therefore it is disabled by default
// buildDir = File(commonBuildDir, project.name)

fun Project.addImplicitDependenciesConfiguration() {
    configurations.maybeCreate("implicitDependencies").apply {
        isCanBeConsumed = false
        isCanBeResolved = false
    }

    if (kotlinBuildProperties.isInIdeaSync.get()) {
        afterEvaluate {
            // IDEA manages to download dependencies from `implicitDependencies`, even if it is created with `isCanBeResolved = false`
            // Clear `implicitDependencies` to avoid downloading unnecessary dependencies during import
            configurations.implicitDependencies.get().dependencies.clear()
        }
    }
}

fun Project.addEmbeddedConfigurations() {
    configurations.maybeCreate("embedded").apply {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        }
    }

    configurations.maybeCreate("embeddedElements").apply {
        extendsFrom(configurations["embedded"])
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named("embedded-java-runtime"))
        }
    }
}

fun Project.configureJavaCompile() {
    plugins.withType<JavaPlugin> {
        tasks.withType<JavaCompile>().configureEach {
            options.compilerArgs.add("-Xlint:deprecation")
            options.compilerArgs.add("-Xlint:unchecked")
            if (!kotlinBuildProperties.disableWerror) {
                options.compilerArgs.add("-Werror")
            }
        }
    }
}

fun Project.configureJavaBasePlugin() {
    plugins.withId("java-base") {
        fun File.toProjectRootRelativePathOrSelf() = (relativeToOrNull(rootDir)?.takeUnless { it.startsWith("..") } ?: this).path

        fun FileCollection.printClassPath(role: String) =
            println("${project.path} $role classpath:\n  ${joinToString("\n  ") { it.toProjectRootRelativePathOrSelf() }}")

        val javaExtension = javaPluginExtension()
        tasks {
            register("printCompileClasspath") { doFirst { javaExtension.sourceSets["main"].compileClasspath.printClassPath("compile") } }
            register("printRuntimeClasspath") { doFirst { javaExtension.sourceSets["main"].runtimeClasspath.printClassPath("runtime") } }
            register("printTestCompileClasspath") { doFirst { javaExtension.sourceSets["test"].compileClasspath.printClassPath("test compile") } }
            register("printTestRuntimeClasspath") { doFirst { javaExtension.sourceSets["test"].runtimeClasspath.printClassPath("test runtime") } }
        }
    }
}

val projectsUsedInIntelliJKotlinPlugin: Array<String> by rootProject.extra
val kotlinApiVersionForProjectsUsedInIntelliJKotlinPlugin: String by rootProject.extra

/**
 * In all specified modules `-XXexplicit-return-types` flag will be added to warn about
 *   not specified return types for public declarations
 */
@Suppress("UNCHECKED_CAST")
val modulesWithRequiredExplicitTypes = rootProject.extra["firAllCompilerModules"] as Array<String>

fun Project.configureKotlinCompilationOptions() {
    plugins.withType<KotlinBasePluginWrapper> {
        val commonCompilerArgs = listOfNotNull(
            "-opt-in=kotlin.RequiresOptIn",
            "-progressive".takeIf { getBooleanProperty("test.progressive.mode") ?: false },
            "-Xdont-warn-on-error-suppression",
            "-Xcontext-parameters", // KT-72222
        )

        val kotlinLanguageVersion: String by rootProject.extra
        val renderDiagnosticNames by extra(project.kotlinBuildProperties.renderDiagnosticNames.get())

        tasks.withType<KotlinCompilationTask<*>>().configureEach {
            compilerOptions {
                freeCompilerArgs.addAll(commonCompilerArgs)
                languageVersion.set(KotlinVersion.fromVersion(kotlinLanguageVersion))
                apiVersion.set(KotlinVersion.fromVersion(kotlinLanguageVersion))
                freeCompilerArgs.add("-Xskip-prerelease-check")

                if (project.path in projectsUsedInIntelliJKotlinPlugin) {
                    apiVersion.set(KotlinVersion.fromVersion(kotlinApiVersionForProjectsUsedInIntelliJKotlinPlugin))
                }
                if (project.path in modulesWithRequiredExplicitTypes) {
                    freeCompilerArgs.add("-XXexplicit-return-types=warning")
                }
            }

            val layout = project.layout
            val rootDir = rootDir
            val useAbsolutePathsInKlib = kotlinBuildProperties.booleanProperty("kotlin.build.use.absolute.paths.in.klib").get()

            // Workaround to avoid remote build cache misses due to absolute paths in relativePathBaseArg
            // This is a workaround for KT-50876, but with no clear explanation why doFirst is used.
            // However, KGP with Native targets is used in the native-xctest project, and this code fails with
            //  The value for property 'freeCompilerArgs' is final and cannot be changed any further.
            if (project.path != ":native:kotlin-test-native-xctest" &&
                !project.path.startsWith(":native:objcexport-header-generator") &&
                !project.path.startsWith(":libraries:tools:analysis-api-based-klib-reader") &&
                !project.path.startsWith(":native:external-projects-test-utils") &&
                !project.path.startsWith(":plugins:plugin-sandbox:plugin-annotations")
            ) {
                doFirst {
                    if (!useAbsolutePathsInKlib && this !is KotlinJvmCompile && this !is KotlinCompileCommon) {
                        @Suppress("DEPRECATION_ERROR", "DEPRECATION")
                        (this as KotlinCompile<*>).kotlinOptions.freeCompilerArgs +=
                            "-Xklib-relative-path-base=${layout.buildDirectory.get().asFile},${layout.projectDirectory.asFile},$rootDir"
                    }
                }
            }
        }

        val projectsWithOptInToUnsafeCastFunctionsFromAddToStdLib: List<String> by rootProject.extra

        tasks.withType<KotlinJvmCompile>().configureEach {
            compilerOptions {
                if (renderDiagnosticNames) {
                    freeCompilerArgs.add("-Xrender-internal-diagnostic-names")
                }
                allWarningsAsErrors.set(!kotlinBuildProperties.disableWerror)
                if (project.path in projectsWithOptInToUnsafeCastFunctionsFromAddToStdLib) {
                    freeCompilerArgs.add("-opt-in=org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction")
                }

                if (!skipJvmDefaultForModule(project.path)) {
                    freeCompilerArgs.add(
                        if (project.shouldUseOldJvmDefaultArgument())
                            "-Xjvm-default=all"
                        else
                            "-jvm-default=no-compatibility"
                    )
                } else {
                    freeCompilerArgs.add(
                        if (project.shouldUseOldJvmDefaultArgument())
                            "-Xjvm-default=disable"
                        else
                            "-jvm-default=disable"
                    )
                }
            }
        }
    }
}

private fun Project.shouldUseOldJvmDefaultArgument(): Boolean {
    @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
    val isOldCompilerVersion =
        MavenComparableVersion(kotlinExtension.compilerVersion.get()) < MavenComparableVersion("2.2")

    return isOldCompilerVersion
}

fun Project.configureArtifacts() {
    tasks.withType<Javadoc>().configureEach {
        enabled = false
    }

    /**
     * Bit mask: `rw-r--r--`
     */
    fun ConfigurableFilePermissions.configureDefaultFilePermissions() {
        user {
            read = true
            write = true
            execute = false
        }
        group {
            read = true
            write = false
            execute = false
        }
        other {
            read = true
            write = false
            execute = false
        }
    }

    /**
     * Bit mask: `rwxr-xr-x`
     * Applies to both directories and executable files
     */
    fun ConfigurableFilePermissions.configureDefaultExecutableFilePermissions() {
        user {
            read = true
            write = true
            execute = true
        }
        group {
            read = true
            write = false
            execute = true
        }
        other {
            read = true
            write = false
            execute = true
        }
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        filePermissions {
            configureDefaultFilePermissions()
        }
        dirPermissions {
            configureDefaultExecutableFilePermissions()
        }
        filesMatching("**/bin/*") {
            permissions {
                configureDefaultExecutableFilePermissions()
            }
        }
        filesMatching("**/bin/*.bat") {
            permissions {
                configureDefaultFilePermissions()
            }
        }
    }

    normalization {
        runtimeClasspath {
            ignore("META-INF/MANIFEST.MF")
            ignore("META-INF/compiler.version")
            ignore("META-INF/plugin.xml")
            ignore("kotlin/KotlinVersionCurrentValue.class")
        }
    }

    fun Task.listConfigurationContents(configName: String) {
        doFirst {
            project.configurations.findByName(configName)?.let {
                println("$configName configuration files:\n${it.allArtifacts.files.files.joinToString("\n  ", "  ")}")
            }
        }
    }

    tasks.register("listArchives") { listConfigurationContents("archives") }
    tasks.register("listDistJar") { listConfigurationContents("distJar") }
}

fun Project.configureTests() {
    val concurrencyLimitService = project.gradle.sharedServices.registerIfAbsent(
        "concurrencyLimitService",
        ConcurrencyLimitService::class
    ) {
        maxParallelUsages.set(1)
    }

    tasks.withType<Test>().configureEach {
        val notCacheableTestProjects: List<String> = listOf(
            ":analysis:analysis-api",
            ":analysis:analysis-api-fe10",
            ":analysis:analysis-api-fir",
            ":analysis:analysis-api-standalone",
            ":analysis:analysis-api-standalone:analysis-api-standalone-native",
            ":analysis:low-level-api-fir",
            ":analysis:low-level-api-fir:low-level-api-fir-native-compiler-tests",
            ":analysis:stubs",
            ":compiler",
            ":compiler:android-tests",
            ":compiler:arguments",
            ":compiler:build-tools:kotlin-build-tools-api",
            ":compiler:build-tools:kotlin-build-tools-api-tests",
            ":compiler:build-tools:kotlin-build-tools-compat",
            ":compiler:build-tools:kotlin-build-tools-options-generator",
            ":compiler:fir:modularized-tests",
            ":compiler:fir:raw-fir:light-tree2fir",
            ":compiler:fir:raw-fir:psi2fir",
            ":compiler:incremental-compilation-impl",
            ":compiler:ir.backend.common",
            ":compiler:multiplatform-parsing",
            ":compiler:psi:psi-api",
            ":compiler:test-infrastructure-utils",
            ":compiler:tests-different-jdk",
            ":compiler:tests-integration",
            ":compose-compiler-gradle-plugin",
            ":examples:scripting-jvm-embeddable-host",
            ":examples:scripting-jvm-maven-deps-host",
            ":examples:scripting-jvm-simple-script-host",
            ":generators",
            ":jps:jps-common",
            ":jps:jps-plugin",
            ":kotlin-annotation-processing",
            ":kotlin-annotation-processing-base",
            ":kotlin-annotation-processing-cli",
            ":kotlin-atomicfu-compiler-plugin",
            ":kotlin-build-common",
            ":kotlin-compiler-client-embeddable",
            ":kotlin-compiler-embeddable",
            ":kotlin-daemon-client",
            ":kotlin-daemon-tests",
            ":kotlin-dataframe-compiler-plugin",
            ":kotlin-gradle-plugin",
            ":kotlin-gradle-plugin-dsl-codegen",
            ":kotlin-gradle-plugin-idea",
            ":kotlin-gradle-plugin-idea-proto",
            ":kotlin-gradle-plugin-integration-tests",
            ":kotlin-gradle-statistics",
            ":kotlin-main-kts",
            ":kotlin-main-kts-test",
            ":kotlin-metadata-jvm",
            ":kotlin-native:Interop:Indexer",
            ":kotlin-native:Interop:StubGenerator",
            ":kotlin-native:Interop:StubGeneratorConsistencyCheck",
            ":kotlin-native:common:env",
            ":kotlin-native:common:files",
            ":kotlin-native:libclangInterop",
            ":kotlin-native:llvmInterop",
            ":kotlin-native:tools:kdumputil",
            ":kotlin-scripting-common",
            ":kotlin-scripting-compiler",
            ":kotlin-scripting-dependencies",
            ":kotlin-scripting-dependencies-maven",
            ":kotlin-scripting-dependencies-maven-all",
            ":kotlin-scripting-ide-services-test",
            ":kotlin-scripting-jsr223-test",
            ":kotlin-scripting-jvm",
            ":kotlin-scripting-jvm-host-test",
            ":kotlin-stdlib",
            ":kotlin-stdlib-jdk8",
            ":kotlin-stdlib:samples",
            ":kotlin-test",
            ":kotlin-tooling-core",
            ":kotlin-tooling-metadata",
            ":kotlin-util-klib",
            ":kotlinx-metadata-klib",
            ":kotlinx-serialization-compiler-plugin",
            ":libraries:tools:abi-validation:abi-tools",
            ":libraries:tools:abi-validation:abi-tools-api",
            ":libraries:tools:abi-validation:abi-tools-tests",
            ":libraries:tools:abi-validation:kgp-integration-tests",
            ":libraries:tools:analysis-api-based-klib-reader",
            ":native:kotlin-klib-commonizer",
            ":native:kotlin-klib-commonizer-api",
            ":native:kotlin-native-utils",
            ":native:native.tests:driver",
            ":native:native.tests:gc-fuzzing-tests",
            ":native:native.tests:gc-fuzzing-tests:engine",
            ":native:objcexport-header-generator",
            ":native:objcexport-header-generator-analysis-api",
            ":native:objcexport-header-generator-k1",
            ":native:swift:sir-light-classes",
            ":native:swift:sir-printer",
            ":native:swift:swift-export-embeddable",
            ":native:swift:swift-export-ide",
            ":native:swift:swift-export-standalone-integration-tests:coroutines",
            ":native:swift:swift-export-standalone-integration-tests:external",
            ":native:swift:swift-export-standalone-integration-tests:simple",
            ":plugins:compose-compiler-plugin:compiler-hosted",
            ":plugins:compose-compiler-plugin:compiler-hosted:integration-tests",
            ":plugins:jvm-abi-gen",
            ":plugins:parcelize:parcelize-compiler",
            ":plugins:plugins-interactions-testing",
            ":plugins:plugin-sandbox:plugin-sandbox-ic-test",
            ":plugins:scripting:scripting-tests",
            ":repo:artifacts-tests",
            ":repo:codebase-tests",
            ":tools:binary-compatibility-validator",
            ":tools:ide-plugin-dependencies-validator",
            ":tools:jdk-api-validator",
            ":wasm:wasm.ir",
        )
        val projectPath = project.path
        val hasTestInputCheckPlugin = plugins.hasPlugin("test-inputs-check")
        if (!hasTestInputCheckPlugin) {
            outputs.doNotCacheIf("https://youtrack.jetbrains.com/issue/KTI-112") { true }
        }
        doFirst {
            if (!hasTestInputCheckPlugin) {
                if (projectPath !in notCacheableTestProjects) {
                    throw GradleException(
                        """
                        Tests are not cacheable in: $projectPath
                        Apply id("test-inputs-check") to the project to make the tests cacheable.
                    """.trimIndent()
                    )
                }
            } else {
                if (projectPath in notCacheableTestProjects) {
                    throw GradleException("Tests are cacheable in: ${projectPath}, but we listed it in `notCacheableTestProjects`")
                }
            }
        }
        if (project.kotlinBuildProperties.limitTestTasksConcurrency) {
            usesService(concurrencyLimitService)
        }
    }

    // Aggregate task for build related checks
    tasks.register("checkBuild")
    val mppProjects: List<String> by rootProject.extra
    if (path !in mppProjects) {
        configureTestRetriesForTestTasks()
    }
}

// TODO: migrate remaining modules to the new JVM default scheme.
fun skipJvmDefaultForModule(path: String): Boolean =
// Gradle plugin modules are disabled because different Gradle versions bundle different Kotlin compilers,
    // and not all of them support the new JVM default scheme.
    "-gradle" in path || "-runtime" in path || path == ":kotlin-project-model" ||
            // Workaround a Proguard issue:
            //     java.lang.IllegalAccessError: tried to access method kotlin.reflect.jvm.internal.impl.types.checker.ClassicTypeSystemContext$substitutionSupertypePolicy$2.<init>(
            //       Lkotlin/reflect/jvm/internal/impl/types/checker/ClassicTypeSystemContext;Lkotlin/reflect/jvm/internal/impl/types/TypeSubstitutor;
            //     )V from class kotlin.reflect.jvm.internal.impl.resolve.OverridingUtilTypeSystemContext
            // KT-54749
            path == ":core:descriptors"


// Workaround for #KT-65266
afterEvaluate {
    val versionString = version.toString()
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        val realFriendPaths = (friendPaths as DefaultConfigurableFileCollection).shallowCopy()
        val friendPathsWithoutVersion = friendPaths.filter { !it.name.contains(versionString) }
        friendPaths.setFrom(friendPathsWithoutVersion)
        doFirst {
            friendPaths.setFrom(realFriendPaths)
        }
    }
}
