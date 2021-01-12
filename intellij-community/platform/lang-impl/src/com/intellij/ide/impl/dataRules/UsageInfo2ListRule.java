/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.ide.impl.dataRules;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.usages.UsageView;
import com.intellij.usageView.UsageInfo;

import java.util.Collections;

public class UsageInfo2ListRule implements GetDataRule {
  @Override
  @Nullable
  public Object getData(@NotNull final DataProvider dataProvider) {
    UsageInfo usageInfo = (UsageInfo)dataProvider.getData(UsageView.USAGE_INFO_KEY.getName());
    if (usageInfo != null) return Collections.singletonList(usageInfo);
    return null;
  }
}
