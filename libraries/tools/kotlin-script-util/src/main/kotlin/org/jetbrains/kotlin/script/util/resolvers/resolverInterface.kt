/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script.util.resolvers

import org.jetbrains.kotlin.script.util.DependsOn
import org.jetbrains.kotlin.script.util.Repository
import java.io.File

interface Resolver {
    fun tryResolve(dependsOn: DependsOn): Iterable<File>?
    fun tryAddRepo(annotation: Repository): Boolean
}