// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere.statistics;

import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class SearchFieldStatisticsCollector implements Disposable {

  private final Project myProject;
  private final JTextField myTextField;

  private int mySymbolKeysTyped;
  private int myNavKeysTyped;

  private SearchFieldStatisticsCollector(JTextField field, Project project) {
    myProject = project;
    myTextField = field;
  }

  public static SearchFieldStatisticsCollector createAndStart(JTextField field,
                                                              Project project) {
    SearchFieldStatisticsCollector res = new SearchFieldStatisticsCollector(field, project);
    res.initListeners();
    return res;
  }

  @Override
  public void dispose() {
    saveStat();
  }

  private void initListeners() {
    myTextField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyTyped(KeyEvent e) {
        mySymbolKeysTyped++;
      }

      @Override
      public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_KP_UP || code == KeyEvent.VK_PAGE_UP
            || code == KeyEvent.VK_DOWN || code == KeyEvent.VK_KP_DOWN || code == KeyEvent.VK_PAGE_DOWN) {
          myNavKeysTyped++;
        }
      }
    });
  }

  private void saveStat() {
    FeatureUsageData data = new FeatureUsageData()
      .addData(SearchEverywhereUsageTriggerCollector.TYPED_NAVIGATION_KEYS, myNavKeysTyped)
      .addData(SearchEverywhereUsageTriggerCollector.TYPED_SYMBOL_KEYS, mySymbolKeysTyped);
    SearchEverywhereUsageTriggerCollector.trigger(myProject, SearchEverywhereUsageTriggerCollector.SESSION_FINISHED, data);
  }
}
