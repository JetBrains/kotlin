// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration;

import com.google.common.collect.ImmutableList;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ui.configuration.SdkListItem.*;
import com.intellij.openapi.roots.ui.configuration.SdkDetector.DetectedSdkListener;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel.NewSdkAction;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Consumer;
import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.EventListener;
import java.util.Map;
import java.util.Objects;

public class SdkListModelBuilder {
  @Nullable private final Project myProject;
  @NotNull private final ProjectSdksModel mySdkModel;
  @NotNull private final Condition<? super Sdk> mySdkFilter;
  @NotNull private final Condition<? super SdkTypeId> mySdkTypeFilter;
  @NotNull private final Condition<? super SdkTypeId> mySdkTypeCreationFilter;
  @NotNull private final Consumer<Sdk> myOnNewSdkAdded;

  @NotNull private final EventDispatcher<ModelListener> myModelListener = EventDispatcher.create(ModelListener.class);

  private boolean mySuggestedItemsConnected = false;
  private boolean myIsSdkDetectorInProgress = false;

  private SdkListItem myFirstItem = null;
  private ImmutableList<SdkItem> myHead = ImmutableList.of();
  private ImmutableList<ActionItem> myDownloadActions = ImmutableList.of();
  private ImmutableList<ActionItem> myAddActions = ImmutableList.of();
  private ImmutableList<SuggestedItem> mySuggestions = ImmutableList.of();
  private InvalidSdkItem myInvalidItem = null;

  public SdkListModelBuilder(@Nullable Project project,
                             @NotNull ProjectSdksModel sdkModel,
                             @Nullable Condition<? super SdkTypeId> sdkTypeFilter,
                             @Nullable Condition<? super SdkTypeId> sdkTypeCreationFilter,
                             @Nullable Condition<? super Sdk> sdkFilter) {
    myProject = project;
    mySdkModel = sdkModel;

    mySdkTypeFilter = type -> type != null
                              && (sdkTypeFilter == null || sdkTypeFilter.value(type));

    mySdkTypeCreationFilter = type -> type != null
                                      && (!(type instanceof SdkType) || ((SdkType)type).allowCreationByUser())
                                      && mySdkTypeFilter.value(type)
                                      && (sdkTypeCreationFilter == null || sdkTypeCreationFilter.value(type));

    mySdkFilter = sdk -> sdk != null
                         && mySdkTypeFilter.value(sdk.getSdkType())
                         && (sdkFilter == null || sdkFilter.value(sdk));
    myOnNewSdkAdded = sdk -> {
      if (sdk != null) myModelListener.getMulticaster().onNewSdkAdded(sdk);
    };
  }

  /**
   * Implement this listener to turn a given {@link SdkListModel}
   * into a specific model and apply it for the control
   * @see #addModelListener(ModelListener)
   */
  public interface ModelListener extends EventListener {
    /**
     * Implement this method to turn a given {@link SdkListModel}
     * into a specific model and apply it for the control
     */
    default void syncModel(@NotNull SdkListModel model) {}

    /**
     * A callback executed when a new Sdk was created and added
     */
    default void onNewSdkAdded(@NotNull Sdk sdk) {}
  }

  public void addModelListener(@NotNull ModelListener listener) {
    myModelListener.addListener(listener);
  }

  public void removeListener(@NotNull ModelListener listener) {
    myModelListener.removeListener(listener);
  }

  private void syncModel() {
    myModelListener.getMulticaster().syncModel(buildModel());
  }

  @NotNull
  public SdkListModel buildModel() {
    ImmutableList.Builder<SdkListItem> newModel = ImmutableList.builder();

    if (myFirstItem instanceof ProjectSdkItem) {
      Sdk projectSdk = mySdkModel.getProjectSdk();
      if (projectSdk == null || mySdkFilter.value(projectSdk)) {
        newModel.add(myFirstItem);
      }
    }
    else if (myFirstItem != null) {
      newModel.add(myFirstItem);
    }

    newModel.addAll(myHead);
    if (myInvalidItem != null) {
      newModel.add(myInvalidItem);
    }

    ImmutableList<ActionItem> subItems = ImmutableList.<ActionItem>builder()
      .addAll(myDownloadActions)
      .addAll(myAddActions)
      .build();

    if (subItems.size() > 3 && !newModel.build().isEmpty()) {
      newModel.add(new GroupItem(AllIcons.General.Add, "Add SDK", subItems));
    }
    else {
      newModel.addAll(subItems);
    }

    for (SuggestedItem item : mySuggestions) {
      if (!isApplicableSuggestedItem(item)) continue;
      newModel.add(item);
    }

    return new SdkListModel(myIsSdkDetectorInProgress, newModel.build());
  }

  private boolean isApplicableSuggestedItem(@NotNull SuggestedItem item) {
    if (!mySdkTypeFilter.value(item.getSdkType())) return false;

    for (Sdk sdk : mySdkModel.getSdks()) {
      if (FileUtil.pathsEqual(sdk.getHomePath(), item.getHomePath())) return false;
    }
    return true;
  }

  @NotNull
  public SdkListItem showProjectSdkItem() {
    return setFirstItem(new ProjectSdkItem());
  }

  @NotNull
  public SdkListItem showNoneSdkItem() {
    return setFirstItem(new NoneSdkItem());
  }

  @NotNull
  public SdkListItem setFirstItem(@NotNull SdkListItem firstItem) {
    if (Objects.equals(myFirstItem, firstItem)) return myFirstItem;
    myFirstItem = firstItem;
    syncModel();
    return firstItem;
  }

  @NotNull
  public SdkListItem setInvalidSdk(String name) {
    InvalidSdkItem invalidItem = new InvalidSdkItem(name);
    if (Objects.equals(myInvalidItem, invalidItem)) return myInvalidItem;
    myInvalidItem = invalidItem;
    syncModel();
    return myInvalidItem;
  }

  public void reloadSdks() {
    ImmutableList.Builder<SdkItem> newHead = new ImmutableList.Builder<>();
    for (Sdk sdk : sortSdks(mySdkModel.getSdks())) {
      if (!mySdkFilter.value(sdk)) continue;

      newHead.add(new SdkItem(sdk) {
        @Override
        boolean hasSameSdk(@NotNull Sdk value) {
          return Objects.equals(getSdk(), value) || Objects.equals(mySdkModel.findSdk(getSdk()), value);
        }
      });
    }

    myHead = newHead.build();
    syncModel();
  }

  public void reloadActions(@NotNull JComponent parent, @Nullable Sdk selectedSdk) {
    Map<SdkType, NewSdkAction> downloadActions = mySdkModel.createDownloadActions(parent, selectedSdk, myOnNewSdkAdded, mySdkTypeCreationFilter);
    Map<SdkType, NewSdkAction> addActions = mySdkModel.createAddActions(parent, selectedSdk, myOnNewSdkAdded, mySdkTypeCreationFilter);

    myDownloadActions = createActions(parent, ActionRole.DOWNLOAD, downloadActions);
    myAddActions = createActions(parent, ActionRole.ADD, addActions);
    syncModel();
  }

  public void detectItems(@NotNull JComponent parent, @NotNull Disposable lifetime) {
    if (mySuggestedItemsConnected) return;
    mySuggestedItemsConnected = true;

    SdkDetector.getInstance().getDetectedSdksWithUpdate(myProject, lifetime, ModalityState.stateForComponent(parent), new DetectedSdkListener() {
      @Override
      public void onSearchStarted() {
        mySuggestions = ImmutableList.of();
        myIsSdkDetectorInProgress = true;
        syncModel();
      }

      @Override
      public void onSdkDetected(@NotNull SdkType type, @NotNull String version, @NotNull String home) {
        SuggestedItem item = new SuggestedItem(type, version, home) {
          @Override
          public void executeAction() {
            mySdkModel.addSdk(getSdkType(), getHomePath(), myOnNewSdkAdded);
          }
        };

        mySuggestions = ImmutableList.<SuggestedItem>builder()
          .addAll(mySuggestions)
          .add(item)
          .build();

        syncModel();
      }

      @Override
      public void onSearchCompleted() {
        myIsSdkDetectorInProgress = false;
        syncModel();
      }
    });
  }

  @NotNull
  private static ImmutableList<ActionItem> createActions(@NotNull JComponent parent,
                                                         @NotNull ActionRole role,
                                                         @NotNull Map<SdkType, NewSdkAction> actions) {
    ImmutableList.Builder<ActionItem> builder = ImmutableList.builder();
    for (NewSdkAction action : actions.values()) {
      builder.add(new ActionItem(role, action, null) {
        @Override
        public void executeAction() {
          DataContext dataContext = DataManager.getInstance().getDataContext(parent);
          AnActionEvent event = new AnActionEvent(null,
                                                  dataContext,
                                                  ActionPlaces.UNKNOWN,
                                                  new Presentation(""),
                                                  ActionManager.getInstance(),
                                                  0);
          myAction.actionPerformed(event);
        }
      });
    }
    return builder.build();
  }

  @NotNull
  private static Sdk[] sortSdks(@NotNull final Sdk[] sdks) {
    Sdk[] clone = sdks.clone();
    Arrays.sort(clone, (sdk1, sdk2) -> {
      SdkType sdkType1 = (SdkType)sdk1.getSdkType();
      SdkType sdkType2 = (SdkType)sdk2.getSdkType();
      return !sdkType1.equals(sdkType2)
             ? StringUtil.compare(sdkType1.getPresentableName(), sdkType2.getPresentableName(), true)
             : sdkType1.getComparator().compare(sdk1, sdk2);
    });
    return clone;
  }
}
