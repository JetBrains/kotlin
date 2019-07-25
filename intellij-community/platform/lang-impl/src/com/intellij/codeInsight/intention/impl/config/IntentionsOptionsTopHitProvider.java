// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.config;

import com.intellij.ide.ui.OptionsTopHitProvider;
import com.intellij.ide.ui.search.BooleanOptionDescription;
import com.intellij.ide.ui.search.OptionDescription;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Sergey.Malenkov
 */
final class IntentionsOptionsTopHitProvider implements OptionsTopHitProvider.ApplicationLevelProvider {
  @NotNull
  @Override
  public String getId() {
    return "intentions";
  }

  @NotNull
  @Override
  public Collection<OptionDescription> getOptions() {
    IntentionManagerSettings settings = IntentionManagerSettings.getInstance();
    Collection<BooleanOptionDescription> options = new ArrayList<>();
    for (IntentionActionMetaData data : settings.getMetaData()) {
      options.add(new Option(settings, data));
    }
    return Collections.unmodifiableCollection(options);
  }

  private static final class Option extends BooleanOptionDescription {
    private final IntentionManagerSettings mySettings;
    private final IntentionActionMetaData myMetaData;

    private Option(@NotNull IntentionManagerSettings settings, IntentionActionMetaData data) {
      super(getOptionName(data), IntentionSettingsConfigurable.HELP_ID);

      mySettings = settings;
      myMetaData = data;
    }

    @Override
    public boolean isOptionEnabled() {
      return mySettings.isEnabled(myMetaData);
    }

    @Override
    public void setOptionState(boolean enabled) {
      mySettings.setEnabled(myMetaData, enabled);
    }

    private static String getOptionName(@NotNull IntentionActionMetaData data) {
      StringBuilder sb = new StringBuilder();
      for (String category : data.myCategory) {
        sb.append(category).append(": ");
      }
      return sb.append(data.getFamily()).toString();
    }
  }
}
