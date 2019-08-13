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
package com.intellij.ide.fileTemplates;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.CharsetToolkit;
import org.apache.velocity.runtime.parser.ParseException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * @author MYakovlev
 */
public interface FileTemplate extends Cloneable {
  FileTemplate[] EMPTY_ARRAY = new FileTemplate[0];

  String ourEncoding = CharsetToolkit.UTF8;

  String ATTRIBUTE_EXCEPTION = "EXCEPTION";
  String ATTRIBUTE_EXCEPTION_TYPE = "EXCEPTION_TYPE";
  String ATTRIBUTE_DESCRIPTION = "DESCRIPTION";
  String ATTRIBUTE_DISPLAY_NAME = "DISPLAY_NAME";

  String ATTRIBUTE_EXPRESSION = "EXPRESSION";
  String ATTRIBUTE_EXPRESSION_TYPE = "EXPRESSION_TYPE";

  String ATTRIBUTE_RETURN_TYPE = "RETURN_TYPE";
  String ATTRIBUTE_DEFAULT_RETURN_VALUE = "DEFAULT_RETURN_VALUE";
  String ATTRIBUTE_CALL_SUPER = "CALL_SUPER";
  String ATTRIBUTE_PLAIN_CALL_SUPER = "PLAIN_CALL_SUPER";

  String ATTRIBUTE_CLASS_NAME = "CLASS_NAME";
  String ATTRIBUTE_SIMPLE_CLASS_NAME = "SIMPLE_CLASS_NAME";
  String ATTRIBUTE_METHOD_NAME = "METHOD_NAME";
  String ATTRIBUTE_PACKAGE_NAME = "PACKAGE_NAME";

  String ATTRIBUTE_NAME = "NAME";

  /** Relative path of containing directory */
  String ATTRIBUTE_DIR_PATH = "DIR_PATH";
  /** File name with extension */
  String ATTRIBUTE_FILE_NAME = "FILE_NAME";

  /** Name without extension */
  @NotNull
  String getName();

  void setName(@NotNull String name);

  boolean isTemplateOfType(@NotNull FileType fType);

  boolean isDefault();

  @NotNull
  String getDescription();

  @NotNull
  String getText();

  void setText(String text);

  @NotNull
  String getText(@NotNull Map attributes) throws IOException;

  @NotNull
  String getText(@NotNull Properties attributes) throws IOException;

  @NotNull
  String getExtension();

  void setExtension(@NotNull String extension);

  boolean isReformatCode();

  void setReformatCode(boolean reformat);

  boolean isLiveTemplateEnabled();

  void setLiveTemplateEnabled(boolean value);

  @NotNull
  FileTemplate clone();

  @NotNull
  String[] getUnsetAttributes(@NotNull Properties properties, @NotNull Project project) throws ParseException;
}