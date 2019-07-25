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
package com.intellij.testFramework.assertions;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
public final class Assertions extends org.assertj.core.api.Assertions {
  @NotNull
  public static JdomAssert assertThat(@Nullable Element element) {
    return new JdomAssert(element);
  }

  @NotNull
  public static PathAssertEx assertThat(@Nullable Path actual) {
    return new PathAssertEx(actual);
  }

  @NotNull
  public static StringAssertEx assertThat(@Nullable String actual) {
    return new StringAssertEx(actual);
  }

  @NotNull
  public static <ELEMENT> ListAssertEx<ELEMENT> assertThat(@Nullable List<? extends ELEMENT> actual) {
    return new ListAssertEx<>(actual);
  }
}