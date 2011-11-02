package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsName;
import org.jetbrains.jet.lang.psi.JetPropertyAccessor;

import java.util.Set;

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
    private static final String CLASS_CREATE_METHOD_NAME = "create";
    private static final String SETTER_PREFIX = "_set_";
    private static final String GETTER_PREFIX = "_get_";

    public static JsName getClassObjectName() {
        //TODO dummy representation
        return new JsName(null, "Class", "Class", "Class");
    }

    public static JsName getNameForAccessor(String propertyName, JetPropertyAccessor accessor) {
        String name = propertyName;
        if (accessor.isGetter()) {
            name += GETTER_PREFIX;
        }
        if (accessor.isSetter()) {
            name += SETTER_PREFIX;
        }
        return new JsName(null, name, name, name);
    }

}
