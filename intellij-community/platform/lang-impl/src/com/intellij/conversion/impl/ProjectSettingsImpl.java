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

package com.intellij.conversion.impl;

import com.intellij.conversion.ProjectSettings;
import com.intellij.conversion.CannotConvertException;

import java.io.File;

/**
 * @author nik
 */
public class ProjectSettingsImpl extends ComponentManagerSettingsImpl implements ProjectSettings {
  public ProjectSettingsImpl(File file, ConversionContextImpl context) throws CannotConvertException {
    super(file, context);
  }
}
