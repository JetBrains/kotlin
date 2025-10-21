/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.coverage.runtime

public object HitWriter {
    public fun writeBoolean(filePath: String, segments: Map<Long, BooleanArray>) {
        FileRoutines.writeToFile(filePath) {
            writeInt(100500)
            writeInt(segments.size)
            for ((id, array) in segments) {
                val moduleId = (id shr 32).toInt()
                val segmentNumber = id.toInt()
                writeInt(moduleId)
                writeInt(segmentNumber)
                writeInt(array.size)
                writeBooleanArray(array)
            }
        }
    }
}