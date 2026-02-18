/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

/**
 * This precompiled script plugin is intended to be a temporary solution for KT-70247.
 * It should be removed after a proper resolution is provided.
 * Also, update the mention of this plugin from `gradle.properties`
 *
 * This plugin limits AV/LV for modules embedded into KGP, so it's safe to use a version compatible with bootstrap for Gradle-related modules.
 * KGP during embedding of these dependencies cuts off the Kotlin metadata as it's only required at compilation time and we do not expose those modules publicly.
 */

plugins {
    kotlin("jvm")
}

limitLanguageAndApiVersions(KotlinVersion.KOTLIN_2_2)
