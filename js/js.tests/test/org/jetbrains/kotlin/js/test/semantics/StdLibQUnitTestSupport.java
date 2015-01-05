/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.test.semantics;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.config.EcmaVersion;
import org.jetbrains.kotlin.js.test.rhino.RhinoQUnitResultChecker;
import org.jetbrains.kotlin.js.test.rhino.RhinoResultChecker;

import java.util.List;

/**
 * A base class for any JS compile test cases which should run the generated JS file as a QUnit test case
 */
public abstract class StdLibQUnitTestSupport extends StdLibTestBase {

    @Nullable
    @Override
    protected RhinoResultChecker getResultChecker() {
        return new RhinoQUnitResultChecker();
    }

    @NotNull
    @Override
    protected List<String> additionalJsFiles(@NotNull EcmaVersion ecmaVersion) {
        List<String> files = Lists.newArrayList(super.additionalJsFiles(ecmaVersion));
        files.add("js/js.translator/qunit/qunit.js");
        files.add("js/js.translator/qunit/headless.js");
        return files;
    }
}
