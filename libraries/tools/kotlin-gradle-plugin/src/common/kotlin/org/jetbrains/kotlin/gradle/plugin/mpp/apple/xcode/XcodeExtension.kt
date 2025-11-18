/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.xcode

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * The main entry point for the Xcode plugin configuration.
 *
 * Example Usage:
 * ```kotlin
 * xcode {
 *     projectPath.set(file("iosApp.xcodeproj"))
 *     scheme.set("iosApp")
 *     simulators {
 *         register("iPhone15") {
 *             deviceType.set("iPhone 15")
 *             osVersion.set("iOS 17.2")
 *         }
 *     }
 * }
 * ```
 */
abstract class XcodeExtension @Inject constructor(objects: ObjectFactory) {

    /** The path to the .xcodeproj or .xcworkspace directory. */
    abstract val projectPath: DirectoryProperty

    /** The Scheme to build (e.g., "iosApp"). */
    abstract val scheme: Property<String>

    /** The build configuration (e.g., "Debug", "Release"). Defaults to "Debug". */
    abstract val configuration: Property<String>

    /**
     * Container for configuring Simulator targets.
     * These are virtual devices managed by `xcrun simctl`.
     */
    val simulators: NamedDomainObjectContainer<SimulatorTarget> =
        objects.domainObjectContainer(SimulatorTarget::class.java)

    /**
     * Container for configuring Physical Device targets.
     * These are hardware devices connected via USB/WiFi.
     */
    val devices: NamedDomainObjectContainer<DeviceTarget> =
        objects.domainObjectContainer(DeviceTarget::class.java)

    init {
        // Set safe defaults
        configuration.convention("Debug")
    }
}

/**
 * Represents a virtual simulator device configuration.
 */
abstract class SimulatorTarget(private val name: String) : Named {
    override fun getName(): String = name

    /**
     * The device type identifier used by Apple.
     * Run `xcrun simctl list devicetypes` to see options.
     * Example: "iPhone 15"
     */
    abstract val deviceType: Property<String>

    /**
     * The OS version for the runtime.
     * Run `xcrun simctl list runtimes` to see options.
     * Example: "iOS 17.2"
     */
    abstract val osVersion: Property<String>
}

/**
 * Represents a physical hardware device configuration.
 */
abstract class DeviceTarget(private val name: String) : Named {
    override fun getName(): String = name

    /**
     * The Unique Device Identifier (UDID).
     * Run `xcrun devicectl list devices` or `xcodebuild -showdestinations` to find this.
     */
    abstract val udid: Property<String>

    /**
     * Optional friendly model name for logging/reporting.
     * Example: "My iPhone 14 Pro"
     */
    abstract val modelName: Property<String>
}