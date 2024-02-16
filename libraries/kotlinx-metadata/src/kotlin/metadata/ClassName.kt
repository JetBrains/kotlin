/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("ClassNameKt")

package kotlin.metadata

/**
 * A fully qualified name of a classifier from the Kotlin's point of view. May differ from the JVM name of the class
 * which is the runtime representation of this Kotlin classifier (for example, Kotlin class "kotlin/Int" -> JVM class "java/lang/Integer")
 *
 * Package names in this name are separated by '/', and class names are separated by '.', for example: `"org/foo/bar/Baz.Nested"`.
 *
 * If this name starts with '.', it represents a local class or an anonymous object. This is used by the Kotlin compiler
 * to prevent lookup of this name in the resolution.
 */
public typealias ClassName = String // Not a value class because of Java usages

/**
 * Checks whether a class name [this] represents a local class or an anonymous object.
 *
 * A class name represents a local class or an anonymous object if it starts with '.' (dot).
 */
public fun ClassName.isLocalClassName(): Boolean = this.startsWith(".")

@Deprecated(
    "Renamed to isLocalClassName() to avoid confusion with String properties",
    ReplaceWith("isLocalClassName()"),
    level = DeprecationLevel.ERROR
)
public val ClassName.isLocal: Boolean get() = isLocalClassName()
