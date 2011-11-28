package org.jetbrains.k2js.translate.utils;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsScope;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Talanov Pavel
 */

/*
 * This class is a dummy and should completely change in the future
 */
//TODO: rework into stateful class and include into context

public final class Namer {

    private static final String INITIALIZE_METHOD_NAME = "initialize";
    private static final String CLASS_OBJECT_NAME = "Class";
    private static final String TRAIT_OBJECT_NAME = "Trait";
    private static final String NAMESPACE_OBJECT_NAME = "Namespace";
    private static final String SETTER_PREFIX = "set_";
    private static final String GETTER_PREFIX = "get_";
    private static final String BACKING_FIELD_PREFIX = "$";
    // TODO: work on the unified approach to string constants
    public static final String SUPER_METHOD_NAME = "super_init";
    private static final String KOTLIN_OBJECT_NAME = "Kotlin";

    @NotNull
    public static JsNameRef initializeMethodReference() {
        return AstUtil.newQualifiedNameRef(INITIALIZE_METHOD_NAME);
    }

    @NotNull
    public static JsNameRef superMethodReference() {
        return AstUtil.newQualifiedNameRef(SUPER_METHOD_NAME);
    }

    @NotNull
    public static String nameForClassesVariable() {
        return "classes";
    }

    public static String getNameForAccessor(String propertyName, boolean isGetter) {
        if (isGetter) {
            return getNameForGetter(propertyName);
        } else {
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

    private Namer(@NotNull JsScope rootScope) {
        kotlinName = rootScope.declareName(KOTLIN_OBJECT_NAME);
        kotlinScope = new JsScope(rootScope, "Kotlin standard object");
        traitName = kotlinScope.declareName(TRAIT_OBJECT_NAME);
        namespaceName = kotlinScope.declareName(NAMESPACE_OBJECT_NAME);
        className = kotlinScope.declareName(CLASS_OBJECT_NAME);
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
    private JsNameRef createMethodReference(@NotNull JsName name) {
        JsNameRef qualifier = name.makeRef();
        JsNameRef reference = AstUtil.newQualifiedNameRef("create");
        AstUtil.setQualifier(reference, qualifier);
        return reference;
    }

    @NotNull
    private JsNameRef kotlin(@NotNull JsNameRef reference) {
        JsNameRef kotlinReference = kotlinName.makeRef();
        AstUtil.setQualifier(reference, kotlinReference);
        return reference;
    }

    @NotNull
    public JsName declareStandardClass(@NotNull String name) {
        return kotlinScope.declareName(name);
    }

    @NotNull
    public JsNameRef isOperationReference() {
        return kotlin(AstUtil.newQualifiedNameRef("isType"));
    }
}
