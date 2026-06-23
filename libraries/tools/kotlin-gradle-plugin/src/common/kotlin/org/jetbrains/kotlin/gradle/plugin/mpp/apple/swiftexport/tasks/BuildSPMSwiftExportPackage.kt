/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.*
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.gradle.utils.relativeOrAbsolute
import org.jetbrains.kotlin.gradle.utils.runCommand
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "Swift Export is experimental, so no caching for now")
internal abstract class BuildSPMSwiftExportPackage @Inject constructor(
    providerFactory: ProviderFactory,
    objectFactory: ObjectFactory,
) : DefaultTask(), UsesKotlinToolingDiagnostics {
    init {
        onlyIf { HostManager.hostIsMac }
    }

    @get:Input
    abstract val swiftApiModuleName: Property<String>

    @get:Input
    abstract val swiftLibraryName: Property<String>

    @get:Input
    abstract val target: Property<KonanTarget>

    @get:Input
    abstract val configuration: Property<String>

    @get:Input
    val deploymentTargetSettingName: Property<String> = objectFactory.property<String>().convention(
        providerFactory.environmentVariable("DEPLOYMENT_TARGET_SETTING_NAME")
    )

    @get:Internal
    val deploymentTarget: Provider<String> = deploymentTargetSettingName.flatMap {
        providerFactory.environmentVariable(it)
    }

    @get:Optional
    @get:Input
    val targetDeviceIdentifier: Property<String> = objectFactory.property<String>().convention(
        providerFactory.environmentVariable("TARGET_DEVICE_IDENTIFIER")
    )

    /**
     * Additional arguments for the Swift compilation of the synthetic package, e.g. search paths and
     * module maps making the Objective-C modules of re-exported local cinterops visible to swiftc.
     */
    @get:Input
    abstract val swiftcExtraArgs: ListProperty<String>

    /**
     * Objective-C module names of cinterop klibs of resolved dependencies that are re-exported by
     * Swift Export. The generated Swift code imports these modules, and the consuming Xcode build is
     * responsible for providing them — so the inner xcodebuild inherits the outer build's search paths
     * (see [outerBuiltProductsDir], [outerObjroot]).
     */
    @get:Input
    abstract val dependencyCinteropModuleNames: SetProperty<String>

    /**
     * The outer Xcode build's BUILT_PRODUCTS_DIR, set when KGP runs inside an Xcode Run Script phase.
     * Points the inner xcodebuild's swiftc at products that the outer Xcode build has already resolved —
     * e.g. Swift Package products providing the Objective-C modules of [dependencyCinteropModuleNames].
     * Unset when KGP runs outside Xcode, in which case no extra search paths are injected.
     */
    @get:Optional
    @get:Input
    val outerBuiltProductsDir: Property<String> = objectFactory.property<String>().convention(
        providerFactory.environmentVariable("BUILT_PRODUCTS_DIR")
    )

    /**
     * The outer Xcode build's OBJROOT — the root of `Intermediates.noindex`, where Xcode emits
     * `GeneratedModuleMaps<EffectivePlatformName>/<TargetName>.modulemap` files for SPM-resolved Clang
     * modules. Combined with [outerEffectivePlatformName], lets the inner build pass those module maps
     * to swiftc via `-Xcc -fmodule-map-file=`.
     */
    @get:Optional
    @get:Input
    val outerObjroot: Property<String> = objectFactory.property<String>().convention(
        providerFactory.environmentVariable("OBJROOT")
    )

    /**
     * The outer Xcode build's EFFECTIVE_PLATFORM_NAME (e.g. `-iphonesimulator`) — the suffix of the
     * `GeneratedModuleMaps` directory under [outerObjroot].
     */
    @get:Optional
    @get:Input
    val outerEffectivePlatformName: Property<String> = objectFactory.property<String>().convention(
        providerFactory.environmentVariable("EFFECTIVE_PLATFORM_NAME")
    )

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val packageRoot: DirectoryProperty

    @get:OutputDirectory
    abstract val packageDerivedData: DirectoryProperty

    @get:OutputDirectory
    abstract val packageBuildDir: DirectoryProperty

    @get:OutputDirectory
    val interfacesPath: DirectoryProperty = objectFactory.directoryProperty().apply {
        set(packageBuildDir.dir("dd-interfaces"))
    }

    @get:OutputDirectory
    val objectFilesPath: DirectoryProperty = objectFactory.directoryProperty().apply {
        set(packageBuildDir.dir("dd-o-files"))
    }

    @get:OutputDirectory
    val libraryFilesPath: DirectoryProperty = objectFactory.directoryProperty().apply {
        set(packageBuildDir.dir("dd-a-files"))
    }

    @get:OutputFile
    val packageLibrary: RegularFileProperty = objectFactory.fileProperty().apply {
        set(libraryFilesPath.file(swiftLibraryName.map { "lib${it}.a" }))
    }

    private val libraryTools by lazy { LibraryTools(logger) }

    private val packageRootPath get() = packageRoot.getFile()

    @TaskAction
    fun run() {
        buildSyntheticPackage()
        packObjectFilesIntoLibrary()
    }

    private fun buildSyntheticPackage() {
        val intermediatesDestination = mapOf(
            // Thin/universal object files
            "TARGET_BUILD_DIR" to objectFilesPath.getFile().absolutePath,
            // .swiftmodule interface
            "BUILT_PRODUCTS_DIR" to interfacesPath.getFile().absolutePath,
        )

        val swiftModuleName = swiftApiModuleName.get()
        val deploymentTargetSettingName = deploymentTargetSettingName.get()
        val deploymentTarget = deploymentTarget.get()

        val swiftFlags = buildList {
            /*
            We need to add -public-autolink-library flag because bridge module is imported with @_implementationOnly
            All object files will be merged in `lib${swiftApiModuleName}.a`
            More information can be found here: https://github.com/swiftlang/swift/pull/35936
             */
            addAll(listOf("-Xfrontend", "-public-autolink-library", "-Xfrontend", swiftModuleName))
            addAll(swiftcExtraArgs.get())
            addAll(outerXcodeBuildSearchPathArgs())
        }

        val buildArguments = mapOf(
            "ARCHS" to target.map { it.appleArchitecture }.get().xcodebuildArch,
            "CONFIGURATION" to configuration.get(),
            "DEPLOYMENT_TARGET_SETTING_NAME" to deploymentTargetSettingName,
            deploymentTargetSettingName to deploymentTarget,
            "OTHER_SWIFT_FLAGS" to swiftFlags.joinToString(" ")
        )

        val derivedData = packageDerivedData.getFile()

        val command = listOf(
            "xcodebuild",
            "-derivedDataPath", derivedData.relativeOrAbsolute(packageRootPath),
            "-scheme", swiftModuleName,
            "-destination", destination(),
        ) + (intermediatesDestination + buildArguments).map { (k, v) -> "$k=$v" }

        // FIXME: This will not work with dynamic libraries
        runCommand(
            command,
            logger = logger,
            processConfiguration = {
                environment().apply {
                    keys.filter {
                        AppleSdk.xcodeEnvironmentDebugDylibVars.contains(it)
                    }.forEach {
                        remove(it)
                    }
                }

                directory(packageRootPath)
            }
        )
    }

    /**
     * The generated Swift code imports the Objective-C modules of [dependencyCinteropModuleNames], and the
     * modules are provided by the consuming Xcode build (e.g. as Swift Package products declared in the
     * consumer's project). When KGP runs inside an Xcode Run Script phase, the outer build's search paths
     * and generated module maps make those modules visible to the inner swiftc.
     */
    private fun outerXcodeBuildSearchPathArgs(): List<String> {
        if (dependencyCinteropModuleNames.get().isEmpty()) return emptyList()

        val builtProductsDir = outerBuiltProductsDir.orNull
        return buildList {
            addAll(listOf("-F", builtProductsDir))
            addAll(listOf("-I", "$builtProductsDir/include"))

            // Xcode emits Clang module maps for SPM-resolved targets under
            // `$OBJROOT/GeneratedModuleMaps<EffectivePlatformName>/<TargetName>.modulemap`. Forward them
            // to the Clang importer so that `import <Name>` resolves.
            val objroot = outerObjroot.orNull
            val platformName = outerEffectivePlatformName.orNull
            val moduleMapsDir = File(objroot, "GeneratedModuleMaps$platformName")
            moduleMapsDir.listFiles { _, name -> name.endsWith(".modulemap") }?.forEach { moduleMap ->
                addAll(listOf("-Xcc", "-fmodule-map-file=${moduleMap.absolutePath}"))
            }
        }
    }

    private fun packObjectFilesIntoLibrary() {
        val objectFilePaths = objectFilesPath.asFileTree.filter {
            it.extension == "o"
        }.files.toList()

        if (objectFilePaths.isEmpty()) {
            error("Synthetic package build didn't produce any object files")
        }

        libraryTools.mergeLibraries(objectFilePaths, packageLibrary.getFile())
    }

    private fun destination(): String {
        val deviceId = targetDeviceIdentifier.orNull
        if (deviceId != null) return "id=$deviceId"

        return target.get().appleTarget.genericPlatformDestination
    }
}
