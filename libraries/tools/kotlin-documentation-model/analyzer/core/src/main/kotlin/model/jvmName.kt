/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

import org.jetbrains.dokka.links.DRI

fun DRI.isJvmName(): Boolean = packageName == "kotlin.jvm" && classNames == "JvmName"

fun Annotations.Annotation.isJvmName(): Boolean = dri.isJvmName()
