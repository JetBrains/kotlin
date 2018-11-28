/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.io


/** Prints the line separator to the standard output stream. */
public expect fun println()

/** Prints the given [message] and the line separator to the standard output stream. */
public expect fun println(message: Any?)

/** Prints the given [message] to the standard output stream. */
public expect fun print(message: Any?)


internal expect interface Serializable
