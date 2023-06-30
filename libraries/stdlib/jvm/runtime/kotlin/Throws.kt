/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * This annotation indicates what exceptions should be declared by a function when compiled to a JVM method.
 *
 * Example:
 *
 * ```
 * @Throws(IOException::class)
 * fun readFile(name: String): String {...}
 * ```
 *
 * will be translated to
 *
 * ```
 * String readFile(String name) throws IOException {...}
 * ```
 */
@SinceKotlin("1.4")
@Suppress("ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT") // @Retention(BINARY) on expect needed for native target
public actual typealias Throws = kotlin.jvm.Throws
