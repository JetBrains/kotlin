/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.framework.detection.impl.exclude;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColoredListCellRenderer;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
* @author nik
*/
abstract class ExcludeListItem {
  public static final Comparator<ExcludeListItem> COMPARATOR =
    (o1, o2) -> StringUtil.comparePairs(o1.getPresentableFrameworkName(), o1.getFileUrl(), o2.getPresentableFrameworkName(), o2.getFileUrl(), true);

  public abstract void renderItem(ColoredListCellRenderer renderer);

  @Nullable
  public abstract String getPresentableFrameworkName();

  @Nullable
  public abstract String getFrameworkTypeId();

  @Nullable
  public abstract String getFileUrl();
}
