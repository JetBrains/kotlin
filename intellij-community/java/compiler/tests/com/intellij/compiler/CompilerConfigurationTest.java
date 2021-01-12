// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler;

import com.intellij.module.ModuleGroupTestsKt;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.testFramework.HeavyPlatformTestCase;
import org.jdom.JDOMException;
import org.jetbrains.jps.model.java.compiler.ProcessorConfigProfile;
import org.jetbrains.jps.model.java.impl.compiler.ProcessorConfigProfileImpl;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.intellij.testFramework.assertions.Assertions.assertThat;

public class CompilerConfigurationTest extends HeavyPlatformTestCase {
  public void testUpdateTargetLevelOnModuleRename() {
    Module module = createModule("foo");
    getConfiguration().setBytecodeTargetLevel(module, "1.6");

    ModuleGroupTestsKt.renameModule(module, "bar");

    assertEquals("1.6", getConfiguration().getBytecodeTargetLevel(module));
  }

  public void testLoadState() throws IOException, JDOMException {
    Module module = createModule("foo");
    CompilerConfigurationImpl configuration = getConfiguration();
    configuration.setBytecodeTargetLevel(module, "1.6");
    assertThat(configuration.getState()).isEqualTo("<state>\n" +
                                                   "  <bytecodeTargetLevel>\n" +
                                                   "    <module name=\"foo\" target=\"1.6\" />\n" +
                                                   "  </bytecodeTargetLevel>\n" +
                                                   "</state>");

    configuration.loadState(JDOMUtil.load("<state>\n" +
                                          "  <bytecodeTargetLevel>\n" +
                                          "    <module name=\"foo\" target=\"1.7\" />\n" +
                                          "  </bytecodeTargetLevel>\n" +
                                          "</state>"));

    assertThat(configuration.getBytecodeTargetLevel(module)).isEqualTo("1.7");
  }

  public void testUpdateOptionsOnModuleRename() {
    Module module = createModule("foo");
    List<String> options = Arrays.asList("-nowarn");
    getConfiguration().setAdditionalOptions(module, options);

    ModuleGroupTestsKt.renameModule(module, "bar");

    assertEquals(options, getConfiguration().getAdditionalOptions(module));
  }

  public void testUpdateAnnotationsProfilesOnModuleRename() {
    Module module = createModule("foo");
    ProcessorConfigProfileImpl profile = new ProcessorConfigProfileImpl("foo");
    profile.addModuleName("foo");
    getConfiguration().addModuleProcessorProfile(profile);
    assertSame(profile, getConfiguration().getAnnotationProcessingConfiguration(module));

    ModuleGroupTestsKt.renameModule(module, "bar");

    ProcessorConfigProfile newProfile = getConfiguration().getAnnotationProcessingConfiguration(module);
    assertNotNull(newProfile);
    assertEquals("bar", assertOneElement(newProfile.getModuleNames()));
  }

  private CompilerConfigurationImpl getConfiguration() {
    return (CompilerConfigurationImpl)CompilerConfiguration.getInstance(myProject);
  }
}
