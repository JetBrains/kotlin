// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.libraries.ui.impl;

import com.intellij.ide.util.ChooseElementsDialog;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.LibraryRootType;
import com.intellij.openapi.roots.libraries.ui.DetectedLibraryRoot;
import com.intellij.openapi.roots.libraries.ui.LibraryRootsComponentDescriptor;
import com.intellij.openapi.roots.libraries.ui.LibraryRootsDetector;
import com.intellij.openapi.roots.libraries.ui.OrderRoot;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsContexts.DialogTitle;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public final class RootDetectionUtil {
  private static final Logger LOG = Logger.getInstance(RootDetectionUtil.class);

  private RootDetectionUtil() {
  }

  @NotNull
  public static List<OrderRoot> detectRoots(@NotNull final Collection<? extends VirtualFile> rootCandidates,
                                            @Nullable Component parentComponent,
                                            @Nullable Project project,
                                            @NotNull final LibraryRootsComponentDescriptor rootsComponentDescriptor) {
    return detectRoots(rootCandidates, parentComponent, project, rootsComponentDescriptor.getRootsDetector(),
                       rootsComponentDescriptor.getRootTypes());
  }

  @NotNull
  public static List<OrderRoot> detectRoots(@NotNull final Collection<? extends VirtualFile> rootCandidates, @Nullable Component parentComponent,
                                            @Nullable Project project, @NotNull final LibraryRootsDetector detector,
                                            OrderRootType @NotNull [] rootTypesAllowedToBeSelectedByUserIfNothingIsDetected) {
    final List<OrderRoot> result = new ArrayList<>();
    final List<SuggestedChildRootInfo> suggestedRoots = new ArrayList<>();
    new Task.Modal(project, ProjectBundle.message("progress.title.scanning.for.roots"), true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        try {
          for (VirtualFile rootCandidate : rootCandidates) {
            final Collection<DetectedLibraryRoot> roots = detector.detectRoots(rootCandidate, indicator);
            if (!roots.isEmpty() && allRootsHaveOneTypeAndEqualToOrAreDirectParentOf(roots, rootCandidate)) {
              for (DetectedLibraryRoot root : roots) {
                final LibraryRootType libraryRootType = root.getTypes().get(0);
                result.add(new OrderRoot(root.getFile(), libraryRootType.getType(), libraryRootType.isJarDirectory()));
              }
            }
            else {
              for (DetectedLibraryRoot root : roots) {
                final HashMap<LibraryRootType, String> names = new HashMap<>();
                for (LibraryRootType type : root.getTypes()) {
                  final String typeName = detector.getRootTypeName(type);
                  LOG.assertTrue(typeName != null, "Unexpected root type " + type.getType().name() + (type.isJarDirectory() ? " (JAR directory)" : "") + ", detectors: " + detector);
                  names.put(type, typeName);
                }
                suggestedRoots.add(new SuggestedChildRootInfo(rootCandidate, root, names));
              }
            }
          }
        }
        catch (ProcessCanceledException ignored) {
        }
      }
    }.queue();

    if (!suggestedRoots.isEmpty()) {
      final DetectedRootsChooserDialog dialog = parentComponent != null
                                                ? new DetectedRootsChooserDialog(parentComponent, suggestedRoots)
                                                : new DetectedRootsChooserDialog(project, suggestedRoots);
      if (!dialog.showAndGet()) {
        return Collections.emptyList();
      }
      for (SuggestedChildRootInfo rootInfo : dialog.getChosenRoots()) {
        final LibraryRootType selectedRootType = rootInfo.getSelectedRootType();
        result.add(new OrderRoot(rootInfo.getDetectedRoot().getFile(), selectedRootType.getType(), selectedRootType.isJarDirectory()));
      }
    }

    if (result.isEmpty() && rootTypesAllowedToBeSelectedByUserIfNothingIsDetected.length > 0) {
      Map<String, Pair<OrderRootType, Boolean>> types = new HashMap<>();
      for (OrderRootType type : rootTypesAllowedToBeSelectedByUserIfNothingIsDetected) {
        for (boolean isJarDirectory : new boolean[]{false, true}) {
          final String typeName = detector.getRootTypeName(new LibraryRootType(type, isJarDirectory));
          if (typeName != null) {
            types.put(StringUtil.capitalizeWords(typeName, true), Pair.create(type, isJarDirectory));
          }
        }
      }
      LOG.assertTrue(!types.isEmpty(), "No allowed root types found for " + detector);
      List<String> names = new ArrayList<>(types.keySet());
      if (names.size() == 1) {
        String title = LangBundle.message("dialog.title.attach.roots");
        String typeName = names.get(0);
        String message =
          LangBundle.message("dialog.message.cannot.determine", ApplicationNamesInfo.getInstance().getProductName(), typeName);
        int answer = parentComponent != null
                     ? Messages.showYesNoDialog(parentComponent, message, title, null)
                     : Messages.showYesNoDialog(project, message, title, null);
        if (answer == Messages.YES) {
          Pair<OrderRootType, Boolean> pair = types.get(typeName);
          for (VirtualFile candidate : rootCandidates) {
            result.add(new OrderRoot(candidate, pair.getFirst(), pair.getSecond()));
          }
        }
      }
      else {
        String title = LangBundle.message("dialog.title.choose.categories.selected.files");
        String description = XmlStringUtil.wrapInHtml(ApplicationNamesInfo.getInstance().getProductName() + " cannot determine what kind of files the chosen items contain.<br>" +
                                                      "Choose the appropriate categories from the list.");
        ChooseElementsDialog<String> dialog;
        if (parentComponent != null) {
          dialog = new ChooseRootTypeElementsDialog(parentComponent, names, title, description);
        }
        else {
          dialog = new ChooseRootTypeElementsDialog(project, names, title, description);
        }
        for (String rootType : dialog.showAndGetResult()) {
          final Pair<OrderRootType, Boolean> pair = types.get(rootType);
          for (VirtualFile candidate : rootCandidates) {
            result.add(new OrderRoot(candidate, pair.getFirst(), pair.getSecond()));
          }
        }
      }
    }

    return result;
  }

  private static boolean allRootsHaveOneTypeAndEqualToOrAreDirectParentOf(Collection<? extends DetectedLibraryRoot> roots, VirtualFile candidate) {
    for (DetectedLibraryRoot root : roots) {
      if (root.getTypes().size() > 1 || !root.getFile().equals(candidate) && !root.getFile().equals(candidate.getParent())) {
        return false;
      }
    }
    return true;
  }

  private static class ChooseRootTypeElementsDialog extends ChooseElementsDialog<String> {
    ChooseRootTypeElementsDialog(Project project, List<String> names, @DialogTitle String title, @NlsContexts.Label String description) {
      super(project, names, title, description, true);
    }

    private ChooseRootTypeElementsDialog(Component parent,
                                         List<String> names,
                                         @DialogTitle String title,
                                         @NlsContexts.Label String description) {
      super(parent, names, title, description, true);
    }

    @Override
    protected String getItemText(String item) {
      return item;
    }

    @Nullable
    @Override
    protected Icon getItemIcon(String item) {
      return null;
    }
  }
}
