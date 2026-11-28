/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

import org.jetbrains.kotlin.documentation.KdElementId

internal const val JVM_FIELD_PACKAGE_NAME: String = "kotlin.jvm"
internal const val JVM_FIELD_CLASS_NAMES: String = "JvmField"

internal val JVM_FIELD_DECLARATION_ID = KdElementId("$JVM_FIELD_PACKAGE_NAME/$JVM_FIELD_CLASS_NAMES")

internal fun Annotations.Annotation.isJvmField(): Boolean = classLikeId == JVM_FIELD_DECLARATION_ID
