package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

/**
 * Instance which describes specific runtimes for JS and Wasm targets
 *
 * It encapsulates necessary information about a tool to run application and tests
 */
abstract class EnvSpec<T : AbstractEnv> {

    /**
     * Specify whether we need to download the tool
     */
    abstract val download: Property<Boolean>

    /**
     * Specify url to add repository from which the tool is going to be downloaded
     *
     * If the property has no value, repository is not added,
     * so this can be used to add your own repository where the tool is located
     */
    abstract val downloadBaseUrl: Property<String>

    /**
     * Indicates whether the use of an insecure protocol is allowable for downloading the tool.
     *
     * This property determines if insecure protocols (such as HTTP instead of HTTPS) can be permitted
     * for downloading the required tool.
     */
    abstract val allowInsecureProtocol: Property<Boolean>

    /**
     * Specify where the tool is installed
     */
    abstract val installationDirectory: DirectoryProperty

    /**
     * Specify a version of the tool is installed
     */
    abstract val version: Property<String>

    /**
     * Specify a command to run the tool
     */
    abstract val command: Property<String>

    /**
     * Full serializable cache-friendly entity without Gradle Provider API
     */
    internal abstract val env: Provider<T>

    /**
     * Provider with full executable path
     */
    abstract val executable: Provider<String>

    /**
     * Produce  full serializable cache-friendly entity without Gradle Provider API
     */
    protected abstract fun produceEnv(): Provider<T>
}
