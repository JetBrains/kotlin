/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("ClassNameKt")

package kotlinx.metadata

/**
 * A fully qualified name of a classifier from the Kotlin's point of view. May differ from the JVM name of the class
 * which is the runtime representation of this Kotlin classifier (for example, Kotlin class "kotlin/Int" -> JVM class "java/lang/Integer")
 *
 * Package names in this name are separated by '/', and class names are separated by '.', for example: `"org/foo/bar/Baz.Nested"`.
 *
 * If this name starts with '.', it represents a local class or an anonymous object. This is used by the Kotlin compiler
 * to prevent lookup of this name in the resolution.
 */
// TODO: use inline class in 1.3
typealias ClassName = String

val ClassName.isLocal: Boolean
    get() = this.startsWith(".")
