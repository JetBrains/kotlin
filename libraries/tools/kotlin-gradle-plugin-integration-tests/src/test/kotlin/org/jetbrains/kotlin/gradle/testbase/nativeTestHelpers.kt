/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import java.util.*

val DEFAULT_CURRENT_PLATFORM_TARGET_NAME_POSTFIX = HostManager.host.presetName.lowercase(Locale.getDefault())