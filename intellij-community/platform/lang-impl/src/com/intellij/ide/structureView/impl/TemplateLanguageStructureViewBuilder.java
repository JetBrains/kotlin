// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.structureView.impl;

import com.intellij.ide.structureView.StructureView;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.ide.util.StructureViewCompositeModel;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageStructureViewBuilder;
import com.intellij.lang.LanguageUtil;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PairFunction;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author peter
 */
public abstract class TemplateLanguageStructureViewBuilder extends TreeBasedStructureViewBuilder {

  @NotNull
  public static TemplateLanguageStructureViewBuilder create(@NotNull PsiFile psiFile, @Nullable PairFunction<? super PsiFile, ? super Editor, ? extends StructureViewModel> modelFactory) {
    return new TemplateLanguageStructureViewBuilder(psiFile) {
      @Override
      protected TreeBasedStructureViewBuilder createMainBuilder(@NotNull PsiFile psi) {
        return modelFactory == null ? null : new TreeBasedStructureViewBuilder() {
          @Override
          public boolean isRootNodeShown() {
            return false;
          }

          @NotNull
          @Override
          public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
            return modelFactory.fun(psi, editor);
          }
        };
      }
    };
  }

  private final VirtualFile myVirtualFile;
  private final Project myProject;

  protected TemplateLanguageStructureViewBuilder(PsiElement psiElement) {
    myProject = psiElement.getProject();
    myVirtualFile = psiElement.getContainingFile().getVirtualFile();
  }

  @Override
  public boolean isRootNodeShown() {
    return false;
  }

  @Override
  @NotNull
  public StructureView createStructureView(FileEditor fileEditor, @NotNull Project project) {
    List<StructureViewComposite.StructureViewDescriptor> viewDescriptors = new ArrayList<>();
    VirtualFile file = fileEditor == null ? null : fileEditor.getFile();
    PsiFile psiFile = file == null || !file.isValid()? null : PsiManager.getInstance(project).findFile(file);
    List<Language> languages = getLanguages(psiFile).toList();
    for (Language language : languages) {
      StructureViewBuilder builder = getBuilder(ObjectUtils.notNull(psiFile), language);
      if (builder == null) continue;
      StructureView structureView = builder.createStructureView(fileEditor, project);
      String title = language.getDisplayName();
      Icon icon = ObjectUtils.notNull(LanguageUtil.getLanguageFileType(language), FileTypes.UNKNOWN).getIcon();
      viewDescriptors.add(new StructureViewComposite.StructureViewDescriptor(title, structureView, icon));
    }
    StructureViewComposite.StructureViewDescriptor[] array = viewDescriptors.toArray(new StructureViewComposite.StructureViewDescriptor[0]);
    return new StructureViewComposite(array) {
      @Override
      public boolean isOutdated() {
        VirtualFile file = fileEditor == null ? null : fileEditor.getFile();
        PsiFile psiFile = file == null || !file.isValid() ? null : PsiManager.getInstance(project).findFile(file);
        List<Language> newLanguages = getLanguages(psiFile).toList();
        if (!Comparing.equal(languages, newLanguages)) return true;
        if (psiFile == null) return true;
        FileViewProvider viewProvider = psiFile.getViewProvider();
        Language baseLanguage = viewProvider.getBaseLanguage();
        StructureViewDescriptor[] views = getStructureViews();
        boolean hasMainView = views.length > 0 && Comparing.equal(views[0].title, baseLanguage.getDisplayName());
        JBIterable<Language> newAcceptedLanguages = JBIterable.from(newLanguages)
          .filter(o -> o == baseLanguage && hasMainView ||
                       o != baseLanguage && isAcceptableBaseLanguageFile(viewProvider.getPsi(o)));
        return views.length != newAcceptedLanguages.size();
      }
    };
  }

  @Override
  @NotNull
  public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
    List<StructureViewComposite.StructureViewDescriptor> viewDescriptors = new ArrayList<>();
    PsiFile psiFile = ObjectUtils.notNull(PsiManager.getInstance(myProject).findFile(myVirtualFile));
    for (Language language : getLanguages(psiFile)) {
      StructureViewBuilder builder = getBuilder(psiFile, language);
      if (!(builder instanceof TreeBasedStructureViewBuilder)) continue;
      StructureViewModel model = ((TreeBasedStructureViewBuilder)builder).createStructureViewModel(editor);
      String title = language.getDisplayName();
      Icon icon = ObjectUtils.notNull(LanguageUtil.getLanguageFileType(language), FileTypes.UNKNOWN).getIcon();
      viewDescriptors.add(new StructureViewComposite.StructureViewDescriptor(title, model, icon));
    }
    return new StructureViewCompositeModel(psiFile, editor, viewDescriptors);
  }

  @NotNull
  private static JBIterable<Language> getLanguages(@Nullable PsiFile psiFile) {
    if (psiFile == null) return JBIterable.empty();
    FileViewProvider provider = psiFile.getViewProvider();

    Language baseLanguage = provider.getBaseLanguage();
    Language dataLanguage = provider instanceof TemplateLanguageFileViewProvider
                            ? ((TemplateLanguageFileViewProvider)provider).getTemplateDataLanguage() : null;
    return JBIterable.of(baseLanguage)
      .append(dataLanguage)
      .append(JBIterable.from(provider.getLanguages()).filter(o -> o != baseLanguage && o != dataLanguage));
  }

  @Nullable
  private StructureViewBuilder getBuilder(@NotNull PsiFile psiFile, @NotNull Language language) {
    FileViewProvider viewProvider = psiFile.getViewProvider();
    Language baseLanguage = viewProvider.getBaseLanguage();
    PsiFile psi = viewProvider.getPsi(language);
    if (psi == null) return null;
    if (language == baseLanguage) return createMainBuilder(psi);
    if (!isAcceptableBaseLanguageFile(psi)) return null;
    PsiStructureViewFactory factory = LanguageStructureViewBuilder.INSTANCE.forLanguage(language);
    return factory == null ? null : factory.getStructureViewBuilder(psi);
  }

  protected boolean isAcceptableBaseLanguageFile(PsiFile dataFile) {
    return true;
  }

  @Nullable
  protected TreeBasedStructureViewBuilder createMainBuilder(@NotNull PsiFile psi) {
    //noinspection deprecation
    StructureViewComposite.StructureViewDescriptor descriptor = createMainView(null, psi);
    if (descriptor == null) return null;
    return new TreeBasedStructureViewBuilder() {
      @NotNull
      @Override
      public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
        Disposer.register(descriptor.structureModel, descriptor.structureView);
        return descriptor.structureModel;
      }

      @NotNull
      @Override
      public StructureView createStructureView(FileEditor fileEditor, @NotNull Project project) {
        return descriptor.structureView;
      }
    };
  }

  /** @deprecated override {@link #createMainBuilder(PsiFile)} instead */
  @Deprecated
  protected StructureViewComposite.StructureViewDescriptor createMainView(FileEditor fileEditor, PsiFile mainFile) {
    throw new AssertionError();
  }
}
