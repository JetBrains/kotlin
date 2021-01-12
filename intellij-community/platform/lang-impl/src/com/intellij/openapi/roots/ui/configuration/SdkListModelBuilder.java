// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration;

import com.google.common.collect.ImmutableList;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.projectRoots.SimpleJavaSdkType;
import com.intellij.openapi.roots.ui.configuration.SdkDetector.DetectedSdkListener;
import com.intellij.openapi.roots.ui.configuration.SdkListItem.*;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel.NewSdkAction;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.util.Consumer;
import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.EventListener;
import java.util.Map;
import java.util.Objects;

public final class SdkListModelBuilder {
  @Nullable private final Project myProject;
  @NotNull private final ProjectSdksModel mySdkModel;
  @NotNull private final Condition<? super Sdk> mySdkFilter;
  @NotNull private final Condition<? super SdkTypeId> mySdkTypeFilter;
  @NotNull private final Condition<? super SdkTypeId> mySdkTypeCreationFilter;

  @NotNull private final EventDispatcher<ModelListener> myModelListener = EventDispatcher.create(ModelListener.class);

  private boolean mySuggestedItemsConnected = false;
  private boolean myIsSdkDetectorInProgress = false;

  private ImmutableList<SdkItem> myHead = ImmutableList.of();
  private ImmutableList<ActionItem> myDownloadActions = ImmutableList.of();
  private ImmutableList<ActionItem> myAddActions = ImmutableList.of();
  private ImmutableList<SuggestedItem> mySuggestions = ImmutableList.of();
  private ProjectSdkItem myProjectSdkItem = null;
  private NoneSdkItem myNoneSdkItem = null;
  private InvalidSdkItem myInvalidItem = null;
  private ImmutableList<SdkReferenceItem> myReferenceItems = ImmutableList.of();

  public SdkListModelBuilder(@Nullable Project project,
                             @NotNull ProjectSdksModel sdkModel,
                             @Nullable Condition<? super SdkTypeId> sdkTypeFilter,
                             @Nullable Condition<? super SdkTypeId> sdkTypeCreationFilter,
                             @Nullable Condition<? super Sdk> sdkFilter) {
    myProject = project;
    mySdkModel = sdkModel;

    mySdkTypeFilter = type -> type != null
                              && (sdkTypeFilter == null || sdkTypeFilter.value(type));

    Condition<SdkTypeId> simpleJavaTypeFix = SimpleJavaSdkType.notSimpleJavaSdkTypeIfAlternativeExists();
    mySdkTypeCreationFilter = type -> type != null
                                      && (!(type instanceof SdkType) || ((SdkType)type).allowCreationByUser())
                                      && mySdkTypeFilter.value(type)
                                      && (sdkTypeCreationFilter == null || sdkTypeCreationFilter.value(type))
                                      && simpleJavaTypeFix.value(type);

    mySdkFilter = sdk -> sdk != null
                         && mySdkTypeFilter.value(sdk.getSdkType())
                         && (sdkFilter == null || sdkFilter.value(sdk));
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
  }

  public void addModelListener(@NotNull ModelListener listener) {
    myModelListener.addListener(listener);
  }

  public void removeListener(@NotNull ModelListener listener) {
    myModelListener.removeListener(listener);
  }

  @NotNull
  public SdkReferenceItem addSdkReferenceItem(@NotNull SdkType type,
                                              @NotNull String name,
                                              @Nullable String versionString,
                                              boolean isValid) {
    SdkReferenceItem element = new SdkReferenceItem(type, name, versionString, isValid);
    //similar element might already be included!
    removeSdkReferenceItem(element);

    ImmutableList.Builder<SdkReferenceItem> builder = ImmutableList.builder();
    builder.addAll(myReferenceItems);
    builder.add(element);

    myReferenceItems = builder.build();
    syncModel();

    return element;
  }

  public void removeSdkReferenceItem(@NotNull SdkReferenceItem item) {
    ImmutableList.Builder<SdkReferenceItem> builder = ImmutableList.builder();
    for (SdkReferenceItem element : myReferenceItems) {
      if (Objects.equals(element, item)) continue;

      builder.add(element);
    }

    myReferenceItems = builder.build();
    syncModel();
  }

  @NotNull
  private SdkListModel syncModel() {
    SdkListModel model = buildModel();
    myModelListener.getMulticaster().syncModel(model);
    return model;
  }

  @NotNull
  public SdkListModel buildModel() {
    ImmutableList.Builder<SdkListItem> newModel = ImmutableList.builder();


    if (myNoneSdkItem != null) {
      newModel.add(myNoneSdkItem);
    }
    if (myProjectSdkItem != null) {
      Sdk projectSdk = mySdkModel.getProjectSdk();
      if (projectSdk == null || mySdkFilter.value(projectSdk)) {
        newModel.add(myProjectSdkItem);
      }
    }
    if (myInvalidItem != null) {
      newModel.add(myInvalidItem);
    }

    newModel.addAll(myReferenceItems);

    newModel.addAll(myHead);

    ImmutableList<ActionItem> subItems = ImmutableList.<ActionItem>builder()
      .addAll(myDownloadActions)
      .addAll(myAddActions)
      .build();

    if (subItems.size() > 3 && !newModel.build().isEmpty()) {
      newModel.add(new GroupItem(AllIcons.General.Add, ProjectBundle.message("combobox.item.add.sdk"), subItems));
    }
    else {
      newModel.addAll(subItems);
    }

    for (SuggestedItem item : mySuggestions) {
      if (!isApplicableSuggestedItem(item)) continue;
      newModel.add(item);
    }

    return new SdkListModel(myIsSdkDetectorInProgress, newModel.build(), () -> mySdkModel.getProjectSdk());
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
    ProjectSdkItem projectSdkItem = new ProjectSdkItem();
    if (Objects.equals(myProjectSdkItem, projectSdkItem)) return myProjectSdkItem;
    myProjectSdkItem = projectSdkItem;
    syncModel();
    return myProjectSdkItem;
  }

  @NotNull
  public SdkListItem showNoneSdkItem() {
    NoneSdkItem noneSdkItem = new NoneSdkItem();
    if (Objects.equals(myNoneSdkItem, noneSdkItem)) return myNoneSdkItem;
    myNoneSdkItem = noneSdkItem;
    syncModel();
    return myNoneSdkItem;
  }

  @NotNull
  public SdkListItem showInvalidSdkItem(@NotNull String name) {
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

      newHead.add(newSdkItem(sdk));
    }

    myHead = newHead.build();
    syncModel();
  }

  @NotNull
  private SdkItem newSdkItem(@NotNull Sdk sdk) {
    return new SdkItem(sdk) {
      @Override
      boolean hasSameSdk(@NotNull Sdk value) {
        return Objects.equals(getSdk(), value) || Objects.equals(mySdkModel.findSdk(getSdk()), value);
      }
    };
  }

  /**
   * Executes an action that is associated with the given {@param item}.
   * <br/>
   * If there are no actions associated, method returns {@code false},
   * the {@param afterExecution} is NOT executed
   * <br/>
   * If there is action associated, it is scheduled for execution. The
   * {@param afterExecution} callback is ONLY if the action execution
   * ended up successfully and a new item was added to the model. In that
   * case the callback is executed after the model is updated and the
   * {@link #syncModel()} is invoked. The implementation may not
   * execute the callback or and model update for any internal and
   * non-selectable items
   *
   * @return {@code true} if action was started and the {@param afterExecution}
   *                      callback could happen later, {@code false} otherwise
   */
  public boolean executeAction(@NotNull JComponent parent,
                               @NotNull SdkListItem item,
                               @NotNull Consumer<? super SdkListItem> afterExecution) {
    Consumer<Sdk> onNewSdkAdded = sdk -> {
      reloadSdks();
      SdkItem sdkItem = newSdkItem(sdk);
      afterExecution.consume(sdkItem);
    };

    if (item instanceof ActionItem) {
      NewSdkAction action = ((ActionItem)item).myAction;
      action.actionPerformed(null, parent, onNewSdkAdded);
      return true;
    }

    if (item instanceof SuggestedItem) {
      SuggestedItem suggestedItem = (SuggestedItem)item;
      String homePath = suggestedItem.getHomePath();

      ProgressManager.getInstance().run(new Task.Modal(myProject,
                                                       ProjectBundle.message("progress.title.jdk.combo.box.resolving.jdk.home"),
                                                       true) {

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          indicator.setText(ProjectBundle.message("progress.text.jdk.combo.box.resolving.jdk.home", StringUtil.trimMiddle(homePath, 50)));
          // IDEA-237709
          // make sure VFS has the right image of our SDK to avoid empty SDK from being created
          VfsUtil.markDirtyAndRefresh(false, true, true, new File(homePath));
        }
      });

      mySdkModel.addSdk(suggestedItem.getSdkType(), homePath, onNewSdkAdded);
      return true;
    }

    return false;
  }

  /**
   * Executes an action that is associated with the given {@param item}.
   * <br/>
   * If there are no actions associated, the {@param onSelectableItem} callback
   * is executed directly and the method returns,
   * the {@param afterExecution} is NOT executed
   * <br/>
   * If there is action associated, it is scheduled for execution. The
   * {@param afterExecution} callback is ONLY if the action execution
   * ended up successfully and a new item was added to the model. In that
   * case the callback is executed after the model is updated and the
   * {@link #syncModel()} is invoked. The implementation may not
   * execute the callback or and model update for any internal and
   * non-selectable items
   */
  public void processSelectedElement(@NotNull JComponent parent,
                                     @NotNull SdkListItem item,
                                     @NotNull Consumer<? super SdkListItem> afterNewItemAdded,
                                     @NotNull Consumer<? super SdkListItem> onSelectableItem) {
    if (!executeAction(parent, item,afterNewItemAdded)) {
      onSelectableItem.consume(item);
    }
  }

  public void reloadActions() {
    Map<SdkType, NewSdkAction> downloadActions = mySdkModel.createDownloadActions(mySdkTypeCreationFilter);
    Map<SdkType, NewSdkAction> addActions = mySdkModel.createAddActions(mySdkTypeCreationFilter);

    myDownloadActions = createActions(ActionRole.DOWNLOAD, downloadActions);
    myAddActions = createActions(ActionRole.ADD, addActions);
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
        SuggestedItem item = new SuggestedItem(type, version, home);

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
  private static ImmutableList<ActionItem> createActions(@NotNull ActionRole role,
                                                         @NotNull Map<SdkType, NewSdkAction> actions) {
    ImmutableList.Builder<ActionItem> builder = ImmutableList.builder();
    for (NewSdkAction action : actions.values()) {
      builder.add(new ActionItem(role, action, null));
    }
    return builder.build();
  }

  private static Sdk @NotNull [] sortSdks(final Sdk @NotNull [] sdks) {
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
