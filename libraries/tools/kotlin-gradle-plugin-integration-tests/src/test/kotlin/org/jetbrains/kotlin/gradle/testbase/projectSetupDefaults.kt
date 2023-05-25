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
            id "org.jetbrains.kotlin.native.cocoapods" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.multiplatform" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.multiplatform.pm20" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.allopen" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.spring" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.jpa" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.noarg" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.lombok" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.sam.with.receiver" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.serialization" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.assignment" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.test.fixes.android" version "${'$'}test_fixes_version"
            id "org.jetbrains.kotlin.gradle-subplugin-example" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.plugin.atomicfu" version "${'$'}kotlin_version"
            id "org.jetbrains.kotlin.test.gradle-warnings-detector" version "${'$'}test_fixes_version"
            id "org.jetbrains.kotlin.test.kotlin-compiler-args-properties" version "${'$'}test_fixes_version"
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
                }
            }
        }
    }

    plugins {
        id("org.jetbrains.kotlin.test.gradle-warnings-detector")
    }
    """.trimIndent()

@Language("kts")
internal val DEFAULT_KOTLIN_SETTINGS_FILE =
    """
    pluginManagement {
        repositories {
            mavenLocal()
            mavenCentral()
            google()
            gradlePluginPortal()
        }

        val kotlin_version: String by settings
        val android_tools_version: String by settings
        val test_fixes_version: String by settings
        plugins {
            id("org.jetbrains.kotlin.jvm") version kotlin_version
            id("org.jetbrains.kotlin.kapt") version kotlin_version
            id("org.jetbrains.kotlin.android") version kotlin_version
            id("org.jetbrains.kotlin.js") version kotlin_version
            id("org.jetbrains.kotlin.native.cocoapods") version kotlin_version
            id("org.jetbrains.kotlin.multiplatform") version kotlin_version
            id("org.jetbrains.kotlin.multiplatform.pm20") version kotlin_version
            id("org.jetbrains.kotlin.plugin.allopen") version kotlin_version
            id("org.jetbrains.kotlin.plugin.spring") version kotlin_version
            id("org.jetbrains.kotlin.plugin.jpa") version kotlin_version
            id("org.jetbrains.kotlin.plugin.noarg") version kotlin_version
            id("org.jetbrains.kotlin.plugin.lombok") version kotlin_version
            id("org.jetbrains.kotlin.plugin.sam.with.receiver") version kotlin_version
            id("org.jetbrains.kotlin.plugin.serialization") version kotlin_version
            id("org.jetbrains.kotlin.plugin.assignment") version kotlin_version
            id("org.jetbrains.kotlin.test.fixes.android") version test_fixes_version
            id("org.jetbrains.kotlin.gradle-subplugin-example") version kotlin_version
            id("org.jetbrains.kotlin.plugin.atomicfu") version kotlin_version
            id("org.jetbrains.kotlin.test.gradle-warnings-detector") version test_fixes_version
            id("org.jetbrains.kotlin.test.kotlin-compiler-args-properties") version test_fixes_version
        }
        
        resolutionStrategy {
            eachPlugin {
                when (requested.id.id) {
                    "com.android.application",
                    "com.android.library",
                    "com.android.test",
                    "com.android.dynamic-feature",
                    "com.android.asset-pack",
                    "com.android.asset-pack-bundle",
                    "com.android.lint",
                    "com.android.instantapp",
                    "com.android.feature" -> useModule("com.android.tools.build:gradle:${'$'}android_tools_version")
                }
            }
        }
    }

    plugins {
        id("org.jetbrains.kotlin.test.gradle-warnings-detector")
    }
    """.trimIndent()
