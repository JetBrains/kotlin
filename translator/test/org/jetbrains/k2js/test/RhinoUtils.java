package org.jetbrains.k2js.test;

import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Talanov
 */
public final class RhinoUtils {

    private RhinoUtils() {

    }

    public static NativeObject extractObject(String objectName, Scriptable scope) {
        Object nativeObject = scope.get(objectName, scope);
        assertTrue(objectName + " should be JSON Object", nativeObject instanceof NativeObject);
        return (NativeObject) nativeObject;
    }
}
