/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.tooling.builder;

import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.model.BuildScriptClasspathModel;
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Vladislav.Soroka
 */
public class ModelBuildScriptClasspathBuilderImplTest extends AbstractModelBuilderTest {

  public ModelBuildScriptClasspathBuilderImplTest(@NotNull String gradleVersion) {
    super(gradleVersion);
  }

  @Test
  @TargetVersions("2.0+")
  public void testModelBuildScriptClasspathBuilder() {

    DomainObjectSet<? extends IdeaModule> ideaModules = allModels.getModel(IdeaProject.class).getModules();

    List<BuildScriptClasspathModel> ideaModule =
      ContainerUtil.mapNotNull(ideaModules, (Function<IdeaModule, BuildScriptClasspathModel>)module -> {
        BuildScriptClasspathModel classpathModel = allModels.getModel(module, BuildScriptClasspathModel.class);

        if (module.getName().equals("moduleWithAdditionalClasspath")) {
          assertNotNull(classpathModel);
          assertEquals(3, classpathModel.getClasspath().size());

          assertEquals("junit-4.11.jar", new File(classpathModel.getClasspath().getAt(0).getClasses().iterator().next()).getName());
          assertEquals("hamcrest-core-1.3.jar", new File(classpathModel.getClasspath().getAt(1).getClasses().iterator().next()).getName());
          assertEquals("someDep.jar", new File(classpathModel.getClasspath().getAt(2).getClasses().iterator().next()).getName());
        }
        else if (module.getName().equals("baseModule") ||
                 module.getName().equals("moduleWithInheritedClasspath")) {
          assertNotNull("Null build classpath for module: " + module.getName(), classpathModel);
          assertEquals("Wrong build classpath for module: " + module.getName(), 3, classpathModel.getClasspath().size());

          assertEquals("Wrong build classpath for module: " + module.getName(), "junit-4.11.jar",
                       new File(classpathModel.getClasspath().getAt(0).getClasses().iterator().next()).getName());
          assertEquals("Wrong build classpath for module: " + module.getName(), "hamcrest-core-1.3.jar",
                       new File(classpathModel.getClasspath().getAt(1).getClasses().iterator().next()).getName());
          assertEquals("Wrong build classpath for module: " + module.getName(), "inheritedDep.jar",
                       new File(classpathModel.getClasspath().getAt(2).getClasses().iterator().next()).getName());
        }
        else if (module.getName().equals("moduleWithoutAdditionalClasspath") ||
                 module.getName().equals("testModelBuildScriptClasspathBuilder")) {
          assertNotNull("Wrong build classpath for module: " + module.getName(), classpathModel);
          assertEquals("Wrong build classpath for module: " + module.getName(), 2, classpathModel.getClasspath().size());
        }
        else {
          fail("Unexpected module found: " + module.getName());
        }

        return classpathModel;
      });

    assertEquals(5, ideaModule.size());
  }

  @Override
  protected Set<Class> getModels() {
    return ContainerUtil.set(BuildScriptClasspathModel.class);
  }
}
