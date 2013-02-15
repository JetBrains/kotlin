/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.k2js.test.rhino;

import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;

/**
 * Assert that a Rhino function returns a Native Java object which when unwrapped is equal to the expected result
 */
public class RhinoFunctionNativeObjectResultChecker extends RhinoFunctionResultChecker {

    public RhinoFunctionNativeObjectResultChecker(@Nullable String namespaceName, String functionName, Object expectedResult) {
        super(namespaceName, functionName, expectedResult);
    }

    public RhinoFunctionNativeObjectResultChecker(String functionName, Object expectedResult) {
        super(functionName, expectedResult);
    }

    @Override
    protected void assertResultValid(Object result, Context context) {
        if (result instanceof NativeJavaObject) {
            NativeJavaObject nativeJavaObject = (NativeJavaObject) result;
            Object unwrap = nativeJavaObject.unwrap();
            super.assertResultValid(unwrap, context);
        } else {
            super.assertResultValid(result, context);
        }
    }
}
