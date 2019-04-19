/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

/**
 * Custom folding provider for <a href="http://ui.netbeans.org/docs/ui/code_folding/cf_uispec.html#menus">NetBeans folding conventions.</a>
 * @author Rustam Vishnyakov
 */
public class NetBeansCustomFoldingProvider extends CustomFoldingProvider {
  @Override
  public boolean isCustomRegionStart(String elementText) {
    return elementText.contains("<editor-fold");
  }

  @Override
  public boolean isCustomRegionEnd(String elementText) {
    return elementText.contains("</editor-fold");
  }

  @Override
  public String getPlaceholderText(String elementText) {
    String customText = elementText.replaceFirst(".*desc\\s*=\\s*\"([^\"]*)\".*", "$1").trim();
    return customText.isEmpty() ? "..." : customText;
  }

  @Override
  public String getDescription() {
    return "<editor-fold...> Comments";
  }

  @Override
  public String getStartString() {
    return "<editor-fold desc=\"?\">";
  }

  @Override
  public String getEndString() {
    return "</editor-fold>";
  }

  @Override
  public boolean isCollapsedByDefault(String text) {
    return super.isCollapsedByDefault(text) || text.matches(".*defaultstate\\s*=\\s*\"collapsed\".*");
  }
}
