package org.jetbrains.kotlin.gradle.plugin.experimental

import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.language.BinaryCollection
import org.gradle.language.ComponentWithBinaries
import org.gradle.language.ComponentWithDependencies
import org.gradle.nativeplatform.test.TestSuiteComponent
import org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets.KotlinNativeSourceSet
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 *  Class representing a Kotlin/Native component: application or library (both klib and dynamic)
 *  built for different targets.
 */
interface KotlinNativeComponent: ComponentWithBinaries, ComponentWithDependencies {

    /**
     * Defines the source files or directories of this component. You can add files or directories to this collection.
     * When a directory is added, all source files are included for compilation. When this collection is empty,
     * the directory src/main/kotlin is used by default.
     */
    val sources: KotlinNativeSourceSet

    /** Specifies Kotlin/Native targets used to build this component. */
    val konanTargets: SetProperty<KonanTarget>

    /** Returns the binaries for this library. */
    override fun getBinaries(): BinaryCollection<out KotlinNativeBinary>

    /** Returns the implementation dependencies of this component. */
    fun getImplementationDependencies(): Configuration

    // region DSL

    /** Set native targets for this component. */
    fun target(vararg targets: String)

    /** Set additional compiler options for this component. */
    val extraOpts: Collection<String>

    fun extraOpts(vararg values: Any)
    fun extraOpts(values: List<Any>)

    // endregion
}

/**
 * Class representing a test suite for Kotlin/Native
 */
interface KotlinNativeTestComponent : KotlinNativeComponent, TestSuiteComponent {
    val testedComponent: KotlinNativeComponent

    override fun getTestBinary(): Provider<KotlinNativeTestExecutable>
}