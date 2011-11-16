package org.jetbrains.k2js.test;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * @author Talanov Pavel
 */
public class RhinoPropertyTypesChecker implements RhinoResultChecker {

    final private String objectName;
    final private Map<String, Class<? extends Scriptable>> propertyToType;

    public RhinoPropertyTypesChecker(String objectName, Map<String, Class<? extends Scriptable>> propertyToType) {
        this.objectName = objectName;
        this.propertyToType = propertyToType;
    }

    @Override
    public void runChecks(Context context, Scriptable scope) throws Exception {
        NativeObject object = RhinoUtils.extractObject(objectName, scope);
        verifyObjectHasExpectedPropertiesOfExpectedTypes(object, propertyToType);
    }

    private void verifyObjectHasExpectedPropertiesOfExpectedTypes
            (NativeObject object, Map<String, Class<? extends Scriptable>> nameToClassMap) {
        for (Map.Entry<String, Class<? extends Scriptable>> entry : nameToClassMap.entrySet()) {
            String name = entry.getKey();
            Class expectedClass = entry.getValue();
            assertTrue(object + " must contain key " + name, object.containsKey(name));
            assertTrue(object + "'s property " + name + " must be of type " + expectedClass,
                    expectedClass.isInstance(object.get(name)));
        }
    }

}
