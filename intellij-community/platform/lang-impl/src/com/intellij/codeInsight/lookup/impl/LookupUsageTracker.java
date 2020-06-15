// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.lookup.impl;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupEvent;
import com.intellij.codeInsight.lookup.LookupListener;
import com.intellij.ide.ui.UISettings;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger;
import com.intellij.internal.statistic.utils.PluginInfoDetectorKt;
import com.intellij.lang.Language;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class LookupUsageTracker {
  private static final String GROUP_ID = "completion";
  private static final String EVENT_ID = "finished";

  private LookupUsageTracker() {
  }

  static void trackLookup(long createdTimestamp, @NotNull LookupImpl lookup) {
    lookup.addLookupListener(new MyLookupTracker(createdTimestamp, lookup));
  }

  private static class MyLookupTracker implements LookupListener {
    private final LookupImpl myLookup;
    private final long myCreatedTimestamp;
    private final long myTimeToShow;
    private final boolean myIsDumbStart;
    private final Language myLanguage;
    private final MyTypingTracker myTypingTracker;

    private int mySelectionChangedCount = 0;


    MyLookupTracker(long createdTimestamp, @NotNull LookupImpl lookup) {
      myLookup = lookup;
      myCreatedTimestamp = createdTimestamp;
      myTimeToShow = System.currentTimeMillis() - createdTimestamp;
      myIsDumbStart = DumbService.isDumb(lookup.getProject());
      myLanguage = getLanguageAtCaret(lookup);
      myTypingTracker = new MyTypingTracker();
      lookup.addPrefixChangeListener(myTypingTracker, lookup);
    }

    @Override
    public void currentItemChanged(@NotNull LookupEvent event) {
      mySelectionChangedCount += 1;
    }

    private boolean isSelectedByTyping(@NotNull LookupElement item) {
      if (myLookup.itemPattern(item).equals(item.getLookupString())) {
        return true;
      }
      return false;
    }

    @Override
    public void itemSelected(@NotNull LookupEvent event) {
      LookupElement item = event.getItem();
      char completionChar = event.getCompletionChar();
      if (item == null) {
        triggerLookupUsed(FinishType.CANCELED_BY_TYPING, null, completionChar);
      }
      else {
        if (isSelectedByTyping(item)) {
          triggerLookupUsed(FinishType.TYPED, item, completionChar);
        }
        else {
          triggerLookupUsed(FinishType.EXPLICIT, item, completionChar);
        }
      }
    }

    @Override
    public void lookupCanceled(@NotNull LookupEvent event) {
      LookupElement item = myLookup.getCurrentItem();
      if (item != null && isSelectedByTyping(item)) {
        triggerLookupUsed(FinishType.TYPED, item, event.getCompletionChar());
      }
      else {
        FinishType detailedCancelType = event.isCanceledExplicitly() ? FinishType.CANCELED_EXPLICITLY : FinishType.CANCELED_BY_TYPING;
        triggerLookupUsed(detailedCancelType, null, event.getCompletionChar());
      }
    }

    private void triggerLookupUsed(@NotNull FinishType finishType, @Nullable LookupElement currentItem,
                                   char completionChar) {
      FeatureUsageData data = new FeatureUsageData();
      addCommonUsageInfo(data, finishType, currentItem, completionChar);

      LookupUsageDescriptor.EP_NAME.forEachExtensionSafe(usageDescriptor -> {
        if (PluginInfoDetectorKt.getPluginInfo(usageDescriptor.getClass()).isSafeToReport()) {
          FeatureUsageData additionalData = new FeatureUsageData();
          usageDescriptor.fillUsageData(myLookup, additionalData);
          data.addAll(additionalData);
        }
      });

      FUCounterUsageLogger.getInstance().logEvent(GROUP_ID, EVENT_ID, data);
    }

    private void addCommonUsageInfo(@NotNull FeatureUsageData data,
                                    @NotNull FinishType finishType,
                                    @Nullable LookupElement currentItem,
                                    char completionChar) {
      // Basic info
      data.addLanguage(myLanguage);
      data.addData("alphabetically", UISettings.getInstance().getSortLookupElementsLexicographically());

      // Quality
      data.addData("finish_type", finishType.toString());
      data.addData("duration", System.currentTimeMillis() - myCreatedTimestamp);
      data.addData("selected_index", myLookup.getSelectedIndex());
      data.addData("selection_changed", mySelectionChangedCount);
      data.addData("typing", myTypingTracker.typing);
      data.addData("backspaces", myTypingTracker.backspaces);
      data.addData("completion_char", CompletionChar.of(completionChar).toString());

      // Details
      if (currentItem != null) {
        data.addData("token_length", currentItem.getLookupString().length());
        data.addData("query_length", myLookup.itemPattern(currentItem).length());
      }

      // Performance
      data.addData("time_to_show", myTimeToShow);

      // Indexing
      data.addData("dumb_start", myIsDumbStart);
      data.addData("dumb_finish", DumbService.isDumb(myLookup.getProject()));
    }

    @Nullable
    private static Language getLanguageAtCaret(@NotNull LookupImpl lookup) {
      PsiFile psiFile = lookup.getPsiFile();
      if (psiFile != null) {
        return PsiUtilCore.getLanguageAtOffset(psiFile, lookup.getEditor().getCaretModel().getOffset());
      }
      return null;
    }

    private static class MyTypingTracker implements PrefixChangeListener {
      int backspaces = 0;
      int typing = 0;

      @Override
      public void beforeTruncate() {
        backspaces += 1;
      }

      @Override
      public void beforeAppend(char c) {
        typing += 1;
      }
    }
  }

  private enum FinishType {
    TYPED, EXPLICIT, CANCELED_EXPLICITLY, CANCELED_BY_TYPING
  }

  private enum CompletionChar {
    ENTER, TAB, COMPLETE_STATEMENT, AUTO_INSERT, OTHER;

    static CompletionChar of(char completionChar) {
      switch (completionChar) {
        case Lookup.NORMAL_SELECT_CHAR:
          return ENTER;
        case Lookup.REPLACE_SELECT_CHAR:
          return TAB;
        case Lookup.AUTO_INSERT_SELECT_CHAR:
          return AUTO_INSERT;
        case Lookup.COMPLETE_STATEMENT_SELECT_CHAR:
          return COMPLETE_STATEMENT;
        default:
          return OTHER;
      }
    }
  }
}
