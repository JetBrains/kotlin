/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@JvmInline
value class Size(val bytes: Long) {
    constructor(bytes: Int) : this(bytes.toLong())

    operator fun times(value: Long) = Size(bytes * value)
    operator fun times(value: Int) = Size(bytes * value)
    operator fun plus(other: Size) = Size(bytes + other.bytes)
    operator fun minus(other: Size) = Size(bytes - other.bytes)

    val inWholeBytes get() = bytes

    val inWholeKilobytes get() = bytes / 1000L

    val inWholeKiB get() = bytes / 1024L

    val inWholeMegabytes get() = bytes / (1000L * 1000L)

    val inWholeMiB get() = bytes / (1024L * 1024L)

    val inWholeGigabytes get() = bytes / (1000L * 1000L * 1000L)

    val inWholeGiB get() = bytes / (1024L * 1024L * 1024L)
}

val Int.bytes: Size get() = Size(this)

val Int.kilobytes: Size get() = bytes * 1000

val Int.KiB: Size get() = bytes * 1024

val Int.megabytes: Size get() = kilobytes * 1000

val Int.MiB: Size get() = KiB * 1024

val Int.gigabytes: Size get() = megabytes * 1000

val Int.GiB: Size get() = MiB * 1024
