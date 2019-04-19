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
package com.intellij.lang.javascript.boilerplate;

import com.google.common.collect.Sets;
import com.intellij.BundleBase;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.platform.WebProjectGenerator;
import com.intellij.platform.templates.github.GithubTagInfo;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.ReloadableComboBoxPanel;
import com.intellij.util.ui.ReloadablePanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * @author Sergey Simonchik
 */
public class GithubProjectGeneratorPeer implements WebProjectGenerator.GeneratorPeer<GithubTagInfo> {

  public static String getGithubZipballUrl(String ghUserName,String ghRepoName, String branch) {
    return String.format("https://github.com/%s/%s/zipball/%s", ghUserName, ghRepoName, branch);
  }

  private void createUIComponents() {
    myReloadableComboBoxPanel = new ReloadableComboBoxPanel<GithubTagInfo>() {

      @SuppressWarnings("unchecked")
      @Override
      protected void doUpdateValues(@NotNull Set<GithubTagInfo> tags) {
        if (!shouldUpdate(tags)) {
          return;
        }

        List<GithubTagInfo> sortedTags = createSortedTagList(tags);
        GithubTagInfo selectedItem = getSelectedValue();
        if (selectedItem == null && sortedTags.size() > 0) {
          selectedItem = sortedTags.get(0);
        }
        myComboBox.removeAllItems();
        myComboBox.addItem(myMasterTag);
        for (GithubTagInfo tag : sortedTags) {
          myComboBox.addItem(tag);
        }
        if (selectedItem != null) {
          // restore previously selected item
          for (int i = 0; i < myComboBox.getItemCount(); i++) {
            GithubTagInfo item = GithubTagInfo.tryCast(myComboBox.getItemAt(i));
            if (item != null && item.getName().equals(selectedItem.getName())) {
              myComboBox.setSelectedIndex(i);
              break;
            }
          }
        }
        myComboBox.updateUI();
        fireStateChanged();
      }

      private boolean shouldUpdate(Set<GithubTagInfo> newTags) {
        if (myComboBox.getItemCount() == 0) {
          return true;
        }
        int count = myComboBox.getItemCount();
        Set<GithubTagInfo> oldTags = Sets.newHashSet();
        for (int i = 1; i < count; i++) {
          GithubTagInfo item = ObjectUtils.tryCast(myComboBox.getItemAt(i), GithubTagInfo.class);
          if (item != null) {
            oldTags.add(item);
          }
        }
        return !oldTags.equals(newTags);
      }

      @SuppressWarnings("unchecked")
      @NotNull
      @Override
      protected JComboBox createValuesComboBox() {
        JComboBox box = super.createValuesComboBox();
        box.setRenderer(new ListCellRendererWrapper<GithubTagInfo>() {
          @Override
          public void customize(JList list, GithubTagInfo tag, int index, boolean selected, boolean hasFocus) {
            final String text;
            if (tag == null) {
              text = isBackgroundJobRunning() ? "Loading..." : "Unavailable";
            }
            else {
              text = tag.getName();
            }
            setText(text);
          }
        });

        return box;
      }
    };

    myVersionPanel = myReloadableComboBoxPanel.getMainPanel();
  }

  private final List<WebProjectGenerator.SettingsStateListener> myListeners = ContainerUtil.newArrayList();
  private final GithubTagInfo myMasterTag;
  private final GithubTagListProvider myTagListProvider;
  private JComponent myComponent;
  private JPanel myVersionPanel;
  private ReloadablePanel<GithubTagInfo> myReloadableComboBoxPanel;

  public GithubProjectGeneratorPeer(@NotNull AbstractGithubTagDownloadedProjectGenerator generator) {
    String ghUserName = generator.getGithubUserName();
    String ghRepoName = generator.getGithubRepositoryName();
    myMasterTag = new GithubTagInfo(
      "master",
      getGithubZipballUrl(ghUserName, ghRepoName, "master")
    );

    myTagListProvider = new GithubTagListProvider(ghUserName, ghRepoName);

    myReloadableComboBoxPanel.setDataProvider(new ReloadableComboBoxPanel.DataProvider<GithubTagInfo>() {
      @Override
      public Set<GithubTagInfo> getCachedValues() {
        return myTagListProvider.getCachedTags();
      }

      @Override
      public void updateValuesAsynchronously() {
        myTagListProvider.updateTagListAsynchronously(GithubProjectGeneratorPeer.this);
      }
    });

    myReloadableComboBoxPanel.reloadValuesInBackground();
  }

  void onTagsUpdated(@NotNull Set<GithubTagInfo> tags) {
    myReloadableComboBoxPanel.onUpdateValues(tags);
  }

  void onTagsUpdateError(@NotNull final String errorMessage) {
    myReloadableComboBoxPanel.onValuesUpdateError(errorMessage);
  }

  @NotNull
  private static List<GithubTagInfo> createSortedTagList(@NotNull Collection<GithubTagInfo> tags) {
    List<GithubTagInfo> sortedTags = ContainerUtil.newArrayList(tags);
    Collections.sort(sortedTags, (tag1, tag2) -> {
      GithubTagInfo.Version v1 = tag1.getVersion();
      GithubTagInfo.Version v2 = tag2.getVersion();
      return v2.compareTo(v1);
    });
    for (GithubTagInfo tag : sortedTags) {
      tag.setRecentTag(false);
    }
    if (!sortedTags.isEmpty()) {
      sortedTags.get(0).setRecentTag(true);
    }
    return sortedTags;
  }


  @NotNull
  @Override
  public JComponent getComponent() {
    return myComponent;
  }

  @Override
  public void buildUI(@NotNull SettingsStep settingsStep) {
    settingsStep.addSettingsField(BundleBase.replaceMnemonicAmpersand("&Version:"), myVersionPanel);
    settingsStep.addSettingsComponent(myReloadableComboBoxPanel.getErrorComponent());
  }

  @NotNull
  @Override
  public GithubTagInfo getSettings() {
    GithubTagInfo tag = myReloadableComboBoxPanel.getSelectedValue();
    if (tag == null) {
      throw new RuntimeException("[internal error] No versions available.");
    }
    return tag;
  }

  @Override
  @Nullable
  public ValidationInfo validate() {
    GithubTagInfo tag = myReloadableComboBoxPanel.getSelectedValue();
    if (tag != null) {
      return null;
    }
    String errorMessage = StringUtil.notNullize(myReloadableComboBoxPanel.getErrorComponent().getText());
    if (errorMessage.isEmpty()) {
      errorMessage = "Versions have not been loaded yet.";
    }
    return new ValidationInfo(errorMessage);
  }

  @Override
  public boolean isBackgroundJobRunning() {
    return myReloadableComboBoxPanel.isBackgroundJobRunning();
  }

  @Override
  public void addSettingsStateListener(@NotNull WebProjectGenerator.SettingsStateListener listener) {
    myListeners.add(listener);
  }


  private void fireStateChanged() {
    GithubTagInfo tag = myReloadableComboBoxPanel.getSelectedValue();
    for (WebProjectGenerator.SettingsStateListener listener : myListeners) {
      listener.stateChanged(tag != null);
    }
  }
}
