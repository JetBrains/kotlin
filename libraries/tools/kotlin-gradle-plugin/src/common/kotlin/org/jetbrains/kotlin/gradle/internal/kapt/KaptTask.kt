/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.Incremental
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.internal.tasks.TaskWithLocalState
import org.jetbrains.kotlin.gradle.tasks.BaseKapt
//
//internal interface BaseKaptTask : BaseKapt, TaskWithLocalState {
//    @get:Input
//    var incremental: Boolean
//
//    @get:Input
//    val verbose: Property<Boolean>
//
//    @get:Internal
//    var useBuildCache: Boolean
//
//    @get:PathSensitive(PathSensitivity.NONE)
//    @get:Incremental
//    @get:IgnoreEmptyDirectories
//    @get:NormalizeLineEndings
//    @get:Optional
//    @get:InputFiles
//    val classpathStructure: ConfigurableFileCollection
//}
