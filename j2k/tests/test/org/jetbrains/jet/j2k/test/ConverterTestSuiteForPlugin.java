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

package org.jetbrains.jet.j2k.test;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.jet.j2k.ast.CallChainExpression;
import org.jetbrains.jet.j2k.ast.LocalVariable;
import org.jetbrains.jet.j2k.ast.types.Type;

import static org.jetbrains.jet.j2k.test.TestPackage.suiteForDirectory;

public class ConverterTestSuiteForPlugin {
    private ConverterTestSuiteForPlugin() {
    }

    public static Test suite() {

        TestSuite suite = new TestSuite();
        suite.addTest(suiteForDirectory("j2k/tests/testData", "/plugin", new NamedTestFactory() {
            public Test createTest(String dataPath, String name) {
                //noinspection JUnitTestCaseWithNoTests
                return new StandaloneJavaToKotlinConverterTest(dataPath, name) {
                    @Override
                    protected void runTest() {
                        CallChainExpression.forceDotCall = true;
                        LocalVariable.specifyTypeExplicitly = false;
                        LocalVariable.forceImmutable = true;
                        Type.forceNotNullTypes = true;
                        super.runTest();
                    }
                };
            }
        }));
        return suite;
    }
}
