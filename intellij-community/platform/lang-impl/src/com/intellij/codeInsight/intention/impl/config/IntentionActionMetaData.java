// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.config;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.diagnostic.PluginException;
import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.openapi.extensions.PluginId;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class IntentionActionMetaData extends BeforeAfterActionMetaData {
  @NotNull private final IntentionAction myAction;
  public final String @NotNull [] myCategory;
  private String myDirName;
  @NonNls private static final String INTENTION_DESCRIPTION_FOLDER = "intentionDescriptions";

  public IntentionActionMetaData(@NotNull IntentionAction action,
                                 @Nullable ClassLoader loader,
                                 String @NotNull [] category,
                                 @NotNull String descriptionDirectoryName) {
    super(loader, descriptionDirectoryName);

    myAction = action;
    myCategory = category;
  }

  @Override
  public String toString() {
    return getFamily();
  }

  @Nullable
  public PluginId getPluginId() {
    if (myLoader instanceof PluginClassLoader) {
      return ((PluginClassLoader)myLoader).getPluginId();
    }
    return null;
  }

  @NotNull
  public String getFamily() {
    return myAction.getFamilyName();
  }

  @NotNull
  public IntentionAction getAction() {
    return myAction;
  }

  @Override
  protected String getResourceLocation(String resourceName) {
    if (myDirName == null) {
      String dirName = myDescriptionDirectoryName;

      if (myLoader != null && myLoader.getResource(getResourceLocationStatic(dirName, resourceName)) == null) {
        dirName = getFamily();

        if (myLoader.getResource(getResourceLocationStatic(dirName, resourceName)) == null) {
          PluginId pluginId = getPluginId();
          String errorMessage = "Intention Description Dir URL is null: " + getFamily() + "; " + myDescriptionDirectoryName + "; while looking for " + resourceName;
          if (pluginId != null) {
            throw new PluginException(errorMessage, pluginId);
          } else {
            throw new RuntimeException(errorMessage);
          }
        }
      }
      myDirName = dirName;
    }

    return getResourceLocationStatic(myDirName, resourceName);
  }

  @NotNull
  private static String getResourceLocationStatic(String dirName, String resourceName) {
    return INTENTION_DESCRIPTION_FOLDER + "/" + dirName + "/" + resourceName;
  }

  public String getDescriptionDirectoryName() {
    return myDescriptionDirectoryName;
  }
}