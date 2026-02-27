/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.nio.file.Path
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.map
import kotlin.collections.mapNotNull
import kotlin.collections.orEmpty

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * The subsystem declaration as parsed from the 'subsystems.yaml' file
 */
internal data class DeclaredSubsystem(
    val name: String,
    /**
     * The 'home' directory of the subsystem.
     * The home directory is used for storing contract dumps.
     * The directory must be a relative path to from the repository root.
     * Note: The home directory is not automatically included!
     */
    val home: String,

    /**
     * Files matching these 'glob' patterns will be included in this subsystem.
     * - e.g., 'compiler/​**' will include all files under the 'compiler' subdirectory
     * - e.g., '**​/​*gradle* will include all files containing the word 'gradle'
     */
    val includes: List<String>,

    /**
     * Files matching these 'glob' patterns will be excluded from this subsystem
     * See [includes]
     */
    val excludes: List<String>,

    /**
     * Subsystems do have a child/parent relationship.
     * All [subsystems] will be marked as affected if their parent is affected.
     */
    val subsystems: List<DeclaredSubsystem>,
)

internal fun JsonNode.toDeclaredTestSystem(key: String): DeclaredSubsystem {
    return DeclaredSubsystem(
        name = key,
        home = get("home")?.asText() ?: error("No home for $key"),
        includes = get("include")?.valueStream()?.toList().orEmpty().map { it.asText() },
        excludes = get("excludes")?.valueStream()?.toList().orEmpty().map { it.asText() },
        subsystems = get("subsystems")?.properties().orEmpty().mapNotNull { (key, value) -> value.toDeclaredTestSystem(key) },
    )
}

internal fun Path.readDeclaredSubsystems(): List<DeclaredSubsystem> {
    val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
    val node = mapper.readTree(this.toFile())
    return node.properties().mapNotNull { (key, value) ->
        if (key.startsWith("$")) return@mapNotNull null
        value.toDeclaredTestSystem(key)
    }
}
