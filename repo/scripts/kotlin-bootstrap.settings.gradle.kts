/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Apply this settings script in the project settings.gradle following way:
// pluginManagement {
//    apply from: 'kotlin-bootstrap.settings.gradle.kts'
// }

import java.util.Properties
import org.gradle.api.internal.GradleInternal

object Config {
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

    const val IS_JPS_BUILD_ENABLED = "jpsBuild"
}

abstract class PropertiesValueSource : ValueSource<Properties, PropertiesValueSource.Parameters> {
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

fun getRootSettings(
    settings: Settings,
    gradle: Gradle
): Settings {
    // Gradle interface neither exposes flag if it is a root of composite-build hierarchy
    // nor gives access to Settings object. Fortunately it is availaibe inside GradleInternal internal api
    // used by build scan plugin.
    //
    // Included builds for build logic are evaluated earlier then root settings leading to error that root settings object is not yet
    // available. For such cases we fallback to included build settings object and later manual mapping for kotlinRootDir
    val gradleInternal = (gradle as GradleInternal)
    return when {
        gradleInternal.isRootBuild() ||
                setOf("gradle-settings-conventions", "gradle-build-conventions").contains(settings.rootProject.name) -> {
            settings
        }
        else -> {
            val gradleParent = gradle.parent ?: error("Could not get includedBuild parent build for ${settings.rootDir}!")
            getRootSettings(gradleParent.settings, gradleParent)
        }
    }
}

val rootSettings = getRootSettings(settings, settings.gradle)

// Workaround for the case when included build could be run directly via --project-dir option.
// In this case `rootSettings.rootDir` will point to --project-dir location rather then Kotlin repo real root.
// So in such case script falls back to manual mapping
val kotlinRootDir: File = when (rootSettings.rootProject.name) {
    "buildSrc" -> {
        val parentDir = rootSettings.rootDir.parentFile
        when (parentDir.name) {
            "benchmarksAnalyzer", "performance-server" -> parentDir.parentFile.parentFile.parentFile
            "performance" -> parentDir.parentFile.parentFile
            "ui" -> parentDir.parentFile.parentFile.parentFile.parentFile
            else -> parentDir
        }
    }
    "benchmarksAnalyzer", "performance-server" -> rootSettings.rootDir.parentFile.parentFile.parentFile
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

fun loadLocalOrGradleProperty(
    propertyName: String
): Provider<String> {
    // Workaround for https://github.com/gradle/gradle/issues/19114
    // as in the includedBuild GradleProperties are empty on configuration cache reuse
    return if ((gradle as GradleInternal).isRootBuild()) {
        localProperties.map { it.getProperty(propertyName) }
            .orElse(providers.gradleProperty(propertyName))
            .orElse(rootGradleProperties.map { it.getProperty(propertyName) })
    } else {
        localProperties.map { it.getProperty(propertyName) }
            .orElse(rootSettings.providers.gradleProperty(propertyName))
            .orElse(rootGradleProperties.map { it.getProperty(propertyName) })
    }
}

fun Project.logBootstrapApplied(message: String) {
    if (this == rootProject) logger.lifecycle(message) else logger.info(message)
}

fun String?.propValueToBoolean(default: Boolean = false): Boolean {
    return when {
        this == null -> default
        isEmpty() -> true // has property without value means 'true'
        else -> trim().toBoolean()
    }
}

fun Provider<String>.mapToBoolean(): Provider<Boolean> = map { it.propValueToBoolean() }

fun RepositoryHandler.addBootstrapRepo(
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
            // kotlin-build-gradle-plugin and non bootstrap-versions
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

@OptIn(kotlin.ExperimentalStdlibApi::class)
fun getAdditionalBootstrapRepos(
    bootstrapRepo: String,
    bootstrapKotlinVersion: String,
    isJpsBuildEnabled: Boolean
): List<String> {
    return buildList {
        if (bootstrapRepo.startsWith("https://buildserver.labs.intellij.net")
                || bootstrapRepo.startsWith("https://teamcity.jetbrains.com")) {
            add(bootstrapRepo.replace("artifacts/content/maven", "artifacts/content/internal/repo"))
        }

        if (isJpsBuildEnabled) {
            add(
                "https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:Kotlin_KotlinPublic_Aggregate)," +
                        "number:$bootstrapKotlinVersion,branch:default:any/artifacts/content/internal/repo/"
            )
        }
    }
}

fun Settings.applyBootstrapConfiguration(
    bootstrapVersion: String,
    bootstrapRepo: String,
    isJpsBuildEnabled: Boolean,
    logMessage: String
) {
    settings.pluginManagement.repositories.addBootstrapRepo(bootstrapRepo, bootstrapVersion)
    settings.pluginManagement.resolutionStrategy.eachPlugin {
        if (requested.id.id.startsWith("org.jetbrains.kotlin.")) {
            useVersion(bootstrapVersion)
        }
    }

    val additionalRepos = getAdditionalBootstrapRepos(bootstrapRepo, bootstrapVersion, isJpsBuildEnabled)
    gradle.beforeProject {
        bootstrapKotlinVersion = bootstrapVersion
        bootstrapKotlinRepo = bootstrapRepo

        repositories.addBootstrapRepo(bootstrapRepo, bootstrapVersion, additionalRepos)

        logBootstrapApplied(logMessage)
    }
}

val isLocalBootstrapEnabled: Provider<Boolean> = loadLocalOrGradleProperty(Config.LOCAL_BOOTSTRAP)
    .mapToBoolean().orElse(false)

val localBootstrapVersion: Provider<String> = loadLocalOrGradleProperty(Config.LOCAL_BOOTSTRAP_VERSION)
    .orElse(loadLocalOrGradleProperty(Config.DEFAULT_SNAPSHOT_VERSION))

val localBootstrapPath: Provider<String> = loadLocalOrGradleProperty(Config.LOCAL_BOOTSTRAP_PATH)
val teamCityBootstrapVersion = loadLocalOrGradleProperty(Config.TEAMCITY_BOOTSTRAP_VERSION)
val teamCityBootstrapBuildNumber = loadLocalOrGradleProperty(Config.TEAMCITY_BOOTSTRAP_BUILD_NUMBER)
val teamCityBootstrapProject = loadLocalOrGradleProperty(Config.TEAMCITY_BOOTSTRAP_PROJECT)
val teamCityBootstrapUrl = loadLocalOrGradleProperty(Config.TEAMCITY_BOOTSTRAP_URL)
val customBootstrapVersion = loadLocalOrGradleProperty(Config.CUSTOM_BOOTSTRAP_VERSION)
val customBootstrapRepo = loadLocalOrGradleProperty(Config.CUSTOM_BOOTSTRAP_REPO)
val defaultBootstrapVersion = loadLocalOrGradleProperty(Config.DEFAULT_BOOTSTRAP_VERSION)
val isJpsBuildEnabled = loadLocalOrGradleProperty(Config.IS_JPS_BUILD_ENABLED)
    .mapToBoolean().orElse(false)

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

// Get bootstrap kotlin version and repository url
// and set it using pluginManagement and dependencyManangement
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
            isJpsBuildEnabled.get(),
            "Using Kotlin local bootstrap version $bootstrapVersion from $bootstrapRepo"
        )
    }
    teamCityBootstrapVersion.orNull != null -> {
        val bootstrapVersion = teamCityBootstrapVersion.get()

        val query = "branch:default:any"
        val baseRepoUrl = teamCityBootstrapUrl.orNull ?: "https://buildserver.labs.intellij.net"
        val teamCityProjectId = teamCityBootstrapProject.orNull ?: "Kotlin_KotlinDev_Compiler"
        val teamCityBuildNumber = teamCityBootstrapBuildNumber.orNull ?: bootstrapVersion

        val bootstrapRepo = "$baseRepoUrl/guestAuth/app/rest/builds/buildType:(id:$teamCityProjectId),number:$teamCityBuildNumber,$query/artifacts/content/maven/"

        applyBootstrapConfiguration(
            bootstrapVersion,
            bootstrapRepo,
            isJpsBuildEnabled.get(),
            "Using Kotlin TeamCity bootstrap version $bootstrapVersion from $bootstrapRepo"
        )
    }
    customBootstrapVersion.orNull != null -> {
        val bootstrapVersion = customBootstrapVersion.get()
        val bootstrapRepo = customBootstrapRepo.get()

        applyBootstrapConfiguration(
            bootstrapVersion,
            bootstrapRepo,
            isJpsBuildEnabled.get(),
            "Using Kotlin custom bootstrap version $bootstrapVersion from $bootstrapRepo"
        )
    }
    else -> {
        val bootstrapVersion = defaultBootstrapVersion.get()
        val bootstrapRepo = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap"

        applyBootstrapConfiguration(
            bootstrapVersion,
            bootstrapRepo,
            isJpsBuildEnabled.get(),
            "Using Kotlin Space bootstrap version $bootstrapVersion from $bootstrapRepo"
        )
    }
}
