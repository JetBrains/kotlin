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
package com.intellij.packaging.impl.elements;

import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtil;

import java.util.List;

/**
 * @author nik
 */
class FilePathValidator implements InputValidator {
  @Override
  public boolean checkInput(String inputString) {
    final List<String> fileNames = StringUtil.split(FileUtil.toSystemIndependentName(inputString), "/");
    if (fileNames.isEmpty()) {
      return false;
    }
    for (String fileName : fileNames) {
      if (!PathUtil.isValidFileName(fileName)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean canClose(String inputString) {
    return checkInput(inputString);
  }
}
