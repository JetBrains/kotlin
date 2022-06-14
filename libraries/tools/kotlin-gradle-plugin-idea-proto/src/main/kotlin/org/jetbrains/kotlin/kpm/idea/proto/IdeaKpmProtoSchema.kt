/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

internal object IdeaKpmProtoSchema {
    const val versionMajor = 1
    const val versionMinor = 0
    const val versionPatch = 0

    internal val infos = listOf(
        ideaKpmSchemaInfoProto {
            sinceSchemaVersionMajor = 1
            sinceSchemaVersionMinor = 0
            sinceSchemaVersionPatch = 0
            severity = IdeaKpmSchemaInfoProto.Severity.INFO
            message = "Initial version of IdeaKpmProto*"
        }
    )
}
