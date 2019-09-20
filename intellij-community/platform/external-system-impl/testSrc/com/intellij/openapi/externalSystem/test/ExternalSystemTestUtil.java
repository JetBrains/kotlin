// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.test;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.util.HashMap;
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
    Map<?, ?> local = new HashMap<Object, Object>(expected);
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
