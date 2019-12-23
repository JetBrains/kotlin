// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel.NewSdkAction;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Objects;

public abstract class SdkListItem {
  private SdkListItem() {}

  public static abstract class SdkItem extends SdkListItem {
    private final Sdk mySdk;

    SdkItem(@NotNull Sdk sdk) {
      mySdk = sdk;
    }

    @NotNull
    public final Sdk getSdk() {
      return mySdk;
    }

    @Override
    public final boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof SdkItem)) return false;
      SdkItem item = (SdkItem)o;
      return mySdk.equals(item.mySdk);
    }

    @Override
    public final int hashCode() {
      return Objects.hash(mySdk);
    }

    abstract boolean hasSameSdk(@NotNull Sdk value);
  }

  public static final class ProjectSdkItem extends SdkListItem {
    @Override
    public int hashCode() {
      return 42;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof ProjectSdkItem;
    }
  }

  public static final class NoneSdkItem extends SdkListItem {
    @Override
    public int hashCode() {
      return 42;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof NoneSdkItem;
    }
  }

  public static final class InvalidSdkItem extends SdkListItem {
    private final String mySdkName;

    InvalidSdkItem(@NotNull String name) {
      mySdkName = name;
    }

    @NotNull
    public String getSdkName() {
      return mySdkName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof InvalidSdkItem)) return false;
      InvalidSdkItem item = (InvalidSdkItem)o;
      return mySdkName.equals(item.mySdkName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(mySdkName);
    }
  }

  public static final class SuggestedItem extends SdkListItem {
    private final SdkType mySdkType;
    private final String myHomePath;
    private final String myVersion;

    SuggestedItem(@NotNull SdkType sdkType, @NotNull String version, @NotNull String homePath) {
      mySdkType = sdkType;
      myHomePath = homePath;
      myVersion = version;
    }

    @NotNull
    public SdkType getSdkType() {
      return mySdkType;
    }

    @NotNull
    public String getHomePath() {
      return myHomePath;
    }

    @NotNull
    public String getVersion() {
      return myVersion;
    }
  }

  enum ActionRole {
    DOWNLOAD, ADD
  }

  public static final class ActionItem extends SdkListItem {
    @Nullable final GroupItem myGroup;
    @NotNull final ActionRole myRole;
    @NotNull  final NewSdkAction myAction;

    ActionItem(@NotNull ActionRole role, @NotNull NewSdkAction action, @Nullable GroupItem group) {
      myRole = role;
      myAction = action;
      myGroup = group;
    }

    @NotNull
    ActionItem withGroup(@NotNull GroupItem group) {
      return new ActionItem(myRole, myAction, group);
    }
  }

  public static final class GroupItem extends SdkListItem {
    final Icon myIcon;
    final String myCaption;
    final List<? extends SdkListItem> mySubItems;

    GroupItem(@NotNull Icon icon,
              @NotNull String caption,
              @NotNull List<ActionItem> subItems) {
      myIcon = icon;
      myCaption = caption;
      mySubItems = ImmutableList.copyOf(ContainerUtil.map(subItems, it -> it.withGroup(this)));
    }
  }
}
