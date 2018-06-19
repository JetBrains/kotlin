package org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeComponent
import org.jetbrains.kotlin.gradle.plugin.experimental.internal.AbstractKotlinNativeComponent
import org.jetbrains.kotlin.konan.target.KonanTarget

interface KotlinNativeSourceSet: Named {

    val kotlin: SourceDirectorySet
    val component: KotlinNativeComponent

    /** Returns sources added in the source set via an expectedBy dependency. */
    fun getCommonMultiplatformSources(): FileCollection

    /** Returns sources common for all native targets. */
    fun getCommonNativeSources(): SourceDirectorySet

    /** Returns native sources specific for the given target. */
    fun getPlatformSources(target: KonanTarget): SourceDirectorySet

    /** Returns all (common multiplatform, common native and target-specific) sources for the given target. */
    fun getAllSources(target: KonanTarget): FileCollection

    fun kotlin(configureClosure: Closure<*>): KotlinNativeSourceSet
    fun kotlin(configureAction: Action<in SourceDirectorySet>): KotlinNativeSourceSet
    fun kotlin(configureLambda: SourceDirectorySet.() -> Unit): KotlinNativeSourceSet

    fun component(configureClosure: Closure<*>): KotlinNativeSourceSet
    fun component(configureAction: Action<in AbstractKotlinNativeComponent>): KotlinNativeSourceSet
    fun component(configureLambda: AbstractKotlinNativeComponent.() -> Unit): KotlinNativeSourceSet

    fun target(target: String): SourceDirectorySet
    fun target(vararg targets: String): KotlinNativeSourceSet
    fun target(vararg targets: String, configureClosure: Closure<*>): KotlinNativeSourceSet
    fun target(vararg targets: String, configureAction: Action<in SourceDirectorySet>): KotlinNativeSourceSet
    fun target(vararg targets: String, configureLambda: SourceDirectorySet.() -> Unit): KotlinNativeSourceSet
    fun target(targets: Iterable<String>): KotlinNativeSourceSet
    fun target(targets: Iterable<String>, configureClosure: Closure<*>): KotlinNativeSourceSet
    fun target(targets: Iterable<String>, configureAction: Action<in SourceDirectorySet>): KotlinNativeSourceSet
    fun target(targets: Iterable<String>, configureLambda: SourceDirectorySet.() -> Unit): KotlinNativeSourceSet
}
