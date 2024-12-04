/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs.extras

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinExtra
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.readWriteProperty
import java.io.Serializable

var IdeaKotlinBinaryDependency.klibExtra by KlibExtra.key.readWriteProperty

@IdeaKotlinExtra
data class KlibExtra(
    val builtInsPlatform: String?,
    val uniqueName: String?,
    val shortName: String?,
    val packageFqName: String?,
    val nativeTargets: List<String>?,
    val commonizerNativeTargets: List<String>?,
    val commonizerTarget: String?,
    val isInterop: Boolean?
) : Serializable {
    companion object {
        private const val serialVersionUID = 0L
        val key = extrasKeyOf<KlibExtra>()
    }
}
