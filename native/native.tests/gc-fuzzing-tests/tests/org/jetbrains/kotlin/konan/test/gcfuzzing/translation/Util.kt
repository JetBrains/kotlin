/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.translation

import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.Definition
import org.jetbrains.kotlin.konan.test.gcfuzzing.translation.OutputFileBuilder.LineBuilder

fun GlobalScopeResolver.functionObjCDeclaration(contents: LineBuilder, definition: Definition.Function) {
    contents.append("id ")
    contents.append(computeName(definition))
    contents.parens(*definition.parameters.mapIndexed { index, _ -> fun LineBuilder.() = append("id l${index}") }.toTypedArray())
}

fun GlobalScopeResolver.initObjCDeclaration(contents: LineBuilder, definition: Definition.Class) {
    contents.append("- (instancetype)")
    contents.selector(
        { append("initWith") }, *definition.fields.mapIndexed { index, _ ->
        "f$index" to fun LineBuilder.() = append("(id)f${index}")
    }.toTypedArray()
    )
}

private fun LineBuilder.selector(
    selector: LineBuilder.() -> Unit,
    vararg args: Pair<String, LineBuilder.() -> Unit>,
) {
    selector()
    args.forEachIndexed { index, (name, arg) ->
        if (index == 0) {
            append(name.uppercase())
        } else {
            append(name)
        }
        append(":")
        arg()
        if (index < args.size - 1) append(" ")
    }
}

fun LineBuilder.selectorCall(
    receiver: LineBuilder.() -> Unit,
    selector: LineBuilder.() -> Unit,
    vararg args: Pair<String, LineBuilder.() -> Unit>,
) {
    append("[")
    receiver()
    append(" ")
    selector(selector, *args)
    append("]")
}

fun LineBuilder.functionCall(
    name: LineBuilder.() -> Unit,
    vararg args: LineBuilder.() -> Unit,
) {
    name()
    parens(*args)
}