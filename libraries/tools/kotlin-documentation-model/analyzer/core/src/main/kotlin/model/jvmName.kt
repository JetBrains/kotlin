package org.jetbrains.dokka.model

import org.jetbrains.dokka.links.DRI

fun DRI.isJvmName(): Boolean = packageName == "kotlin.jvm" && classNames == "JvmName"

fun Annotations.Annotation.isJvmName(): Boolean = dri.isJvmName()
