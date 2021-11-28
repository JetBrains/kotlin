/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.intellij.lang.annotations.Language

@Language("Groovy")
internal val DEFAULT_GROOVY_SETTINGS_FILE =
    """
    pluginManagement {
        repositories {
            mavenLocal()
            mavenCentral()
            google()
            gradlePluginPortal()
        }

        plugins {
            id "org.jetbrains.kotlin.jvm" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.kapt" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.android" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.js" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.multiplatform" version "${'$'}kotlin_version"
        }
        
        resolutionStrategy {
            eachPlugin {
                switch (requested.id.id) {
                    case "com.android.application":
                    case "com.android.library":
                    case "com.android.test":
                    case "com.android.dynamic-feature":
                    case "com.android.asset-pack":
                    case "com.android.asset-pack-bundle":
                    case "com.android.lint":
                    case "com.android.instantapp":
                    case "com.android.feature":
                        useModule("com.android.tools.build:gradle:${'$'}android_tools_version")
                        break
                    case "kotlin-dce-js":
                        useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}kotlin_version")        
                        break
                }
            }
        }
    }
    """.trimIndent()
