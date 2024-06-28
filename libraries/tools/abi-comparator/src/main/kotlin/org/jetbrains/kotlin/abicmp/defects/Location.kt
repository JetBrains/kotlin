/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.defects

sealed class Location : Comparable<Location> {
    abstract val jarFileName: String
    open val className: String? get() = null
    open val methodName: String? get() = null
    open val fieldName: String? get() = null

    enum class Kind {
        JAR_FILE, CLASS, METHOD, FIELD
    }

    abstract val kind: Kind

    override fun compareTo(other: Location): Int =
        compareValuesBy(
            this, other,
            { it.kind },
            { it.jarFileName },
            { it.className },
            { it.methodName },
            { it.fieldName }
        )

    data class JarFile(
        override val jarFileName: String,
    ) : Location() {
        override val kind get() = Kind.JAR_FILE
    }

    data class Class(
        override val jarFileName: String,
        override val className: String,
    ) : Location() {
        override val kind get() = Kind.CLASS
        fun method(methodName: String) = Method(jarFileName, className, methodName)
        fun field(fieldName: String) = Field(jarFileName, className, fieldName)
    }

    data class Method(
        override val jarFileName: String,
        override val className: String,
        override val methodName: String,
    ) : Location() {
        override val kind get() = Kind.METHOD
    }

    data class Field(
        override val jarFileName: String,
        override val className: String,
        override val fieldName: String,
    ) : Location() {
        override val kind get() = Kind.FIELD
    }
}
