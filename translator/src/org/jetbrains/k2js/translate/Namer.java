package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;

/**
 * @author Talanov Pavel
 */

/*
 * This class is a complete dummy and needs a lot of work
 */
public final class Namer {

    private Namer() {

    }

    public static final String INITIALIZE_METHOD_NAME = "initialize";
    private static final String CLASS_OBJECT_NAME = "Class";
    //private static final String CLASS_CREATE_METHOD_NAME = "create";
    private static final String SETTER_PREFIX = "set_";
    private static final String GETTER_PREFIX = "get_";
    private static final String BACKING_FIELD_PREFIX = "$";
    public static final String SUPER_METHOD_NAME = "super_init";
    // public static final String DEFAULT_SETTER_PARAM_NAME = "val";

    public static String getClassObjectName() {
        //TODO dummy representation
        return CLASS_OBJECT_NAME;
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

    public static String getNameForNamespace(String name) {
        return name;
    }

    //TODO: dummy
    public static JsNameRef classObjectReference() {
        return AstUtil.newQualifiedNameRef("Class");
    }

    public static JsNameRef classCreationMethodReference() {
        return AstUtil.newQualifiedNameRef("Class.create");
    }

    public static JsNameRef traitCreationMethodReference() {
        return AstUtil.newQualifiedNameRef("Trait.create");
    }


}
