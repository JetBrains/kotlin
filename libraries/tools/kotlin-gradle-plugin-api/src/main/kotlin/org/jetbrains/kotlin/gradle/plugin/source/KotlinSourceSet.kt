/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.source

import groovy.lang.Closure
import org.gradle.api.Named
import org.gradle.api.file.SourceDirectorySet

interface KotlinSourceSet: Named {
    val kotlin: SourceDirectorySet

    fun kotlin(configureClosure: Closure<Any?>?): KotlinSourceSet
}