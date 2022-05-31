package org.jetbrains.dokka.model

import org.jetbrains.dokka.links.DRI

const val JVM_FIELD_PACKAGE_NAME = "kotlin.jvm"
const val JVM_FIELD_CLASS_NAMES = "JvmField"

fun DRI.isJvmField(): Boolean = packageName == JVM_FIELD_PACKAGE_NAME && classNames == JVM_FIELD_CLASS_NAMES

fun Annotations.Annotation.isJvmField(): Boolean = dri.isJvmField()
