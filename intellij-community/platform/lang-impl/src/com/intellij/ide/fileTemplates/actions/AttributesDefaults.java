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
package com.intellij.ide.fileTemplates.actions;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.intellij.openapi.util.Pair.pair;

/**
 * @author Roman Chernyatchik
 */
public class AttributesDefaults {
  private final String myDefaultName;
  private final TextRange myDefaultRange;
  private final Map<String, Pair<String, TextRange>> myNamesToValueAndRangeMap = new HashMap<>();
  private Properties myDefaultProperties = null;
  private boolean myFixedName;

  public AttributesDefaults() {
    this(null, null);
  }

  public AttributesDefaults(@Nullable String defaultName) {
    this(defaultName, null);
  }

  public AttributesDefaults(@Nullable String defaultName, @Nullable TextRange defaultRange) {
    myDefaultName = defaultName;
    myDefaultRange = defaultRange;
  }

  @Nullable
  public String getDefaultFileName() {
    return myDefaultName;
  }

  @Nullable
  public TextRange getDefaultFileNameSelection() {
    return myDefaultRange;
  }

  public void add(@NotNull String attributeKey, @NotNull String value) {
    add(attributeKey, value, null);
  }

  public void add(@NotNull String attributeKey, @NotNull String value, @Nullable TextRange selectionRange) {
    myNamesToValueAndRangeMap.put(attributeKey, pair(value, selectionRange));
  }

  public void addPredefined(@NotNull String key, @NotNull String value) {
    if (myDefaultProperties == null) {
      myDefaultProperties = new Properties();
    }
    myDefaultProperties.setProperty(key, value);
  }

  public Properties getDefaultProperties() {
    return myDefaultProperties;
  }

  @Nullable
  public TextRange getRangeFor(@NotNull String attributeKey) {
    final Pair<String, TextRange> valueAndRange = myNamesToValueAndRangeMap.get(attributeKey);
    return Pair.getSecond(valueAndRange);
  }

  @Nullable
  public String getDefaultValueFor(@NotNull String attributeKey) {
    final Pair<String, TextRange> valueAndRange = myNamesToValueAndRangeMap.get(attributeKey);
    return Pair.getFirst(valueAndRange);
  }

  public boolean isFixedName() {
    return myFixedName;
  }

  public AttributesDefaults withFixedName(boolean fixedName) {
    myFixedName = fixedName;
    return this;
  }
}