// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.util.text.StringUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.jetbrains.plugins.gradle.settings.TestRunner.PLATFORM;

@State(name = "GradleMigrationSettings", storages = @Storage("gradle.xml"))
public class GradleSettingsMigration implements PersistentStateComponent<Element> {
  private Element myElement = new Element("settings");

  public int getMigrationVersion() {
    return StringUtil.parseInt(myElement.getAttributeValue("migrationVersion"), 0);
  }

  public void setMigrationVersion(int version) {
    myElement.setAttribute("migrationVersion", String.valueOf(version));
  }

  @NotNull
  @Override
  public Element getState() {
    return myElement;
  }

  @Override
  public void loadState(@NotNull Element state) {
    myElement = state;
  }

  @State(name = "DefaultGradleProjectSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
  public static class LegacyDefaultGradleProjectSettings implements PersistentStateComponent<LegacyDefaultGradleProjectSettings.MyState> {
    @Nullable private MyState myState = null;

    @Nullable
    @Override
    public MyState getState() {
      return myState;
    }

    @Override
    public void loadState(@NotNull MyState state) {
      myState = state;
    }

    public static class MyState {
      public @NotNull TestRunner testRunner = PLATFORM;
      public boolean delegatedBuild = false;
    }
  }
}