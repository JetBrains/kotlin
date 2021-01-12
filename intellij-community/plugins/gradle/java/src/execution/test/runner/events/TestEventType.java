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
package org.jetbrains.plugins.gradle.execution.test.runner.events;

/**
 * @author Vladislav.Soroka
 */
public enum TestEventType {
  CONFIGURATION_ERROR("configurationError"),
  REPORT_LOCATION("reportLocation"),
  BEFORE_TEST("beforeTest"),
  ON_OUTPUT("onOutput"),
  AFTER_TEST("afterTest"),
  BEFORE_SUITE("beforeSuite"),
  AFTER_SUITE("afterSuite"),
  UNKNOWN_EVENT("unknown");
  private final String value;

  TestEventType(String v) {
    value = v;
  }

  public String value() {
    return value;
  }

  public static TestEventType fromValue(String v) {
    for (TestEventType c : TestEventType.values()) {
      if (c.value.equals(v)) {
        return c;
      }
    }
    return UNKNOWN_EVENT;
  }
}
