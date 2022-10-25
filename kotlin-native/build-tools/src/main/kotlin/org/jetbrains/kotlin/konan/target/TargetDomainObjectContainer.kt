/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.getByType

/**
 * Associative container from a [KonanTarget] with optional [SanitizerKind] to [T].
 *
 * Serves similar purpose to [NamedDomainObjectContainer][org.gradle.api.NamedDomainObjectContainer]
 * except this is keyed on a target instead of a name. Also this implementation does not support lazy
 * creation.
 *
 * Plugin extensions can inherit from this to automatically get API suitable for `build.gradle.kts`.
 * The extension must set [factory] field for [T].
 *
 * Example usage:
 * ```
 * someExtension {
 *    allTargets {
 *        // This is a lambda inside of a T scope. Called once for each known target with their respective sanitizer.
 *    }
 *    target(someTarget) {
 *        // This is a lambda inside of a T scope. Called for `someTarget` without any sanitizer.
 *    }
 *    target(someTarget, someSanitizer) {
 *        // This is a lambda inside of a T scope. Called for `someTarget` with `someSanitizer`.
 *    }
 *    hostTarget {
 *        // This is a lambda inside of a T scope. Called for the host target without any sanitizer.
 *    }
 *    hostTarget(someSanitizer) {
 *        // This is a lambda inside of a T scope. Called for the host target with `someSanitizer`.
 *    }
 * }
 *
 * someExtension.target(someTarget) // returns T if `someTarget` was configured. Otherwise fails with UnknownDomainObjectException.
 * someExtension.allTargets // returns a Provider<List<T>> of all created configurations.
 * ```
 */
// TODO: Consider splitting out interface and the default implementation. Plugins will inherit from the interface via delegation to the implementation.
// TODO: Consider implementing everything from `NamedDomainObjectContainer` but keyed on a target instead of a name.
open class TargetDomainObjectContainer<T : Any> constructor(
        private val providerFactory: ProviderFactory,
        private val platformManager: PlatformManager,
) {
    constructor(project: Project) : this(project.providers, project.extensions.getByType<PlatformManager>())

    /**
     * How to create [T]. Must be set before using the rest of API.
     */
    lateinit var factory: (KonanTarget, SanitizerKind?) -> T

    private val targets: MutableMap<Pair<KonanTarget, SanitizerKind?>, T> = mutableMapOf()

    /**
     * Create or update configuration [T] for [target] with optional [sanitizer] and apply [action] to it.
     *
     * @param target target of the configuration
     * @param sanitizer optional sanitizer for [target]
     * @param action action to apply to the configuration
     * @return resulting configuration
     */
    fun target(target: KonanTarget, sanitizer: SanitizerKind? = null, action: Action<in T>): T {
        val key = target to sanitizer
        val element = targets.getOrPut(key) { factory(target, sanitizer) }
        action.execute(element)
        return element
    }

    /**
     * Get configuration [T] for [target] with optional [sanitizer].
     *
     * @param target target of the configuration
     * @param sanitizer optional sanitizer for [target]
     * @return resulting configuration
     * @throws UnknownDomainObjectException if configuration for [target] and [sanitizer] does not exist
     */
    fun target(target: KonanTarget, sanitizer: SanitizerKind? = null): T {
        val key = target to sanitizer
        return targets.get(key) ?: throw UnknownDomainObjectException("Configuration for $target${sanitizer.targetSuffix} does not exists")
    }

    /**
     * Create or update configurations [T] for all known targets with their sanitizers and apply [action] to each of it.
     *
     * @param action action to apply to the configuration
     * @return list of configurations
     */
    fun allTargets(action: Action<in T>): List<T> {
        return platformManager.enabled.flatMap { target ->
            val sanitizers = target.supportedSanitizers() + listOf(null)
            sanitizers.map { sanitizer ->
                this.target(target, sanitizer, action)
            }
        }
    }

    /**
     * Get all created configurations [T].
     *
     * @return [Provider] with list of all created configurations
     */
    val allTargets: Provider<List<T>> = providerFactory.provider {
        targets.values.toList()
    }

    /**
     * Create or update configuration [T] for [host target][HostManager.host] with optional [sanitizer] and apply [action] to it.
     *
     * @param sanitizer optional sanitizer for [host target][HostManager.host]
     * @param action action to apply to the configuration
     * @return resulting configuration
     */
    fun hostTarget(sanitizer: SanitizerKind? = null, action: Action<in T>): T {
        return target(HostManager.host, sanitizer, action)
    }

    /**
     * Get configuration [T] for [host target][HostManager.host] with optional [sanitizer].
     *
     * @param sanitizer optional sanitizer for [host target][HostManager.host]
     * @return resulting configuration
     * @throws UnknownDomainObjectException if configuration for [host target][HostManager.host] and [sanitizer] does not exist
     */
    fun hostTarget(sanitizer: SanitizerKind? = null): T {
        return target(HostManager.host, sanitizer)
    }

    /**
     * Get configuration [T] for [host target][HostManager.host] without sanitizer.
     *
     * @return resulting configuration
     * @throws UnknownDomainObjectException if configuration for [host target][HostManager.host] without sanitizer does not exist
     */
    val hostTarget: T
        get() = hostTarget()
}
