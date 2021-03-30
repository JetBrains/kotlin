/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling

internal data class SchemaVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<SchemaVersion> {
    override fun compareTo(other: SchemaVersion): Int {
        (this.major - other.major).takeIf { it != 0 }?.let { return it }
        (this.minor - other.minor).takeIf { it != 0 }?.let { return it }
        return this.patch - other.patch
    }

    override fun toString(): String {
        return "$major.$minor.$patch"
    }

    companion object {
        val current by lazy { SchemaVersion.parseStringOrThrow(KotlinToolingMetadata.currentSchemaVersion) }
    }
}

internal fun SchemaVersion.Companion.parseStringOrThrow(schemaVersion: String): SchemaVersion {
    fun throwIllegalSchemaVersion(): Nothing = throw IllegalArgumentException(
        "Illegal schemaVersion $schemaVersion. Expected {major}.{minor}.{patch}"
    )

    val parts = schemaVersion.split(".")
    if (parts.size != 3) throwIllegalSchemaVersion()

    return SchemaVersion(
        major = parts[0].toIntOrNull() ?: throwIllegalSchemaVersion(),
        minor = parts[1].toIntOrNull() ?: throwIllegalSchemaVersion(),
        patch = parts[2].toIntOrNull() ?: throwIllegalSchemaVersion()
    )
}

internal fun SchemaVersion.isCompatible(to: SchemaVersion): Boolean {
    if (this.major != to.major) {
        return false
    }

    return (this.minor >= to.minor)
}
