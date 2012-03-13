/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.context;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsScope;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.setQualifier;

/**
 * @author Pavel Talanov
 *         <p/>
 *         Encapuslates different types of constants and naming conventions.
 */
public final class Namer {

    private static final String INITIALIZE_METHOD_NAME = "initialize";
    private static final String CLASS_OBJECT_NAME = "Class";
    private static final String TRAIT_OBJECT_NAME = "Trait";
    private static final String NAMESPACE_OBJECT_NAME = "Namespace";
    private static final String OBJECT_OBJECT_NAME = "object";
    private static final String SETTER_PREFIX = "set_";
    private static final String GETTER_PREFIX = "get_";
    private static final String BACKING_FIELD_PREFIX = "$";
    private static final String SUPER_METHOD_NAME = "super_init";
    private static final String KOTLIN_OBJECT_NAME = "Kotlin";
    private static final String ANONYMOUS_NAMESPACE = "Anonymous";
    private static final String RECEIVER_PARAMETER_NAME = "receiver";
    private static final String CLASSES_OBJECT_NAME = "classes";

    @NotNull
    public static String getReceiverParameterName() {
        return RECEIVER_PARAMETER_NAME;
    }

    @NotNull
    public static String getAnonymousNamespaceName() {
        return ANONYMOUS_NAMESPACE;
    }

    @NotNull
    public static JsNameRef initializeMethodReference() {
        return AstUtil.newQualifiedNameRef(INITIALIZE_METHOD_NAME);
    }

    @NotNull
    public static String superMethodName() {
        return SUPER_METHOD_NAME;
    }

    @NotNull
    public static String nameForClassesVariable() {
        return CLASSES_OBJECT_NAME;
    }

    @NotNull
    public static String getNameForAccessor(@NotNull String propertyName, boolean isGetter) {
        if (isGetter) {
            return getNameForGetter(propertyName);
        }
        else {
            return getNameForSetter(propertyName);
        }
    }

    public static String getKotlinBackingFieldName(String propertyName) {
        return getNameWithPrefix(propertyName, BACKING_FIELD_PREFIX);
    }

    public static String getNameForGetter(String propertyName) {
        return getNameWithPrefix(propertyName, GETTER_PREFIX);
    }

    public static String getNameForSetter(String propertyName) {
        return getNameWithPrefix(propertyName, SETTER_PREFIX);
    }

    private static String getNameWithPrefix(String name, String prefix) {
        return prefix + name;
    }

    public static Namer newInstance(@NotNull JsScope rootScope) {
        return new Namer(rootScope);
    }

    @NotNull
    private final JsName kotlinName;
    @NotNull
    private final JsScope kotlinScope;
    @NotNull
    private final JsName className;
    @NotNull
    private final JsName traitName;
    @NotNull
    private final JsName namespaceName;
    @NotNull
    private final JsName objectName;

    private Namer(@NotNull JsScope rootScope) {
        kotlinName = rootScope.declareName(KOTLIN_OBJECT_NAME);
        kotlinScope = new JsScope(rootScope, "Kotlin standard object");
        traitName = kotlinScope.declareName(TRAIT_OBJECT_NAME);
        namespaceName = kotlinScope.declareName(NAMESPACE_OBJECT_NAME);
        className = kotlinScope.declareName(CLASS_OBJECT_NAME);
        objectName = kotlinScope.declareName(OBJECT_OBJECT_NAME);
    }

    @NotNull
    public JsNameRef classCreationMethodReference() {
        return kotlin(createMethodReference(className));
    }

    @NotNull
    public JsNameRef traitCreationMethodReference() {
        return kotlin(createMethodReference(traitName));
    }

    @NotNull
    public JsNameRef namespaceCreationMethodReference() {
        return kotlin(createMethodReference(namespaceName));
    }

    @NotNull
    public JsNameRef objectCreationMethodReference() {
        return kotlin(createMethodReference(objectName));
    }

    @NotNull
    private static JsNameRef createMethodReference(@NotNull JsName name) {
        JsNameRef qualifier = name.makeRef();
        JsNameRef reference = AstUtil.newQualifiedNameRef("create");
        setQualifier(reference, qualifier);
        return reference;
    }

    @NotNull
    private JsNameRef kotlin(@NotNull JsNameRef reference) {
        JsNameRef kotlinReference = kotlinName.makeRef();
        setQualifier(reference, kotlinReference);
        return reference;
    }

    @NotNull
    public JsNameRef kotlinObject() {
        return kotlinName.makeRef();
    }

    @NotNull
    public JsNameRef isOperationReference() {
        return kotlin(AstUtil.newQualifiedNameRef("isType"));
    }

    @NotNull
        /*package*/ JsScope getKotlinScope() {
        return kotlinScope;
    }
}
