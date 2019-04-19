// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.actions;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.DataManager;
import com.intellij.ide.IdeBundle;
import com.intellij.lang.documentation.CompositeDocumentationProvider;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.lang.documentation.ExternalDocumentationHandler;
import com.intellij.lang.documentation.ExternalDocumentationProvider;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public class ExternalJavaDocAction extends AnAction {

  public ExternalJavaDocAction() {
    setInjectedContext(true);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) {
      return;
    }

    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    PsiElement element = getElement(dataContext, editor);
    if (element == null) {
      Messages.showMessageDialog(
        project,
        IdeBundle.message("message.please.select.element.for.javadoc"),
        IdeBundle.message("title.no.element.selected"),
        Messages.getErrorIcon()
      );
      return;
    }


    PsiFile context = CommonDataKeys.PSI_FILE.getData(dataContext);

    PsiElement originalElement = getOriginalElement(context, editor);
    DocumentationManager.storeOriginalElement(project, originalElement, element);

    showExternalJavadoc(element, originalElement, null, dataContext);
  }

  public static void showExternalJavadoc(PsiElement element, PsiElement originalElement, String docUrl, DataContext dataContext) {
    DocumentationProvider provider = DocumentationManager.getProviderFromElement(element);
    if (provider instanceof ExternalDocumentationHandler &&
        ((ExternalDocumentationHandler)provider).handleExternal(element, originalElement)) {
      return;
    }
    FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassists.javadoc.external");
    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    final Component contextComponent = PlatformDataKeys.CONTEXT_COMPONENT.getData(dataContext);
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      List<String> urls;
      if (StringUtil.isEmptyOrSpaces(docUrl)) {
        urls = ReadAction.compute(() -> provider.getUrlFor(element, originalElement));
      }
      else {
        urls = Collections.singletonList(docUrl);
      }
      if (provider instanceof ExternalDocumentationProvider && urls != null && urls.size() > 1) {
        for (String url : urls) {
          List<String> thisUrlList = Collections.singletonList(url);
          String doc = ((ExternalDocumentationProvider)provider).fetchExternalDocumentation(project, element, thisUrlList);
          if (doc != null) {
            urls = thisUrlList;
            break;
          }
        }
      }
      final List<String> finalUrls = urls;
      ApplicationManager.getApplication().invokeLater(() -> {
        if (ContainerUtil.isEmpty(finalUrls)) {
          if (element != null && provider instanceof ExternalDocumentationProvider) {
            ExternalDocumentationProvider externalDocumentationProvider = (ExternalDocumentationProvider)provider;
            if (externalDocumentationProvider.canPromptToConfigureDocumentation(element)) {
              externalDocumentationProvider.promptToConfigureDocumentation(element);
            }
          }
        }
        else if (finalUrls.size() == 1) {
          BrowserUtil.browse(finalUrls.get(0));
        }
        else {
          JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<String>("Choose external documentation root",
                                                                                     ArrayUtil.toStringArray(finalUrls)) {
            @Override
            public PopupStep onChosen(final String selectedValue, final boolean finalChoice) {
              BrowserUtil.browse(selectedValue);
              return FINAL_CHOICE;
            }
          }).showInBestPositionFor(DataManager.getInstance().getDataContext(contextComponent));
        }
      }, ModalityState.NON_MODAL);
    });

  }

  @Nullable
  private static PsiElement getOriginalElement(final PsiFile context, final Editor editor) {
    return (context!=null && editor!=null)? context.findElementAt(editor.getCaretModel().getOffset()):null;
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();
    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    PsiElement element = getElement(dataContext, editor);
    final PsiElement originalElement = getOriginalElement(CommonDataKeys.PSI_FILE.getData(dataContext), editor);
    DocumentationManager.storeOriginalElement(CommonDataKeys.PROJECT.getData(dataContext), originalElement, element);
    final DocumentationProvider provider = DocumentationManager.getProviderFromElement(element);
    boolean enabled;
    if (provider instanceof ExternalDocumentationProvider) {
      final ExternalDocumentationProvider edProvider = (ExternalDocumentationProvider)provider;
      enabled = CompositeDocumentationProvider.hasUrlsFor(provider, element, originalElement) || edProvider.canPromptToConfigureDocumentation(element);
    }
    else {
      final List<String> urls = provider.getUrlFor(element, originalElement);
      enabled = urls != null && !urls.isEmpty();
    }
    if (editor != null) {
      presentation.setEnabled(enabled);
      if (ActionPlaces.isMainMenuOrActionSearch(event.getPlace())) {
        presentation.setVisible(true);
      }
      else {
        presentation.setVisible(enabled);
      }
    }
    else{
      presentation.setEnabled(enabled);
      presentation.setVisible(true);
    }
  }

  private static PsiElement getElement(DataContext dataContext, Editor editor) {
    PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
    if (element == null && editor != null) {
      PsiReference reference = TargetElementUtil.findReference(editor, editor.getCaretModel().getOffset());
      if (reference != null) {
        element = reference.getElement();
      }
    }
    return element;
  }
}