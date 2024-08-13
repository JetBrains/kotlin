/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.compilerconfig

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

public class ClassNamingMapping(
    public val kotlinClassFqName: String,
    public val objCClassName: String,
) : Serializable

public class CompilerConfig(
    public val classNamingMappings: List<ClassNamingMapping>
) : Serializable {
    public fun writeToFile(file: File) {
        ObjectOutputStream(FileOutputStream(file)).use {
            it.writeObject(this)
        }
    }

    public companion object {
        public fun parseFromFile(file: File): CompilerConfig = ObjectInputStream(FileInputStream(file)).use {
            it.readObject() as CompilerConfig
        }
    }
}