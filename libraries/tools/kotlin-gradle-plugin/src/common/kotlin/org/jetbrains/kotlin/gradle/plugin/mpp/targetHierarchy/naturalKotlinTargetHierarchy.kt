/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchyDescriptor
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.isTest
import org.jetbrains.kotlin.gradle.plugin.sources.android.AndroidVariantType
import org.jetbrains.kotlin.gradle.plugin.sources.android.type

internal val naturalKotlinTargetHierarchy = KotlinTargetHierarchyDescriptor {
    /* natural hierarchy is only applied to default 'main'/'test' compilations (by default) */
    filterCompilations { compilation ->
        compilation.isMain() || compilation.isTest() || compilation.isAndroidMain() || compilation.isAndroidUnitTest()
    }

    common {
        group("native") {
            withNative()

            group("apple") {
                withApple()

                group("ios") {
                    withIos()
                }

                group("tvos") {
                    withTvos()
                }

                group("watchos") {
                    withWatchos()
                }

                group("macos") {
                    withMacos()
                }
            }

            group("linux") {
                withLinux()
            }

            group("mingw") {
                withMingw()
            }

            group("androidNative") {
                withAndroidNative()
            }
        }
    }
}

private fun KotlinCompilation<*>.isAndroidMain(): Boolean =
    this is KotlinJvmAndroidCompilation && this.androidVariant.type == AndroidVariantType.Main

private fun KotlinCompilation<*>.isAndroidUnitTest(): Boolean =
    this is KotlinJvmAndroidCompilation && this.androidVariant.type == AndroidVariantType.UnitTest
