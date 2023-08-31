/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

import org.jetbrains.dokka.links.DRI

const val JVM_FIELD_PACKAGE_NAME = "kotlin.jvm"
const val JVM_FIELD_CLASS_NAMES = "JvmField"

fun DRI.isJvmField(): Boolean = packageName == JVM_FIELD_PACKAGE_NAME && classNames == JVM_FIELD_CLASS_NAMES

fun Annotations.Annotation.isJvmField(): Boolean = dri.isJvmField()
