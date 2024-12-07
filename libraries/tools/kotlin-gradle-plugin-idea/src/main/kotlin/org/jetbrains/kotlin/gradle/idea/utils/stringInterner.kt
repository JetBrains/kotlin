/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.utils

import org.jetbrains.kotlin.tooling.core.WeakInterner

/**
 * Can be used for all entities in this module to intern strings
 */
internal val stringInterner = WeakInterner()
