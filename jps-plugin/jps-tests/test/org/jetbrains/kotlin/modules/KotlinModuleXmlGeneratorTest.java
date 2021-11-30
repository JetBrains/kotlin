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

package org.jetbrains.kotlin.modules;

import junit.framework.TestCase;
import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType;
import org.jetbrains.kotlin.build.JvmSourceRoot;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

public class KotlinModuleXmlGeneratorTest extends TestCase {
    public void testBasic() {
        String actual = new KotlinModuleXmlBuilder().addModule(
                "name",
                "output",
                Arrays.asList(new File("s1"), new File("s2")),
                Collections.singletonList(new JvmSourceRoot(new File("java"), null)),
                Arrays.asList(new File("cp1"), new File("cp2")),
                Collections.emptyList(),
                null,
                JavaModuleBuildTargetType.PRODUCTION.getTypeId(),
                JavaModuleBuildTargetType.PRODUCTION.isTests(),
                Collections.emptySet(),
                Collections.emptyList()
        ).asText().toString();
        KotlinTestUtils.assertEqualsToFile(new File("/basic.xml"), actual);
    }

    public void testFiltered() {
        String actual = new KotlinModuleXmlBuilder().addModule(
                "name",
                "output",
                Arrays.asList(new File("s1"), new File("s2")),
                Collections.emptyList(),
                Arrays.asList(new File("cp1"), new File("cp2")),
                Collections.emptyList(),
                null,
                JavaModuleBuildTargetType.PRODUCTION.getTypeId(),
                JavaModuleBuildTargetType.PRODUCTION.isTests(),
                Collections.singleton(new File("cp1")),
                Collections.emptyList()
        ).asText().toString();
        KotlinTestUtils.assertEqualsToFile(new File("/filtered.xml"), actual);
    }

    public void testMultiple() {
        KotlinModuleXmlBuilder builder = new KotlinModuleXmlBuilder();
        builder.addModule(
                "name",
                "output",
                Arrays.asList(new File("s1"), new File("s2")),
                Collections.emptyList(),
                Arrays.asList(new File("cp1"), new File("cp2")),
                Collections.emptyList(),
                null,
                JavaModuleBuildTargetType.PRODUCTION.getTypeId(),
                JavaModuleBuildTargetType.PRODUCTION.isTests(),
                Collections.singleton(new File("cp1")),
                Collections.emptyList()
        );
        builder.addModule(
                "name2",
                "output2",
                Arrays.asList(new File("s12"), new File("s22")),
                Collections.emptyList(),
                Arrays.asList(new File("cp12"), new File("cp22")),
                Collections.emptyList(),
                null,
                JavaModuleBuildTargetType.TEST.getTypeId(),
                JavaModuleBuildTargetType.TEST.isTests(),
                Collections.singleton(new File("cp12")),
                Collections.emptyList()
        );
        String actual = builder.asText().toString();
        KotlinTestUtils.assertEqualsToFile(new File("/multiple.xml"), actual);
    }

    public void testModularJdkRoot() {
        String actual = new KotlinModuleXmlBuilder().addModule(
                "name",
                "output",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                new File("/path/to/modular/jdk"),
                JavaModuleBuildTargetType.PRODUCTION.getTypeId(),
                JavaModuleBuildTargetType.PRODUCTION.isTests(),
                Collections.emptySet(),
                Collections.emptyList()
        ).asText().toString();
        KotlinTestUtils.assertEqualsToFile(new File("/modularJdkRoot.xml"), actual);
    }
}
