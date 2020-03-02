// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.ui.ComboBoxPopupState;
import com.intellij.util.Producer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

import static com.intellij.openapi.roots.ui.configuration.SdkListItem.*;

public final class SdkListModel extends AbstractListModel<SdkListItem> implements ComboBoxPopupState<SdkListItem> {
  private final boolean myIsSearching;
  private final ImmutableList<SdkListItem> myItems;
  private final Producer<? extends Sdk> myGetProjectSdk;
  private final ImmutableMap<SdkListItem, String> mySeparators;

  @NotNull
  public static SdkListModel emptyModel() {
    return new SdkListModel(false, ImmutableList.of(), () -> null);
  }

  SdkListModel(boolean isSearching,
               @NotNull List<? extends SdkListItem> items,
               @NotNull Producer<? extends Sdk> getProjectSdk) {
    myIsSearching = isSearching;
    myItems = ImmutableList.copyOf(items);
    myGetProjectSdk = getProjectSdk;

    boolean myFirstSepSet = false;
    boolean mySuggestedSep = false;
    ImmutableMap.Builder<SdkListItem, String> sep = ImmutableMap.builder();

    int lastSepIndex = 0; //putting 0 to avoid first separator
    for (int i = 0; i < myItems.size(); i++) {
      SdkListItem it = myItems.get(i);

      if (!myFirstSepSet && (it instanceof GroupItem || it instanceof ActionItem)) {
        myFirstSepSet = true;
        if (lastSepIndex < i) {
          sep.put(it, "");
          lastSepIndex = i;
        }
      }

      if (!mySuggestedSep && it instanceof SuggestedItem) {
        mySuggestedSep = true;
        if (lastSepIndex < i) {
          sep.put(it, ProjectBundle.message("jdk.combo.box.autodetected"));
          lastSepIndex = i;
        }
      }
    }
    mySeparators = sep.build();
  }

  @Nullable
  public SdkListItem findProjectSdkItem() {
    return findFirstItemOfType(ProjectSdkItem.class);
  }

  @Nullable
  public SdkListItem findNoneSdkItem() {
    return findFirstItemOfType(NoneSdkItem.class);
  }

  @Nullable
  private SdkListItem findFirstItemOfType(Class<? extends SdkListItem> itemClass) {
    return getItems().stream().filter(itemClass::isInstance).findFirst().orElse(null);
  }

  @Nullable
  Sdk resolveProjectSdk() {
    return myGetProjectSdk.produce();
  }

  @Override
  public int getSize() {
    return myItems.size();
  }

  @Override
  @NotNull
  public SdkListItem getElementAt(int index) {
    return myItems.get(index);
  }

  @Nullable
  @Override
  public SdkListModel onChosen(SdkListItem selectedValue) {
    if (!(selectedValue instanceof GroupItem)) return null;
    return new SdkListModel(myIsSearching, ((GroupItem)selectedValue).mySubItems, myGetProjectSdk);
  }

  @Override
  public boolean hasSubstep(SdkListItem selectedValue) {
    return selectedValue instanceof GroupItem;
  }

  public boolean isSearching() {
    return myIsSearching;
  }

  @NotNull
  public List<SdkListItem> getItems() {
    return myItems;
  }

  @Nullable
  public String getSeparatorTextAbove(@NotNull SdkListItem value) {
    return mySeparators.get(value);
  }

  @Nullable
  public SdkItem findSdkItem(@Nullable Sdk value) {
    if (value == null) return null;
    for (SdkListItem item : myItems) {
      if (!(item instanceof SdkItem)) continue;
      SdkItem sdkItem = (SdkItem)item;
      if (sdkItem.hasSameSdk(value)) {
        return sdkItem;
      }
    }
    return null;
  }

  @Nullable
  public SdkItem findSdkItem(@Nullable String sdkName) {
    if (sdkName == null) return null;
    for (SdkListItem item : myItems) {
      if (!(item instanceof SdkItem)) continue;
      SdkItem sdkItem = (SdkItem)item;
      Sdk sdk = sdkItem.getSdk();
      if (sdkName.equals(sdk.getName())) {
        return sdkItem;
      }
    }
    return null;
  }
}
