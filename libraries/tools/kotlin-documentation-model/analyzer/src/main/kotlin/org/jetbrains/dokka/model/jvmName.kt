/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

import org.jetbrains.dokka.links.DRI

public fun DRI.isJvmName(): Boolean = packageName == "kotlin.jvm" && classNames == "JvmName"

public fun Annotations.Annotation.isJvmName(): Boolean = dri.isJvmName()
