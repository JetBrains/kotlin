/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy

import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchy.ModuleName
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchyDescriptor

internal val defaultKotlinTargetHierarchy = KotlinTargetHierarchyDescriptor {
    /* natural hierarchy is only applied to default 'main'/'test' compilations (by default) */
    withModule(ModuleName.main, ModuleName.test)

    common {
        /* All compilations shall be added to the common group by default */
        withCompilations { true }

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
