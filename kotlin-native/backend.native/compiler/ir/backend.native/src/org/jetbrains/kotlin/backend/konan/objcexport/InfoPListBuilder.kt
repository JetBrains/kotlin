/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.konan.target.AppleConfigurables
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.platformName
import org.jetbrains.kotlin.name.Name

/**
 * Creates an Info.plist file for an Apple framework.
 *
 * Some documentation about its contents can be found here:
 * https://developer.apple.com/library/archive/documentation/General/Reference/InfoPlistKeyReference/Introduction/Introduction.html
 */
internal class InfoPListBuilder(
        private val config: KonanConfig,
) {
    private val configuration = config.configuration

    fun build(
            name: String,
            mainPackageGuesser: MainPackageGuesser,
            moduleDescriptor: ModuleDescriptor,
    ): String {
        val bundleId = computeBundleID(name, mainPackageGuesser, moduleDescriptor)

        val bundleShortVersionString = configuration[BinaryOptions.bundleShortVersionString] ?: "1.0"
        val bundleVersion = configuration[BinaryOptions.bundleVersion] ?: "1"
        val properties = config.platform.configurables as AppleConfigurables
        val platform = properties.platformName()
        val minimumOsVersion = properties.osVersionMin

        val contents = StringBuilder()
        contents.append("""
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>CFBundleExecutable</key>
                <string>$name</string>
                <key>CFBundleIdentifier</key>
                <string>$bundleId</string>
                <key>CFBundleInfoDictionaryVersion</key>
                <string>6.0</string>
                <key>CFBundleName</key>
                <string>$name</string>
                <key>CFBundlePackageType</key>
                <string>FMWK</string>
                <key>CFBundleShortVersionString</key>
                <string>$bundleShortVersionString</string>
                <key>CFBundleSupportedPlatforms</key>
                <array>
                    <string>$platform</string>
                </array>
                <key>CFBundleVersion</key>
                <string>$bundleVersion</string>

        """.trimIndent())

        fun addUiDeviceFamilies(vararg values: Int) {
            val xmlValues = values.joinToString(separator = "\n") {
                "        <integer>$it</integer>"
            }
            contents.append("""
                |    <key>MinimumOSVersion</key>
                |    <string>$minimumOsVersion</string>
                |    <key>UIDeviceFamily</key>
                |    <array>
                |$xmlValues       
                |    </array>

                """.trimMargin())
        }
        val target = config.target
        // UIDeviceFamily mapping:
        // 1 - iPhone
        // 2 - iPad
        // 3 - AppleTV
        // 4 - Apple Watch
        when (target.family) {
            Family.IOS -> addUiDeviceFamilies(1, 2)
            Family.TVOS -> addUiDeviceFamilies(3)
            Family.WATCHOS -> addUiDeviceFamilies(4)
            else -> {}
        }

        if (target == KonanTarget.IOS_ARM64) {
            contents.append("""
                |    <key>UIRequiredDeviceCapabilities</key>
                |    <array>
                |        <string>arm64</string>
                |    </array>

                """.trimMargin()
            )
        }

        if (target == KonanTarget.IOS_ARM32) {
            contents.append("""
                |    <key>UIRequiredDeviceCapabilities</key>
                |    <array>
                |        <string>armv7</string>
                |    </array>

                """.trimMargin()
            )
        }

        contents.append("""
            </dict>
            </plist>
        """.trimIndent())

        // TODO: Xcode also add some number of DT* keys.
        return contents.toString()
    }

    private fun computeBundleID(
            bundleName: String,
            mainPackageGuesser: MainPackageGuesser,
            moduleDescriptor: ModuleDescriptor,
    ): String {
        val deprecatedBundleIdOption = configuration[KonanConfigKeys.BUNDLE_ID]
        val bundleIdOption = configuration[BinaryOptions.bundleId]
        if (deprecatedBundleIdOption != null && bundleIdOption != null && deprecatedBundleIdOption != bundleIdOption) {
            configuration.report(
                    CompilerMessageSeverity.ERROR,
                    "Both the deprecated -Xbundle-id=<id> and the new -Xbinary=bundleId=<id> options supplied with different values: " +
                            "'$deprecatedBundleIdOption' and '$bundleIdOption'. " +
                            "Please use only one of the options or make sure they have the same value."
            )
        }
        deprecatedBundleIdOption?.let { return it } ?: bundleIdOption?.let { return it }

        val mainPackage = mainPackageGuesser.guess(
                moduleDescriptor,
                moduleDescriptor.getIncludedLibraryDescriptors(config),
                moduleDescriptor.getExportedDependencies(config),
        )
        val bundleID = mainPackage.child(Name.identifier(bundleName)).asString()

        if (mainPackage.isRoot) {
            configuration.report(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "Cannot infer a bundle ID from packages of source files and exported dependencies, " +
                            "use the bundle name instead: $bundleName. " +
                            "Please specify the bundle ID explicitly using the -Xbinary=bundleId=<id> compiler flag."
            )
        }
        return bundleID
    }
}