/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.analyzer

// Report with changes of different fields.
class ChangeReport<T>(val entityName: String, val changes: List<FieldChange<T>>) {
    fun renderAsTextReport(): String {
        var content = ""
        if (!changes.isEmpty()) {
            content = "$content$entityName changes\n"
            content = "$content====================\n"
            changes.forEach {
                content = "$content${it.renderAsText()}"
            }
        }
        return content
    }
}

// Change of report field.
class FieldChange<T>(val field: String, val previous: T, val current: T) {
    companion object {
        fun <T> getFieldChangeOrNull(field: String, previous: T, current: T): FieldChange<T>? {
            if (previous != current) {
                return FieldChange(field, previous, current)
            }
            return null
        }
    }

    fun renderAsText(): String {
        return "$field: $previous -> $current\n"
    }
}