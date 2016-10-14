/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.resolve.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.Renderers

private val DIAGNOSTIC_FACTORY_TO_RENDERER by lazy {
    with(DiagnosticFactoryToRendererMap("JS")) {

        put(ErrorsJs.NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN,
            "Annotation ''{0}'' is allowed only on member functions of declaration annotated as ''kotlin.js.native'' or on toplevel extension functions", Renderers.RENDER_TYPE)
        put(ErrorsJs.NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER, "Native {0}''s first parameter type should be ''kotlin.String'' or subtype of ''kotlin.Number''", Renderers.STRING)
        put(ErrorsJs.NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS, "Native {0}''s parameter can not have default value", Renderers.STRING)
        put(ErrorsJs.NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE, "Native getter's return type should be nullable")
        put(ErrorsJs.NATIVE_SETTER_WRONG_RETURN_TYPE, "Native setter's return type should be 'Unit' or a supertype of the second parameter's type")
        put(ErrorsJs.NATIVE_INDEXER_WRONG_PARAMETER_COUNT, "Expected {0} parameters for native {1}", Renderers.TO_STRING, Renderers.STRING)
        put(ErrorsJs.JSCODE_ERROR, "JavaScript: {0}", JsCallDataTextRenderer)
        put(ErrorsJs.JSCODE_WARNING, "JavaScript: {0}", JsCallDataTextRenderer)
        put(ErrorsJs.JSCODE_ARGUMENT_SHOULD_BE_CONSTANT, "Argument must be string constant")
        put(ErrorsJs.NOT_SUPPORTED, "Cannot translate (not supported yet): ''{0}''", RenderFirstLineOfElementText)
        put(ErrorsJs.REFERENCE_TO_BUILTIN_MEMBERS_NOT_SUPPORTED, "Callable references for builtin members are not supported yet: ''{0}''", RenderFirstLineOfElementText)
        put(ErrorsJs.JSCODE_NO_JAVASCRIPT_PRODUCED, "Argument must be non-empty JavaScript code")
        put(ErrorsJs.NATIVE_INNER_CLASS_PROHIBITED, "Native inner classes are prohibited")
        put(ErrorsJs.JS_NAME_CLASH, "JavaScript name ({0}) generated for this declaration clashes with another declaration: {1}",
            Renderers.STRING, Renderers.COMPACT)
        put(ErrorsJs.JS_FAKE_NAME_CLASH, "JavaScript name {0} is generated for different inherited members: {1} and {2}",
            Renderers.STRING, Renderers.COMPACT, Renderers.COMPACT)
        put(ErrorsJs.JS_NAME_ON_PRIMARY_CONSTRUCTOR_PROHIBITED, "@JsName annotation is prohibited for primary constructors")
        put(ErrorsJs.JS_NAME_ON_ACCESSOR_AND_PROPERTY, "@JsName can be either on a property or its accessors, not both of them")
        put(ErrorsJs.JS_NAME_IS_NOT_ON_ALL_ACCESSORS, "@JsName should be on all of the property accessors")
        put(ErrorsJs.JS_NAME_PROHIBITED_FOR_OVERRIDE, "@JsName is prohibited for overridden members")
        put(ErrorsJs.JS_NAME_PROHIBITED_FOR_EXTENSION_PROPERTY, "@JsName is prohibited for extension properties")
        put(ErrorsJs.JS_NAME_PROHIBITED_FOR_NAMED_NATIVE, "@JsName is prohibited for @native declaration with explicit name")

        this
    }
}

class DefaultErrorMessagesJs : DefaultErrorMessages.Extension {
    override fun getMap(): DiagnosticFactoryToRendererMap = DIAGNOSTIC_FACTORY_TO_RENDERER
}
