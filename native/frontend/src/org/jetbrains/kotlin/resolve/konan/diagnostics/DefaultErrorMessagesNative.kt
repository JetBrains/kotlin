/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.*
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.diagnostics.rendering.Renderers.RENDER_TYPE

private val DIAGNOSTIC_FACTORY_TO_RENDERER by lazy {
    DiagnosticFactoryToRendererMap("Native").apply {
        put(ErrorsNative.THROWS_LIST_EMPTY, "@Throws must have non-empty class list")
        put(
            ErrorsNative.INCOMPATIBLE_THROWS_OVERRIDE, "Member overrides different @Throws filter from {0}",
            Renderers.NAME
        )
        put(
            ErrorsNative.INCOMPATIBLE_THROWS_INHERITED, "Member inherits different @Throws filters from {0}",
            CommonRenderers.commaSeparated(Renderers.NAME)
        )
        put(
            ErrorsNative.MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND,
            "@Throws on suspend declaration must have {0} (or any of its superclasses) listed",
            Renderer { it.shortName().asString() }
        )
        put(
            ErrorsNative.INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY,
            "@SharedImmutable is applicable only to val with backing field or to property with delegation"
        )
        put(ErrorsNative.INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL, "@SharedImmutable is applicable only to top level declarations")
        put(
            ErrorsNative.VARIABLE_IN_SINGLETON_WITHOUT_THREAD_LOCAL,
            "With old Native GC, variable in singleton without @ThreadLocal can't be changed after initialization"
        )
        put(ErrorsNative.VARIABLE_IN_ENUM, "With old Native GC, variable in enum class can't be changed after initialization")
        put(
            ErrorsNative.INAPPLICABLE_THREAD_LOCAL,
            "@ThreadLocal is applicable only to property with backing field, to property with delegation or to objects"
        )
        put(ErrorsNative.INAPPLICABLE_THREAD_LOCAL_TOP_LEVEL, "@ThreadLocal is applicable only to top level declarations")
        put(ErrorsNative.INVALID_CHARACTERS_NATIVE, "Name {0}", CommonRenderers.STRING);
        put(
            ErrorsNative.LEAKING_PHANTOM_TYPE,
            "Declaration signature might vary for different platforms due to the exposed type: {0}. " +
                    "Exposed types should be stable across all platforms that share this declaration.",
            RENDER_TYPE,
        )
        put(
            ErrorsNative.LEAKING_PHANTOM_TYPE_IN_SUPERTYPES,
            "Declaration might vary for different platforms due to the exposed type in supertypes: {0}. " +
                    "Exposed types should be stable across all platforms that share this declaration.",
            RENDER_TYPE,
        )
        put(
            ErrorsNative.LEAKING_PHANTOM_TYPE_IN_TYPE_PARAMETERS,
            "Declaration might vary for different platforms due to the exposed type in type parameters: {0}. " +
                    "Exposed types should be stable across all platforms that share this declaration.",
            RENDER_TYPE,
        )
        put(
            ErrorsNative.PHANTOM_CLASSIFIER,
            "Type {0} is intangible and represents common API, so it shouldn't be used explicitly",
            RENDER_TYPE
        )
    }
}

class DefaultErrorMessagesNative : DefaultErrorMessages.Extension {
    override fun getMap(): DiagnosticFactoryToRendererMap = DIAGNOSTIC_FACTORY_TO_RENDERER
}
