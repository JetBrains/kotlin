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

package com.intellij.ide.fileTemplates.ui;

import com.intellij.ide.fileTemplates.impl.AllFileTemplatesConfigurable;
import com.intellij.openapi.options.ex.SingleConfigurableEditor;
import com.intellij.openapi.project.Project;

/*
 * @author: MYakovlev
 */
public class ConfigureTemplatesDialog extends SingleConfigurableEditor{
  private static final String DIMENSION_KEY = "#com.intellij.ide.fileTemplates.ui.ConfigureTemplatesDialog";

  public ConfigureTemplatesDialog(Project project){
    this(project, new AllFileTemplatesConfigurable(project));
  }

  public ConfigureTemplatesDialog(Project project, AllFileTemplatesConfigurable fileTemplatesConfigurable){
    super(project, fileTemplatesConfigurable);
  }

  @Override
  protected String getDimensionServiceKey(){
    return DIMENSION_KEY;
  }
}
