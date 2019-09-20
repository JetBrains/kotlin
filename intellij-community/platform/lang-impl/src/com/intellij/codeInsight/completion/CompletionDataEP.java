/*
 * Copyright 2000-2019 JetBrains s.r.o.
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

package com.intellij.codeInsight.completion;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.LazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;

/**
 * @deprecated see {@link CompletionContributor}
 * @author yole
 */
@Deprecated
public class CompletionDataEP extends AbstractExtensionPointBean {
  public static final ExtensionPointName<CompletionDataEP> EP_NAME = new ExtensionPointName<>("com.intellij.completionData");

  // these must be public for scrambling compatibility
  @Attribute("fileType")
  public String fileType;
  @Attribute("className")
  public String className;


  private final LazyInstance<CompletionData> myHandler = new LazyInstance<CompletionData>() {
    @Override
    protected Class<CompletionData> getInstanceClass() {
      return findExtensionClass(className);
    }
  };

  public CompletionData getHandler() {
    return myHandler.getValue();
  }
}