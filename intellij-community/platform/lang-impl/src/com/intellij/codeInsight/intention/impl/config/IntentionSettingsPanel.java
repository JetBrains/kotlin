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

package com.intellij.codeInsight.intention.impl.config;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.ide.ui.search.SearchUtil;
import com.intellij.ide.ui.search.SearchableOptionsRegistrar;
import com.intellij.openapi.options.MasterDetails;
import com.intellij.openapi.ui.DetailsComponent;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.util.Alarm;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IntentionSettingsPanel implements MasterDetails {
  private JPanel myPanel;
  private final IntentionSettingsTree myIntentionSettingsTree;
  private final IntentionDescriptionPanel myIntentionDescriptionPanel = new IntentionDescriptionPanel();

  private JPanel myTreePanel;
  private JPanel myDescriptionPanel;
  private DetailsComponent myDetailsComponent;

  private final Alarm myResetAlarm = new Alarm();

  public IntentionSettingsPanel() {
    myDescriptionPanel.setBorder(IdeBorderFactory.createTitledBorder(
      CodeInsightBundle.message("dialog.intention.settings.description.panel.title"), false, JBUI.insetsTop(8)).setShowLine(false));

    myIntentionSettingsTree = new IntentionSettingsTree() {
      @Override
      protected void selectionChanged(Object selected) {
        if (selected instanceof IntentionActionMetaData) {
          final IntentionActionMetaData actionMetaData = (IntentionActionMetaData)selected;
          final Runnable runnable = () -> {
            intentionSelected(actionMetaData);
            if (myDetailsComponent != null) {
              String[] text = new String[actionMetaData.myCategory.length + 1];
              System.arraycopy(actionMetaData.myCategory, 0, text,0,actionMetaData.myCategory.length);
              text[text.length - 1] = actionMetaData.getFamily();
              myDetailsComponent.setText(text);
            }
          };
          myResetAlarm.cancelAllRequests();
          myResetAlarm.addRequest(runnable, 100);
        }
        else {
          categorySelected((String)selected);
          if (myDetailsComponent != null) {
            myDetailsComponent.setText((String)selected);
          }
        }
      }

      @Override
      protected List<IntentionActionMetaData> filterModel(String filter, final boolean force) {
        final List<IntentionActionMetaData> list = IntentionManagerSettings.getInstance().getMetaData();
        if (filter == null || filter.length() == 0) return list;
        final HashSet<String> quoted = new HashSet<>();
        List<Set<String>> keySetList = SearchUtil.findKeys(filter, quoted);
        List<IntentionActionMetaData> result = new ArrayList<>();
        for (IntentionActionMetaData metaData : list) {
          if (isIntentionAccepted(metaData, filter, force, keySetList, quoted)){
            result.add(metaData);
          }
        }
        final Set<String> filters = SearchableOptionsRegistrar.getInstance().getProcessedWords(filter);
        if (force && result.isEmpty()){
          if (filters.size() > 1){
            result = filterModel(filter, false);
          }
        }
        return result;
      }
    };
    myTreePanel.setLayout(new BorderLayout());
    myTreePanel.add(myIntentionSettingsTree.getComponent(), BorderLayout.CENTER);

    GuiUtils.replaceJSplitPaneWithIDEASplitter(myPanel);

    myDescriptionPanel.setLayout(new BorderLayout());
    myDescriptionPanel.add(myIntentionDescriptionPanel.getComponent(), BorderLayout.CENTER);
  }

  private void intentionSelected(IntentionActionMetaData actionMetaData) {
    myIntentionDescriptionPanel.reset(actionMetaData, myIntentionSettingsTree.getFilter());
  }

  private void categorySelected(String intentionCategory) {
    myIntentionDescriptionPanel.reset(intentionCategory);
  }

  public void reset() {
    myIntentionSettingsTree.reset();
    SwingUtilities.invokeLater(() -> myIntentionDescriptionPanel.init(myPanel.getWidth() / 2));
  }

  @Override
  public void initUi() {
    myDetailsComponent = new DetailsComponent();
    myDetailsComponent.setContent(myDescriptionPanel);
  }

  @Override
  public JComponent getToolbar() {
    return myIntentionSettingsTree.getToolbarPanel();
  }

  @Override
  public JComponent getMaster() {
    return myTreePanel;
  }

  @Override
  public DetailsComponent getDetails() {
    return myDetailsComponent;
  }

  public void apply() {
    myIntentionSettingsTree.apply();
  }

  public JPanel getComponent() {
    return myPanel;
  }

  public JTree getIntentionTree(){
    return myIntentionSettingsTree.getTree();
  }

  public boolean isModified() {
    return myIntentionSettingsTree.isModified();
  }

  public void dispose() {
    myIntentionSettingsTree.dispose();
    myIntentionDescriptionPanel.dispose();
  }

  public void selectIntention(String familyName) {
    myIntentionSettingsTree.selectIntention(familyName);
  }

  private static boolean isIntentionAccepted(IntentionActionMetaData metaData, @NonNls String filter, boolean forceInclude,
                                             final List<? extends Set<String>> keySetList, final HashSet<String> quoted) {
    if (StringUtil.containsIgnoreCase(metaData.getFamily(), filter)) {
      return true;
    }
    for (String category : metaData.myCategory) {
      if (category != null && StringUtil.containsIgnoreCase(category, filter)) {
        return true;
      }
    }
    for (String stripped : quoted) {
      if (StringUtil.containsIgnoreCase(metaData.getFamily(), stripped)) {
        return true;
      }
      for (String category : metaData.myCategory) {
        if (category != null && StringUtil.containsIgnoreCase(category, stripped)) {
          return true;
        }
      }
      try {
        final TextDescriptor description = metaData.getDescription();
        if (StringUtil.containsIgnoreCase(description.getText(), stripped)){
          if (!forceInclude) return true;
        } else if (forceInclude) return false;
      }
      catch (IOException e) {
        //skip then
      }
    }
    for (Set<String> keySet : keySetList) {
      if (keySet.contains(metaData.getFamily())) {
        if (!forceInclude) {
          return true;
        }
      }
      else {
        if (forceInclude) {
          return false;
        }
      }
    }
    return forceInclude;
  }

  public Runnable showOption(final String option) {
    return () -> {
      myIntentionSettingsTree.filter(myIntentionSettingsTree.filterModel(option, true));
      myIntentionSettingsTree.setFilter(option);
    };
  }
}
