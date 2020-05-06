/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.compiler

data class CompletionOptions (
    val filterOutShadowedDescriptors: Boolean = true,
    val nameFilter: (String, String) -> Boolean = { name, namePart -> name.startsWith(namePart) },
)
