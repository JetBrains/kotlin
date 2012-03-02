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

package org.jetbrains.k2js.test.semantics;

import org.jetbrains.k2js.test.SingleFileTranslationTest;

/**
 * @author Pavel Talanov
 */
public final class ClassInheritanceTest extends SingleFileTranslationTest {

    public ClassInheritanceTest() {
        super("inheritance/");
    }

    public void testInitializersOfBasicClassExecute() throws Exception {
        runFunctionOutputTest("initializersOfBasicClassExecute.kt", "foo", "box", 3);
    }

    public void testMethodOverride() throws Exception {
        checkFooBoxIsTrue("methodOverride.kt");
    }

    public void testInitializationOrder() throws Exception {
        checkFooBoxIsTrue("initializationOrder.kt");
    }

    public void testComplexInitializationOrder() throws Exception {
        checkFooBoxIsTrue("complexInitializationOrder.kt");
    }

    public void testValuePassedToAncestorConstructor() throws Exception {
        checkFooBoxIsTrue("valuePassedToAncestorConstructor.kt");
    }

    public void testBaseClassDefinedAfterDerived() throws Exception {
        checkFooBoxIsTrue("baseClassDefinedAfterDerived.kt");
    }

    public void testDefinitionOrder() throws Exception {
        checkFooBoxIsTrue("definitionOrder.kt");
    }

    public void testAbstractVarOverride() throws Exception {
        checkFooBoxIsTrue("abstractVarOverride.kt");
    }
}


