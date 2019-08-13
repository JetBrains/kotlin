/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.diagnostic.logging;

import junit.framework.TestCase;

import java.util.Date;

public class LogConsolePreferencesTest extends TestCase {

  public void testWhenFoundOnce() {
    assertClassifiedAs(LogConsolePreferences.DEBUG, "<DEBUG> some debug data");
    assertClassifiedAs(LogConsolePreferences.DEBUG, "some debug data");
    assertClassifiedAs(LogConsolePreferences.DEBUG, "debugger");

    assertClassifiedAs(LogConsolePreferences.ERROR, "2017.02.16 09:00:00 [ERROR] some problem");
    assertClassifiedAs(LogConsolePreferences.ERROR, "Thu Feb 16 17:00:00 CET 2017 <error> Throwable at ");
    assertClassifiedAs(LogConsolePreferences.ERROR, "MESSAGE: we have not seen any errors for long time");

    assertClassifiedAs(LogConsolePreferences.WARNING, "09:00:00 WARN: some problem");
    assertClassifiedAs(LogConsolePreferences.WARNING, new Date().toString() + " WARNING: another one");
    assertClassifiedAs(LogConsolePreferences.WARNING, "WARN: at start of the line");
    assertClassifiedAs(LogConsolePreferences.WARNING, "warn");
    assertClassifiedAs(LogConsolePreferences.WARNING, "this is the last wArNiNg!");

    assertClassifiedAs(LogConsolePreferences.INFO, "09:02:02 INFO: something ");
    assertClassifiedAs(LogConsolePreferences.INFO, "<info>: and more data");
    assertClassifiedAs(LogConsolePreferences.INFO, "we need more information");
  }

  public void testWhenNotFound() {
    assertClassifiedAs(null, "this text does not match the patterns");
    assertClassifiedAs(null, "");
  }

  public void testWhenFoundMany() {
    assertClassifiedAs(LogConsolePreferences.INFO, "09:02:02 INFO: No errors for long time");
    assertClassifiedAs(LogConsolePreferences.ERROR, "09:02:02 error: somethimg bad happens but we need more info: ");
    assertClassifiedAs(LogConsolePreferences.DEBUG, "<DEBUG>: info: 42, warnings : 0, errors: 0");

    assertClassifiedAs(LogConsolePreferences.DEBUG, "some debug info");
    assertClassifiedAs(LogConsolePreferences.INFO, "some info about debug");
  }

  private static void assertClassifiedAs(String expectedType, String text) {
    assertEquals("for input: " + text, expectedType, LogConsolePreferences.getType(text));
  }
}
