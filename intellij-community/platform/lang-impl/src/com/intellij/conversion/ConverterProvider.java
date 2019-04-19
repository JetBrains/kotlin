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

package com.intellij.conversion;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public abstract class ConverterProvider {
  public static final ExtensionPointName<ConverterProvider> EP_NAME = ExtensionPointName.create("com.intellij.project.converterProvider");
  private final String myId;

  protected ConverterProvider(@NotNull @NonNls String id) {
    myId = id;
  }

  public String[] getPrecedingConverterIds() {
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  public final String getId() {
    return myId;
  }

  @NotNull
  public abstract String getConversionDescription();

  @NotNull
  public abstract ProjectConverter createConverter(@NotNull ConversionContext context);

  @Nullable
  public String getConversionDialogText(ConversionContext context) {
    return null;
  }

  /**
   * @return {@code false} if the converter cannot determine that the conversion was already performed using project files only.
   * In such case the information about performed conversion will be stored in .ipr file so the converter will not be asked to perform
   * the conversion again.
   */
  public boolean canDetermineIfConversionAlreadyPerformedByProjectFiles() {
    return true;
  }
}
