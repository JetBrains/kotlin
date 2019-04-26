// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.ide.util.gotoByName.ChooseByNameFilterConfiguration;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.function.Function;

public class PersistentSearchEverywhereContributorFilter<T> {

  private final ChooseByNameFilterConfiguration<? super T> myPersistentConfiguration;
  private final List<T> myElements;
  private final Function<? super T, String> myTextExtractor;
  private final Function<? super T, ? extends Icon> myIconExtractor;

  public PersistentSearchEverywhereContributorFilter(@NotNull List<T> elements,
                                                     @NotNull ChooseByNameFilterConfiguration<? super T> configuration,
                                                     Function<? super T, String> textExtractor,
                                                     Function<? super T, ? extends Icon> iconExtractor) {
    myElements = elements;
    myPersistentConfiguration = configuration;
    myTextExtractor = textExtractor;
    myIconExtractor = iconExtractor;
  }

  public List<T> getAllElements() {
    return myElements;
  }

  public List<T> getSelectedElements() {
    return ContainerUtil.filter(myElements, myPersistentConfiguration::isFileTypeVisible);
  }

  public boolean isSelected(T element) {
    return myPersistentConfiguration.isFileTypeVisible(element);
  }

  public void setSelected(T element, boolean selected) {
    myPersistentConfiguration.setVisible(element, selected);
  }

  public String getElementText(T element) {
    return myTextExtractor.apply(element);
  }

  public Icon getElementIcon(T element) {
    return myIconExtractor.apply(element);
  }
}
