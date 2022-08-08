/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources.android

import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal object MultiplatformLayoutV1KotlinAndroidSourceSetNaming : KotlinAndroidSourceSetNaming {
    override fun kotlinSourceSetName(disambiguationClassifier: String, androidSourceSetName: String, type: AndroidVariantType?): String =
        lowerCamelCaseName(disambiguationClassifier, androidSourceSetName)
}