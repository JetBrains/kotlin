/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.file.Path


class KonanTargetTest {
    @Test
    fun allPredefinedTargetsRegistered() {
        assertEquals(
            KonanTarget::class.sealedSubclasses.mapNotNull { it.objectInstance }.toSet(),
            KonanTarget.predefinedTargets.values.toSet(),
            "Some of predefined KonanTarget instances are not listed in 'KonanTarget.predefinedTargets'",
        )
    }

    @DisplayName("Test checks that all KonanTarget objects can be successfully serialized and deserialized")
    @Test
    fun checkKonanTargetsSerializationAndDeserialization(@TempDir serializedTargetsDir: Path) {
        val konanTargetsBeforeSerialization = KonanTarget::class.sealedSubclasses.mapNotNull { it.objectInstance }.toSet()

        val serializedTargetsFile = serializedTargetsDir.resolve("serializedTargets.txt")

        ObjectOutputStream(FileOutputStream(serializedTargetsFile.toFile())).use {
            it.writeObject(konanTargetsBeforeSerialization)
        }
        val konanTargetsAfterDeserialization = ObjectInputStream(FileInputStream(serializedTargetsFile.toFile())).use {
            it.readObject()
        }

        assertEquals(konanTargetsBeforeSerialization, konanTargetsAfterDeserialization)
    }
}
