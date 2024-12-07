/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.provider.Provider

internal val KotlinCommonCompilerOptions.usesK2: Provider<Boolean>
    get() = this.languageVersion
        .orElse(KotlinVersion.DEFAULT)
        .map { version -> version >= KotlinVersion.KOTLIN_2_0 }
