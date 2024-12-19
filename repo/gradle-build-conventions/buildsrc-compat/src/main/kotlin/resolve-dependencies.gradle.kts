@file:Suppress("DEPRECATION")
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.d8.D8EnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.spdx.sbom.gradle.SpdxSbomExtension
import java.net.URI

val resolveDependenciesInAllProjects by tasks.registering {
    description = "Resolves dependencies in all projects (for dependency verification or populating caches)."
    notCompatibleWithConfigurationCache("Uses project during task execution")
    doNotTrackState("The task must always re-run to ensure that all dependencies are downloaded.")
    doLast {
        allprojects {
            logger.lifecycle("Resolving dependencies in ${project.displayName}")

            // resolve implicit dependencies one by one to avoid conflicts between them
            configurations.implicitDependencies.get().allDependencies.forEach { implicitDependency ->
                configurations.detachedConfiguration(implicitDependency).resolve()
            }

            configurations.findByName("commonCompileClasspath")?.resolve()
            configurations.findByName("testRuntimeClasspath")?.resolve()
            configurations.findByName(NATIVE_TEST_DEPENDENCY_KLIBS_CONFIGURATION_NAME)?.resolve()

            project.extensions.findByType<SpdxSbomExtension>()?.run {
                targets.forEach { target ->
                    target.configurations.get().forEach { configurationName ->
                        val pomDependencies = project.configurations[configurationName].incoming.resolutionResult.allComponents
                            .map { it.id }
                            .filterIsInstance<ModuleComponentIdentifier>()
                            .map { project.dependencies.create(it.displayName + "@pom") }

                        project.configurations.detachedConfiguration(*pomDependencies.toTypedArray()).resolve()
                    }
                }
            }
        }
    }
}

val resolveJsTools by tasks.registering {
    description = "Resolves JavaScript tools (for dependency verification or populating caches)."
    notCompatibleWithConfigurationCache("Uses project during task execution")
    doNotTrackState("The task must always re-run to ensure that all dependencies are downloaded.")

    doLast {
        fun Project.resolveDependencies(
            vararg dependency: String,
            repositoryHandler: RepositoryHandler.() -> ArtifactRepository,
        ) {
            val repo = repositories.repositoryHandler()
            dependency.forEach {
                configurations.detachedConfiguration(dependencies.create(it)).resolve()
            }
            repo.run { repositories.remove(this) }
        }

        @OptIn(ExperimentalWasmDsl::class)
        allprojects {
            extensions.findByType<D8EnvSpec>()?.run {
                val versionValue = version.get()
                project.resolveDependencies(
                    "google.d8:v8:linux64-rel-$versionValue@zip",
                    "google.d8:v8:win64-rel-$versionValue@zip",
                    "google.d8:v8:mac-arm64-rel-$versionValue@zip",
                    "google.d8:v8:mac64-rel-$versionValue@zip"
                ) {
                    ivy {
                        name = "D8-ResolveDependencies"
                        url = requireNotNull(downloadBaseUrl.get()) { "downloadBaseUrl was null for $name repository" }.let(::URI)
                        patternLayout {
                            artifact("[artifact]-[revision].[ext]")
                        }
                        metadataSources { artifact() }
                        content { includeModule("google.d8", "v8") }
                    }
                }
            }

            extensions.findByType<BinaryenRootEnvSpec>()?.run {
                val versionValue = version.get()

                project.resolveDependencies(
                    "com.github.webassembly:binaryen:$versionValue:arm64-macos@tar.gz",
                    "com.github.webassembly:binaryen:$versionValue:x86_64-linux@tar.gz",
                    "com.github.webassembly:binaryen:$versionValue:x86_64-macos@tar.gz",
                    "com.github.webassembly:binaryen:$versionValue:x86_64-windows@tar.gz"
                ) {
                    ivy {
                        name = "Binaryen-ResolveDependencies"
                        url = requireNotNull(downloadBaseUrl.get()) { "downloadBaseUrl was null for $name repository" }.let(::URI)
                        patternLayout {
                            artifact("version_[revision]/binaryen-version_[revision]-[classifier].[ext]")
                        }
                        metadataSources { artifact() }
                        content { includeModule("com.github.webassembly", "binaryen") }
                    }
                }
            }

            extensions.findByType<NodeJsEnvSpec>()?.run {
                val versionValue = version.get()

                project.resolveDependencies(
                    "org.nodejs:node:$versionValue:linux-x64@tar.gz",
                    "org.nodejs:node:$versionValue:win-x64@zip",
                    "org.nodejs:node:$versionValue:darwin-x64@tar.gz",
                    "org.nodejs:node:$versionValue:darwin-arm64@tar.gz"
                ) {
                    ivy {
                        name = "NodeJs-ResolveDependencies"
                        url = requireNotNull(downloadBaseUrl.get()) { "downloadBaseUrl was null for $name repository" }.let(::URI)
                        patternLayout {
                            artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
                        }
                        metadataSources { artifact() }
                        content { includeModule("org.nodejs", "node") }
                    }
                }
            }

            extensions.findByType<YarnRootEnvSpec>()?.run {
                project.resolveDependencies("com.yarnpkg:yarn:${version.get()}@tar.gz") {
                    ivy {
                        name = "Yarn-ResolveDependencies"
                        url = requireNotNull(downloadBaseUrl.get()) { "downloadBaseUrl was null for $name repository" }.let(::URI)
                        patternLayout {
                            artifact("v[revision]/[artifact](-v[revision]).[ext]")
                        }
                        metadataSources { artifact() }
                        content { includeModule("com.yarnpkg", "yarn") }
                    }
                }
            }
        }
    }
}

/**
 * When called with `--write-verification-metadata` resolves all build dependencies including implicit dependencies for all platforms and
 * dependencies downloaded by plugins.
 *
 * Useful for populating Gradle dependency cache or updating `verification-metadata.xml` properly.
 *
 * `./gradlew resolveDependencies --write-verification-metadata md5,sha256 -Pkotlin.native.enabled=true`
 */
tasks.register("resolveDependencies") {
    description = "Resolves all dependencies, including implicit dependencies, in all projects for dependency verification."
    group = "build setup"

    dependsOn(
        resolveDependenciesInAllProjects,
        resolveJsTools,
    )
}

tasks.register("resolveJavaToolchainsInAllProjects") {
    // Currently unused.
    // It is supposed to run during agent image build to populate caches, along with resolveDependencies.
    description = "Resolves Java Toolchains in all Java projects."
    notCompatibleWithConfigurationCache("Uses project during task execution")
    doNotTrackState("The task must always re-run to ensure that all dependencies are downloaded.")
    doLast("Resolve Java toolchains in all projects") {
        allprojects {
            plugins.withId("java-base") {
                val service = project.extensions.getByType<JavaToolchainService>()
                val javaExtension = extensions.getByType<JavaPluginExtension>()
                val compiler = service.compilerFor(javaExtension.toolchain).get()
                logger.lifecycle("Resolved Java Toolchain ${compiler.metadata} in ${project.displayName}")
            }
        }
    }
}
