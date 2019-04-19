// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import com.google.common.collect.Collections2;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author peter
 */
@State(name="ConsoleFoldingSettings", storages=@Storage("consoleFolding.xml"))
public class ConsoleFoldingSettings implements PersistentStateComponent<ConsoleFoldingSettings.MyBean> {
  private final List<String> myPositivePatterns = new ArrayList<>();
  private final List<String> myNegativePatterns = new ArrayList<>();

  public ConsoleFoldingSettings() {
    for (CustomizableConsoleFoldingBean regexp : CustomizableConsoleFoldingBean.EP_NAME.getExtensions()) {
      patternList(regexp.negate).add(regexp.substring);
    }
  }

  public static ConsoleFoldingSettings getSettings() {
    return ServiceManager.getService(ConsoleFoldingSettings.class);
  }

  public boolean shouldFoldLine(String line) {
    return containsAny(line, myPositivePatterns) && !containsAny(line, myNegativePatterns);
  }

  private static boolean containsAny(String line, List<String> patterns) {
    for (String pattern : patterns) {
      if (line.contains(pattern)) {
        return true;
      }
    }
    return false;
  }

  public List<String> getPositivePatterns() {
    return myPositivePatterns;
  }

  public List<String> getNegativePatterns() {
    return myNegativePatterns;
  }

  @Override
  public MyBean getState() {
    MyBean result = new MyBean();
    writeDiff(result.addedPositive, result.removedPositive, false);
    writeDiff(result.addedNegative, result.removedNegative, true);
    return result;
  }

  private void writeDiff(List<String> added, List<String> removed, boolean negated) {
    Set<String> baseline = ContainerUtil.newTroveSet();
    for (CustomizableConsoleFoldingBean regexp : CustomizableConsoleFoldingBean.EP_NAME.getExtensions()) {
      if (regexp.negate == negated) {
        baseline.add(regexp.substring);
      }
    }

    final List<String> current = patternList(negated);
    added.addAll(current);
    added.removeAll(baseline);

    baseline.removeAll(current);
    removed.addAll(baseline);
  }

  private List<String> patternList(boolean negated) {
    return negated ? myNegativePatterns : myPositivePatterns;
  }

  private Collection<String> filterEmptyStringsFromCollection(Collection<String> collection) {
    return Collections2.filter(collection, input -> !StringUtil.isEmpty(input));
  }

  @Override
  public void loadState(@NotNull MyBean state) {
    myPositivePatterns.clear();
    myNegativePatterns.clear();

    Set<String> removedPositive = new HashSet<>(state.removedPositive);
    Set<String> removedNegative = new HashSet<>(state.removedNegative);

    for (CustomizableConsoleFoldingBean regexp : CustomizableConsoleFoldingBean.EP_NAME.getExtensions()) {
      if (!(regexp.negate ? removedNegative : removedPositive).contains(regexp.substring)) {
        patternList(regexp.negate).add(regexp.substring);
      }
    }

    myPositivePatterns.addAll(filterEmptyStringsFromCollection(state.addedPositive));
    myNegativePatterns.addAll(filterEmptyStringsFromCollection(state.addedNegative));

  }

  public static class MyBean {
    public List<String> addedPositive = new ArrayList<>();
    public List<String> addedNegative = new ArrayList<>();
    public List<String> removedPositive = new ArrayList<>();
    public List<String> removedNegative = new ArrayList<>();
  }

}
