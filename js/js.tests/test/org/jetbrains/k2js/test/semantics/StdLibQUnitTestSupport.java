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

package org.jetbrains.k2js.test.semantics;

import com.google.common.collect.Lists;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.test.rhino.RhinoQUnitResultChecker;
import org.jetbrains.k2js.test.rhino.RhinoResultChecker;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.k2js.test.rhino.RhinoUtils.runRhinoTest;

/**
 * A base class for any JS compile test cases which should run the generated JS file as a QUnit test case
 */
public abstract class StdLibQUnitTestSupport extends StdLibTestBase {

    @Override
    protected void performChecksOnGeneratedJavaScript(String path, EcmaVersion version) throws Exception {
        runQUnitTestCase(path, version);
    }

    protected void runQUnitTestCase(String path, EcmaVersion version) throws Exception {
        runQUnitTestCase(path, version, new HashMap<String, Object>());
    }

    protected void runQUnitTestCase(String path, EcmaVersion version, Map<String, Object> variables) throws Exception {
        String moduleId = moduleIdFromOutputFile(path);
        RhinoResultChecker checker = new RhinoQUnitResultChecker(moduleId);
        runRhinoTest(Lists.newArrayList(path),
                     checker, variables, version,
                     Lists.newArrayList(
                             "js/js.translator/qunit/qunit.js",
                             "js/js.translator/qunit/headless.js"
                     ));
    }
}
