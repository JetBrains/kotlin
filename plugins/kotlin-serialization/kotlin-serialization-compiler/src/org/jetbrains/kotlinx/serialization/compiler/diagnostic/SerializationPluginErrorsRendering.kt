/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.diagnostic

import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.Renderers

object SerializationPluginErrorsRendering : DefaultErrorMessages.Extension {
    private val MAP = DiagnosticFactoryToRendererMap("SerializationPlugin")
    override fun getMap() = MAP

    init {
        MAP.put(
            SerializationErrors.INLINE_CLASSES_NOT_SUPPORTED,
            "Inline classes are not supported by kotlinx.serialization yet"
        )
        MAP.put(
            SerializationErrors.PLUGIN_IS_NOT_ENABLED,
            "kotlinx.serialization compiler plugin is not applied to the module, so this annotation would not be processed. " +
                    "Make sure that you've setup your buildscript correctly and re-import project."
        )
        MAP.put(
            SerializationErrors.SERIALIZABLE_ANNOTATION_IGNORED,
            "@Serializable annotation is ignored because it is impossible to serialize automatically interfaces or enums. " +
                    "Provide serializer manually via e.g. companion object"
        )
        MAP.put(
            SerializationErrors.NON_SERIALIZABLE_PARENT_MUST_HAVE_NOARG_CTOR,
            "Impossible to make this class serializable because its parent is not serializable and does not have exactly one constructor without parameters"
        )
        MAP.put(
            SerializationErrors.PRIMARY_CONSTRUCTOR_PARAMETER_IS_NOT_A_PROPERTY,
            "This class is not serializable automatically because it has primary constructor parameters of which are not properties"
        )
        MAP.put(
            SerializationErrors.DUPLICATE_SERIAL_NAME,
            "Serializable class has duplicate serial name of property ''{0}'', either in the class itself or its supertypes",
            Renderers.STRING
        )
        MAP.put(
            SerializationErrors.SERIALIZER_NOT_FOUND,
            "Serializer has not been found for type ''{0}''. " +
                    "To use context serializer as fallback, explicitly annotate type or property with @ContextualSerialization",
            Renderers.RENDER_TYPE_WITH_ANNOTATIONS
        )
        MAP.put(
            SerializationErrors.SERIALIZER_NULLABILITY_INCOMPATIBLE,
            "Type ''{1}'' is non-nullable and therefore can not be serialized with serializer for nullable type ''{0}''",
            Renderers.RENDER_TYPE_WITH_ANNOTATIONS,
            Renderers.RENDER_TYPE_WITH_ANNOTATIONS
        )
        MAP.put(
            SerializationErrors.TRANSIENT_MISSING_INITIALIZER,
            "This property is marked as @Transient and therefore must have an initializing expression"
        )
        MAP.put(
            SerializationErrors.TRANSIENT_IS_REDUNDANT,
            "Property does not have backing field which makes it non-serializable and therefore @Transient is redundant"
        )
    }
}