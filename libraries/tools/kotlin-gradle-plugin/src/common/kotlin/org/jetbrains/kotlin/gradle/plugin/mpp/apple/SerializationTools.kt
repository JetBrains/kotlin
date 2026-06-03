/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import kotlinx.serialization.serializer
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import java.io.Serializable

internal object SerializationTools {
    inline fun <reified T : Serializable> writeToJson(objects: T): String =
        KgpJson.prettyPrinted.encodeToString(serializer(), objects)

    inline fun <reified T : Serializable> readFromJson(jsonString: String): T =
        KgpJson.default.decodeFromString(serializer(), jsonString)
}
