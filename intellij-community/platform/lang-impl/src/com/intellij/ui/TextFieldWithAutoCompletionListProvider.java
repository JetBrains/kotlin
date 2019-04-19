// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.AutoCompletionPolicy;
import com.intellij.codeInsight.lookup.CharFilter;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.concurrency.SensitiveProgressWrapper;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.util.textCompletion.DefaultTextCompletionValueDescriptor;
import com.intellij.util.textCompletion.TextCompletionProvider;
import com.intellij.util.textCompletion.TextCompletionValueDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Extend this provider for:
 * <ol><li> caching (implement {@link #getItems(String, boolean, CompletionParameters)}, see {@link #fillCompletionVariants(CompletionParameters, String, CompletionResultSet)});
 * <li> changing completion variants (see {@link #setItems(Collection)}).</ol>
 * <p>
 * Otherwise, use {@link com.intellij.util.textCompletion.ValuesCompletionProvider} for completion from a fixed set of elements
 * or {@link com.intellij.util.TextFieldCompletionProvider} in other cases.
 *
 * @author Roman.Chernyatchik
 */
public abstract class TextFieldWithAutoCompletionListProvider<T> extends DefaultTextCompletionValueDescriptor<T> implements
                                                                                                                 TextCompletionProvider {
  private static final Logger LOG = Logger.getInstance(TextFieldWithAutoCompletionListProvider.class);
  @NotNull protected Collection<T> myVariants;
  @Nullable
  private String myCompletionAdvertisement;

  protected TextFieldWithAutoCompletionListProvider(@Nullable final Collection<T> variants) {
    setItems(variants);
    myCompletionAdvertisement = null;
  }

  @Nullable
  @Override
  public String getPrefix(@NotNull String text, int offset) {
    return getCompletionPrefix(text, offset);
  }

  @NotNull
  @Override
  public CompletionResultSet applyPrefixMatcher(@NotNull CompletionResultSet result, @NotNull String prefix) {
    PrefixMatcher prefixMatcher = createPrefixMatcher(prefix);
    if (prefixMatcher != null) {
      return result.withPrefixMatcher(prefixMatcher);
    }
    return result;
  }

  @Nullable
  @Override
  public CharFilter.Result acceptChar(char c) {
    return null;
  }

  @Override
  public void fillCompletionVariants(@NotNull CompletionParameters parameters,
                                     @NotNull String prefix,
                                     @NotNull CompletionResultSet result) {
    Collection<T> items = getItems(prefix, true, parameters);
    addCompletionElements(result, this, items, -10000);

    final ProgressManager progressManager = ProgressManager.getInstance();
    ProgressIndicator mainIndicator = progressManager.getProgressIndicator();
    final ProgressIndicator indicator = mainIndicator != null ? new SensitiveProgressWrapper(mainIndicator) : new EmptyProgressIndicator();
    Future<Collection<T>>
      future =
      ApplicationManager.getApplication().executeOnPooledThread(() -> progressManager.runProcess(() -> getItems(prefix, false, parameters), indicator));

    while (true) {
      try {
        Collection<T> tasks = future.get(100, TimeUnit.MILLISECONDS);
        if (tasks != null) {
          addCompletionElements(result, this, tasks, 0);
          return;
        }
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Exception ignore) {

      }
      ProgressManager.checkCanceled();
    }
  }

  private static <T> void addCompletionElements(final CompletionResultSet result,
                                                final TextCompletionValueDescriptor<T> descriptor,
                                                final Collection<T> items,
                                                final int index) {
    final AutoCompletionPolicy completionPolicy = ApplicationManager.getApplication().isUnitTestMode()
                                                  ? AutoCompletionPolicy.ALWAYS_AUTOCOMPLETE
                                                  : AutoCompletionPolicy.NEVER_AUTOCOMPLETE;
    int grouping = index;
    for (final T item : items) {
      if (item == null) {
        LOG.error("Null item from " + descriptor);
        continue;
      }

      LookupElementBuilder builder = descriptor.createLookupBuilder(item);
      result.addElement(PrioritizedLookupElement.withGrouping(builder.withAutoCompletionPolicy(completionPolicy), grouping--));
    }
  }

  public void setItems(@Nullable final Collection<T> variants) {
    myVariants = (variants != null) ? variants : Collections.emptyList();
  }

  @NotNull
  public Collection<T> getItems(String prefix, boolean cached, CompletionParameters parameters) {
    if (prefix == null) {
      return Collections.emptyList();
    }

    final List<T> items = new ArrayList<>(myVariants);

    Collections.sort(items, this);
    return items;
  }

  /**
   * Completion list advertisement text, if null advertisement for documentation
   * popup will be shown
   *
   * @return text
   */
  @Override
  @Nullable
  public String getAdvertisement() {
    if (myCompletionAdvertisement != null) return myCompletionAdvertisement;
    String shortcut = KeymapUtil.getFirstKeyboardShortcutText((IdeActions.ACTION_QUICK_JAVADOC));
    String advertisementTail = getQuickDocHotKeyAdvertisementTail(shortcut);
    if (advertisementTail == null) {
      return null;
    }
    return "Pressing " + shortcut + " would show " + advertisementTail;
  }

  @Nullable
  protected String getQuickDocHotKeyAdvertisementTail(@NotNull final String shortcut) {
    return null;
  }

  public void setAdvertisement(@Nullable String completionAdvertisement) {
    myCompletionAdvertisement = completionAdvertisement;
  }

  @Nullable
  public PrefixMatcher createPrefixMatcher(@NotNull final String prefix) {
    return new PlainPrefixMatcher(prefix);
  }

  @NotNull
  public static String getCompletionPrefix(CompletionParameters parameters) {
    String text = parameters.getOriginalFile().getText();
    int offset = parameters.getOffset();
    return getCompletionPrefix(text, offset);
  }

  @NotNull
  private static String getCompletionPrefix(String text, int offset) {
    int i = text.lastIndexOf(' ', offset - 1) + 1;
    int j = text.lastIndexOf('\n', offset - 1) + 1;
    return text.substring(Math.max(i, j), offset);
  }
}
