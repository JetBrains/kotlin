// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.conversion;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.ArrayUtilRt;
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
    return ArrayUtilRt.EMPTY_STRING_ARRAY;
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
