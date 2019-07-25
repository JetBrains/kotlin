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
package com.intellij.lang.customFolding;

import com.intellij.lang.folding.CustomFoldingProvider;
import com.intellij.openapi.util.text.StringUtil;

/**
 * Supports <a href="http://msdn.microsoft.com/en-us/library/9a1ybwek%28v=vs.100%29.aspx">VisualStudio custom foldings.</a>
 * @author Rustam Vishnyakov
 */
public class VisualStudioCustomFoldingProvider extends CustomFoldingProvider {
  @Override
  public boolean isCustomRegionStart(String elementText) {
    return elementText.contains("region") && elementText.matches("[/*#-]*\\s*region.*");
  }

  @Override
  public boolean isCustomRegionEnd(String elementText) {
    return elementText.contains("endregion");
  }

  @Override
  public String getPlaceholderText(String elementText) {
    String textAfterMarker = elementText.replaceFirst("[/*#-]*\\s*region(.*)", "$1");
    String result = elementText.startsWith("/*") ? StringUtil.trimEnd(textAfterMarker, "*/").trim() : textAfterMarker.trim();
    return result.isEmpty() ? "..." : result;
  }

  @Override
  public String getDescription() {
    return "region...endregion Comments";
  }

  @Override
  public String getStartString() {
    return "region ?";
  }

  @Override
  public String getEndString() {
    return "endregion";
  }
}
