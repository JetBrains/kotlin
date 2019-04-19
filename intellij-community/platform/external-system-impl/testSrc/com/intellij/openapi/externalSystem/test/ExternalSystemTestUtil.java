/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.test;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.util.Map;

/**
 * @author Denis Zhdanov
 */
public class ExternalSystemTestUtil {

  public static final ProjectSystemId TEST_EXTERNAL_SYSTEM_ID = new ProjectSystemId("TEST_EXTERNAL_SYSTEM_ID");

  public static final Topic<TestExternalSystemSettingsListener> SETTINGS_TOPIC = Topic.create(
    "TEST_EXTERNAL_SYSTEM_SETTINGS", TestExternalSystemSettingsListener.class
  );

  private ExternalSystemTestUtil() {
  }

  public static void assertMapsEqual(@NotNull Map<?, ?> expected, @NotNull Map<?, ?> actual) {
    Map<?, ?> local = ContainerUtilRt.newHashMap(expected);
    for (Map.Entry<?, ?> entry : actual.entrySet()) {
      Object expectedValue = local.remove(entry.getKey());
      if (expectedValue == null) {
        Assert.fail(String.format("Expected to find '%s' -> '%s' mapping but it doesn't exist", entry.getKey(), entry.getValue()));
      }
      if (!expectedValue.equals(entry.getValue())) {
        Assert.fail(
          String.format("Expected to find '%s' value for the key '%s' but got '%s'", expectedValue, entry.getKey(), entry.getValue())
        );
      }
    }
    if (!local.isEmpty()) {
      Assert.fail("No mappings found for the following keys: " + local.keySet());
    }
  }
}
