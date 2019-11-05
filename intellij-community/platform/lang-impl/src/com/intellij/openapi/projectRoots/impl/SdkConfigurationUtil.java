/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkAdditionalData;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Consumer;
import com.intellij.util.NullableConsumer;
import com.intellij.util.text.UniqueNameGenerator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yole
 */
public class SdkConfigurationUtil {
  private static final Logger LOG = Logger.getInstance(SdkConfigurationUtil.class);
  private SdkConfigurationUtil() { }

  public static void createSdk(@Nullable final Project project,
                               @NotNull Sdk[] existingSdks,
                               @NotNull NullableConsumer<? super Sdk> onSdkCreatedCallBack,
                               final boolean createIfExists,
                               @NotNull SdkType... sdkTypes) {
    createSdk(project, existingSdks, onSdkCreatedCallBack, createIfExists, true, sdkTypes);
  }

  public static void createSdk(@Nullable final Project project,
                               @NotNull Sdk[] existingSdks,
                               @NotNull NullableConsumer<? super Sdk> onSdkCreatedCallBack,
                               final boolean createIfExists,
                               final boolean followSymLinks,
                               @NotNull SdkType... sdkTypes) {
    if (sdkTypes.length == 0) {
      onSdkCreatedCallBack.consume(null);
      return;
    }

    FileChooserDescriptor descriptor = createCompositeDescriptor(sdkTypes);
    // XXX: Workaround for PY-21787 since the native macOS dialog always follows symlinks
    if (!followSymLinks) {
      descriptor.setForcedToUseIdeaFileChooser(true);
    }
    VirtualFile suggestedDir = getSuggestedSdkRoot(sdkTypes[0]);
    FileChooser.chooseFiles(descriptor, project, suggestedDir, new FileChooser.FileChooserConsumer() {
      @Override
      public void consume(List<VirtualFile> selectedFiles) {
        for (SdkType sdkType : sdkTypes) {
          final String path = selectedFiles.get(0).getPath();
          if (sdkType.isValidSdkHome(path)) {
            Sdk newSdk = null;
            if (!createIfExists) {
              for (Sdk sdk : existingSdks) {
                if (path.equals(sdk.getHomePath())) {
                  newSdk = sdk;
                  break;
                }
              }
            }
            if (newSdk == null) {
              newSdk = setupSdk(existingSdks, selectedFiles.get(0), sdkType, false, null, null);
            }
            onSdkCreatedCallBack.consume(newSdk);
            return;
          }
        }
        onSdkCreatedCallBack.consume(null);
      }

      @Override
      public void cancelled() {
        onSdkCreatedCallBack.consume(null);
      }
    });
  }

  public static void createSdk(@Nullable final Project project,
                               @NotNull Sdk[] existingSdks,
                               @NotNull NullableConsumer<? super Sdk> onSdkCreatedCallBack,
                               @NotNull SdkType... sdkTypes) {
    createSdk(project, existingSdks, onSdkCreatedCallBack, true, sdkTypes);
  }

  @NotNull
  private static FileChooserDescriptor createCompositeDescriptor(@NotNull SdkType... sdkTypes) {
    return new FileChooserDescriptor(sdkTypes[0].getHomeChooserDescriptor()) {
      @Override
      public void validateSelectedFiles(@NotNull final VirtualFile[] files) throws Exception {
        if (files.length > 0) {
          for (SdkType type : sdkTypes) {
            if (type.isValidSdkHome(files[0].getPath())) {
              return;
            }
          }
        }
        String key = files.length > 0 && files[0].isDirectory() ? "sdk.configure.home.invalid.error" : "sdk.configure.home.file.invalid.error";
        throw new Exception(ProjectBundle.message(key, sdkTypes[0].getPresentableName()));
      }
    };
  }

  public static void addSdk(@NotNull final Sdk sdk) {
    ApplicationManager.getApplication().runWriteAction(() -> ProjectJdkTable.getInstance().addJdk(sdk));
  }

  public static void removeSdk(@NotNull Sdk sdk) {
    ApplicationManager.getApplication().runWriteAction(() -> ProjectJdkTable.getInstance().removeJdk(sdk));
  }

  @Nullable
  public static Sdk setupSdk(@NotNull Sdk[] allSdks,
                             @NotNull VirtualFile homeDir,
                             @NotNull SdkType sdkType,
                             final boolean silent,
                             @Nullable final SdkAdditionalData additionalData,
                             @Nullable final String customSdkSuggestedName) {
    ProjectJdkImpl sdk = null;
    try {
      sdk = createSdk(Arrays.asList(allSdks), homeDir, sdkType, additionalData, customSdkSuggestedName);

      sdkType.setupSdkPaths(sdk);
    }
    catch (Throwable e) {
      LOG.warn("Error creating or configuring sdk: homeDir=[" + homeDir + "]; " +
               "sdkType=[" + sdkType + "]; " +
               "additionalData=[" + additionalData + "]; " +
               "customSdkSuggestedName=[" + customSdkSuggestedName + "]; " +
               "sdk=[" + sdk + "]", e);
      if (!silent) {
        Messages.showErrorDialog("Error configuring SDK: " +
                                 e.getMessage() +
                                 ".\nPlease make sure that " +
                                 FileUtil.toSystemDependentName(homeDir.getPath()) +
                                 " is a valid home path for this SDK type.", "Error Configuring SDK");
      }
      return null;
    }
    return sdk;
  }

  /**
   * @deprecated Use {@link SdkConfigurationUtil#createSdk(Collection, VirtualFile, SdkType, SdkAdditionalData, String)} instead.
   * This method will be removed in 2020.1.
   */
  @NotNull
  @Deprecated
  @ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
  public static ProjectJdkImpl createSdk(@NotNull Sdk[] allSdks,
                                         @NotNull VirtualFile homeDir,
                                         @NotNull SdkType sdkType,
                                         @Nullable SdkAdditionalData additionalData,
                                         @Nullable String customSdkSuggestedName) {
    return createSdk(Arrays.asList(allSdks), homeDir, sdkType, additionalData, customSdkSuggestedName);
  }

  @NotNull
  public static ProjectJdkImpl createSdk(@NotNull Collection<? extends Sdk> allSdks,
                                         @NotNull VirtualFile homeDir,
                                         @NotNull SdkType sdkType,
                                         @Nullable SdkAdditionalData additionalData,
                                         @Nullable String customSdkSuggestedName) {
    return createSdk(allSdks, sdkType.sdkPath(homeDir), sdkType, additionalData, customSdkSuggestedName);
  }

  @NotNull
  public static ProjectJdkImpl createSdk(@NotNull Collection<? extends Sdk> allSdks,
                                         @NotNull String homePath,
                                         @NotNull SdkType sdkType,
                                         @Nullable SdkAdditionalData additionalData,
                                         @Nullable String customSdkSuggestedName) {
    final String sdkName = customSdkSuggestedName == null
                           ? createUniqueSdkName(sdkType, homePath, allSdks)
                           : createUniqueSdkName(customSdkSuggestedName, allSdks);

    final ProjectJdkImpl sdk = new ProjectJdkImpl(sdkName, sdkType);

    if (additionalData != null) {
      // additional initialization.
      // E.g. some ruby sdks must be initialized before
      // setupSdkPaths() method invocation
      sdk.setSdkAdditionalData(additionalData);
    }

    sdk.setHomePath(homePath);
    return sdk;
  }

  public static void setDirectoryProjectSdk(@NotNull final Project project, @Nullable final Sdk sdk) {
    ApplicationManager.getApplication().runWriteAction(() -> {
      ProjectRootManager.getInstance(project).setProjectSdk(sdk);
      final Module[] modules = ModuleManager.getInstance(project).getModules();
      if (modules.length > 0) {
        ModuleRootModificationUtil.setSdkInherited(modules[0]);
      }
    });
  }

  public static void configureDirectoryProjectSdk(@NotNull Project project,
                                                  @Nullable Comparator<? super Sdk> preferredSdkComparator,
                                                  @NotNull SdkType... sdkTypes) {
    Sdk existingSdk = ProjectRootManager.getInstance(project).getProjectSdk();
    if (existingSdk != null && ArrayUtil.contains(existingSdk.getSdkType(), sdkTypes)) {
      return;
    }

    Sdk sdk = findOrCreateSdk(preferredSdkComparator, sdkTypes);
    if (sdk != null) {
      setDirectoryProjectSdk(project, sdk);
    }
  }

  @Nullable
  public static Sdk findOrCreateSdk(@Nullable Comparator<? super Sdk> comparator, @NotNull SdkType... sdkTypes) {
    final Project defaultProject = ProjectManager.getInstance().getDefaultProject();
    final Sdk sdk = ProjectRootManager.getInstance(defaultProject).getProjectSdk();
    if (sdk != null) {
      for (SdkType type : sdkTypes) {
        if (sdk.getSdkType() == type) {
          return sdk;
        }
      }
    }
    for (SdkType type : sdkTypes) {
      List<Sdk> sdks = ProjectJdkTable.getInstance().getSdksOfType(type);
      if (!sdks.isEmpty()) {
        if (comparator != null) {
          Collections.sort(sdks, comparator);
        }
        return sdks.get(0);
      }
    }
    for (SdkType sdkType : sdkTypes) {
      final String suggestedHomePath = sdkType.suggestHomePath();
      if (suggestedHomePath != null && sdkType.isValidSdkHome(suggestedHomePath)) {
        Sdk an_sdk = createAndAddSDK(suggestedHomePath, sdkType);
        if (an_sdk != null) return an_sdk;
      }
    }
    return null;
  }

  /**
   * Tries to create an SDK identified by path; if successful, add the SDK to the global SDK table.
   * <p>
   * Must be called from the EDT (because it uses {@link WriteAction#compute} under the hood).
   *
   * @param path    identifies the SDK
   * @return newly created SDK, or null.
   */
  @Nullable
  public static Sdk createAndAddSDK(@NotNull String path, @NotNull SdkType sdkType) {
    VirtualFile sdkHome =
      WriteAction.compute(() -> LocalFileSystem.getInstance().refreshAndFindFileByPath(path));
    if (sdkHome != null) {
      final Sdk newSdk = setupSdk(ProjectJdkTable.getInstance().getAllJdks(), sdkHome, sdkType, true, null, null);
      if (newSdk != null) {
        addSdk(newSdk);
      }
      return newSdk;
    }
    return null;
  }

  @NotNull
  public static String createUniqueSdkName(@NotNull SdkType type, String home, final Collection<? extends Sdk> sdks) {
    return createUniqueSdkName(type.suggestSdkName(null, home), sdks);
  }

  @NotNull
  public static String createUniqueSdkName(@NotNull String suggestedName, @NotNull Collection<? extends Sdk> sdks) {
    Set<String> nameList = sdks.stream().map( jdk -> jdk.getName()).collect(Collectors.toSet());

    return UniqueNameGenerator.generateUniqueName(suggestedName, "", "", " (", ")", o -> !nameList.contains(o));
  }

  public static void selectSdkHome(@NotNull final SdkType sdkType, @NotNull final Consumer<? super String> consumer) {
    selectSdkHome(sdkType, null, consumer);
  }

  public static void selectSdkHome(@NotNull final SdkType sdkType,
                                   @Nullable Component component,
                                   @NotNull final Consumer<? super String> consumer) {
    final FileChooserDescriptor descriptor = sdkType.getHomeChooserDescriptor();
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      Sdk sdk = ProjectJdkTable.getInstance().findMostRecentSdkOfType(sdkType);
      if (sdk == null) throw new RuntimeException("No SDK of type " + sdkType + " found");
      consumer.consume(sdk.getHomePath());
      return;
    }
    // passing project instance here seems to be the right idea, but it would make the dialog
    // selecting the last opened project path, instead of the suggested detected JDK home (one of many).
    // The behaviour may also depend on the FileChooser implementations which does not reuse that code
    FileChooser.chooseFiles(descriptor, null, component, getSuggestedSdkRoot(sdkType), chosen -> {
      final String path = chosen.get(0).getPath();
      if (sdkType.isValidSdkHome(path)) {
        consumer.consume(path);
        return;
      }

      final String adjustedPath = sdkType.adjustSelectedSdkHome(path);
      if (sdkType.isValidSdkHome(adjustedPath)) {
        consumer.consume(adjustedPath);
      }
    });
  }

  @Nullable
  public static VirtualFile getSuggestedSdkRoot(@NotNull SdkType sdkType) {
    final String homePath = sdkType.suggestHomePath();
    return homePath == null ? null : LocalFileSystem.getInstance().findFileByPath(homePath);
  }

  @NotNull
  public static List<String> filterExistingPaths(@NotNull SdkType sdkType, Collection<String> sdkHomes, final Sdk[] sdks) {
    List<String> result = new ArrayList<>();
    for (String sdkHome : sdkHomes) {
      if (findByPath(sdkType, sdks, sdkHome) == null) {
        result.add(sdkHome);
      }
    }
    return result;
  }

  @Nullable
  private static Sdk findByPath(@NotNull SdkType sdkType, @NotNull Sdk[] sdks, @NotNull String sdkHome) {
    for (Sdk sdk : sdks) {
      final String path = sdk.getHomePath();
      if (sdk.getSdkType() == sdkType && path != null &&
          FileUtil.pathsEqual(FileUtil.toSystemIndependentName(path), FileUtil.toSystemIndependentName(sdkHome))) {
        return sdk;
      }
    }
    return null;
  }
}
