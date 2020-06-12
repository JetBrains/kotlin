// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.templates;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.completion.OffsetTranslator;
import com.intellij.codeInsight.template.CustomLiveTemplateBase;
import com.intellij.codeInsight.template.CustomTemplateCallback;
import com.intellij.codeInsight.template.impl.CustomLiveTemplateLookupElement;
import com.intellij.codeInsight.template.postfix.completion.PostfixTemplateLookupElement;
import com.intellij.codeInsight.template.postfix.settings.PostfixTemplatesSettings;
import com.intellij.diagnostic.AttachmentFactory;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.undo.UndoConstants;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.templateLanguages.TemplateLanguageUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class PostfixLiveTemplate extends CustomLiveTemplateBase {
  public static final String POSTFIX_TEMPLATE_ID = "POSTFIX_TEMPLATE_ID";
  private static final Logger LOG = Logger.getInstance(PostfixLiveTemplate.class);

  @NotNull
  public Set<String> getAllTemplateKeys(PsiFile file, int offset) {
    Set<String> keys = new HashSet<String>();
    Language language = PsiUtilCore.getLanguageAtOffset(file, offset);
    for (PostfixTemplateProvider provider : LanguagePostfixTemplate.LANG_EP.allForLanguage(language)) {
      ProgressManager.checkCanceled();
      keys.addAll(getKeys(provider));
    }
    return keys;
  }

  @Nullable
  private static String computeTemplateKeyWithoutContextChecking(@NotNull PostfixTemplateProvider provider,
                                                                 @NotNull CharSequence documentContent,
                                                                 int currentOffset) {
    int startOffset = currentOffset;
    if (documentContent.length() < startOffset) {
      return null;
    }

    while (startOffset > 0) {
      ProgressManager.checkCanceled();
      char currentChar = documentContent.charAt(startOffset - 1);
      if (!Character.isJavaIdentifierPart(currentChar)) {
        if (!provider.isTerminalSymbol(currentChar)) {
          return null;
        }
        startOffset--;
        break;
      }
      startOffset--;
    }
    return String.valueOf(documentContent.subSequence(startOffset, currentOffset));
  }

  @Nullable
  @Override
  public String computeTemplateKey(@NotNull CustomTemplateCallback callback) {
    Editor editor = callback.getEditor();
    CharSequence charsSequence = editor.getDocument().getCharsSequence();
    int offset = editor.getCaretModel().getOffset();
    for (PostfixTemplateProvider provider : LanguagePostfixTemplate.LANG_EP.allForLanguage(getLanguage(callback))) {
      String key = computeTemplateKeyWithoutContextChecking(provider, charsSequence, offset);
      if (key != null && isApplicableTemplate(provider, key, callback.getFile(), editor)) {
        return key;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public String computeTemplateKeyWithoutContextChecking(@NotNull CustomTemplateCallback callback) {
    Editor editor = callback.getEditor();
    int currentOffset = editor.getCaretModel().getOffset();
    for (PostfixTemplateProvider provider : LanguagePostfixTemplate.LANG_EP.allForLanguage(getLanguage(callback))) {
      ProgressManager.checkCanceled();
      String key = computeTemplateKeyWithoutContextChecking(provider, editor.getDocument().getCharsSequence(), currentOffset);
      if (key != null) return key;
    }
    return null;
  }

  @Override
  public boolean supportsMultiCaret() {
    return false;
  }

  @Override
  public void expand(@NotNull final String key, @NotNull final CustomTemplateCallback callback) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    Editor editor = callback.getEditor();
    PsiFile file = callback.getContext().getContainingFile();
    for (PostfixTemplateProvider provider : LanguagePostfixTemplate.LANG_EP.allForLanguage(getLanguage(callback))) {
      PostfixTemplate postfixTemplate = findApplicableTemplate(provider, key, editor, file);
      if (postfixTemplate != null) {
        expandTemplate(key, callback, editor, provider, postfixTemplate);
        return;
      }
    }

    // don't care about errors in multiCaret mode
    if (editor.getCaretModel().getAllCarets().size() == 1) {
      LOG.error("Template not found by key: " + key + "; offset = " + callback.getOffset(),
                AttachmentFactory.createAttachment(callback.getFile().getVirtualFile()));
    }
  }

  public static void expandTemplate(@NotNull String key,
                                    @NotNull CustomTemplateCallback callback,
                                    @NotNull Editor editor,
                                    @NotNull PostfixTemplateProvider provider,
                                    @NotNull PostfixTemplate postfixTemplate) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    FeatureUsageTracker.getInstance().triggerFeatureUsed("editing.completion.postfix");
    final PsiFile file = callback.getContext().getContainingFile();
    if (isApplicableTemplate(provider, key, file, editor, postfixTemplate)) {
      int offset = deleteTemplateKey(file, editor, key);
      try {
        provider.preExpand(file, editor);
        PsiElement context = CustomTemplateCallback.getContext(file, positiveOffset(offset));
        expandTemplate(postfixTemplate, editor, context);
      }
      finally {
        provider.afterExpand(file, editor);
      }
    }
    // don't care about errors in multiCaret mode
    else if (editor.getCaretModel().getAllCarets().size() == 1) {
      LOG.error("Template not found by key: " + key + "; offset = " + callback.getOffset(),
                AttachmentFactory.createAttachment(callback.getFile().getVirtualFile()));
    }
  }

  @Override
  public boolean isApplicable(@NotNull CustomTemplateCallback callback, int offset, boolean wrapping) {
    PostfixTemplatesSettings settings = PostfixTemplatesSettings.getInstance();
    if (wrapping || !settings.isPostfixTemplatesEnabled()) {
      return false;
    }
    PsiFile contextFile = callback.getFile();
    Language language = PsiUtilCore.getLanguageAtOffset(contextFile, offset);
    CharSequence fileText = callback.getEditor().getDocument().getImmutableCharSequence();
    for (PostfixTemplateProvider provider : LanguagePostfixTemplate.LANG_EP.allForLanguage(language)) {
      if (StringUtil.isNotEmpty(computeTemplateKeyWithoutContextChecking(provider, fileText, offset + 1))) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean supportsWrapping() {
    return false;
  }

  @Override
  public void wrap(@NotNull String selection, @NotNull CustomTemplateCallback callback) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public String getTitle() {
    return "Postfix";
  }

  @Override
  public char getShortcut() {
    return (char)PostfixTemplatesSettings.getInstance().getShortcut();
  }

  @Override
  public boolean hasCompletionItem(@NotNull CustomTemplateCallback callback, int offset) {
    return true;
  }

  @NotNull
  @Override
  public Collection<? extends CustomLiveTemplateLookupElement> getLookupElements(@NotNull PsiFile file,
                                                                                 @NotNull Editor editor,
                                                                                 int offset) {
    Collection<CustomLiveTemplateLookupElement> result = new HashSet<>();
    CustomTemplateCallback callback = new CustomTemplateCallback(editor, file);
    Disposable parentDisposable = Disposer.newDisposable();
    try {
      for (PostfixTemplateProvider provider : LanguagePostfixTemplate.LANG_EP.allForLanguage(getLanguage(callback))) {
        ProgressManager.checkCanceled();
        String key = computeTemplateKeyWithoutContextChecking(callback);
        if (key != null && editor.getCaretModel().getCaretCount() == 1) {
          Condition<PostfixTemplate> isApplicationTemplateFunction =
            createIsApplicationTemplateFunction(provider, key, file, editor, parentDisposable);
          for (PostfixTemplate postfixTemplate : PostfixTemplatesUtils.getAvailableTemplates(provider)) {
            ProgressManager.checkCanceled();
            if (isApplicationTemplateFunction.value(postfixTemplate)) {
              result.add(new PostfixTemplateLookupElement(this, postfixTemplate, postfixTemplate.getKey(), provider, false));
            }
          }
        }
      }
    }
    finally {
      Disposer.dispose(parentDisposable);
    }

    return result;
  }

  private static void expandTemplate(@NotNull final PostfixTemplate template,
                                     @NotNull final Editor editor,
                                     @NotNull final PsiElement context) {
    PostfixTemplateLogger.log(template, context);
    if (template.startInWriteAction()) {
      ApplicationManager.getApplication().runWriteAction(() -> CommandProcessor.getInstance()
                                                                               .executeCommand(context.getProject(),
                                                                                               () -> template.expand(context, editor),
                                                                                               CodeInsightBundle.message("command.expand.postfix.template"),
                                                                                               POSTFIX_TEMPLATE_ID));
    }
    else {
      template.expand(context, editor);
    }
  }


  private static int deleteTemplateKey(@NotNull final PsiFile file, @NotNull final Editor editor, @NotNull final String key) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    final int currentOffset = editor.getCaretModel().getOffset();
    final int newOffset = currentOffset - key.length();
    ApplicationManager.getApplication().runWriteAction(() -> CommandProcessor.getInstance().runUndoTransparentAction(() -> {
      Document document = editor.getDocument();
      document.deleteString(newOffset, currentOffset);
      editor.getCaretModel().moveToOffset(newOffset);
      PsiDocumentManager.getInstance(file.getProject()).commitDocument(document);
    }));
    return newOffset;
  }

  private static Condition<PostfixTemplate> createIsApplicationTemplateFunction(@NotNull final PostfixTemplateProvider provider,
                                                                                @NotNull String key,
                                                                                @NotNull PsiFile file,
                                                                                @NotNull Editor editor,
                                                                                @NotNull Disposable parentDisposable) {
    if (file.getFileType().isBinary()) {
      return Conditions.alwaysFalse();
    }

    int currentOffset = editor.getCaretModel().getOffset();
    final int newOffset = currentOffset - key.length();
    CharSequence fileContent = editor.getDocument().getCharsSequence();
    StringBuilder fileContentWithoutKey = new StringBuilder();
    fileContentWithoutKey.append(fileContent.subSequence(0, newOffset));
    fileContentWithoutKey.append(fileContent.subSequence(currentOffset, fileContent.length()));
    PsiFile copyFile = copyFile(file, fileContentWithoutKey);
    Document copyDocument = copyFile.getViewProvider().getDocument();
    if (copyDocument == null) {
      return Conditions.alwaysFalse();
    }

    copyFile = provider.preCheck(copyFile, editor, newOffset);
    copyDocument = copyFile.getViewProvider().getDocument();
    if (copyDocument == null) {
      return Conditions.alwaysFalse();
    }

    // The copy document doesn't contain live template key.
    // Register offset translator to make getOriginalElement() work in the copy.
    Document fileDocument = file.getViewProvider().getDocument();
    if (fileDocument != null && fileDocument.getTextLength() < currentOffset) {
      LOG.error("File document length (" + fileDocument.getTextLength() + ") is less than offset (" + currentOffset + ")",
                AttachmentFactory.createAttachment(fileDocument), AttachmentFactory.createAttachment(editor.getDocument()));
    }
    Document originalDocument = editor.getDocument();
    OffsetTranslator translator = new OffsetTranslator(originalDocument, file, copyDocument, newOffset, currentOffset, "");
    Disposer.register(parentDisposable, translator);

    final PsiElement context = CustomTemplateCallback.getContext(copyFile, positiveOffset(newOffset));
    final Document finalCopyDocument = copyDocument;
    return template -> template != null && isDumbEnough(template, context) &&
                       template.isEnabled(provider) && template.isApplicable(context, finalCopyDocument, newOffset);
  }

  private static boolean isDumbEnough(@NotNull PostfixTemplate template, @NotNull PsiElement context) {
    DumbService dumbService = DumbService.getInstance(context.getProject());
    return !dumbService.isDumb() || DumbService.isDumbAware(template);
  }

  @NotNull
  public static PsiFile copyFile(@NotNull PsiFile file, @NotNull StringBuilder fileContentWithoutKey) {
    final PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(file.getProject());
    Language language = LanguageUtil.getLanguageForPsi(file.getProject(), file.getVirtualFile());
    PsiFile copy = language != null ? psiFileFactory.createFileFromText(file.getName(), language, fileContentWithoutKey, false, true)
                                    : psiFileFactory.createFileFromText(file.getName(), file.getFileType(), fileContentWithoutKey);

    if (copy instanceof PsiFileImpl) {
      ((PsiFileImpl)copy).setOriginalFile(TemplateLanguageUtil.getBaseFile(file));
    }

    VirtualFile vFile = copy.getVirtualFile();
    if (vFile != null) {
      vFile.putUserData(UndoConstants.DONT_RECORD_UNDO, Boolean.TRUE);
    }
    return copy;
  }

  public static boolean isApplicableTemplate(@NotNull PostfixTemplateProvider provider,
                                             @NotNull String key,
                                             @NotNull PsiFile file,
                                             @NotNull Editor editor) {
    return findApplicableTemplate(provider, key, editor, file) != null;
  }

  private static boolean isApplicableTemplate(@NotNull PostfixTemplateProvider provider,
                                              @NotNull String key,
                                              @NotNull PsiFile file,
                                              @NotNull Editor editor,
                                              @Nullable PostfixTemplate template) {
    Disposable parentDisposable = Disposer.newDisposable();
    try {
      return createIsApplicationTemplateFunction(provider, key, file, editor, parentDisposable).value(template);
    }
    finally {
      Disposer.dispose(parentDisposable);
    }
  }

  @NotNull
  private static Set<String> getKeys(@NotNull PostfixTemplateProvider provider) {
    Set<String> result = new HashSet<>();
    for (PostfixTemplate template : PostfixTemplatesUtils.getAvailableTemplates(provider)) {
      result.add(template.getKey());
    }
    return result;
  }

  @Nullable
  private static PostfixTemplate findApplicableTemplate(@NotNull PostfixTemplateProvider provider,
                                                        @Nullable String key,
                                                        @NotNull Editor editor,
                                                        @NotNull PsiFile file) {
    for (PostfixTemplate template : PostfixTemplatesUtils.getAvailableTemplates(provider)) {
      if (template.getKey().equals(key) && isApplicableTemplate(provider, key, file, editor, template)) {
        return template;
      }
    }
    return null;
  }

  private static Language getLanguage(@NotNull CustomTemplateCallback callback) {
    return PsiUtilCore.getLanguageAtOffset(callback.getFile(), callback.getOffset());
  }

  private static int positiveOffset(int offset) {
    return offset > 0 ? offset - 1 : offset;
  }
}
