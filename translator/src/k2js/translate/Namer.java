package k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.jet.lang.psi.JetPropertyAccessor;

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
    private static final String SETTER_PREFIX = "_set_";
    private static final String GETTER_PREFIX = "_get_";
    private static final String BACKING_FIELD_PREFIX = "_$_";
   // public static final String DEFAULT_SETTER_PARAM_NAME = "val";

    public static String getClassObjectName() {
        //TODO dummy representation
        return CLASS_OBJECT_NAME;
    }

    public static String getNameForAccessor(String propertyName, JetPropertyAccessor accessor) {
        if (accessor.isGetter()) {
            return getNameForGetter(propertyName);
        }
        if (accessor.isSetter()) {
            return getNameForSetter(propertyName);
        }
        throw new AssertionError("accessor should be a getter or a setter!");
    }

    public static String getBackingFieldNameForProperty(String propertyName) {
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

    public static JsNameRef classObjectReference() {
        //TODO dummy
        return AstUtil.newQualifiedNameRef("Class");
    }

    public static JsNameRef creationMethodReference() {
        return AstUtil.newQualifiedNameRef("Class.create");
    }



}
