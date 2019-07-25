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
package org.jetbrains.plugins.gradle.service.execution;

import com.intellij.openapi.externalSystem.service.execution.NotSupportedException;
import org.gradle.tooling.CancellationToken;
import org.gradle.tooling.CancellationTokenSource;

/**
 * @author Vladislav.Soroka
 */
public class UnsupportedCancellationToken implements CancellationTokenSource {
  @Override
  public void cancel() {
    throw new NotSupportedException("Configured version of Gradle does not support cancellation. \nPlease, use Gradle 2.1 or newer.");
  }

  @Override
  public CancellationToken token() {
    return null;
  }
}
