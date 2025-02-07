package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.konan.target.REMOVED_TARGET_MESSAGE

/**
 * Provides a DSL to define the structure of [KotlinSourceSets][KotlinSourceSet] inside the [KotlinSourceSetTree].
 *
 * For example, to build a custom [KotlinSourceSetTree] using a DSL call to `kotlin.applyHierarchyTemplate { .. }`
 * in a multiplatform project.
 */
@KotlinTargetsDsl
@ExperimentalKotlinGradlePluginApi
interface KotlinHierarchyBuilder {

    /**
     * The root node in the hierarchy DSL structure.
     */
    @KotlinTargetsDsl
    @ExperimentalKotlinGradlePluginApi
    interface Root : KotlinHierarchyBuilder {

        /**
         * Defines the [KotlinSourceSetTrees][KotlinSourceSetTree] that the described hierarchy is applied to.
         *
         * Examples of DSL:
         *
         * 1. Only apply a hierarchy for the "main" and "test" [KotlinSourceSetTree]:
         *
         * ```kotlin
         * applyHierarchyTemplate {
         *     sourceSetTrees(KotlinSourceSetTree.main, KotlinSourceSetTree.test)
         *     common {
         *         withJvm()
         *         group("ios") {
         *             withIos()
         *         }
         *     }
         * }
         *```
         *
         * When given the `iosX64()`, `iosArm64()`, and `jvm()` targets, this DSL creates the following trees:
         * ```
         *             "main"                      "test"
         *           commonMain                 commonTest
         *                |                          |
         *           +----+-----+               +----+-----+
         *           |          |               |          |
         *        iosMain    jvmMain         iosTest    jvmTest
         *           |                          |
         *       +---+----+                 +---+----+
         *       |        |                 |        |
         * iosX64Main  iosArm64Main   iosX64Test  iosArm64Test
         * ```
         *
         * 2. Use a different hierarchy for "main" and "test" [KotlinSourceSetTree]:
         *
         *```kotlin
         * applyHierarchyTemplate {
         *    sourceSetTrees(SourceSetTree.main)  // ! <- only applied to the "main" tree
         *    common {
         *        withJvm()
         *        group("ios") {
         *            withIos()
         *        }
         *    }
         * }
         *
         * applyHierarchyTemplate {
         *     sourceSetTrees(SourceSetTree.test) // ! <- only applied to the "test" tree
         *     common {
         *         withJvm()
         *         withIos()
         *     }
         * }
         * ```
         *
         * When given the `iosX64()`, `iosArm64()`, and `jvm()` targets, this DSL creates the following trees:
         * ```
         *             "main"                            "test"
         *           commonMain                        commonTest
         *                |                                 |
         *           +----+-----+               +-----------+-----------+
         *           |          |               |           |           |
         *        iosMain    jvmMain      iosX64Test   iosArm64Test  jvmTest
         *           |
         *       +---+----+
         *       |        |
         * iosX64Main  iosArm64Main
         * ```
         */
        fun sourceSetTrees(vararg tree: KotlinSourceSetTree)

        /**
         * Adds the given [trees][tree] into this descriptor.
         *
         * @see sourceSetTrees
         */
        fun withSourceSetTree(vararg tree: KotlinSourceSetTree)

        /**
         * Removes the given [trees][tree] from this descriptor.
         * @see sourceSetTrees
         */
        fun excludeSourceSetTree(vararg tree: KotlinSourceSetTree)
    }

    /**
     * Creates a group of [KotlinSourceSets][KotlinSourceSet] with the given [name] and structure provided via the [build] block.
     *
     * @see common
     */
    fun group(name: String, build: KotlinHierarchyBuilder.() -> Unit = {})

    /**
     * Creates a group with the name "common". It's a shortcut for `group("common") { }`.
     *
     * Most hierarchies attach their nodes/groups to "common".
     *
     * The following example applies the shown hierarchy to the "main" compilations and creates a `nativeMain` source set
     * that depends on the usual 'commonMain' source set:
     * ```
     * common {
     *     group("native") {
     *         withIos()
     *         withMacos()
     *     }
     * }
     * ```
     */
    fun common(build: KotlinHierarchyBuilder.() -> Unit) = group("common", build)

    /**
     * Allows including only those [KotlinCompilations][KotlinCompilation] for which the [predicate] returns `true`.
     *
     * This is a low-level API. Try to avoid using it.
     */
    fun withCompilations(predicate: (KotlinCompilation<*>) -> Boolean)

    /**
     * Allows including only those [KotlinCompilations][KotlinCompilation] for which the [predicate] returns `true`.
     *
     * This is a low-level API. Try to avoid using it.
     */
    fun excludeCompilations(predicate: (KotlinCompilation<*>) -> Boolean)

    /**
     * @suppress
     */
    @Deprecated("Use 'excludeCompilations' instead", ReplaceWith("excludeCompilations(predicate)"), level = DeprecationLevel.ERROR)
    fun withoutCompilations(predicate: (KotlinCompilation<*>) -> Boolean) = excludeCompilations(predicate)

    /**
     * @suppress
     */
    @Deprecated(
        "Use plain 'withoutCompilations(!predicate) instead'. Scheduled for removal in Kotlin 2.3.",
        ReplaceWith("withoutCompilations { !predicate(it) }"),
        level = DeprecationLevel.ERROR
    )
    fun filterCompilations(predicate: (KotlinCompilation<*>) -> Boolean) = excludeCompilations { !predicate(it) }

    /**
     * Only includes targets for Kotlin/Native in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withNative()

    /**
     * Only includes Kotlin's Apple targets in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withApple()

    /**
     * Only includes Kotlin's Apple/iOS targets in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withIos()

    /**
     * Only includes Kotlin's Apple/watchOS targets in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withWatchos()

    /**
     * Only includes Kotlin's Apple/macOS targets in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withMacos()

    /**
     * Only includes Kotlin's Apple/tvOS targets in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withTvos()

    /**
     * Only includes Kotlin's MinGW targets in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withMingw()

    /**
     * Only includes Kotlin's Linux targets in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withLinux()

    /**
     * Only includes Kotlin's Android/Native targets in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withAndroidNative()

    /**
     * Only includes targets for Kotlin/JS in this [group].
     */
    fun withJs()

    /**
     * @suppress
     */
    @Deprecated(
        "Renamed to 'withWasmJs'. Scheduled for removal in Kotlin 2.3.",
        replaceWith = ReplaceWith("withWasmJs()"),
        level = DeprecationLevel.ERROR
    )
    fun withWasm()

    /**
     * Only includes Kotlin's Wasm/JS targets in this [group].
     */
    fun withWasmJs()

    /**
     * Only includes Kotlin's Wasm/WASI targets in this [group].
     */
    fun withWasmWasi()

    /**
     * Only includes targets for Kotlin/JVM in this [group].
     */
    fun withJvm()

    /**
     * Only includes Kotlin's Android targets in this [group].
     */
    fun withAndroidTarget()

    /**
     * Only includes Kotlin's Android/androidNativeX64 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withAndroidNativeX64()

    /**
     * Only includes Kotlin's Android/androidNativeX86 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withAndroidNativeX86()

    /**
     * Only includes Kotlin's Android/androidNativeArm32 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withAndroidNativeArm32()

    /**
     * Only includes Kotlin's Android/androidNativeArm64 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withAndroidNativeArm64()

    /**
     * Only includes Kotlin's Apple/iosArm64 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withIosArm64()

    /**
     * Only includes Kotlin's Apple/iosX64 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withIosX64()

    /**
     * Only includes Kotlin's Apple/iosSimulatorArm64 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withIosSimulatorArm64()

    /**
     * Only includes Kotlin's Apple/watchosArm32 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withWatchosArm32()

    /**
     * Only includes Kotlin's Apple/watchosArm64 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withWatchosArm64()

    /**
     * Only includes Kotlin's Apple/watchosX64 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withWatchosX64()

    /**
     * Only includes Kotlin's Apple/watchosSimulatorArm64 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withWatchosSimulatorArm64()

    /**
     * Only includes Kotlin's Apple/watchosDeviceArm64 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withWatchosDeviceArm64()

    /**
     * Only includes Kotlin's Apple/tvosArm64 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withTvosArm64()

    /**
     * Only includes Kotlin's Apple/tvosX64 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withTvosX64()

    /**
     * Only includes Kotlin's Apple/tvosSimulatorArm64 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withTvosSimulatorArm64()

    /**
     * Only includes Kotlin's linuxX64 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withLinuxX64()

    /**
     * Only includes Kotlin's mingwX64 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withMingwX64()

    /**
     * Only includes Kotlin's Apple/macosX64 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withMacosX64()

    /**
     * Only includes Kotlin's Apple/macosArm64 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withMacosArm64()

    /**
     * Only includes Kotlin's linuxArm64 target in this [group].
     *
     * For more information, see [Native targets overview](https://kotlinlang.org/docs/native-target-support.html).
     */
    fun withLinuxArm64()
}
