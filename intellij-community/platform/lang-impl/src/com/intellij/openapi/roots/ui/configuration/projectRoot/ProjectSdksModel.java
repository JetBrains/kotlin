// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration.projectRoot;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl;
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.MasterDetailsComponent;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.Consumer;
import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author anna
 */
public class ProjectSdksModel implements SdkModel {
  private static final Logger LOG = Logger.getInstance(ProjectSdksModel.class);

  private final HashMap<Sdk, Sdk> myProjectSdks = new HashMap<>();
  private final EventDispatcher<Listener> mySdkEventsDispatcher = EventDispatcher.create(Listener.class);

  private boolean myModified;

  private Sdk myProjectSdk;
  private boolean myInitialized;

  @NotNull
  @Override
  public Listener getMulticaster() {
    return mySdkEventsDispatcher.getMulticaster();
  }

  @NotNull
  @Override
  public Sdk[] getSdks() {
    return myProjectSdks.values().toArray(new Sdk[0]);
  }

  @Override
  @Nullable
  public Sdk findSdk(@NotNull String sdkName) {
    for (Sdk projectJdk : myProjectSdks.values()) {
      if (sdkName.equals(projectJdk.getName())) return projectJdk;
    }
    return null;
  }

  @Override
  public void addListener(@NotNull Listener listener) {
    mySdkEventsDispatcher.addListener(listener);
  }

  @Override
  public void removeListener(@NotNull Listener listener) {
    mySdkEventsDispatcher.removeListener(listener);
  }

  public void reset(@Nullable Project project) {
    myProjectSdks.clear();
    final Sdk[] projectSdks = ProjectJdkTable.getInstance().getAllJdks();
    for (Sdk sdk : projectSdks) {
      try {
        myProjectSdks.put(sdk, (Sdk)sdk.clone());
      }
      catch (CloneNotSupportedException e) {
        LOG.error(e);
      }
    }
    if (project != null) {
      String sdkName = ProjectRootManager.getInstance(project).getProjectSdkName();
      myProjectSdk = sdkName == null ? null : findSdk(sdkName);
    }
    myModified = false;
    myInitialized = true;
  }

  public void disposeUIResources() {
    myProjectSdks.clear();
    myInitialized = false;
  }

  @NotNull
  public HashMap<Sdk, Sdk> getProjectSdks() {
    return myProjectSdks;
  }

  public boolean isModified() {
    return myModified;
  }

  public void apply() throws ConfigurationException {
    apply(null);
  }

  public void apply(@Nullable MasterDetailsComponent configurable) throws ConfigurationException {
    apply(configurable, false);
  }

  public void apply(@Nullable MasterDetailsComponent configurable, boolean addedOnly) throws ConfigurationException {
    String[] errorString = new String[1];
    if (!canApply(errorString, configurable, addedOnly)) {
      throw new ConfigurationException(errorString[0]);
    }

    doApply();
    myModified = false;
  }

  private void doApply() {
    ApplicationManager.getApplication().runWriteAction(() -> {
      final ArrayList<Sdk> itemsInTable = new ArrayList<>();
      final ProjectJdkTable jdkTable = ProjectJdkTable.getInstance();
      final Sdk[] allFromTable = jdkTable.getAllJdks();

      // Delete removed and fill itemsInTable
      for (final Sdk tableItem : allFromTable) {
        if (myProjectSdks.containsKey(tableItem)) {
          itemsInTable.add(tableItem);
        }
        else {
          jdkTable.removeJdk(tableItem);
        }
      }

      // Now all removed items are deleted from table, itemsInTable contains all items in table
      for (Sdk originalJdk : itemsInTable) {
        final Sdk modifiedJdk = myProjectSdks.get(originalJdk);
        LOG.assertTrue(modifiedJdk != null);
        LOG.assertTrue(originalJdk != modifiedJdk);
        jdkTable.updateJdk(originalJdk, modifiedJdk);
      }
      // Add new items to table
      final Sdk[] allJdks = jdkTable.getAllJdks();
      for (final Sdk projectJdk : myProjectSdks.keySet()) {
        LOG.assertTrue(projectJdk != null);
        if (ArrayUtilRt.find(allJdks, projectJdk) == -1) {
          jdkTable.addJdk(projectJdk);
          jdkTable.updateJdk(projectJdk, myProjectSdks.get(projectJdk));
        }
      }
    });
  }

  private boolean canApply(@NotNull String[] errorString, @Nullable MasterDetailsComponent rootConfigurable, boolean addedOnly) throws ConfigurationException {
    Map<Sdk, Sdk> sdks = new LinkedHashMap<>(myProjectSdks);
    if (addedOnly) {
      Sdk[] allJdks = ProjectJdkTable.getInstance().getAllJdks();
      for (Sdk jdk : allJdks) {
        sdks.remove(jdk);
      }
    }
    ArrayList<String> allNames = new ArrayList<>();
    Sdk itemWithError = null;
    for (Sdk currItem : sdks.values()) {
      String currName = currItem.getName();
      if (currName.isEmpty()) {
        itemWithError = currItem;
        errorString[0] = ProjectBundle.message("sdk.list.name.required.error");
        break;
      }
      if (allNames.contains(currName)) {
        itemWithError = currItem;
        errorString[0] = ProjectBundle.message("sdk.list.unique.name.required.error");
        break;
      }
      final SdkAdditionalData sdkAdditionalData = currItem.getSdkAdditionalData();
      if (sdkAdditionalData instanceof ValidatableSdkAdditionalData) {
        try {
          ((ValidatableSdkAdditionalData)sdkAdditionalData).checkValid(this);
        }
        catch (ConfigurationException e) {
          if (rootConfigurable != null) {
            final Object projectJdk = rootConfigurable.getSelectedObject();
            if (!(projectJdk instanceof Sdk) ||
                !Comparing.strEqual(((Sdk)projectJdk).getName(), currName)) { //do not leave current item with current name
              rootConfigurable.selectNodeInTree(currName);
            }
          }
          throw new ConfigurationException(ProjectBundle.message("sdk.configuration.exception", currName) + " " + e.getMessage());
        }
      }
      allNames.add(currName);
    }
    if (itemWithError == null) return true;
    if (rootConfigurable != null) {
      rootConfigurable.selectNodeInTree(itemWithError.getName());
    }
    return false;
  }

  public void removeSdk(@NotNull Sdk editableObject) {
    Sdk projectJdk = null;
    for (Sdk jdk : myProjectSdks.keySet()) {
      if (myProjectSdks.get(jdk) == editableObject) {
        projectJdk = jdk;
        break;
      }
    }
    if (projectJdk != null) {
      myProjectSdks.remove(projectJdk);
      mySdkEventsDispatcher.getMulticaster().beforeSdkRemove(projectJdk);
      myModified = true;
    }
  }

  public void createAddActions(@NotNull DefaultActionGroup group, @NotNull JComponent parent, @NotNull Consumer<? super Sdk> updateTree) {
    createAddActions(group, parent, updateTree, null);
  }

  public void createAddActions(@NotNull DefaultActionGroup group,
                               @NotNull final JComponent parent,
                               @NotNull final Consumer<? super Sdk> updateTree,
                               @Nullable Condition<? super SdkTypeId> filter) {
    createAddActions(group, parent, null, updateTree, filter);
  }

  public void createAddActions(@NotNull DefaultActionGroup group,
                               @NotNull final JComponent parent,
                               @Nullable final Sdk selectedSdk,
                               @NotNull final Consumer<? super Sdk> updateTree,
                               @Nullable Condition<? super SdkTypeId> filter) {
    final SdkType[] types = SdkType.getAllTypes();
    for (final SdkType type : types) {
      if (!type.allowCreationByUser()) continue;
      if (filter != null && !filter.value(type)) continue;
      final AnAction addAction = new DumbAwareAction(type.getPresentableName(), null, type.getIconForAddAction()) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          doAdd(parent, selectedSdk, type, updateTree);
        }
      };
      group.add(addAction);
    }
  }

  public void doAdd(@NotNull JComponent parent, @NotNull final SdkType type, @NotNull final Consumer<? super Sdk> callback) {
    doAdd(parent, null, type, callback);
  }

  public void doAdd(@NotNull JComponent parent, @Nullable final Sdk selectedSdk, @NotNull final SdkType type, @NotNull final Consumer<? super Sdk> callback) {
    myModified = true;
    if (type.supportsCustomCreateUI()) {
      type.showCustomCreateUI(this, parent, selectedSdk, sdk -> setupSdk(sdk, callback));
    }
    else {
      SdkConfigurationUtil.selectSdkHome(type, home -> addSdk(type, home, callback));
    }
  }

  public void addSdk(@NotNull SdkType type, @NotNull String home, @Nullable Consumer<? super Sdk> callback) {
    final Sdk newJdk = createSdk(type, home);
    setupSdk(newJdk, callback);
  }

  @NotNull
  public Sdk createSdk(@NotNull SdkType type, @NotNull String home) {
    String newSdkName = SdkConfigurationUtil.createUniqueSdkName(type, home, myProjectSdks.values());
    final ProjectJdkImpl newJdk = new ProjectJdkImpl(newSdkName, type);
    newJdk.setHomePath(home);
    return newJdk;
  }

  private void setupSdk(@NotNull Sdk newJdk, @Nullable Consumer<? super Sdk> callback) {
    String home = newJdk.getHomePath();
    SdkType sdkType = (SdkType)newJdk.getSdkType();
    if (!sdkType.setupSdkPaths(newJdk, this)) return;

    if (newJdk.getVersionString() == null) {
      String message = ProjectBundle.message("sdk.java.corrupt.error", home);
      Messages.showMessageDialog(message, ProjectBundle.message("sdk.java.corrupt.title"), Messages.getErrorIcon());
    }

    doAdd(newJdk, callback);
  }

  @Override
  public void addSdk(@NotNull Sdk sdk) {
    doAdd(sdk, null);
  }

  public void doAdd(@NotNull Sdk newSdk, @Nullable Consumer<? super Sdk> updateTree) {
    myModified = true;
    try {
      Sdk editableCopy = (Sdk)newSdk.clone();
      myProjectSdks.put(newSdk, editableCopy);
      if (updateTree != null) {
        updateTree.consume(editableCopy);
      }
      mySdkEventsDispatcher.getMulticaster().sdkAdded(editableCopy);
    }
    catch (CloneNotSupportedException e) {
      LOG.error(e);
    }
  }

  @Nullable
  public Sdk findSdk(@Nullable final Sdk modelJdk) {
    for (Map.Entry<Sdk, Sdk> entry : myProjectSdks.entrySet()) {
      Sdk jdk = entry.getKey();
      if (Comparing.equal(entry.getValue(), modelJdk)) {
        return jdk;
      }
    }
    return null;
  }

  @Nullable
  public Sdk getProjectSdk() {
    if (!myProjectSdks.containsValue(myProjectSdk)) return null;
    return myProjectSdk;
  }

  public void setProjectSdk(final Sdk projectSdk) {
    myProjectSdk = projectSdk;
  }

  public boolean isInitialized() {
    return myInitialized;
  }
}
