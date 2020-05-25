// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.actions.CopyReferenceAction;
import com.intellij.ide.actions.GotoClassPresentationUpdater;
import com.intellij.ide.structureView.StructureView;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.gotoByName.FilteringGotoByModel;
import com.intellij.ide.util.gotoByName.GotoClassModel2;
import com.intellij.ide.util.gotoByName.GotoClassSymbolConfiguration;
import com.intellij.ide.util.gotoByName.LanguageRef;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.lang.LanguageStructureViewBuilder;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.navigation.AnonymousElementProvider;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.MinusculeMatcher;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.IdeUICustomization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Konstantin Bulenkov
 */
public class ClassSearchEverywhereContributor extends AbstractGotoSEContributor {

  private static final Pattern ourPatternToDetectAnonymousClasses = Pattern.compile("([.\\w]+)((\\$[\\d]+)*(\\$)?)");
  private static final Pattern ourPatternToDetectMembers = Pattern.compile("(.+)(#)(.*)");

  private final PersistentSearchEverywhereContributorFilter<LanguageRef> myFilter;

  public ClassSearchEverywhereContributor(@NotNull AnActionEvent event) {
    super(event);
    myFilter = createLanguageFilter(event.getRequiredData(CommonDataKeys.PROJECT));
  }

  @NotNull
  @Override
  public String getGroupName() {
    return GotoClassPresentationUpdater.getTabTitlePluralized();
  }

  @NotNull
  @Override
  public String getFullGroupName() {
    return String.join("/", GotoClassPresentationUpdater.getActionTitlePluralized());
  }

  @NotNull
  public String includeNonProjectItemsText() {
    return IdeUICustomization.getInstance().projectMessage("checkbox.include.non.project.items");
  }

  @Override
  public int getSortWeight() {
    return 100;
  }

  @NotNull
  @Override
  protected FilteringGotoByModel<LanguageRef> createModel(@NotNull Project project) {
    GotoClassModel2 model = new GotoClassModel2(project);
    if (myFilter != null) {
      model.setFilterItems(myFilter.getSelectedElements());
    }
    return model;
  }

  @NotNull
  @Override
  public List<AnAction> getActions(@NotNull Runnable onChanged) {
    return doGetActions(includeNonProjectItemsText(), myFilter, onChanged);
  }

  @NotNull
  @Override
  public String filterControlSymbols(@NotNull String pattern) {
    if (pattern.indexOf('#') != -1) {
      pattern = applyPatternFilter(pattern, ourPatternToDetectMembers);
    }

    if (pattern.indexOf('$') != -1) {
      pattern = applyPatternFilter(pattern, ourPatternToDetectAnonymousClasses);
    }

    return super.filterControlSymbols(pattern);
  }

  @Override
  public int getElementPriority(@NotNull Object element, @NotNull String searchPattern) {
    return super.getElementPriority(element, searchPattern) + 5;
  }

  @Override
  protected PsiElement preparePsi(PsiElement psiElement, int modifiers, String searchText) {
    String path = pathToAnonymousClass(searchText);
    if (path != null) {
      psiElement = getElement(psiElement, path);
    }
    return super.preparePsi(psiElement, modifiers, searchText);
  }

  @Nullable
  @Override
  protected Navigatable createExtendedNavigatable(PsiElement psi, String searchText, int modifiers) {
    Navigatable res = super.createExtendedNavigatable(psi, searchText, modifiers);
    if (res != null) {
      return res;
    }

    VirtualFile file = PsiUtilCore.getVirtualFile(psi);
    String memberName = getMemberName(searchText);
    if (file != null && memberName != null) {
      Navigatable delegate = findMember(memberName, searchText, psi, file);
      if (delegate != null) {
        return new Navigatable() {
          @Override
          public void navigate(boolean requestFocus) {
            NavigationUtil.activateFileWithPsiElement(psi, openInCurrentWindow(modifiers));
            delegate.navigate(true);

          }

          @Override
          public boolean canNavigate() {
            return delegate.canNavigate();
          }

          @Override
          public boolean canNavigateToSource() {
            return delegate.canNavigateToSource();
          }
        };
      }
    }

    return null;
  }

  private static String pathToAnonymousClass(String searchedText) {
    return pathToAnonymousClass(ourPatternToDetectAnonymousClasses.matcher(searchedText));
  }

  @Nullable
  public static String pathToAnonymousClass(Matcher matcher) {
    if (matcher.matches()) {
      String path = matcher.group(2);
      if (path != null) {
        path = path.trim();
        if (path.endsWith("$") && path.length() >= 2) {
          path = path.substring(0, path.length() - 2);
        }
        if (!path.isEmpty()) return path;
      }
    }

    return null;
  }

  private static String getMemberName(String searchedText) {
    final int index = searchedText.lastIndexOf('#');
    if (index == -1) {
      return null;
    }

    String name = searchedText.substring(index + 1).trim();
    return StringUtil.isEmpty(name) ? null : name;
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

  private static PsiElement @NotNull [] getAnonymousClasses(@NotNull PsiElement element) {
    for (AnonymousElementProvider provider : AnonymousElementProvider.EP_NAME.getExtensionList()) {
      final PsiElement[] elements = provider.getAnonymousElements(element);
      if (elements.length > 0) {
        return elements;
      }
    }
    return PsiElement.EMPTY_ARRAY;
  }

  public static class Factory implements SearchEverywhereContributorFactory<Object> {

    @NotNull
    @Override
    public SearchEverywhereContributor<Object> createContributor(@NotNull AnActionEvent initEvent) {
      return new ClassSearchEverywhereContributor(initEvent);
    }
  }

  @NotNull
  static PersistentSearchEverywhereContributorFilter<LanguageRef> createLanguageFilter(@NotNull Project project) {
    List<LanguageRef> items = LanguageRef.forAllLanguages();
    GotoClassSymbolConfiguration persistentConfig = GotoClassSymbolConfiguration.getInstance(project);
    return new PersistentSearchEverywhereContributorFilter<>(items, persistentConfig, LanguageRef::getDisplayName, LanguageRef::getIcon);
  }
}
