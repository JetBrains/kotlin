/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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