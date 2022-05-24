/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.gradle.android

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import java.io.File
import java.io.Serializable

internal val androidDslKey = extrasKeyOf<AndroidDsl>()
val GradleKpmFragment.androidDsl: AndroidDsl? get() = this.extras[androidDslKey]

class AndroidDsl : Serializable {
    var compileSdk = 0
    var isMinifyEnabled: Boolean = false
    var androidManifest: File? = null
}
