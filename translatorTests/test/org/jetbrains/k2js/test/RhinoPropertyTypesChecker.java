/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.test;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * @author Pavel Talanov
 */
public final class RhinoPropertyTypesChecker implements RhinoResultChecker {

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
