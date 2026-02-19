/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

internal class ModuleMapBuilder {
    var name: String? = null

    var isFramework: Boolean = false
    var isUmbrellaHeader: Boolean = false

    var header: String? = null
    var umbrella: String? = null
    var export: String? = null
    var module: String? = null
    var link: List<String> = emptyList()
}

internal object ModuleMapGenerator {

    fun generateModuleMap(builder: ModuleMapBuilder.() -> Unit): String {
        val moduleMapBuilder = ModuleMapBuilder().apply(builder)

        val isFramework = moduleMapBuilder.isFramework
        val isUmbrellaHeader = moduleMapBuilder.isUmbrellaHeader

        val name = moduleMapBuilder.name
        val module = moduleMapBuilder.module
        val header = moduleMapBuilder.header
        val umbrella = moduleMapBuilder.umbrella
        val export = moduleMapBuilder.export
        val link = moduleMapBuilder.link

        val content = listOfNotNull(
            umbrella?.let { "umbrella ${if (isUmbrellaHeader) "header \"$it\"" else "\"$it\""}" },
            header?.let { "header \"$it\"" },
            export?.let { "export $it" },
            module?.let { "module $it" },
        ) + link.map { "link \"$it\"" }

        return """
            |${if (isFramework) "framework module \"$name\"" else "module \"$name\""} {
            |    ${content.joinToString("\n    ").trimEnd().trimEnd('\n')}
            |}
        """.trimMargin()
    }
}