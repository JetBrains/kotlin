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
package org.jetbrains.plugins.gradle.tooling.internal;

import org.gradle.api.Project;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vladislav.Soroka
 */
public class ExtraModelBuilderTest {

  @Test
  public void testVersionMatching() {

    assertTrue(isMatching("1.8", TestModelBuilderService_v_1_8.class));
    assertTrue(isMatching("1.8-rc-1", TestModelBuilderService_v_1_8.class));
    assertFalse(isMatching("1.7", TestModelBuilderService_v_1_8.class));
    assertFalse(isMatching("1.7-rc-1", TestModelBuilderService_v_1_8.class));
    assertFalse(isMatching("1.9", TestModelBuilderService_v_1_8.class));

    assertTrue(isMatching("1.8", TestModelBuilderService_v_1_8_or_Above.class));
    assertTrue(isMatching("1.8-rc-1", TestModelBuilderService_v_1_8_or_Above.class));
    assertFalse(isMatching("1.7", TestModelBuilderService_v_1_8_or_Above.class));
    assertTrue(isMatching("1.9", TestModelBuilderService_v_1_8_or_Above.class));
    assertTrue(isMatching("1.10", TestModelBuilderService_v_1_8_or_Above.class));

    assertTrue(isMatching("1.8", TestModelBuilderService_between_v_1_8_and_1_9.class));
    assertTrue(isMatching("1.8-20130903233633+0000", TestModelBuilderService_between_v_1_8_and_1_9.class));
    assertFalse(isMatching("1.7", TestModelBuilderService_between_v_1_8_and_1_9.class));
    assertFalse(isMatching("1.7-20130903233633+0000", TestModelBuilderService_between_v_1_8_and_1_9.class));
    assertTrue(isMatching("1.9", TestModelBuilderService_between_v_1_8_and_1_9.class));
    assertTrue(isMatching("1.9-20131007220022+0000", TestModelBuilderService_between_v_1_8_and_1_9.class));
    assertFalse(isMatching("1.10", TestModelBuilderService_between_v_1_8_and_1_9.class));
    assertFalse(isMatching("1.10-20131007220022+0000", TestModelBuilderService_between_v_1_8_and_1_9.class));

    assertTrue(isMatching("1.8", TestModelBuilderService_between_v_1_8_and_1_9_NotBase.class));
    assertFalse(isMatching("1.8-20130903233633+0000", TestModelBuilderService_between_v_1_8_and_1_9_NotBase.class));
    assertFalse(isMatching("1.7", TestModelBuilderService_between_v_1_8_and_1_9_NotBase.class));
    assertFalse(isMatching("1.7-20130903233633+0000", TestModelBuilderService_between_v_1_8_and_1_9_NotBase.class));
    assertTrue(isMatching("1.9", TestModelBuilderService_between_v_1_8_and_1_9_NotBase.class));
    assertTrue(isMatching("1.9-20131007220022+0000", TestModelBuilderService_between_v_1_8_and_1_9_NotBase.class));
    assertFalse(isMatching("1.10", TestModelBuilderService_between_v_1_8_and_1_9_NotBase.class));
    assertFalse(isMatching("1.10-20131007220022+0000", TestModelBuilderService_between_v_1_8_and_1_9_NotBase.class));
  }

  @TargetVersions("1.8")
  public static class TestModelBuilderService_v_1_8 implements ModelBuilderService {
    @Override
    public boolean canBuild(String modelName) {
      return TestModelBuilderService_v_1_8.class.getName().equals(modelName);
    }

    @Override
    public Object buildAll(String modelName, Project project) {
      return null;
    }

    @NotNull
    @Override
    public ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
      return ErrorMessageBuilder.create(project, e, "");
    }
  }

  @TargetVersions("1.8+")
  public static class TestModelBuilderService_v_1_8_or_Above implements ModelBuilderService {
    @Override
    public boolean canBuild(String modelName) {
      return TestModelBuilderService_v_1_8_or_Above.class.getName().equals(modelName);
    }

    @Override
    public Object buildAll(String modelName, Project project) {
      return null;
    }

    @NotNull
    @Override
    public ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
      return ErrorMessageBuilder.create(project, e, "");
    }
  }

  @TargetVersions("1.8 <=> 1.9")
  public static class TestModelBuilderService_between_v_1_8_and_1_9 implements ModelBuilderService {
    @Override
    public boolean canBuild(String modelName) {
      return TestModelBuilderService_between_v_1_8_and_1_9.class.getName().equals(modelName);
    }

    @Override
    public Object buildAll(String modelName, Project project) {
      return null;
    }

    @NotNull
    @Override
    public ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
      return ErrorMessageBuilder.create(project, e, "");
    }
  }

  @TargetVersions(value = "1.8 <=> 1.9", checkBaseVersions = false)
  public static class TestModelBuilderService_between_v_1_8_and_1_9_NotBase implements ModelBuilderService {
    @Override
    public boolean canBuild(String modelName) {
      return TestModelBuilderService_between_v_1_8_and_1_9_NotBase.class.getName().equals(modelName);
    }

    @Override
    public Object buildAll(String modelName, Project project) {
      return null;
    }

    @NotNull
    @Override
    public ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
      return ErrorMessageBuilder.create(project, e, "");
    }
  }


  private static boolean isMatching(String version, Class<?> aClass) {
    return new ExtraModelBuilder(GradleVersion.version(version)).canBuild(aClass.getName());
  }
}
