/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.jetbrains.report.json.*

// List of commits.
class CommitsList(data: JsonElement): ConvertedFromJson {

    val commits: List<Commit>

    init {
        if (data !is JsonObject) {
            error("Commits description is expected to be a json object!")
        }
        val changesElement = data.getOptionalField("change")
        commits = changesElement?.let {
            if (changesElement !is JsonArray) {
                error("Change field is expected to be an array. Please, check source.")
            }
            changesElement.jsonArray.map {
                with(it as JsonObject) {
                    Commit(elementToString(getRequiredField("version"), "version"),
                           elementToString(getRequiredField("username"), "username"),
                           elementToString(getRequiredField("webUrl"), "webUrl")
                    )
                }
            }
        } ?: listOf<Commit>()
    }
}

fun getBuildProperty(buildJsonDescription: String, property: String) =
    with(JsonTreeParser.parse(buildJsonDescription) as JsonObject) {
        if (getPrimitive("count").int == 0) {
            error("No build information on TeamCity for $buildJsonDescription!")
        }
        (getArray("build").getObject(0).getPrimitive(property) as JsonLiteral).unquoted()
    }
