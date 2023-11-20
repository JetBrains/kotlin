package org.jetbrains.kotlin.gradle.plugin

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.konan.target.DEPRECATED_TARGET_MESSAGE

@KotlinTargetsDsl
@ExperimentalKotlinGradlePluginApi
interface KotlinHierarchyBuilder {

    @KotlinTargetsDsl
    @ExperimentalKotlinGradlePluginApi
    interface Root : KotlinHierarchyBuilder {
        /**
         * Defines the trees that the described hierarchy is applied to.
         * ### Example 1: Only apply a hierarchy for the "main" and "test" [KotlinSourceSetTree]
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
         * Will create the following trees given an iosX64(), iosArm64() and jvm() target:
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
         * ### Example 2:
         * Using a different hierarchy for "main" and "test"
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
         * Will create the following trees given an iosX64(), iosArm64() and jvm() target:
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
         * Will add the given [tree]s into for this descriptor.
         * @see sourceSetTrees
         */
        fun withSourceSetTree(vararg tree: KotlinSourceSetTree)

        /**
         * Will remove the given [tree]s from this descriptor
         * @see sourceSetTrees
         */
        fun excludeSourceSetTree(vararg tree: KotlinSourceSetTree)
    }

    /* Declaring groups */

    /**
     * Shortcut for `group("common") { }`:
     * Most hierarchies should attach their nodes/groups to 'common'
     *
     * e.g.
     * ```
     * common {
     *     group("native") {
     *         withIos()
     *         withMacos()
     *     }
     * }
     * ```
     * applying the shown hierarchy to the main compilations will create a 'nativeMain' source set which will
     * depend on the usual 'commonMain'
     *
     */
    fun common(build: KotlinHierarchyBuilder.() -> Unit) = group("common", build)
    fun group(name: String, build: KotlinHierarchyBuilder.() -> Unit = {})

    /* low-level APIs */
    fun withCompilations(predicate: (KotlinCompilation<*>) -> Boolean)

    fun excludeCompilations(predicate: (KotlinCompilation<*>) -> Boolean)

    @Deprecated("Use 'excludeCompilations' instead", ReplaceWith("excludeCompilations(predicate)"))
    fun withoutCompilations(predicate: (KotlinCompilation<*>) -> Boolean) = excludeCompilations(predicate)

    @Deprecated("Use plain 'withoutCompilations(!predicate) instead'", ReplaceWith("withoutCompilations { !predicate(it) }"))
    fun filterCompilations(predicate: (KotlinCompilation<*>) -> Boolean) = excludeCompilations { !predicate(it) }

    /* Convenient groups */
    fun withNative()
    fun withApple()
    fun withIos()
    fun withWatchos()
    fun withMacos()
    fun withTvos()
    fun withMingw()
    fun withLinux()
    fun withAndroidNative()

    /* Actual targets */
    fun withJs()
    @Deprecated("Renamed to 'withWasmJs''", replaceWith = ReplaceWith("withWasmJs()"))
    fun withWasm()
    fun withWasmJs()
    fun withWasmWasi()
    fun withJvm()

    @Deprecated("Renamed to 'withAndroidTarget''", replaceWith = ReplaceWith("withAndroidTarget()"))
    fun withAndroid()
    fun withAndroidTarget()
    fun withAndroidNativeX64()
    fun withAndroidNativeX86()
    fun withAndroidNativeArm32()
    fun withAndroidNativeArm64()
    fun withIosArm32()
    fun withIosArm64()
    fun withIosX64()
    fun withIosSimulatorArm64()
    fun withWatchosArm32()
    fun withWatchosArm64()
    fun withWatchosX64()
    fun withWatchosSimulatorArm64()
    fun withWatchosDeviceArm64()
    fun withTvosArm64()
    fun withTvosX64()
    fun withTvosSimulatorArm64()
    fun withLinuxX64()
    fun withMingwX64()
    fun withMacosX64()
    fun withMacosArm64()
    fun withLinuxArm64()

    @Deprecated(DEPRECATED_TARGET_MESSAGE, level = DeprecationLevel.ERROR)
    fun withWatchosX86()

    @Deprecated(DEPRECATED_TARGET_MESSAGE, level = DeprecationLevel.ERROR)
    fun withMingwX86()

    @Deprecated(DEPRECATED_TARGET_MESSAGE, level = DeprecationLevel.ERROR)
    fun withLinuxArm32Hfp()

    @Deprecated(DEPRECATED_TARGET_MESSAGE, level = DeprecationLevel.ERROR)
    fun withLinuxMips32()

    @Deprecated(DEPRECATED_TARGET_MESSAGE, level = DeprecationLevel.ERROR)
    fun withLinuxMipsel32()

    @Deprecated(DEPRECATED_TARGET_MESSAGE, level = DeprecationLevel.ERROR)
    fun withWasm32()
}
