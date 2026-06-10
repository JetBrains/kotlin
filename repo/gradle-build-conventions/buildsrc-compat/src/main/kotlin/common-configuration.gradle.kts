import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.internal.file.collections.DefaultConfigurableFileCollection
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.config.MavenComparableVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

// Contains common configuration that should be applied to all projects
plugins {
    id("implicit-dependencies")
}

// Common Group and version
val kotlinVersion: String by rootProject.extra
group = "org.jetbrains.kotlin"
version = kotlinVersion

project.configureJvmDefaultToolchain()
project.addEmbeddedConfigurations()
project.configureJavaCompile()
project.configureKotlinCompilationOptions()
project.configureArtifacts()
project.configureTests()
project.checkNoApiDependenciesOnK1Modules()

// There are problems with common build dir:
//  - some tests (in particular js and binary-compatibility-validator depend on the fixed (default) location
//  - idea seems unable to exclude common buildDir from indexing
// therefore it is disabled by default
// buildDir = File(commonBuildDir, project.name)

/**
 * Validates that the project does not expose K1 frontend modules
 * (see `fe10CompilerModules` in `gradle/compilerModules.gradle.kts`) through the `api`
 * configuration. K1 frontend modules must only be depended on via `implementation`,
 * so that the legacy frontend never leaks onto consumers' compile classpaths.
 */
fun Project.checkNoApiDependenciesOnK1Modules() {
    // The IDE-plugin dependency bundles under `:prepare:ide-plugin-dependencies` intentionally
    // re-export compiler modules (including the K1 frontend) via `api`, so that the IntelliJ
    // Kotlin plugin gets them on its classpath. They are the sanctioned re-exporters and are
    // exempt from this invariant.
    if (path.startsWith(":prepare:ide-plugin-dependencies")) return

    afterEvaluate {
        val apiConfiguration = configurations.findByName("api") ?: return@afterEvaluate

        @Suppress("UNCHECKED_CAST")
        val fe10CompilerModules = rootProject.extra["fe10CompilerModules"] as Array<String>

        @Suppress("UNCHECKED_CAST")
        val descriptorModules = rootProject.extra["descriptorsCompilerModules"] as Array<String>

        val k1Modules = (fe10CompilerModules + descriptorModules).toSet()

        val violations = apiConfiguration.dependencies
            .filterIsInstance<ProjectDependency>()
            .map { it.path }
            .filter { it in k1Modules }
            .sorted()

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Project '$path' declares `api` dependencies on K1 frontend modules: " +
                        violations.joinToString(prefix = "[", postfix = "]") + ". " +
                        "K1 frontend modules must only be depended on with the `implementation` " +
                        "configuration (see `fe10CompilerModules` in gradle/compilerModules.gradle.kts)."
            )
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

val projectsDependingOnStableStdlib: Array<String> by rootProject.extra
val kotlinApiVersionForProjectsDependingOnStableStdlib: String by rootProject.extra

fun Project.configureKotlinCompilationOptions() {
    plugins.withType<KotlinBasePluginWrapper> {
        val kotlinLanguageVersion: String by rootProject.extra
        val renderDiagnosticNames by extra(project.kotlinBuildProperties.renderDiagnosticNames.get())

        tasks.withType<KotlinCompilationTask<*>>().configureEach {
            compilerOptions {
                val skipNewLanguageFeatures = skipArgumentForOlderKotlinCompilerVersion()

                val commonCompilerArgs = provider {
                    listOfNotNull(
                        "-opt-in=kotlin.RequiresOptIn",
                        "-progressive".takeIf { getBooleanProperty("test.progressive.mode") ?: false },
                        "-Xdont-warn-on-error-suppression",
                        "-Xcontext-parameters", // KT-72222
                        "-Xexplicit-backing-fields".takeUnless { skipNewLanguageFeatures }, // KT-14663
                        "-Xname-based-destructuring=complete".takeUnless { skipNewLanguageFeatures },
                        // Between making a language feature stable and the next bootstrap, we need to keep providing the compiler argument.
                        // But this produces a warning
                        // "The argument ... is redundant for the current language version ..."
                        // in the bootstrap test and fails because of -Werror.
                        // To work around it, we suppress the warning.
                        @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
                        "-Xwarning-level=REDUNDANT_CLI_ARG:disabled".takeIf {
                            project.kotlinExtension.compilerVersion.get() == project.kotlinToolingVersion.toString()
                        },
                    )
                }

                freeCompilerArgs.addAll(commonCompilerArgs)
                languageVersion.set(KotlinVersion.fromVersion(kotlinLanguageVersion))
                apiVersion.set(KotlinVersion.fromVersion(kotlinLanguageVersion))
                freeCompilerArgs.add("-Xskip-prerelease-check")

                if (project.path in projectsDependingOnStableStdlib) {
                    apiVersion.set(KotlinVersion.fromVersion(kotlinApiVersionForProjectsDependingOnStableStdlib))
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
                !project.path.startsWith(":plugins:plugin-sandbox:plugin-annotations") &&
                !project.path.startsWith(":kotlin-power-assert-runtime")
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

private val kotlinCompilerVersionForGradle = rootProject.extensions
    .getByType(VersionCatalogsExtension::class.java)
    .named("libs")
    .findVersion("kotlin-for-gradle-plugins-compilation")
    .get()
    .displayName

private fun Project.skipArgumentForOlderKotlinCompilerVersion(): Boolean {
    @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
    return MavenComparableVersion(kotlinExtension.compilerVersion.get()) <= MavenComparableVersion(kotlinCompilerVersionForGradle)
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
            ":analysis:analysis-api-standalone:analysis-api-standalone-native",
            ":analysis:low-level-api-fir:low-level-api-fir-native-compiler-tests",
            ":compiler:build-tools:kotlin-build-tools-api",
            ":compiler:build-tools:kotlin-build-tools-compat",
            ":compiler:build-tools:kotlin-build-tools-generator",
            ":compiler:fir:modularized-tests",
            ":compiler:fir:raw-fir:light-tree2fir",
            ":compiler:fir:raw-fir:psi2fir",
            ":compiler:multiplatform-parsing",
            ":compiler:test-infrastructure-utils",
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
            ":kotlin-build-common",
            ":kotlin-compiler-client-embeddable",
            ":kotlin-compiler-embeddable",
            ":kotlin-daemon-client",
            ":kotlin-gradle-plugin",
            ":kotlin-gradle-plugin-dsl-codegen",
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
            ":kotlin-power-assert-runtime", // TODO(KTI-3056): 'test-inputs-check' cannot be combined with 'multiplatform' projects
            ":kotlin-scripting-common",
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
            ":kotlin-util-klib",
            ":kotlinx-metadata-klib",
            ":libraries:tools:abi-validation:abi-tools",
            ":libraries:tools:abi-validation:abi-tools-api",
            ":libraries:tools:abi-validation:abi-tools-tests",
            ":libraries:tools:abi-validation:kgp-integration-tests",
            ":libraries:tools:analysis-api-based-klib-reader",
            ":native:kotlin-native-utils",
            ":native:native.tests:driver",
            ":native:native.tests:gc-fuzzing-tests",
            ":native:native.tests:gc-fuzzing-tests:engine",
            ":native:objcexport-header-generator",
            ":native:objcexport-header-generator-analysis-api",
            ":native:objcexport-header-generator-k1",

            ":plugins:compose-compiler-plugin:compiler-hosted:integration-tests",
            ":plugins:scripting:scripting-tests",
            ":repo:artifacts-tests",
            ":repo:codebase-tests",
            ":tools:binary-compatibility-validator",
            ":tools:ide-plugin-dependencies-validator",
            ":benchmarks",
            ":test-instrumenter"
        )
        val projectPath = project.path
        val hasTestInputCheckPlugin = plugins.hasPlugin("test-inputs-check") || plugins.hasPlugin("test-inputs-check-v2")
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

        /*
        We're disabling test reports on teamcity for Gradle 9.4 as we experienced failures like
        'File name too long' when upgrading to Gradle 9.4 while generating those reports.
        https://github.com/gradle/gradle/issues/36996
         */
        reports {
            configureEach {
                if (GradleVersion.current() == GradleVersion.version("9.4.0")) {
                    this.required = false
                }
            }
        }

    }

    tasks.withType<AbstractTestTask>().configureEach {
        val disableVerificationTasks: Provider<Boolean> = providers.gradleProperty("kotlin.build.disable.verification.tasks")
            .map { it.toBoolean() }
            .orElse(false)
        inputs.property("kotlin.build.disable.verification.tasks", disableVerificationTasks)
        doFirst {
            if (disableVerificationTasks.get()) {
                logger.warn("Task $path is disabled because `kotlin.build.disable.verification.tasks` is true")
                throw StopExecutionException("Verification tasks are disabled.")
            }
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
