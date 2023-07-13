import java.net.URI
import org.jetbrains.kotlin.gradle.targets.js.d8.D8RootExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.spdx.sbom.gradle.SpdxSbomExtension


/*
 * When called with `--write-verification-metadata` resolves all build dependencies including implicit dependecies for all platforms and
 * dependencies downloaded by plugins. Usefull to populate Gradle dependency cache or update `verification-metadata.xml` properly.
 *
 * `./gradlew resolveDependencies --write-verification-metadata md5,sha256`
 */
tasks.register("resolveDependencies") {
    doFirst {
        allprojects {
            logger.info("Resolving dependencies in $this")

            // resolve implicit dependencies one by one to avoid conflicts between them
            configurations.implicitDependencies.get().allDependencies.forEach { implicitDependency ->
                configurations.detachedConfiguration(implicitDependency).resolve()
            }

            configurations.findByName("commonCompileClasspath")?.resolve()

            plugins.withId("java-base") {
                val service = project.extensions.getByType<JavaToolchainService>()
                val javaExtension = extensions.getByType<JavaPluginExtension>()
                service.compilerFor(javaExtension.toolchain).get()
            }

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

        fun Project.resolveDependencies(
            vararg dependency: String,
            repositoryHandler: (RepositoryHandler.() -> ArtifactRepository)? = null
        ) {
            val repo = repositoryHandler?.let { repositories.repositoryHandler() }
            dependency.forEach {
                configurations.detachedConfiguration(dependencies.create(it)).resolve()
            }
            repo?.run { repositories.remove(this) }
        }

        rootProject.extensions.findByType<D8RootExtension>()?.run {
            project.resolveDependencies(
                "google.d8:v8:linux64-rel-$version@zip",
                "google.d8:v8:win64-rel-$version@zip",
                "google.d8:v8:mac-arm64-rel-$version@zip",
                "google.d8:v8:mac64-rel-$version@zip"
            )
        }

        rootProject.extensions.findByType<YarnRootExtension>()?.run {
            project.resolveDependencies("com.yarnpkg:yarn:$version@tar.gz") {
                ivy {
                    url = URI(downloadBaseUrl)
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