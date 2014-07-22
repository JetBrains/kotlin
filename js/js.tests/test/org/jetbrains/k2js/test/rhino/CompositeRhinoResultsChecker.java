/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class CompositeRhinoResultsChecker implements RhinoResultChecker {
    private final RhinoResultChecker[] children;

    public CompositeRhinoResultsChecker(RhinoResultChecker... children) {
        this.children = children;
    }

    @Override
    public void runChecks(Context context, Scriptable scope) throws Exception {
        for (RhinoResultChecker child : children) {
            child.runChecks(context, scope);
        }
    }
}
