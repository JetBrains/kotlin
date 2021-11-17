/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android.multiplatform

import com.android.build.gradle.AppExtension
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.kotlin.dsl.getByType
import java.util.concurrent.Callable

internal fun Project.getAndroidRuntimeJars(): FileCollection {
    return project.files(Callable { project.extensions.getByType<AppExtension>().bootClasspath })
}
