import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.internal.file.collections.DefaultConfigurableFileCollection
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion

// Contains common configuration that should be applied to all projects
plugins {
    id("implicit-dependencies")
    id("java-instrumentation")
    id("jvm-toolchains-convention")
}

// Common Group and version
val kotlinVersion: String = project.kotlinBuildProperties.kotlinVersion.get()
group = "org.jetbrains.kotlin"
version = kotlinVersion

project.configureKotlinJavaCompileHygiene()
project.addEmbeddedConfigurations()
project.configureJavaCompile()
project.configureKotlinCompilationOptions()
project.configureArtifacts()
project.configureTests()
project.registerApiSurfaceTasks()
project.checkNoApiDependenciesOnK1Modules()
project.configureMigratedRootSettings()
project.configureJsCacheRedirector()
project.configurePublishingRetry()
project.exposeCompileAllConfiguration()
project.configureJarEntryCompression()
project.configureTestLifecycleTasksModelBuilder()

// There are problems with common build dir:
//  - some tests (in particular js and binary-compatibility-validator depend on the fixed (default) location
//  - idea seems unable to exclude common buildDir from indexing
// therefore it is disabled by default
// buildDir = File(commonBuildDir, project.name)

/**
 * Registers the `checkApiSurface` and `updateApiSurface` lifecycle tasks of a project.
 *
 * A module that tracks its own API surface — an ABI dump, the foreign classes its public API leaks, the conventions
 * its declarations follow — hangs the verifying half of each such check on `checkApiSurface` and the rewriting half
 * on `updateApiSurface`. Whoever owns a check does that wiring itself, from inside the project, so aggregates over
 * several modules can depend on the two task paths without inspecting anyone else's task container. Gradle's project
 * isolation forbids that inspection, and an unconfigured project answers it with an empty task container, which
 * silently turns such an aggregate into a no-op.
 *
 * Both tasks are deliberately kept out of `check`. `updateApiSurface` rewrites files in the source tree, and
 * `checkApiSurface` duplicates work that `check` already does through the checks' own `check` wiring.
 *
 * The tasks are registered for every project of the build, so a module that starts tracking its API surface only
 * needs to declare the checks themselves.
 */
fun Project.registerApiSurfaceTasks() {
    val checkApiSurface = tasks.register("checkApiSurface") {
        group = "verification"
        description = "Verifies the API surface dumps of this project against its sources"
    }

    val updateApiSurface = tasks.register("updateApiSurface") {
        group = "verification"
        description = "Rewrites the API surface dumps of this project from its sources"
    }

    // The ABI dump tasks come from the Kotlin Gradle plugin's 'abiValidation', and their classes are internal to it,
    // so they cannot be picked up by type the way the checks owned by this repository are. Both names are stable
    // parts of the plugin's contract; see 'KotlinAbiCheckTaskImpl.NAME' and 'KotlinAbiUpdateTask.NAME'.
    checkApiSurface.configure { dependsOn(registeredTaskNames("checkKotlinAbi")) }
    updateApiSurface.configure { dependsOn(registeredTaskNames("updateKotlinAbi")) }
}

/**
 * Those of [names] that this project has registered, as a dependency value for a lifecycle task.
 *
 * Resolved from a provider, so the lookup happens once the execution graph is built. By then the build script of
 * this project has run, and its task names are final.
 */
private fun Project.registeredTaskNames(vararg names: String): Provider<List<String>> {
    val projectTasks = tasks
    return provider { names.filter { it in projectTasks.names } }
}

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

        val fe10CompilerModules = CompilerModules.fe10CompilerModules

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

/**
 * Kotlin JVM projects don't need annotation processing on their `JavaCompile` tasks (Kotlin's own
 * annotation processing goes through kapt/KSP instead), and consistent UTF-8 encoding avoids
 * platform-default-charset-dependent compilation. `compileJava9Java` is excluded because it's
 * configured separately for multi-release-jar compilation (see `configureJava9Compilation`).
 */
fun Project.configureKotlinJavaCompileHygiene() {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        tasks.withType<JavaCompile>()
            .configureEach {
                if (name != "compileJava9Java") {
                    options.compilerArgs.add("-proc:none")
                    options.encoding = "UTF-8"
                }
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

val kotlinApiVersionForProjectsDependingOnStableStdlib: Provider<String> =
    project.providers.gradleProperty("kotlinApiVersionForProjectsDependingOnStableStdlib")

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
                languageVersion.set(kotlinLanguageVersion.map { KotlinVersion.fromVersion(it) })
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
                    jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
                } else {
                    jvmDefault = JvmDefaultMode.DISABLE
                }

            }
        }
    }
}

private val libs = project.the<LibrariesForLibs>()
private val kotlinCompilerVersionForGradle = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()

private fun Project.skipArgumentForOlderKotlinCompilerVersion(): Boolean {
    @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
    return KotlinToolingVersion(kotlinExtension.compilerVersion.get()) <= KotlinToolingVersion(kotlinCompilerVersionForGradle)
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
    plugins.apply("project-tests-convention")
    plugins.apply("test-federation-convention")
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
                    logger.warn("Removed :kotlin-compiler project dependency from $this")
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
                if (project.path in CompilerModules.compilerModules) {
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
