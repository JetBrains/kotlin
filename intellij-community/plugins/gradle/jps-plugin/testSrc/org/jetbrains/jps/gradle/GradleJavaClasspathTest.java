// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.gradle;

import com.intellij.util.ArrayUtilRt;
import com.intellij.util.PathUtil;
import org.jetbrains.jps.builders.CompileScopeTestBuilder;
import org.jetbrains.jps.builders.JpsBuildTestCase;
import org.jetbrains.jps.gradle.model.JpsGradleExtensionService;
import org.jetbrains.jps.gradle.model.JpsGradleModuleExtension;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.module.JpsModuleDependency;
import org.jetbrains.jps.util.JpsPathUtil;

public class GradleJavaClasspathTest extends JpsBuildTestCase {
  public void testProductionOnTestDependency() {
    String srcRoot = PathUtil.getParentPath(createFile("src/A.java", "public class A { ATest test;}"));
    String testRoot = PathUtil.getParentPath(createFile("testSrc/ATest.java", "public class ATest {}"));
    JpsModule m = addModule("m", srcRoot);
    JpsModule commonTests = addModule("common.tests", ArrayUtilRt.EMPTY_STRING_ARRAY,
                                      getAbsolutePath("out/production/common.tests"), getAbsolutePath("out/tests/common.tests"), getJdk());
    commonTests.addSourceRoot(JpsPathUtil.pathToUrl(testRoot), JavaSourceRootType.TEST_SOURCE);

    //this dependency is actually not needed for compilation; it's added to ensure that without proper dependency from production to test targets are build in wrong order
    commonTests.getDependenciesList().addModuleDependency(m);

    JpsModuleDependency dependency = m.getDependenciesList().addModuleDependency(commonTests);
    JpsGradleExtensionService.getInstance().getOrCreateExtension(m, JpsGradleModuleExtension.GRADLE_SOURCE_SET_MODULE_TYPE_KEY);

    doBuild(CompileScopeTestBuilder.rebuild().allModules()).assertFailed();

    JpsGradleExtensionService.getInstance().setProductionOnTestDependency(dependency, true);
    rebuildAllModules();
  }
}
