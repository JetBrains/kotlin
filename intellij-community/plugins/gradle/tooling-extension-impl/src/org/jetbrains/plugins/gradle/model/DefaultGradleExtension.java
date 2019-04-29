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
package org.jetbrains.plugins.gradle.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
public class DefaultGradleExtension extends DefaultGradleProperty implements GradleExtension {
  private static final long serialVersionUID = 1L;
  @Nullable
  private final String myNamedObjectTypeFqn;

  public DefaultGradleExtension(@NotNull String name, @Nullable String typeFqn, @Nullable String namedObjectTypeFqn) {
    super(name, typeFqn, null);
    myNamedObjectTypeFqn = namedObjectTypeFqn;
  }

  public DefaultGradleExtension(GradleExtension extension) {
    this(extension.getName(), extension.getTypeFqn(), extension.getNamedObjectTypeFqn());
  }

  @Nullable
  @Override
  public String getNamedObjectTypeFqn() {
    return myNamedObjectTypeFqn;
  }
}
