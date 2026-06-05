import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.internal.file.collections.DefaultConfigurableFileCollection
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.config.MavenComparableVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

// Contains common configuration that should be applied to all projects
plugins {
    id("implicit-dependencies")
    id("java-instrumentation")
}

// Common Group and version
val kotlinVersion: String = project.kotlinBuildProperties.kotlinVersion.get()
group = "org.jetbrains.kotlin"
version = kotlinVersion

project.configureJvmDefaultToolchain()
project.addEmbeddedConfigurations()
project.configureJavaCompile()
project.configureKotlinCompilationOptions()
project.configureArtifacts()
project.configureTests()
project.checkNoApiDependenciesOnK1Modules()
project.configureMigratedRootSettings()
project.configureJsCacheRedirector()
project.configurePublishingRetry()
project.exposeCompileAllConfiguration()
project.configureJarEntryCompression()

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
        val fe10CompilerModules = CompilerModules.fe10CompilerModules

        @Suppress("UNCHECKED_CAST")
        val descriptorModules = CompilerModules.descriptorsCompilerModules

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
                        "configuration (see `fe10CompilerModules` in repo/kotlin-build-helpers/src/CompilerModules.kt)."
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

val kotlinApiVersionForProjectsDependingOnStableStdlib: Provider<String> = project.providers.gradleProperty("kotlinApiVersionForProjectsDependingOnStableStdlib")

fun Project.configureKotlinCompilationOptions() {
    plugins.withType<KotlinBasePluginWrapper> {
        val kotlinLanguageVersion: Provider<String> = project.providers.gradleProperty("kotlinLanguageVersion")
        val renderDiagnosticNames = project.kotlinBuildProperties.renderDiagnosticNames.get()
        extra.set("renderDiagnosticNames", renderDiagnosticNames)

        tasks.withType<KotlinCompilationTask<*>>().configureEach {
            compilerOptions {
                val skipNewLanguageFeatures = skipArgumentForOlderKotlinCompilerVersion()

                val commonCompilerArgs = provider {
                    listOfNotNull(
                        "-opt-in=kotlin.RequiresOptIn",
                        "-progressive".takeIf { project.kotlinBuildProperties.booleanProperty("test.progressive.mode", false).get() },
                        "-Xdont-warn-on-error-suppression",
                        "-Xcontext-parameters", // KT-72222
                        "-Xexplicit-backing-fields".takeUnless { skipNewLanguageFeatures }, // KT-14663
                        "-Xname-based-destructuring=complete".takeUnless { skipNewLanguageFeatures },
                        "-Xcollection-literals".takeUnless { skipNewLanguageFeatures },
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
                languageVersion.set(kotlinLanguageVersion.map{ KotlinVersion.fromVersion(it) })
                apiVersion.set(kotlinLanguageVersion.map { KotlinVersion.fromVersion(it) })
                freeCompilerArgs.add("-Xskip-prerelease-check")

                if (project.path in CompilerModules.projectsDependingOnStableStdlib) {
                    apiVersion.set(kotlinApiVersionForProjectsDependingOnStableStdlib.map { KotlinVersion.fromVersion(it) })
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

        val projectsWithOptInToUnsafeCastFunctionsFromAddToStdLib = listOf(
            ":analysis:analysis-api-fir",
            ":analysis:decompiled:light-classes-for-decompiled",
            ":analysis:symbol-light-classes",
            ":compiler",
            ":compiler:backend.js",
            ":jps:jps-common",
            ":js:js.tests",
            ":kotlin-build-common",
            ":kotlin-gradle-plugin",
            ":kotlin-scripting-jvm-host-test",
            ":native:kotlin-klib-commonizer",
        )


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

private val libs = project.the<LibrariesForLibs>()
private val kotlinCompilerVersionForGradle = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()

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
            ignore("**/build.txt")
            ignore("*.spdx.json")
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

            ":plugins:compose-compiler-plugin:compiler-hosted:integration-tests",
            ":plugins:scripting:scripting-tests",
            ":repo:auto-code-review", // Runs processes, traverses all repo files. Quick.
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

        val testInventoryListener = TestInventoryListener(name, project.layout.buildDirectory.asFile)
        addTestListener(testInventoryListener)
        outputs.file(testInventoryListener.inventoryFile)

        doFirst {
            if (disableVerificationTasks.get()) {
                logger.warn("Task $path is disabled because `kotlin.build.disable.verification.tasks` is true")
                throw StopExecutionException("Verification tasks are disabled.")
            }
        }
    }
    // Aggregate task for build related checks
    tasks.register("checkBuild")
    configureTestRetriesForTestTasks()
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

private val dependencyOnSnapshotReflectWhitelist = setOf(
    ":kotlin-compiler",
    ":kotlin-reflect",
    ":tools:binary-compatibility-validator",
    ":tools:kotlin-stdlib-gen",
)

// Per-project configuration migrated from the root `allprojects {}` block as part of the
// Gradle Isolated Projects migration. This plugin is already applied to (almost) every project,
// so running these bodies here is equivalent to the previous cross-project configuration.
fun Project.configureMigratedRootSettings() {
    if (kotlinBuildProperties.isInIdeaSync.get()) {
        afterEvaluate {
            configurations.all {
                // Remove kotlin-compiler from dependencies during Idea import. KTI-1598
                if (dependencies.removeIf { (it as? ProjectDependency)?.path == ":kotlin-compiler" }) {
                    logger.warn("Removed :kotlin-compiler project dependency from \$this")
                }
            }
        }
    }

    configurations.all {
        val configuration = this
        if (name != "compileClasspath") {
            return@all
        }
        resolutionStrategy {
            if (!kotlinBuildProperties.localBootstrap.getOrElse(false)) {
                failOnNonReproducibleResolution()
            }
            eachDependency {
                if (requested.group != "org.jetbrains.kotlin") {
                    return@eachDependency
                }

                val isReflect = requested.name == "kotlin-reflect"
                // More strict check for "compilerModules". We can't apply this check for all modules because it would force to
                // exclude kotlin-reflect from transitive dependencies of kotlin-poet, ktor, com.android.tools.build:gradle, etc
                if (project.path in @Suppress("UNCHECKED_CAST") (CompilerModules.compilerModules)) {
                    val expectedReflectVersion = commonDependencyVersion("org.jetbrains.kotlin", "kotlin-reflect")
                    if (isReflect) {
                        check(requested.version == expectedReflectVersion) {
                            """
                            \$configuration: 'kotlin-reflect' should have '\$expectedReflectVersion' version. But it was '\${requested.version}'
                            Suggestions:
                                1. Use 'commonDependency("org.jetbrains.kotlin:kotlin-reflect") { isTransitive = false }'
                                2. Avoid 'kotlin-reflect' leakage from transitive dependencies with 'exclude("org.jetbrains.kotlin")'
                        """.trimIndent()
                        }
                    }
                    if (requested.name.startsWith("kotlin-stdlib")) {
                        check(requested.version != expectedReflectVersion) {
                            """
                            \$configuration: '\${requested.name}' has a wrong version. It's not allowed to be '\$expectedReflectVersion'
                            Suggestions:
                                1. Most likely, it leaked from 'kotlin-reflect' transitive dependencies. Use 'isTransitive = false' for
                                   'kotlin-reflect' dependencies
                                2. Avoid '\${requested.name}' leakage from other transitive dependencies with 'exclude("org.jetbrains.kotlin")'
                        """.trimIndent()
                        }
                    }
                }
                if (isReflect && project.path !in dependencyOnSnapshotReflectWhitelist) {
                    check(requested.version != kotlinVersion) {
                        """
                        \$configuration: 'kotlin-reflect' is not allowed to have '\$kotlinVersion' version.
                        Suggestion: Use 'commonDependency("org.jetbrains.kotlin:kotlin-reflect") { isTransitive = false }'
                    """.trimIndent()
                    }
                }
            }
        }
    }
}

fun Project.exposeCompileAllConfiguration() {
    val compileAllConfig = configurations.consumable("compileAll")
    afterEvaluate {
        val kotlinCompileToolNames = tasks.withType<KotlinCompileTool>().names
        val javaCompileNames = tasks.withType<JavaCompile>().names
        kotlinCompileToolNames.forEach {
            val task = tasks.named<KotlinCompileTool>(it)
            artifacts.add(compileAllConfig.name, task.map { it.destinationDirectory }) { builtBy(task) }
        }
        javaCompileNames.forEach {
            val task = tasks.named<JavaCompile>(it)
            artifacts.add(compileAllConfig.name, task.map { it.destinationDirectory }) { builtBy(task) }
        }
    }
}

fun Project.configureJarEntryCompression() {
    tasks.withType<org.gradle.jvm.tasks.Jar>().configureEach {
        entryCompression = if (kotlinBuildProperties.jarCompression)
            ZipEntryCompression.DEFLATED
        else
            ZipEntryCompression.STORED
    }
}
