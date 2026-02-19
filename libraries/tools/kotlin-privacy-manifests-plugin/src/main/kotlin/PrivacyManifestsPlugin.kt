package org.jetbrains.kotlin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.tasks.FatFrameworkTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import kotlin.concurrent.withLock

class PrivacyManifestsPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            (target.kotlinExtension as ExtensionAware).extensions.create(
                PrivacyManifest::class.java,
                "privacyManifest",
                PrivacyManifest::class.java,
            )
        }
    }

}

abstract class PrivacyManifest @Inject constructor(
    private val project: Project,
) {
    class PrivacyManifestConfiguration(
        val privacyManifest: File,
        val resourceBundleInfoPlist: File?,
        val resourceBundleName: String,
    )

    internal var configuration: PrivacyManifestConfiguration? = null
    internal var onConfigurationChange: ((PrivacyManifestConfiguration) -> (Unit))? = null
    private var alreadyConfigured = false


    /**
     * Sets up privacy manifest (PrivacyInfo.xcprivacy file) copying. How the privacy manifest is copied depends on the type of integration:
     * 1. In case of embedAndSignAppleFrameworkForXcode integration, privacy manifest will be copied to a separate bundle in the application
     * 2. In case of local development pod integration, privacy manifest will be copied using spec.resource_bundles
     * 3. When building an XCFramework for binary distribution, the privacy manifest will be copied to the inner framework bundles
     *
     * @param privacyManifest Privacy manifest file to embed.
     * @param resourceBundleInfoPlist Path to Info.plist file for the resource bundle creation in case of embedAndSignAppleFrameworkForXcode integration.
     * @param resourceBundleName Name of the resource bundle to create in case of embedAndSignAppleFrameworkForXcode integration.
     */
    fun embed(
        privacyManifest: File,
        resourceBundleInfoPlist: File? = null,
        resourceBundleName: String = "KotlinMultiplatformPrivacyManifest",
    ) {
        if (alreadyConfigured) error("Only a single privacy manifest may be embedded")
        alreadyConfigured = true

        val runOnceService = project.gradle.sharedServices.registerIfAbsent(
            "${RunOnceService::class.simpleName}_${RunOnceService::class.java.classLoader.hashCode()}",
            RunOnceService::class.java,
        ) {}

        // Record configuration to read from KGP
        val configuration = PrivacyManifestConfiguration(
            privacyManifest = privacyManifest,
            resourceBundleInfoPlist = resourceBundleInfoPlist,
            resourceBundleName = resourceBundleName,
        )
        this.configuration = configuration
        onConfigurationChange?.let { it(configuration) }

        // This property will be set by KGP in the future
        if (project.extensions.extraProperties.has(disablePrivacyManifestsPluginProperty)) return

        // In case of syncFramework use CocoaPods resource bundles
        configureSyncFrameworkTaskResourcesBundle(resourceBundleName, privacyManifest)

        // Heuristically infer for execution time whether we are in embedAndSign or syncFramework
        val executesInEmbedAndSignContext = project.objects.property(Boolean::class.java)
        val executesInSyncFrameworkContext = project.objects.property(Boolean::class.java)
        project.gradle.taskGraph.whenReady { graph ->
            executesInEmbedAndSignContext.set(
                graph.allTasks.any { task ->
                    task.name.startsWith("embedAndSign") && task.name.endsWith("AppleFrameworkForXcode")
                }
            )
            executesInSyncFrameworkContext.set(
                graph.allTasks.any { task ->
                    task.name == "syncFramework"
                }
            )
        }

        project.tasks.withType(KotlinNativeLink::class.java).configureEach { linkTask ->
            if (linkTask.binary is Framework) {
                val targetBuildDir = project.providers.environmentVariable("TARGET_BUILD_DIR").orElse("")
                val resourcesPath = project.providers.environmentVariable("UNLOCALIZED_RESOURCES_FOLDER_PATH").orElse("")
                val wrapperExtension = project.providers.environmentVariable("WRAPPER_EXTENSION").orElse("")
                val resourcesBundlePath = targetBuildDir.flatMap { targetBuildDirPath ->
                    resourcesPath.map { resourcesDir ->
                        File(targetBuildDirPath)
                            .resolve(resourcesDir)
                            .resolve("${resourceBundleName}.bundle")
                    }
                }
                linkTask.inputs.file(privacyManifest)
                resourceBundleInfoPlist?.let { linkTask.inputs.file(it) }

                val isInXcodeBuild = System.getenv("TARGET_BUILD_DIR") != null
                val isInSyncFrameworkCocoaPodsXcodeBuild = System.getenv("PODS_TARGET_SRCROOT") != null
                val isLikelyInEmbedAndSign = isInXcodeBuild && !isInSyncFrameworkCocoaPodsXcodeBuild
                if (isLikelyInEmbedAndSign) {
                    linkTask.usesService(runOnceService)
                    linkTask.inputs.property("targetBuildDir", targetBuildDir)
                    linkTask.inputs.property("resourcesPath", resourcesPath)
                    linkTask.inputs.property("wrapperExtension", wrapperExtension)
                    linkTask.outputs.dir(resourcesBundlePath)
                }

                val konanTarget = linkTask.binary.target.konanTarget
                val frameworkPath = linkTask.outputFile
                linkTask.doLast {
                    // Skip if running embedAndSign in app extension
                    if (wrapperExtension.get() == "appex") return@doLast
                    // Don't do anything if we are in syncFramework
                    if (executesInSyncFrameworkContext.get()) return@doLast
                    // If we are in embedAndSign, create a resources bundle with Info.plist regardless of linkage
                    if (executesInEmbedAndSignContext.get()) {
                        if (!isLikelyInEmbedAndSign) {
                            error("Privacy manifest copying was not configured for embedAndSign integration, but runs with embedAndSign task in the task graph")
                        }

                        val resourcesBundle = resourcesBundlePath.get()
                        val resourcesDirectory: File
                        val infoPlistDirectory: File
                        if (konanTarget.family == Family.OSX) {
                            resourcesDirectory = resourcesBundle.resolve("Contents/Resources")
                            infoPlistDirectory = resourcesBundle.resolve("Contents")
                        } else {
                            resourcesDirectory = resourcesBundle
                            infoPlistDirectory = resourcesBundle
                        }
                        // Create resources bundle with privacy manifest only once if KotlinNativeLink tasks run concurrent with CC
                        runOnceService.get().doOncePerBuild {
                            createResourcesBundleWithPrivacyManifest(
                                privacyManifest = privacyManifest,
                                resourceBundleInfoPlist = resourceBundleInfoPlist,
                                resourceBundleName = resourceBundleName,
                                resourcesDirectory = resourcesDirectory,
                                infoPlistDirectory = infoPlistDirectory,
                            )
                        }
                    } else {
                        // Otherwise we are likely building an XCFramework. Copy the privacy manifest to the .framework that will be bundled in the .xcframework
                        copyPrivacyManifestToFramework(
                            frameworkPath = frameworkPath.get(),
                            privacyManifest = privacyManifest,
                            target = konanTarget
                        )
                    }
                }
            }
        }

        project.tasks.withType(FatFrameworkTask::class.java).configureEach { fatFrameworkTask ->
            fatFrameworkTask.inputs.file(privacyManifest)
            fatFrameworkTask.doLast {
                if (executesInSyncFrameworkContext.get()) return@doLast
                if (executesInEmbedAndSignContext.get()) return@doLast
                // We are likely building XCFramework again, but with a universal slice. Copy the privacy manifest to the .framework
                copyPrivacyManifestToFramework(
                    frameworkPath = fatFrameworkTask.fatFramework,
                    privacyManifest = privacyManifest,
                    target = fatFrameworkTask.frameworks.first().target
                )
            }
        }
    }

    private fun configureSyncFrameworkTaskResourcesBundle(resourceBundleName: String, privacyManifest: File) {
        project.pluginManager.withPlugin("org.jetbrains.kotlin.native.cocoapods") {
            project.afterEvaluate {
                project.afterEvaluate {
                    val cocoapodsExtension = ((project.kotlinExtension as ExtensionAware).extensions.getByName("cocoapods") as CocoapodsExtension)
                    if (cocoapodsExtension.extraSpecAttributes[resourceBundles].isNullOrEmpty()) {
                        cocoapodsExtension.extraSpecAttributes[resourceBundles] = "{'${resourceBundleName}' => ['${privacyManifest.toRelativeString(project.layout.projectDirectory.asFile)}']}"
                    } else {
                        project.logger.warn("extraSpecAttributes already contains '${resourceBundles}' key. Privacy manifest will not be copied for syncFramework integration")
                    }
                }
            }
        }
    }

    companion object {
        private fun createResourcesBundleWithPrivacyManifest(
            privacyManifest: File,
            resourceBundleInfoPlist: File?,
            resourceBundleName: String,
            resourcesDirectory: File,
            infoPlistDirectory: File,
        ) {
            privacyManifest.copyTo(
                resourcesDirectory.resolve(privacyManifestName),
                overwrite = true,
            )
            val infoPlistPath = infoPlistDirectory.resolve(infoPlistName)
            if (resourceBundleInfoPlist != null) {
                resourceBundleInfoPlist.copyTo(
                    infoPlistPath,
                    overwrite = true
                )
            } else {
                infoPlistPath.writeText(
                    defaultInfoPlist(
                        bundleIdentifier = "${resourceBundleName}-resources",
                        bundleName = resourceBundleName,
                    )
                )
            }
        }

        private fun copyPrivacyManifestToFramework(
            frameworkPath: File,
            privacyManifest: File,
            target: KonanTarget,
        ) {
            if (target.family == Family.OSX) {
                privacyManifest.copyTo(
                    frameworkPath.resolve("Resources").resolve(privacyManifestName),
                    overwrite = true,
                )
            } else {
                privacyManifest.copyTo(
                    frameworkPath.resolve(privacyManifestName),
                    overwrite = true,
                )
            }
        }

        private fun defaultInfoPlist(
            bundleIdentifier: String,
            bundleName: String,
        ): String {
            return """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>CFBundleIdentifier</key>
                <string>${bundleIdentifier}</string>
                <key>CFBundleName</key>
                <string>${bundleName}</string>
                <key>CFBundleInfoDictionaryVersion</key>
                <string>6.0</string>
                <key>CFBundlePackageType</key>
                <string>BNDL</string>
                <key>CFBundleShortVersionString</key>
                <string>1.0</string>
            </dict>
            </plist>
        """.trimIndent()
        }

        private val resourceBundles = "resource_bundles"

        private val privacyManifestName = "PrivacyInfo.xcprivacy"
        private val infoPlistName = "Info.plist"

        val disablePrivacyManifestsPluginProperty = "kotlin.mpp.disablePrivacyManifestsPlugin"
    }
}

internal abstract class RunOnceService : BuildService<BuildServiceParameters.None> {
    private val lock = ReentrantLock()
    private var once = true

    fun doOncePerBuild(action: () -> (Unit)) {
        lock.withLock {
            if (once) {
                action()
                once = false
            }
        }
    }
}