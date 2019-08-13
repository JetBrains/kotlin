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

package com.intellij.compiler.ant.taskdefs;

import com.intellij.compiler.ant.Tag;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 */
public class Target extends Tag{
  public Target(@NonNls String name, @Nullable String depends, @Nullable String description, @Nullable String unlessCondition) {
    super("target", getOptions(name, depends, description, unlessCondition));
  }

  public Target(@NonNls String name, @Nullable String depends, @Nullable String description, @Nullable String unlessCondition, @Nullable String xmlNsName, @Nullable String xmlNsValue) {
    super("target", getOptions(name, depends, description, unlessCondition, xmlNsName, xmlNsValue));
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  private static Pair[] getOptions(@NonNls String... names) {
    final List<Pair> options = new ArrayList<>();
    options.add(Couple.of("name", names[0]));
    appendIfNonEmpty(options, "depends", names[1]);
    appendIfNonEmpty(options, "description", names[2]);
    appendIfNonEmpty(options, "unless", names[3]);
    if (names.length > 5) {
      appendIfNonEmpty(options, names[4], names[5]);
    }
    return options.toArray(new Pair[0]);
  }

  private static void appendIfNonEmpty(List<? super Pair> options, final String paramName, String value) {
    if (!StringUtil.isEmptyOrSpaces(value)) {
      options.add(Couple.of(paramName, value));
    }
  }
}
