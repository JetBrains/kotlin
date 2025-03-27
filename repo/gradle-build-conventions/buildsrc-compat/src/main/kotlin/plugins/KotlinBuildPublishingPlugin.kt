/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package plugins

import capitalize
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.Usage
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import java.util.*
import javax.inject.Inject

class KotlinBuildPublishingPlugin @Inject constructor(
    private val componentFactory: SoftwareComponentFactory,
) : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        apply<MavenPublishPlugin>()

        val publishedRuntime = configurations.maybeCreate(RUNTIME_CONFIGURATION).apply {
            isCanBeConsumed = false
            isCanBeResolved = false
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            }
        }

        val publishedCompile = configurations.maybeCreate(COMPILE_CONFIGURATION).apply {
            isCanBeConsumed = false
            isCanBeResolved = false
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
            }
        }

        val kotlinLibraryComponent = componentFactory.adhoc(ADHOC_COMPONENT_NAME)
        components.add(kotlinLibraryComponent)
        kotlinLibraryComponent.addVariantsFromConfiguration(publishedCompile) { mapToMavenScope("compile") }
        kotlinLibraryComponent.addVariantsFromConfiguration(publishedRuntime) { mapToMavenScope("runtime") }

        pluginManager.withPlugin("java-base") {
            val runtimeElements by configurations
            val apiElements by configurations

            publishedRuntime.extendsFrom(runtimeElements)
            publishedCompile.extendsFrom(apiElements)

            kotlinLibraryComponent.addVariantsFromConfiguration(runtimeElements) {
                mapToMavenScope("runtime")

                if (configurationVariant.artifacts.any { JavaBasePlugin.UNPUBLISHABLE_VARIANT_ARTIFACTS.contains(it.type) }) {
                    skip()
                }
            }
        }

        configure<PublishingExtension> {
            publications {
                create<MavenPublication>(project.mainPublicationName) {
                    from(kotlinLibraryComponent)

                    configureKotlinPomAttributes(project)
                }
            }
        }
        configureDefaultPublishing()
    }

    companion object {
        const val DEFAULT_MAIN_PUBLICATION_NAME = "Main"
        const val MAIN_PUBLICATION_NAME_PROPERTY = "MainPublicationName"
        const val REPOSITORY_NAME = "Maven"
        const val ADHOC_COMPONENT_NAME = "kotlinLibrary"

        const val COMPILE_CONFIGURATION = "publishedCompile"
        const val RUNTIME_CONFIGURATION = "publishedRuntime"
    }
}

var Project.mainPublicationName: String
    get() {
        return if (project.extra.has(KotlinBuildPublishingPlugin.MAIN_PUBLICATION_NAME_PROPERTY))
            project.extra.get(KotlinBuildPublishingPlugin.MAIN_PUBLICATION_NAME_PROPERTY) as String
        else KotlinBuildPublishingPlugin.DEFAULT_MAIN_PUBLICATION_NAME
    }
    set(value) {
        project.extra.set(KotlinBuildPublishingPlugin.MAIN_PUBLICATION_NAME_PROPERTY, value)
    }

private fun humanReadableName(name: String) =
    name.split("-").joinToString(separator = " ") { it.capitalize(Locale.ROOT) }

fun MavenPublication.configureKotlinPomAttributes(
    project: Project,
    explicitDescription: String? = null,
    packaging: String = "jar",
    explicitName: String? = null,
) = configureKotlinPomAttributes(project, project.provider { explicitDescription }, packaging, project.provider { explicitName })

fun MavenPublication.configureKotlinPomAttributes(
    project: Project,
    explicitDescription: Provider<String>,
    packaging: String = "jar",
    explicitName: Provider<String>,
) {
    val publication = this
    pom {
        this.packaging = packaging
        name.set(explicitName.orElse(humanReadableName(publication.artifactId)))
        description.set(explicitDescription.orElse(project.description ?: humanReadableName(publication.artifactId)))
        url.set("https://kotlinlang.org/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        scm {
            url.set("https://github.com/JetBrains/kotlin")
            connection.set("scm:git:https://github.com/JetBrains/kotlin.git")
            developerConnection.set("scm:git:https://github.com/JetBrains/kotlin.git")
        }
        developers {
            developer {
                name.set("Kotlin Team")
                organization.set("JetBrains")
                organizationUrl.set("https://www.jetbrains.com")
            }
        }
    }
}

val Project.signLibraryPublication: Boolean
    get() = project.providers.gradleProperty("signingRequired").orNull?.toBoolean()
        ?: project.providers.gradleProperty("kotlin.build.signing-required").orNull?.toBoolean()
        ?: false

// overload for Groovy
fun Project.configureDefaultPublishing() = configureDefaultPublishing(signingRequired = signLibraryPublication)

fun Project.configureDefaultPublishing(
    signingRequired: Boolean = signLibraryPublication,
) {
    configure<PublishingExtension> {
        repositories {
            maven {
                name = KotlinBuildPublishingPlugin.REPOSITORY_NAME

                val repo: String? = project.properties["kotlin.build.deploy-repo"]?.toString() ?: project.properties["deploy-repo"]?.toString()
                val repoProvider = when (repo) {
                    "sonatype-nexus-staging" -> "sonatype"
                    "sonatype-nexus-snapshots" -> "sonatype"
                    else -> repo
                }

                val deployRepoUrl = (project.properties["kotlin.build.deploy-url"] ?: project.properties["deploy-url"])?.toString()?.takeIf { it.isNotBlank() }

                val sonatypeSnapshotsUrl = "https://oss.sonatype.org/content/repositories/snapshots/".takeIf { repo == "sonatype-nexus-snapshots" }

                val repoUrl: String by extra(
                    (deployRepoUrl ?: sonatypeSnapshotsUrl ?: "file://${
                        project.rootProject.layout.buildDirectory.dir("repo").get().asFile
                    }").toString()
                )

                val username: String? by extra(
                    project.properties["kotlin.build.deploy-username"]?.toString() ?: project.properties["kotlin.${repoProvider}.user"]?.toString()
                )
                val password: String? by extra(
                    project.properties["kotlin.build.deploy-password"]?.toString() ?: project.properties["kotlin.${repoProvider}.password"]?.toString()
                )

                setUrl(repoUrl)
                if (url.scheme != "file" && username != null && password != null) {
                    credentials {
                        this.username = username
                        this.password = password
                    }
                }
            }
        }
    }

    if (signingRequired) {
        apply<SigningPlugin>()
        configureSigning()
    }

    // whitelist of source sets checked for transitive dependencies to be published
    val allowedSourceSetNames = listOf(
        "common",
        "commonMain",
        "jsMain",
        "jvmMain",
        "main",
        "gradle80",
        "gradle81",
        "gradle811",
        "gradle82",
        "gradle85",
        "gradle86",
        "gradle88",
        "jvmJava9",
        "jvmMainJdk7",
        "jvmMainJdk8",
        "nativeWasmMain",
        "wasmCommonMain",
        "wasmJsMain",
        "wasmWasiMain",
        "java9",
        "annotationsCommonMain",
        "assertionsCommonMain",
        "jvmJUnit",
        "jvmJUnit5",
        "jvmTestNG",
    )
    val excludedSourceSetNames = listOf( // checks compilation name not source set name
        KotlinCompilation.TEST_COMPILATION_NAME,
        org.gradle.internal.component.external.model.TestFixturesSupport.TEST_FIXTURE_SOURCESET_NAME,
        "functionalTest",
        "moduleTest",
        "compileOnlyDeclarations", // jvm prefix
        "longRunningTest", // jvm prefix
        "JUnit5Test",
        "TestNGTest",
        "JUnitTest",
    )

    tasks.register("install") {
        group = "publishing"
        description = "Installs the artifacts and itto the local Maven repository."
        dependsOn(tasks.named("publishToMavenLocal")) // depend on publishing this module

        val installTask = this
        pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val kotlinExtension = project.extensions.getByType<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>()
            kotlinExtension.sourceSets.configureEach sourceSet@{
                kotlinExtension.targets.configureEach target@{
                    val kotlinCompilations = compilations.filter { it.allKotlinSourceSets.contains(this@sourceSet) }
                    // use an heuristic that if there's any non-test compilation having this source set, its dependencies should be published
                    val filtered = kotlinCompilations.filter { !excludedSourceSetNames.contains(it.name) }
                    val isMainSourceSet = kotlinCompilations.any { !excludedSourceSetNames.contains(it.name) }
                    if (!isMainSourceSet) return@target
                    require(allowedSourceSetNames.contains(this@sourceSet.name)) {
                        "Source set ${this@sourceSet.name} isn't added to allowed ones. Please add it either to excluded or allowed source set names. ${filtered}"
                    }
                    println("KOTLIN_MPP Processing source set ${this@sourceSet.name} in project ${project.name}...")
                    val configurationsAddingRuntimeDeps =
                        listOf(implementationConfigurationName, apiConfigurationName, runtimeOnlyConfigurationName)
                    configurationsAddingRuntimeDeps.forEach { configurationName ->
                        println("KOTLIN_MPP Processing configuration $configurationName in project ${project.name}...")
                        configurations.named(configurationName) {
                            require(isCanBeDeclared) {
                                "Configuration ${this@sourceSet.name} is expected to be declarable."
                            }
                            dependencies.withType<ProjectDependency> {
                                println("KOTLIN_MPP Adding dependency on ${path} to install task from ${installTask.path} (${this@sourceSet.name})")
                                installTask.dependsOn("${path}:install")
                            }
                        }
                    }
                }
            }
        }
        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            val kotlinExtension = project.extensions.getByType<KotlinJvmProjectExtension>()
            kotlinExtension.sourceSets.configureEach sourceSets@{
                val kotlinCompilations = kotlinExtension.target.compilations.filter { it.allKotlinSourceSets.contains(this) }
                // use an heuristic that if there's any non-test compilation having this source set, its dependencies should be published
                val isMainSourceSet = kotlinCompilations.any { !excludedSourceSetNames.contains(it.name) }
                if (!isMainSourceSet) return@sourceSets
                require(allowedSourceSetNames.contains(name)) {
                    "Source set $name isn't added to allowed ones. Please add it either to excluded or allowed source set names."
                }
                println("KOTLIN Processing source set $name in project ${project.name}...")
                val configurationsAddingRuntimeDeps =
                    listOf(implementationConfigurationName, apiConfigurationName, runtimeOnlyConfigurationName)
                configurationsAddingRuntimeDeps.forEach { configurationName ->
                    println("KOTLIN Processing configuration $configurationName in project ${project.name}...")
                    configurations.named(configurationName) {
                        require(isCanBeDeclared) {
                            "Configuration $configurationName is expected to be declarable."
                        }
                        dependencies.withType<ProjectDependency> {
                            println("KOTLIN Adding dependency on ${path} to install task from ${installTask.path} (${this@sourceSets.name})")
                            installTask.dependsOn("${path}:install")
                        }
                    }
                }
            }
        }
        pluginManager.withPlugin("java") {
            val configurationsAddingRuntimeDeps = listOf("implementation", "runtimeOnly")
            configurationsAddingRuntimeDeps.forEach { configurationName ->
                println("JAVA Processing configuration $configurationName in project ${project.name}...")
                configurations.named(configurationName) {
                    require(isCanBeDeclared) {
                        "Configuration $name is expected to be declarable."
                    }
                    dependencies.withType<ProjectDependency> {
                        println("JAVA Adding dependency on ${path} to install task from ${installTask.path}")
                        installTask.dependsOn("${path}:install")
                    }
                }
            }
        }
        pluginManager.withPlugin("java-library") {
            val configurationsAddingRuntimeDeps = listOf("implementation", "api", "runtimeOnly")
            configurationsAddingRuntimeDeps.forEach { configurationName ->
                println("JAVA_LIBRARY Processing configuration $configurationName in project ${project.name}...")
                configurations.named(configurationName) {
                    require(isCanBeDeclared) {
                        "Configuration $name is expected to be declarable."
                    }
                    dependencies.withType<ProjectDependency> {
                        println("JAVA_LIBRARY Adding dependency on ${path} to install task from ${installTask.path}")
                        installTask.dependsOn("${path}:install")
                    }
                }
            }
        }
    }.also {
        rootProject.tasks.named("mvnInstall").configure {
            dependsOn(it)
        }
    }
}

private fun Project.getSensitiveProperty(name: String): String? {
    return project.findProperty(name) as? String ?: System.getenv(name)
}

private fun Project.configureSigning() {
    configure<SigningExtension> {
        sign(extensions.getByType<PublishingExtension>().publications) // all publications

        val signKeyId = project.getSensitiveProperty("signKeyId")
        if (!signKeyId.isNullOrBlank()) {
            val signKeyPrivate = project.getSensitiveProperty("signKeyPrivate") ?: error("Parameter `signKeyPrivate` not found")
            val signKeyPassphrase = project.getSensitiveProperty("signKeyPassphrase") ?: error("Parameter `signKeyPassphrase` not found")
            useInMemoryPgpKeys(signKeyId, signKeyPrivate, signKeyPassphrase)
        } else {
            useGpgCmd()
        }
    }
}
