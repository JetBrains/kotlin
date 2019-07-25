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
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.openapi.externalSystem.model.LocationAwareExternalSystemException;
import org.gradle.internal.exceptions.LocationAwareException;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vladislav.Soroka
 */
public class BaseProjectImportErrorHandlerTest {
  private BaseProjectImportErrorHandler myErrorHandler;
  private String myProjectPath;

  @Before
  public void setUp() {
    myErrorHandler = new BaseProjectImportErrorHandler();
    myProjectPath = "basic";
  }

  @Test
  public void testGetUserFriendlyError() {
    String causeMsg = "failed to find target current";
    RuntimeException rootCause = new IllegalStateException(causeMsg);
    String locationMsg = "Build file '~/project/build.gradle' line: 86";

    RuntimeException locationError = new RuntimeException(locationMsg, rootCause) {
      @NotNull
      @Override
      public String toString() {
        return LocationAwareException.class.getName() + ": " + super.toString();
      }
    };

    Throwable error = new Throwable(locationError);

    RuntimeException realCause = myErrorHandler.getUserFriendlyError(null, error, myProjectPath, null);
    assertTrue(realCause instanceof LocationAwareExternalSystemException);
    LocationAwareExternalSystemException locationAwareExternalSystemException = (LocationAwareExternalSystemException)realCause;
    assertEquals("~/project/build.gradle", locationAwareExternalSystemException.getFilePath());
    assertEquals(Integer.valueOf(-1), locationAwareExternalSystemException.getColumn());
    assertEquals(Integer.valueOf(86), locationAwareExternalSystemException.getLine());
  }

  @Test
  public void testGetUserFriendlyErrorWithClassNotFoundException() {
    String causeMsg = "com.mypackage.MyImaginaryClass";
    ClassNotFoundException rootCause = new ClassNotFoundException(causeMsg);
    Throwable error = new Throwable(rootCause);
    RuntimeException realCause = myErrorHandler.getUserFriendlyError(null, error, myProjectPath, null);
    assertTrue(realCause.getMessage().contains("Unable to load class 'com.mypackage.MyImaginaryClass'."));
  }
}
