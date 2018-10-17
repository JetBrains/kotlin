/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.konan

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.name.Name

private val STDLIB_MODULE_NAME = Name.special("<$KONAN_STDLIB_NAME>")

fun ModuleDescriptor.isKonanStdlib() = name == STDLIB_MODULE_NAME
