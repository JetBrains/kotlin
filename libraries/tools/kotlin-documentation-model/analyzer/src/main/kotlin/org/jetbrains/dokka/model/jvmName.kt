/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

import org.jetbrains.kotlin.documentation.KdElementId

internal val JVM_NAME_DECLARATION_ID = KdElementId("kotlin.jvm/JvmName")

@Suppress("unused")
internal fun Annotations.Annotation.isJvmName(): Boolean = classLikeId == JVM_NAME_DECLARATION_ID
