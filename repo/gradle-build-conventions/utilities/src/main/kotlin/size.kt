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

    val inWholeBytes: Long get() = bytes

    val inWholeKilobytes: Long get() = bytes / 1000L

    val inWholeKiB: Long get() = bytes / 1024L

    val inWholeMegabytes: Long get() = bytes / (1000L * 1000L)

    val inWholeMiB: Long get() = bytes / (1024L * 1024L)

    val inWholeGigabytes: Long get() = bytes / (1000L * 1000L * 1000L)

    val inWholeGiB: Long get() = bytes / (1024L * 1024L * 1024L)

    companion object {
        fun fromJvmArg(value: String): Size {
            val pattern = Regex("""(?<size>\d+)(?<unit>[kmg])""")
            val match = pattern.matchEntire(value.lowercase()) ?: error("'$value' is not a valid JVM size notation")
            val size = match.groups["size"]!!.value.toInt()
            return when (val unit = match.groups["unit"]!!.value) {
                "k" -> size.KiB
                "m" -> size.MiB
                "g" -> size.GiB
                else -> error("Unrecognized size: $unit")
            }
        }
    }

    fun toJvmArg(): String {
        require(bytes % 1.MiB.bytes == 0L) { "JVM memory size must be a whole number of MiB: $bytes bytes" }
        return "${inWholeMiB}m"
    }
}

val Int.bytes: Size get() = Size(this)

val Int.kilobytes: Size get() = bytes * 1000

val Int.KiB: Size get() = bytes * 1024

val Int.megabytes: Size get() = kilobytes * 1000

val Int.MiB: Size get() = KiB * 1024

val Int.gigabytes: Size get() = megabytes * 1000

val Int.GiB: Size get() = MiB * 1024
