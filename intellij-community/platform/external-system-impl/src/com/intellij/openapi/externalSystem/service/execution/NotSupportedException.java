/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
public class NotSupportedException extends ExternalSystemException {
  public NotSupportedException(@Nullable String message) {
    super(message);
  }

  public NotSupportedException(@Nullable Throwable cause) {
    super(cause);
  }

  public NotSupportedException(@Nullable String message, @NotNull String... quickFixes) {
    super(message, quickFixes);
  }

  public NotSupportedException(@Nullable String message, @Nullable Throwable cause, @NotNull String... quickFixes) {
    super(message, cause, quickFixes);
  }
}
