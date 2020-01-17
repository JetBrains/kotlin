// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.lineMarker;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.lineMarker.RunLineMarkerContributor.Info;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.MarkupEditorFilter;
import com.intellij.openapi.editor.markup.MarkupEditorFilterFactory;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import com.intellij.util.SmartList;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
public class RunLineMarkerProvider extends LineMarkerProviderDescriptor {
  private static final Comparator<Info> COMPARATOR = (a, b) -> {
    if (b.shouldReplace(a)) {
      return 1;
    }
    if (a.shouldReplace(b)) {
      return -1;
    }
    return 0;
  };

  @Nullable
  @Override
  public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
    List<RunLineMarkerContributor> contributors = RunLineMarkerContributor.EXTENSION.allForLanguageOrAny(element.getLanguage());
    Icon icon = null;
    List<Info> infos = null;
    for (RunLineMarkerContributor contributor : contributors) {
      Info info = contributor.getInfo(element);
      if (info == null) {
        continue;
      }
      if (icon == null) {
        icon = info.icon;
      }

      if (infos == null) {
        infos = new SmartList<>();
      }
      infos.add(info);
    }
    if (icon == null) return null;

    if (infos.size() > 1) {
      Collections.sort(infos, COMPARATOR);
      final Info first = infos.get(0);
      for (Iterator<Info> it = infos.iterator(); it.hasNext(); ) {
        Info info = it.next();
        if (info != first && first.shouldReplace(info)) {
          it.remove();
        }
      }
    }

    final DefaultActionGroup actionGroup = new DefaultActionGroup();
    for (Info info : infos) {
      for (AnAction action : info.actions) {
        actionGroup.add(new LineMarkerActionWrapper(element, action));
      }

      if (info != infos.get(infos.size() - 1)) {
        actionGroup.add(new Separator());
      }
    }

    List<Info> finalInfos = infos;
    Function<PsiElement, String> tooltipProvider = element1 -> {
      final StringBuilder tooltip = new StringBuilder();
      for (Info info : finalInfos) {
        if (info.tooltipProvider != null) {
          String string = info.tooltipProvider.apply(element1);
          if (string == null) continue;
          if (tooltip.length() != 0) {
            tooltip.append("\n");
          }
          tooltip.append(string);
        }
      }

      return tooltip.length() == 0 ? null : tooltip.toString();
    };
    return new RunLineMarkerInfo(element, icon, tooltipProvider, actionGroup);
  }

  static class RunLineMarkerInfo extends LineMarkerInfo<PsiElement> {
    private final DefaultActionGroup myActionGroup;

    RunLineMarkerInfo(PsiElement element, Icon icon, Function<PsiElement, String> tooltipProvider, DefaultActionGroup actionGroup) {
      super(element, element.getTextRange(), icon, Pass.LINE_MARKERS, tooltipProvider, null, GutterIconRenderer.Alignment.CENTER);
      myActionGroup = actionGroup;
    }

    @Override
    public GutterIconRenderer createGutterRenderer() {
      return new LineMarkerGutterIconRenderer<PsiElement>(this) {
        @Override
        public AnAction getClickAction() {
          return null;
        }

        @Override
        public boolean isNavigateAction() {
          return true;
        }

        @Override
        public ActionGroup getPopupMenuActions() {
          return myActionGroup;
        }
      };
    }

    @NotNull
    @Override
    public MarkupEditorFilter getEditorFilter() {
      return MarkupEditorFilterFactory.createIsNotDiffFilter();
    }
  }

  @NotNull
  @Override
  public String getName() {
    return ExecutionBundle.message("run.line.marker.name");
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return AllIcons.RunConfigurations.TestState.Run;
  }

  private static final Key<Boolean> HAS_ANYTHING_RUNNABLE = Key.create("HAS_ANYTHING_RUNNABLE");

  @NotNull
  public static ThreeState hadAnythingRunnable(@NotNull VirtualFile file) {
    Boolean data = file.getUserData(HAS_ANYTHING_RUNNABLE);
    return data == null ? ThreeState.UNSURE : ThreeState.fromBoolean(data);
  }

  public static void markRunnable(@NotNull VirtualFile file, boolean isRunnable) {
    file.putUserData(HAS_ANYTHING_RUNNABLE, isRunnable);
  }

}