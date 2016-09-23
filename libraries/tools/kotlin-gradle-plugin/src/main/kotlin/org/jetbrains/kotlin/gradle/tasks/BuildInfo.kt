package org.jetbrains.kotlin.gradle.tasks

import java.io.*

internal data class BuildInfo(val startTS: Long) : Serializable {
    companion object {
        fun read(file: File): BuildInfo? =
                try {
                    ObjectInputStream(FileInputStream(file)).use {
                        it.readObject() as BuildInfo
                    }
                }
                catch (e: Exception) {
                    null
                }

        fun write(buildInfo: BuildInfo, file: File) {
            ObjectOutputStream(FileOutputStream(file)).use {
                it.writeObject(buildInfo)
            }
        }
    }
}