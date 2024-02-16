/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.*

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
            ErrorsNative.INAPPLICABLE_THREAD_LOCAL,
            "@ThreadLocal is applicable only to property with backing field, to property with delegation or to objects"
        )
        put(ErrorsNative.INAPPLICABLE_THREAD_LOCAL_TOP_LEVEL, "@ThreadLocal is applicable only to top level declarations")
        put(ErrorsNative.INVALID_CHARACTERS_NATIVE, "Name {0}", CommonRenderers.STRING)
        put(ErrorsNative.INAPPLICABLE_OBJC_NAME, "@ObjCName is not applicable on overrides")
        put(ErrorsNative.INVALID_OBJC_NAME, "@ObjCName should have a name and/or swiftName")
        put(ErrorsNative.INVALID_OBJC_NAME_CHARS, "@ObjCName contains illegal characters: {0}", CommonRenderers.STRING)
        put(
            ErrorsNative.INVALID_OBJC_NAME_FIRST_CHAR, "@ObjCName contains illegal first characters: {0}",
            CommonRenderers.STRING
        )
        put(ErrorsNative.EMPTY_OBJC_NAME, "Empty @ObjCName names aren't supported")
        put(
            ErrorsNative.INCOMPATIBLE_OBJC_NAME_OVERRIDE, "Member \"{0}\" inherits inconsistent @ObjCName from {1}",
            Renderers.NAME,
            CommonRenderers.commaSeparated(Renderers.NAME)
        )
        put(ErrorsNative.INAPPLICABLE_EXACT_OBJC_NAME, "Exact @ObjCName is only applicable to classes, objects and interfaces")
        put(ErrorsNative.MISSING_EXACT_OBJC_NAME, "Exact @ObjCName is required to have an ObjC name")
        put(ErrorsNative.NON_LITERAL_OBJC_NAME_ARG, "@ObjCName accepts only literal string and boolean values")
        put(ErrorsNative.REDUNDANT_SWIFT_REFINEMENT, "An ObjC refined declaration can't also be refined in Swift")
        put(
            ErrorsNative.INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE,
            "Refined declaration \"{0}\" overrides declarations with different or no refinement from {1}",
            Renderers.NAME,
            CommonRenderers.commaSeparated(Renderers.NAME)
        )
        put(
            ErrorsNative.INVALID_OBJC_HIDES_TARGETS,
            "@HidesFromObjC annotation is only applicable to annotations with targets CLASS, FUNCTION and/or PROPERTY"
        )
        put(
            ErrorsNative.INVALID_REFINES_IN_SWIFT_TARGETS,
            "@RefinesInSwift annotation is only applicable to annotations with targets FUNCTION and/or PROPERTY"
        )
        put(
            ErrorsNative.SUBTYPE_OF_HIDDEN_FROM_OBJC,
            "Only @HiddenFromObjC declaration can be a subtype of @HiddenFromObjC declaration"
        )

        put(
            ErrorsNative.CANNOT_CHECK_FOR_FORWARD_DECLARATION,
            "Cannot check for forward declaration: ''{0}''",
            Renderers.RENDER_TYPE
        )
        put(ErrorsNative.UNCHECKED_CAST_TO_FORWARD_DECLARATION,
            "Unchecked cast to forward declaration: ''{0}'' to ''{1}''",
            Renderers.RENDER_TYPE,
            Renderers.RENDER_TYPE
        )
        put(
            ErrorsNative.FORWARD_DECLARATION_AS_REIFIED_TYPE_ARGUMENT,
            "Cannot pass forward declaration ''{0}'' for reified type parameter",
            Renderers.RENDER_TYPE
        )
        put(
            ErrorsNative.FORWARD_DECLARATION_AS_CLASS_LITERAL,
            "Can't refer to forward declaration ''{0}'' from class literal",
            Renderers.RENDER_TYPE
        )
        put(
            ErrorsNative.CONFLICTING_OBJC_OVERLOADS,
            "Conflicting overloads: {0}. Add @ObjCSignatureOverride to allow collision for functions inherited from Objective-C.",
            CommonRenderers.commaSeparated(Renderers.FQ_NAMES_IN_TYPES)
        )
        put(
            ErrorsNative.INAPPLICABLE_OBJC_OVERRIDE,
            "@ObjCSignatureOverride is only allowed on functions inherited from Objective-C.",
        )
    }
}

class DefaultErrorMessagesNative : DefaultErrorMessages.Extension {
    override fun getMap(): DiagnosticFactoryToRendererMap = DIAGNOSTIC_FACTORY_TO_RENDERER
}
