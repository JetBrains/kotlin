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

package org.jetbrains.k2js.test.semantics;

import org.jetbrains.k2js.test.MultipleFilesTranslationTest;

public class MultiPackageTest extends MultipleFilesTranslationTest {


    public MultiPackageTest() {
        super("multiPackage/");
    }

    public void testFunctionsVisibleFromOtherPackage() throws Exception {
        checkFooBoxIsTrue("functionsVisibleFromOtherPackage");
    }

    //TODO: fails on centos-1 build agent, can't reproduce
    public void TODO_testClassesInheritedFromOtherPackage() throws Exception {
        checkFooBoxIsTrue("classesInheritedFromOtherPackage");
    }

    public void testPackageVariableVisibleFromOtherPackage() throws Exception {
        checkFooBoxIsTrue("packageVariableVisibleFromOtherPackage");
    }

    public void testReflectionFromOtherPackage() throws Exception {
        checkFooBoxIsTrue("reflectionFromOtherPackage");
    }

    public void testNestedPackageFunctionCalledFromOtherPackage() throws Exception {
        runMultiFileTest("nestedPackageFunctionCalledFromOtherPackage", "a.foo", TEST_FUNCTION, true);
    }

    public void testSubpackagesWithClashingNames() throws Exception {
        runMultiFileTest("subpackagesWithClashingNames", "a.foo", TEST_FUNCTION, true);
    }

    public void testSubpackagesWithClashingNamesUsingImport() throws Exception {
        runMultiFileTest("subpackagesWithClashingNamesUsingImport", "a.foo", TEST_FUNCTION, true);
    }

    public void testCreateClassFromOtherPackage() throws Exception {
        runMultiFileTest("createClassFromOtherPackage", "a.foo", TEST_FUNCTION, true);
    }

    public void testCreateClassFromOtherPackageUsingImport() throws Exception {
        runMultiFileTest("createClassFromOtherPackageUsingImport", "a.foo", TEST_FUNCTION, true);
    }
}

