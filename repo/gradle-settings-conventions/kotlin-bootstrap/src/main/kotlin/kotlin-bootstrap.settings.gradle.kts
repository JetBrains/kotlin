/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import java.util.Properties
import org.gradle.api.internal.GradleInternal

private object Config {
    const val LOCAL_BOOTSTRAP = "bootstrap.local"
    const val LOCAL_BOOTSTRAP_VERSION = "bootstrap.local.version"
    const val LOCAL_BOOTSTRAP_PATH = "bootstrap.local.path"

    const val TEAMCITY_BOOTSTRAP_VERSION = "bootstrap.teamcity.kotlin.version"
    const val TEAMCITY_BOOTSTRAP_BUILD_NUMBER = "bootstrap.teamcity.build.number"
    const val TEAMCITY_BOOTSTRAP_PROJECT = "bootstrap.teamcity.project"
    const val TEAMCITY_BOOTSTRAP_URL = "bootstrap.teamcity.url"

    const val CUSTOM_BOOTSTRAP_VERSION = "bootstrap.kotlin.version"
    const val CUSTOM_BOOTSTRAP_REPO = "bootstrap.kotlin.repo"

    const val DEFAULT_SNAPSHOT_VERSION = "defaultSnapshotVersion"
    const val DEFAULT_BOOTSTRAP_VERSION = "bootstrap.kotlin.default.version"

    const val PROJECT_KOTLIN_VERSION = "bootstrapKotlinVersion"
    const val PROJECT_KOTLIN_REPO = "bootstrapKotlinRepo"
}

internal abstract class PropertiesValueSource : ValueSource<Properties, PropertiesValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val fileName: Property<String>
        val rootDir: Property<File>
    }

    override fun obtain(): Properties? {
        val localPropertiesFile = parameters.rootDir.get().resolve(parameters.fileName.get())
        return if (localPropertiesFile.exists()) {
            localPropertiesFile.bufferedReader().use {
                Properties().apply { load(it) }
            }
        } else {
            null
        }
    }
}

private fun getRootSettings(
    settings: Settings,
    gradle: Gradle
): Settings {
    // Gradle interface neither exposes a flag if it is a root of composite-build hierarchy
    // nor gives access to the Settings object.
    // Fortunately, it is available inside the GradleInternal internal api used by the build scan plugin.
    //
    // Included builds for build logic are evaluated earlier than root settings,
    // leading to the error that the root settings object is not yet available.
    // For such cases, we fall back to the included build settings object and later manual mapping for kotlinRootDir.
    val gradleInternal = (gradle as GradleInternal)
    return when {
        gradleInternal.isRootBuild ||
                setOf("gradle-settings-conventions", "gradle-build-conventions").contains(settings.rootProject.name) -> {
            settings
        }
        else -> {
            val gradleParent = gradle.parent ?: error("Could not get includedBuild parent build for ${settings.rootDir}!")
            getRootSettings(gradleParent.settings, gradleParent)
        }
    }
}

private val rootSettings = getRootSettings(settings, settings.gradle)

// Workaround for the case when included build could be run directly via the '--project-dir' option.
// In this case `rootSettings.rootDir` will point to --project-dir location rather than Kotlin repo real root.
// So in such cases, the script falls back to manual mapping.
private val kotlinRootDir: File = when (rootSettings.rootProject.name) {
    "buildSrc" -> {
        val parentDir = rootSettings.rootDir.parentFile
        when (parentDir.name) {
            "benchmarksAnalyzer" -> parentDir.parentFile.parentFile.parentFile
            "performance" -> parentDir.parentFile.parentFile
            "ui" -> parentDir.parentFile.parentFile.parentFile.parentFile
            else -> parentDir
        }
    }
    "benchmarksAnalyzer" -> rootSettings.rootDir.parentFile.parentFile.parentFile
    "gradle-settings-conventions" -> rootSettings.rootDir.parentFile.parentFile
    "gradle-build-conventions" -> rootSettings.rootDir.parentFile.parentFile
    "performance" -> rootSettings.rootDir.parentFile.parentFile
    "ui" -> rootSettings.rootDir.parentFile.parentFile.parentFile.parentFile
    else -> rootSettings.rootDir
}

private val localProperties = providers.of(PropertiesValueSource::class.java) {
    parameters {
        fileName.set("local.properties")
        rootDir.set(kotlinRootDir)
    }
}

private val rootGradleProperties = providers.of(PropertiesValueSource::class.java) {
    parameters {
        fileName.set("gradle.properties")
        rootDir.set(kotlinRootDir)
    }
}

private fun loadLocalOrGradleProperty(
    propertyName: String
): Provider<String> {
    // Workaround for https://github.com/gradle/gradle/issues/19114
    // as in the includedBuild GradleProperties is empty on configuration cache reuse
    return if ((gradle as GradleInternal).isRootBuild) {
        localProperties.map { it.getProperty(propertyName) }
            .orElse(providers.gradleProperty(propertyName))
            .orElse(rootGradleProperties.map { it.getProperty(propertyName) })
    } else {
        localProperties.map { it.getProperty(propertyName) }
            .orElse(rootSettings.providers.gradleProperty(propertyName))
            .orElse(rootGradleProperties.map { it.getProperty(propertyName) })
    }
}

private fun Project.logBootstrapApplied(message: String) {
    if (this == rootProject) logger.lifecycle(message) else logger.info(message)
}

private fun String?.propValueToBoolean(default: Boolean = false): Boolean {
    return when {
        this == null -> default
        isEmpty() -> true // has property without value means 'true'
        else -> trim().toBoolean()
    }
}

private fun Provider<String>.mapToBoolean(): Provider<Boolean> = map { it.propValueToBoolean() }

private fun RepositoryHandler.addBootstrapRepo(
    bootstrapRepo: String,
    bootstrapVersion: String,
    additionalBootstrapRepos: List<String> = emptyList()
) {
    exclusiveContent {
        forRepositories(
            *(listOf(bootstrapRepo) + additionalBootstrapRepos)
                .map {
                    maven { url = uri(it) }
                }
                .toTypedArray()
        )
        filter {
            // kotlin-build-gradle-plugin and non-bootstrap versions
            // should be excluded from strict content filtering
            includeVersionByRegex(
                "org\\.jetbrains\\.kotlin",
                "^(?!kotlin-build-gradle-plugin).*$",
                bootstrapVersion.toRegex().pattern
            )

            // Kotlin Gradle plugins that have slightly separate maven coordinates
            includeVersionByRegex(
                "org\\.jetbrains\\.kotlin\\..*$",
                "org\\.jetbrains\\.kotlin\\..*\\.gradle\\.plugin$",
                bootstrapVersion.toRegex().pattern
            )
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun getAdditionalBootstrapRepos(bootstrapRepo: String): List<String> {
    return buildList {
        if (bootstrapRepo.startsWith("https://buildserver.labs.intellij.net")
            || bootstrapRepo.startsWith("https://teamcity.jetbrains.com")
        ) {
            add(bootstrapRepo.replace("artifacts/content/maven", "artifacts/content/internal/repo"))
        }
    }
}

private fun Settings.applyBootstrapConfiguration(
    bootstrapVersion: String,
    bootstrapRepo: String,
    logMessage: String
) {
    //settings.pluginManagement.repositories.addBootstrapRepo(bootstrapRepo, bootstrapVersion)
    settings.pluginManagement.resolutionStrategy.eachPlugin {
        if (requested.id.id.startsWith("org.jetbrains.kotlin.")) {
            useVersion(bootstrapVersion)
        }
    }

    //val additionalRepos = getAdditionalBootstrapRepos(bootstrapRepo)
    gradle.beforeProject {
        bootstrapKotlinVersion = bootstrapVersion
        bootstrapKotlinRepo = bootstrapRepo

        //repositories.addBootstrapRepo(bootstrapRepo, bootstrapVersion, additionalRepos)

        fun Configuration.substituteProjectsWithBootstrap() {
            if (path == ":kotlin-stdlib") {
                resolutionStrategy.dependencySubstitution {
                    substitute(module("org.jetbrains.kotlin:kotlin-stdlib"))
                        .using(project(":dependencies:bootstrap:kotlin-stdlib-bootstrap"))
                }
            } else if (path == ":kotlin-script-runtime") {
                resolutionStrategy.dependencySubstitution {
                    substitute(module("org.jetbrains.kotlin:kotlin-script-runtime"))
                        .using(project(":dependencies:bootstrap:kotlin-script-runtime-bootstrap"))
                }
            } else if (path == ":kotlin-reflect") {
                resolutionStrategy.dependencySubstitution {
                    substitute(module("org.jetbrains.kotlin:kotlin-reflect"))
                        .using(project(":dependencies:bootstrap:kotlin-reflect-bootstrap"))
                }
            } else if (path == ":kotlin-compiler-embeddable") {
                resolutionStrategy.dependencySubstitution {
                    substitute(module("org.jetbrains.kotlin:kotlin-compiler-embeddable"))
                        .using(project(":dependencies:bootstrap:kotlin-compiler-embeddable-bootstrap"))
                }
            }
        }

        configurations.configureEach {
            // Overriding the Kotlin compiler classpath
            if (name == "kotlinCompilerClasspath") {
                dependencies.add(
                    project.dependencies.enforcedPlatform("org.jetbrains.kotlin:kotlin-bom:$bootstrapVersion")
                )
                dependencies.add(
                    project.dependencies.create("org.jetbrains.kotlin:kotlin-compiler-embeddable:$bootstrapVersion")
                )
                dependencyConstraints.add(
                    project.dependencies.constraints.create("org.jetbrains.kotlin:kotlin-compiler-embeddable") {
                        version {
                            strictly(bootstrapVersion)
                        }
                    }
                )
                substituteProjectsWithBootstrap()
            }

            // Removing scripting support
            if (name == "kotlinCompilerPluginClasspath" &&
                path in listOf(
                    ":kotlin-stdlib",
                    ":kotlin-script-runtime",
                    ":kotlin-scripting-common",
                    ":kotlin-scripting-jvm",
                )
            ) {
                exclude("org.jetbrains.kotlin", "kotlin-scripting-compiler-embeddable")
            }

            // Overriding built tools API classpath
            if (name == "kotlinBuildToolsApiClasspath") {
                if (path == ":compiler:build-tools:kotlin-build-tools-api") {
                    resolutionStrategy.dependencySubstitution {
                        substitute(module("org.jetbrains.kotlin:kotlin-build-tools-api"))
                            .using(project(":dependencies:bootstrap:kotlin-build-tools-api-bootstrap"))
                    }
                } else if (path == ":kotlin-daemon-client") {
                    resolutionStrategy.dependencySubstitution {
                        substitute(module("org.jetbrains.kotlin:kotlin-daemon-client"))
                            .using(project(":dependencies:bootstrap:kotlin-daemon-client-bootstrap"))
                    }
                } else if (path == ":compiler:build-tools:kotlin-build-tools-impl") {
                    resolutionStrategy.dependencySubstitution {
                        substitute(module("org.jetbrains.kotlin:kotlin-build-tools-impl"))
                            .using(project(":dependencies:bootstrap:kotlin-build-tools-impl-bootstrap"))
                    }
                }

                substituteProjectsWithBootstrap()
            }

            if (name == "kotlinKlibCommonizerClasspath" && path == ":kotlin-stdlib") {
                resolutionStrategy.dependencySubstitution {
                    substitute(module("org.jetbrains.kotlin:kotlin-stdlib"))
                        .using(project(":dependencies:bootstrap:kotlin-stdlib-bootstrap"))
                }
            }
        }

        logBootstrapApplied(logMessage)
    }
}

private val isLocalBootstrapEnabled: Provider<Boolean> = loadLocalOrGradleProperty(Config.LOCAL_BOOTSTRAP)
    .mapToBoolean().orElse(false)

private val localBootstrapVersion: Provider<String> = loadLocalOrGradleProperty(Config.LOCAL_BOOTSTRAP_VERSION)
    .orElse(loadLocalOrGradleProperty(Config.DEFAULT_SNAPSHOT_VERSION))

private val localBootstrapPath: Provider<String> = loadLocalOrGradleProperty(Config.LOCAL_BOOTSTRAP_PATH)
private val teamCityBootstrapVersion = loadLocalOrGradleProperty(Config.TEAMCITY_BOOTSTRAP_VERSION)
private val teamCityBootstrapBuildNumber = loadLocalOrGradleProperty(Config.TEAMCITY_BOOTSTRAP_BUILD_NUMBER)
private val teamCityBootstrapProject = loadLocalOrGradleProperty(Config.TEAMCITY_BOOTSTRAP_PROJECT)
private val teamCityBootstrapUrl = loadLocalOrGradleProperty(Config.TEAMCITY_BOOTSTRAP_URL)
private val customBootstrapVersion = loadLocalOrGradleProperty(Config.CUSTOM_BOOTSTRAP_VERSION)
private val customBootstrapRepo = loadLocalOrGradleProperty(Config.CUSTOM_BOOTSTRAP_REPO)
private val defaultBootstrapVersion = loadLocalOrGradleProperty(Config.DEFAULT_BOOTSTRAP_VERSION)

var Project.bootstrapKotlinVersion: String
    get() = property(Config.PROJECT_KOTLIN_VERSION) as String
    set(value) {
        extensions.extraProperties.set(Config.PROJECT_KOTLIN_VERSION, value)
    }

var Project.bootstrapKotlinRepo: String?
    get() = property(Config.PROJECT_KOTLIN_REPO) as String?
    set(value) {
        extensions.extraProperties.set(Config.PROJECT_KOTLIN_REPO, value)
    }

// Get the bootstrap kotlin version and repository url
// and set it using pluginManagement and dependencyManagement
when {
    isLocalBootstrapEnabled.get() -> {
        val bootstrapVersion = localBootstrapVersion.get()

        val localPath = localBootstrapPath.orNull
        val rootDirectory = kotlinRootDir
        val repoPath = if (localPath != null) {
            rootDirectory.resolve(localPath).canonicalFile
        } else {
            rootDirectory.resolve("build").resolve("repo")
        }
        val bootstrapRepo = repoPath.toURI().toString()

        applyBootstrapConfiguration(
            bootstrapVersion,
            bootstrapRepo,
            "Using Kotlin local bootstrap version $bootstrapVersion from $bootstrapRepo"
        )
    }
    teamCityBootstrapVersion.orNull != null -> {
        val bootstrapVersion = teamCityBootstrapVersion.get()

        val query = "branch:default:any"
        val baseRepoUrl = teamCityBootstrapUrl.orNull ?: "https://buildserver.labs.intellij.net"
        val teamCityProjectId = teamCityBootstrapProject.orNull ?: "Kotlin_KotlinDev_Artifacts"
        val teamCityBuildNumber = teamCityBootstrapBuildNumber.orNull ?: bootstrapVersion

        val bootstrapRepo = "$baseRepoUrl/guestAuth/app/rest/builds/buildType:(id:$teamCityProjectId)," +
                "number:$teamCityBuildNumber,$query/artifacts/content/maven.zip!/"

        applyBootstrapConfiguration(
            bootstrapVersion,
            bootstrapRepo,
            "Using Kotlin TeamCity bootstrap version $bootstrapVersion from $bootstrapRepo"
        )
    }
    customBootstrapVersion.orNull != null -> {
        val bootstrapVersion = customBootstrapVersion.get()
        val bootstrapRepo = customBootstrapRepo.get()

        applyBootstrapConfiguration(
            bootstrapVersion,
            bootstrapRepo,
            "Using Kotlin custom bootstrap version $bootstrapVersion from $bootstrapRepo"
        )
    }
    else -> {
        val bootstrapVersion = defaultBootstrapVersion.get()
        val bootstrapRepo = "https://redirector.kotlinlang.org/maven/bootstrap"

        applyBootstrapConfiguration(
            bootstrapVersion,
            bootstrapRepo,
            "Using Kotlin Space bootstrap version $bootstrapVersion from $bootstrapRepo"
        )
    }
}
