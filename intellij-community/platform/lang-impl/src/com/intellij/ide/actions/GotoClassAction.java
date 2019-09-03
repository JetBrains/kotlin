// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.searcheverywhere.ClassSearchEverywhereContributor;
import com.intellij.ide.structureView.StructureView;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.ide.util.gotoByName.*;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageStructureViewBuilder;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.navigation.AnonymousElementProvider;
import com.intellij.navigation.ChooseByNameRegistry;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.playback.commands.ActionCommand;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.MinusculeMatcher;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;

public class GotoClassAction extends GotoActionBase implements DumbAware {
  public GotoClassAction() {
    //we need to change the template presentation to show the proper text for the action in Settings | Keymap
    Presentation presentation = getTemplatePresentation();
    presentation.setText(GotoClassPresentationUpdater.getActionTitle() + "...");
    presentation.setDescription(IdeBundle.message("go.to.class.action.description",
                                                  StringUtil.join(GotoClassPresentationUpdater.getElementKinds(), "/")));
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    boolean dumb = DumbService.isDumb(project);
    if (Registry.is("new.search.everywhere")) {
      if (!dumb || new ClassSearchEverywhereContributor(project, null).isDumbAware()) {
        showInSearchEverywherePopup(ClassSearchEverywhereContributor.class.getSimpleName(), e, true, true);
      }
      else {
        invokeGoToFile(project, e);
      }
    }
    else {
      if (!dumb) {
        super.actionPerformed(e);
      }
      else {
        invokeGoToFile(project, e);
      }
    }
  }

  static void invokeGoToFile(@NotNull Project project, @NotNull AnActionEvent e) {
    String actionTitle = StringUtil.trimEnd(ObjectUtils.notNull(
      e.getPresentation().getText(), GotoClassPresentationUpdater.getActionTitle()), "...");
    String message = IdeBundle.message("go.to.class.dumb.mode.message", actionTitle);
    DumbService.getInstance(project).showDumbModeNotification(message);
    AnAction action = ActionManager.getInstance().getAction(GotoFileAction.ID);
    InputEvent event = ActionCommand.getInputEvent(GotoFileAction.ID);
    Component component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
    ActionManager.getInstance().tryToExecute(action, event, component, e.getPlace(), true);
  }

  @Override
  public void gotoActionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    if (project == null) return;

    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.class");

    PsiDocumentManager.getInstance(project).commitAllDocuments();

    final GotoClassModel2 model = new GotoClassModel2(project);
    String pluralKinds = StringUtil.capitalize(
      StringUtil.join(GotoClassPresentationUpdater.getElementKinds(), s -> StringUtil.pluralize(s), "/"));
    String title = IdeBundle.message("go.to.class.toolwindow.title", pluralKinds);
    showNavigationPopup(e, model, new GotoActionCallback<Language>() {
      @Override
      protected ChooseByNameFilter<Language> createFilter(@NotNull ChooseByNamePopup popup) {
        return new ChooseByNameLanguageFilter(popup, model, GotoClassSymbolConfiguration.getInstance(project), project);
      }

      @Override
      public void elementChosen(ChooseByNamePopup popup, Object element) {
        handleSubMemberNavigation(popup, element);
      }
    }, title, true);
  }

  static void handleSubMemberNavigation(ChooseByNamePopup popup, Object element) {
    if (element instanceof PsiElement && ((PsiElement)element).isValid()) {
      PsiElement psiElement = getElement(((PsiElement)element), popup);
      psiElement = psiElement.getNavigationElement();
      VirtualFile file = PsiUtilCore.getVirtualFile(psiElement);

      if (file != null && popup.getLinePosition() != -1) {
        OpenFileDescriptor descriptor = new OpenFileDescriptor(psiElement.getProject(), file, popup.getLinePosition(), popup.getColumnPosition());
        Navigatable n = descriptor.setUseCurrentWindow(popup.isOpenInCurrentWindowRequested());
        if (n.canNavigate()) {
          n.navigate(true);
          return;
        }
      }

      if (file != null && popup.getMemberPattern() != null) {
        NavigationUtil.activateFileWithPsiElement(psiElement, !popup.isOpenInCurrentWindowRequested());
        Navigatable member = findMember(popup.getMemberPattern(), popup.getTrimmedText(), psiElement, file);
        if (member != null) {
          member.navigate(true);
        }
      }

      NavigationUtil.activateFileWithPsiElement(psiElement, !popup.isOpenInCurrentWindowRequested());
    }
    else {
      EditSourceUtil.navigate(((NavigationItem)element), true, popup.isOpenInCurrentWindowRequested());
    }
  }

  @Nullable
  public static Navigatable findMember(String memberPattern, String fullPattern, PsiElement psiElement, VirtualFile file) {
    final PsiStructureViewFactory factory = LanguageStructureViewBuilder.INSTANCE.forLanguage(psiElement.getLanguage());
    final StructureViewBuilder builder = factory == null ? null : factory.getStructureViewBuilder(psiElement.getContainingFile());
    final FileEditor[] editors = FileEditorManager.getInstance(psiElement.getProject()).getEditors(file);
    if (builder == null || editors.length == 0) {
      return null;
    }

    final StructureView view = builder.createStructureView(editors[0], psiElement.getProject());
    try {
      final StructureViewTreeElement element = findElement(view.getTreeModel().getRoot(), psiElement, 4);
      if (element == null) {
        return null;
      }

      MinusculeMatcher matcher = NameUtil.buildMatcher(memberPattern).build();
      int max = Integer.MIN_VALUE;
      Object target = null;
      for (TreeElement treeElement : element.getChildren()) {
        if (treeElement instanceof StructureViewTreeElement) {
          Object value = ((StructureViewTreeElement)treeElement).getValue();
          if (value instanceof PsiElement && value instanceof Navigatable &&
              fullPattern.equals(CopyReferenceAction.elementToFqn((PsiElement)value))) {
            return (Navigatable)value;
          }

          String presentableText = treeElement.getPresentation().getPresentableText();
          if (presentableText != null) {
            final int degree = matcher.matchingDegree(presentableText);
            if (degree > max) {
              max = degree;
              target = ((StructureViewTreeElement)treeElement).getValue();
            }
          }
        }
      }
      return target instanceof Navigatable ? (Navigatable)target : null;
    }
    finally {
      Disposer.dispose(view);
    }
  }

  @Nullable
  private static StructureViewTreeElement findElement(StructureViewTreeElement node, PsiElement element, int hopes) {
    final Object value = node.getValue();
    if (value instanceof PsiElement) {
      if (((PsiElement)value).isEquivalentTo(element)) return node;
      if (hopes != 0) {
        for (TreeElement child : node.getChildren()) {
          if (child instanceof StructureViewTreeElement) {
            final StructureViewTreeElement e = findElement((StructureViewTreeElement)child, element, hopes - 1);
            if (e != null) {
              return e;
            }
          }
        }
      }
    }
    return null;
  }

  @NotNull
  private static PsiElement getElement(@NotNull PsiElement element, ChooseByNamePopup popup) {
    final String path = popup.getPathToAnonymous();
    if (path != null) {
      return getElement(element, path);
    }
    return element;
  }

  @NotNull
  public static PsiElement getElement(@NotNull PsiElement element, @NotNull String path) {
    final String[] classes = path.split("\\$");
    List<Integer> indexes = new ArrayList<>();
    for (String cls : classes) {
      if (cls.isEmpty()) continue;
      try {
        indexes.add(Integer.parseInt(cls) - 1);
      }
      catch (Exception e) {
        return element;
      }
    }
    PsiElement current = element;
    for (int index : indexes) {
      final PsiElement[] anonymousClasses = getAnonymousClasses(current);
      if (index >= 0 && index < anonymousClasses.length) {
        current = anonymousClasses[index];
      }
      else {
        return current;
      }
    }
    return current;
  }

  @NotNull
  private static PsiElement[] getAnonymousClasses(@NotNull PsiElement element) {
    for (AnonymousElementProvider provider : AnonymousElementProvider.EP_NAME.getExtensionList()) {
      final PsiElement[] elements = provider.getAnonymousElements(element);
      if (elements.length > 0) {
        return elements;
      }
    }
    return PsiElement.EMPTY_ARRAY;
  }

  @Override
  protected boolean hasContributors(@NotNull DataContext dataContext) {
    return ChooseByNameRegistry.getInstance().getClassModelContributors().length > 0;
  }
}
