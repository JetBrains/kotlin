/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import org.jetbrains.kotlin.utils.tryCreateCallableMapping
import kotlin.reflect.KClass
import kotlin.script.experimental.api.ast.Element
import kotlin.script.experimental.api.ast.ParseNode

fun ParseNode<out Element>.toAnnotationObjectIfMatches(vararg expectedAnnClasses: KClass<out Annotation>): ResultWithDiagnostics<Annotation>? {
    val shortName = this.name
    val argumentList = this.children.firstOrNull { it.name == "argumentList" }?.children ?: return null
    val expectedAnnClass = expectedAnnClasses.firstOrNull { it.simpleName == shortName } ?: return null
    val ctor = expectedAnnClass.constructors.firstOrNull() ?: return null
    val mapping =
        tryCreateCallableMapping(
            ctor,
            argumentList.map {
                null
            }
        )
    if (mapping != null) {
        try {
            return ctor.callBy(mapping).asSuccess()
        } catch (e: Exception) { // TODO: find the exact exception type thrown then callBy fails
            return makeFailureResult(e.asDiagnostics())
        }
    }
    return null
}

