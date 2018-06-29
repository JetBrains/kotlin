/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

internal fun lowerCamelCaseName(vararg nameParts: String) =
        nameParts.drop(1).joinToString(separator = "", prefix = nameParts.first(), transform = String::capitalize)