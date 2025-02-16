/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.time

import java.io.*
import kotlin.internal.IMPLEMENTATIONS

@ExperimentalTime
private val systemClock: Clock = IMPLEMENTATIONS.getSystemClock()

@ExperimentalTime
internal actual fun systemClockNow(): Instant = systemClock.now()

@ExperimentalTime
private class InstantSerialized(
    var epochSeconds: Long,
    var nanosecondsOfSecond: Int
) : Externalizable {

    constructor() : this(0L, 0) // for deserialization

    override fun writeExternal(output: ObjectOutput) {
        output.writeLong(epochSeconds)
        output.writeInt(nanosecondsOfSecond)
    }

    override fun readExternal(input: ObjectInput) {
        epochSeconds = input.readLong()
        nanosecondsOfSecond = input.readInt()
    }

    private fun readResolve(): Any =
        Instant.fromEpochSeconds(epochSeconds, nanosecondsOfSecond)

    companion object {
        private const val serialVersionUID: Long = 0L
    }
}

@ExperimentalTime
internal actual fun serializedInstant(instant: Instant): Any =
    InstantSerialized(instant.epochSeconds, instant.nanosecondsOfSecond)