/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir.checkers

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers

object KtDefaultErrorMessagesSerialization : BaseDiagnosticRendererFactory() {
    override val MAP = KtDiagnosticFactoryToRendererMap("Serialization").apply {
        put(
            FirSerializationErrors.INLINE_CLASSES_NOT_SUPPORTED,
            "Inline classes require runtime serialization library version at least {0}, while your classpath has {1}.",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
        )
        put(
            FirSerializationErrors.PLUGIN_IS_NOT_ENABLED,
            "kotlinx.serialization compiler plugin is not applied to the module, so this annotation would not be processed. " +
                    "Make sure that you've setup your buildscript correctly and re-import project."
        )
        put(
            FirSerializationErrors.ANONYMOUS_OBJECTS_NOT_SUPPORTED,
            "Anonymous objects or contained in it classes can not be serializable."
        )
        put(
            FirSerializationErrors.INNER_CLASSES_NOT_SUPPORTED,
            "Inner (with reference to outer this) serializable classes are not supported. Remove @Serializable annotation or 'inner' keyword."
        )
        put(
            FirSerializationErrors.COMPANION_OBJECT_AS_CUSTOM_SERIALIZER_DEPRECATED,
            "Class {0} has implicit custom serializer as its companion object. This behaviour is not properly reflected by @Serializable annotation without arguments and therefore is deprecated. " +
                    "To be able to use companion object as the {0} default serializer, please explicitly mention it in the annotation on {0}: @Serializable({0}.Companion::class). " +
                    "For more details, refer to this YouTrack ticket: https://youtrack.jetbrains.com/issue/KT-54441",
            FirDiagnosticRenderers.DECLARATION_NAME
        )
        put(
            FirSerializationErrors.COMPANION_OBJECT_SERIALIZER_INSIDE_OTHER_SERIALIZABLE_CLASS,
            "This class is a Companion object for @Serializable class {0}, but itself is an external serializer for another class {1}. " +
                    "Such declarations are potentially problematic and user-confusing and therefore are deprecated. " +
                    "Please define external serializers as non-companion, preferably top-level objects. " +
                    "For more details, refer to this YouTrack ticket: https://youtrack.jetbrains.com/issue/KT-54441",
            FirDiagnosticRenderers.RENDER_TYPE,
            FirDiagnosticRenderers.RENDER_TYPE
        )
        put(
            FirSerializationErrors.COMPANION_OBJECT_SERIALIZER_INSIDE_NON_SERIALIZABLE_CLASS,
            "This class is a Companion object for non-serializable class {0}, but itself is an external serializer for another class {1}. " +
                    "Such declarations are potentially problematic and user-confusing and therefore are deprecated. " +
                    "Please define external serializers as non-companion, preferably top-level objects. " +
                    "For more details, refer to this YouTrack ticket: https://youtrack.jetbrains.com/issue/KT-54441",
            FirDiagnosticRenderers.RENDER_TYPE,
            FirDiagnosticRenderers.RENDER_TYPE
        )
        put(
            FirSerializationErrors.EXPLICIT_SERIALIZABLE_IS_REQUIRED,
            "Explicit @Serializable annotation on enum class is required when @SerialName or @SerialInfo annotations are used on its members."
        )
        put(
            FirSerializationErrors.SERIALIZABLE_ANNOTATION_IGNORED,
            "@Serializable annotation without arguments can be used only on sealed interfaces." +
                    "Non-sealed interfaces are polymorphically serializable by default."
        )
        put(
            FirSerializationErrors.NON_SERIALIZABLE_PARENT_MUST_HAVE_NOARG_CTOR,
            "Impossible to make this class serializable because its parent is not serializable and does not have exactly one constructor without parameters"
        )
        put(
            FirSerializationErrors.PRIMARY_CONSTRUCTOR_PARAMETER_IS_NOT_A_PROPERTY,
            "This class is not serializable automatically because it has primary constructor parameters that are not properties"
        )
        put(
            FirSerializationErrors.DUPLICATE_SERIAL_NAME,
            "Serializable class has duplicate serial name of property ''{0}'', either in the class itself or its supertypes",
            CommonRenderers.STRING
        )
        put(
            FirSerializationErrors.DUPLICATE_SERIAL_NAME_ENUM,
            "Enum class ''{0}'' has duplicate serial name ''{1}'' in entry ''{2}''",
            FirDiagnosticRenderers.SYMBOL,
            CommonRenderers.STRING,
            CommonRenderers.STRING
        )
        put(
            FirSerializationErrors.SERIALIZER_NOT_FOUND,
            "Serializer has not been found for type ''{0}''. " +
                    "To use context serializer as fallback, explicitly annotate type or property with @Contextual",
            FirDiagnosticRenderers.RENDER_TYPE_WITH_ANNOTATIONS
        )
        put(
            FirSerializationErrors.SERIALIZER_NULLABILITY_INCOMPATIBLE,
            "Type ''{1}'' is non-nullable and therefore can not be serialized with serializer for nullable type ''{0}''",
            FirDiagnosticRenderers.RENDER_TYPE,
            FirDiagnosticRenderers.RENDER_TYPE
        )
        put(
            FirSerializationErrors.SERIALIZER_TYPE_INCOMPATIBLE,
            "Class ''{1}'', which is serializer for type ''{2}'', is applied here to type ''{0}''. This may lead to errors or incorrect behavior.",
            FirDiagnosticRenderers.RENDER_TYPE,
            FirDiagnosticRenderers.RENDER_TYPE,
            FirDiagnosticRenderers.RENDER_TYPE
        )
        put(
            FirSerializationErrors.ABSTRACT_SERIALIZER_TYPE,
            "Custom serializer ''{1}'' on serializable type ''{0}'' can not be instantiated. It is not allowed to specify the interface, abstract or sealed class as a custom serializer.",
            FirDiagnosticRenderers.RENDER_TYPE,
            FirDiagnosticRenderers.RENDER_TYPE
        )
        put(
            FirSerializationErrors.LOCAL_SERIALIZER_USAGE,
            "Class ''{0}'' can't be used as a serializer since it is local",
            FirDiagnosticRenderers.RENDER_TYPE
        )
        put(
            FirSerializationErrors.CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT,
            "Custom serializer ''{0}'' can not be used for ''{1}'' since it has an invalid number of parameters in primary constructor: {2}",
            FirDiagnosticRenderers.RENDER_TYPE,
            FirDiagnosticRenderers.RENDER_TYPE,
            CommonRenderers.STRING
        )
        put(
            FirSerializationErrors.CUSTOM_SERIALIZER_PARAM_ILLEGAL_TYPE,
            "Custom serializer ''{0}'' can not be used for ''{1}'', type of parameter ''{2}'' in serializer's primary constructor should be ''KSerializer''",
            FirDiagnosticRenderers.RENDER_TYPE,
            FirDiagnosticRenderers.RENDER_TYPE,
            CommonRenderers.STRING
        )
        put(
            FirSerializationErrors.TRANSIENT_MISSING_INITIALIZER,
            "This property is marked as @Transient and therefore must have an initializing expression"
        )
        put(
            FirSerializationErrors.TRANSIENT_IS_REDUNDANT,
            "Property does not have backing field which makes it non-serializable and therefore @Transient is redundant"
        )
        put(
            FirSerializationErrors.INCORRECT_TRANSIENT,
            "@kotlin.jvm.Transient does not affect @Serializable classes. Please use @kotlinx.serialization.Transient instead."
        )
        put(
            FirSerializationErrors.GENERIC_ARRAY_ELEMENT_NOT_SUPPORTED,
            "Serialization of Arrays with generic type arguments is impossible because of unknown compile-time type."
        )
        put(
            FirSerializationErrors.REQUIRED_KOTLIN_TOO_HIGH,
            "Your current Kotlin version is {0}, while kotlinx.serialization core runtime {1} requires at least Kotlin {2}. " +
                    "Please update your Kotlin compiler and IDE plugin.",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
            CommonRenderers.STRING
        )

        put(
            FirSerializationErrors.PROVIDED_RUNTIME_TOO_LOW,
            "Your current kotlinx.serialization core version is {0}, while current Kotlin compiler plugin {1} requires at least {2}. " +
                    "Please update your kotlinx.serialization runtime dependency.",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
            CommonRenderers.STRING
        )

        put(
            FirSerializationErrors.INCONSISTENT_INHERITABLE_SERIALINFO,
            "Argument values for inheritable serial info annotation ''{0}'' must be the same as the values in parent type ''{1}''",
            FirDiagnosticRenderers.RENDER_TYPE,
            FirDiagnosticRenderers.RENDER_TYPE
        )
        put(
            FirSerializationErrors.META_SERIALIZABLE_NOT_APPLICABLE,
            "@MetaSerializable annotation can be used only on top-level annotation classes."
        )
        put(
            FirSerializationErrors.INHERITABLE_SERIALINFO_CANT_BE_REPEATABLE,
            "Repeatable serial info annotations can not be inheritable. Either remove @Repeatable or use a regular @SerialInfo annotation."
        )

        put(
            FirSerializationErrors.EXTERNAL_SERIALIZER_USELESS,
            "@Serializer annotation has no effect on class ''{0}'', because all members of KSerializer are already overridden",
            FirDiagnosticRenderers.SYMBOL,
        )

        put(
            FirSerializationErrors.EXTERNAL_CLASS_NOT_SERIALIZABLE,
            "Cannot generate external serializer ''{0}'': class ''{1}'' have constructor parameters which are not properties and therefore it is not serializable automatically",
            FirDiagnosticRenderers.SYMBOL,
            FirDiagnosticRenderers.RENDER_TYPE
        )

        put(
            FirSerializationErrors.EXTERNAL_CLASS_IN_ANOTHER_MODULE,
            "Cannot generate external serializer ''{0}'': class ''{1}'' is defined in another module",
            FirDiagnosticRenderers.SYMBOL,
            FirDiagnosticRenderers.RENDER_TYPE
        )

        put(
            FirSerializationErrors.EXTERNAL_SERIALIZER_NO_SUITABLE_CONSTRUCTOR,
            "Cannot generate external serializer ''{0}'': it must have a constructor with {2} value parameters, because class ''{1}'' has type parameters",
            FirDiagnosticRenderers.SYMBOL,
            FirDiagnosticRenderers.RENDER_TYPE,
            CommonRenderers.STRING
        )
    }
}
