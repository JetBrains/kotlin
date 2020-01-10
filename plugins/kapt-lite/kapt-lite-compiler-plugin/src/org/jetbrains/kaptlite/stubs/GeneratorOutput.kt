/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite.stubs

import org.jetbrains.kaptlite.stubs.util.CodeScope

interface GeneratorOutput {
    fun produce(internalName: String, path: String, block: CodeScope.() -> Unit)
}