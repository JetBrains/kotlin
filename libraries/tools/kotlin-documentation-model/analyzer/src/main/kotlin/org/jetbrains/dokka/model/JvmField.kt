/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

import org.jetbrains.dokka.links.DRI

public const val JVM_FIELD_PACKAGE_NAME: String = "kotlin.jvm"
public const val JVM_FIELD_CLASS_NAMES: String = "JvmField"

public fun DRI.isJvmField(): Boolean = packageName == JVM_FIELD_PACKAGE_NAME && classNames == JVM_FIELD_CLASS_NAMES

public fun Annotations.Annotation.isJvmField(): Boolean = dri.isJvmField()
