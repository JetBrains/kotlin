/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.kotlin.dsl.getByType
import java.util.concurrent.Callable

internal fun Project.androidBootClasspath(): FileCollection {
    return project.files(Callable { project.extensions.getByType<BaseExtension>().bootClasspath })
}