/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import org.jetbrains.plugins.gradle.model.scala.ScalaCompileOptions;
import org.jetbrains.plugins.gradle.model.scala.ScalaModel;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vladislav.Soroka
 */
public class ScalaModelBuilderImplTest extends AbstractModelBuilderTest {

  public ScalaModelBuilderImplTest(@NotNull String gradleVersion) {
    super(gradleVersion);
  }

  @Test
  public void testScalaModel() {
    DomainObjectSet<? extends IdeaModule> ideaModules = allModels.getModel(IdeaProject.class).getModules();

    List<ScalaModel> scalaModels = ContainerUtil.mapNotNull(
      ideaModules, (Function<IdeaModule, ScalaModel>)module -> allModels.getModel(module, ScalaModel.class));

    assertEquals(1, scalaModels.size());
    ScalaModel scalaModel = scalaModels.get(0);
    ScalaCompileOptions scalaCompileOptions = scalaModel.getScalaCompileOptions();
    assertNotNull(scalaCompileOptions);
    assertEquals(1, scalaCompileOptions.getAdditionalParameters().size());
    assertEquals("-opt:opt", scalaCompileOptions.getAdditionalParameters().iterator().next());
  }

  @Override
  protected Set<Class> getModels() {
    return ContainerUtil.set(ScalaModel.class);
  }
}
