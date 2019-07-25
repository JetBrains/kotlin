// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingLevelManager;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.diagnostic.PluginException;
import com.intellij.lang.ExternalLanguageAnnotators;
import com.intellij.lang.Language;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationSession;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.BackgroundTaskUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.util.ui.update.Update;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author ven
 */
public class ExternalToolPass extends ProgressableTextEditorHighlightingPass {
  private static final Logger LOG = Logger.getInstance(ExternalToolPass.class);

  private final Document myDocument;
  private final AnnotationHolderImpl myAnnotationHolder;
  private final ExternalToolPassFactory myExternalToolPassFactory;
  private final boolean myMainHighlightingPass;
  private final List<MyData> myAnnotationData = new ArrayList<>();

  private static class MyData {
    final ExternalAnnotator annotator;
    final PsiFile psiRoot;
    final Object collectedInfo;
    volatile Object annotationResult;

    MyData(ExternalAnnotator annotator, PsiFile psiRoot, Object collectedInfo) {
      this.annotator = annotator;
      this.psiRoot = psiRoot;
      this.collectedInfo = collectedInfo;
    }
  }

  ExternalToolPass(@NotNull ExternalToolPassFactory factory,
                   @NotNull PsiFile file,
                   @NotNull Editor editor,
                   int startOffset,
                   int endOffset) {
    this(factory, file, editor.getDocument(), editor, startOffset, endOffset, new DefaultHighlightInfoProcessor(), false);
  }

  ExternalToolPass(@NotNull ExternalToolPassFactory factory,
                   @NotNull PsiFile file,
                   @NotNull Document document,
                   @Nullable Editor editor,
                   int startOffset,
                   int endOffset,
                   @NotNull HighlightInfoProcessor processor,
                   boolean mainHighlightingPass) {
    super(file.getProject(), document, "External annotators", file, editor, new TextRange(startOffset, endOffset), false, processor);
    myDocument = document;
    myAnnotationHolder = new AnnotationHolderImpl(new AnnotationSession(file));
    myExternalToolPassFactory = factory;
    myMainHighlightingPass = mainHighlightingPass;
  }

  @Override
  protected void collectInformationWithProgress(@NotNull ProgressIndicator progress) {
    FileViewProvider viewProvider = myFile.getViewProvider();
    HighlightingLevelManager highlightingManager = HighlightingLevelManager.getInstance(myProject);
    Map<PsiFile, List<ExternalAnnotator>> allAnnotators = new HashMap<>();
    int externalAnnotatorsInRoots = 0;
    for (Language language : viewProvider.getLanguages()) {
      PsiFile psiRoot = viewProvider.getPsi(language);
      if (highlightingManager.shouldInspect(psiRoot)) {
        List<ExternalAnnotator> annotators = ExternalLanguageAnnotators.allForFile(language, psiRoot);
        if (!annotators.isEmpty()) {
          externalAnnotatorsInRoots += annotators.size();
          allAnnotators.put(psiRoot, annotators);
        }
      }
    }
    setProgressLimit(externalAnnotatorsInRoots);

    InspectionProfileImpl profile = InspectionProjectProfileManager.getInstance(myProject).getCurrentProfile();
    boolean errorFound = DaemonCodeAnalyzerEx.getInstanceEx(myProject).getFileStatusMap().wasErrorFound(myDocument);
    Editor editor = getEditor();

    DumbService dumbService = DumbService.getInstance(myProject);
    for (PsiFile psiRoot : allAnnotators.keySet()) {
      for (ExternalAnnotator annotator : allAnnotators.get(psiRoot)) {
        progress.checkCanceled();

        try {
          if (dumbService.isDumb() && !DumbService.isDumbAware(annotator)) {
            continue;
          }

          String shortName = annotator.getPairedBatchInspectionShortName();
          if (shortName != null) {
            HighlightDisplayKey key = HighlightDisplayKey.find(shortName);
            if (key == null) {
              if (!ApplicationManager.getApplication().isUnitTestMode()) {
                // tests should care about registering corresponding paired tools
                process(new Exception("Paired tool '" + shortName + "' not found"), annotator, psiRoot);
              }
              continue;
            }
            if (!profile.isToolEnabled(key, myFile)) {
              continue;
            }
          }

          Object collectedInfo = null;
          try {
            collectedInfo = editor != null ? annotator.collectInformation(psiRoot, editor, errorFound) : annotator.collectInformation(psiRoot);
          }
          catch (Throwable t) {
            process(t, annotator, psiRoot);
          }

          if (collectedInfo != null) {
            myAnnotationData.add(new MyData(annotator, psiRoot, collectedInfo));
          }
        }
        finally {
          advanceProgress(1);
        }
      }
    }
  }

  @NotNull
  @Override
  public List<HighlightInfo> getInfos() {
    if (myProject.isDisposed()) {
      return Collections.emptyList();
    }
    if (myMainHighlightingPass) {
      doAnnotate();
      doApply();
      return getHighlights();
    }
    return super.getInfos();
  }

  @Override
  protected void applyInformationWithProgress() {
    long modificationStampBefore = myDocument.getModificationStamp();

    Update update = new Update(myFile) {
      @Override
      public void setRejected() {
        super.setRejected();
        doFinish(getHighlights(), modificationStampBefore);
      }

      @Override
      public void run() {
        if (!documentChanged(modificationStampBefore) && !myProject.isDisposed()) {
          BackgroundTaskUtil.runUnderDisposeAwareIndicator(myProject, () -> {
            doAnnotate();
            ReadAction.run(() -> {
              ProgressManager.checkCanceled();
              if (!documentChanged(modificationStampBefore)) {
                doApply();
                doFinish(getHighlights(), modificationStampBefore);
              }
            });
          });
        }
      }
    };

    myExternalToolPassFactory.scheduleExternalActivity(update);
  }

  private boolean documentChanged(long modificationStampBefore) {
    return myDocument.getModificationStamp() != modificationStampBefore;
  }

  @SuppressWarnings("unchecked")
  private void doAnnotate() {
    for (MyData data : myAnnotationData) {
      try {
        data.annotationResult = data.annotator.doAnnotate(data.collectedInfo);
      }
      catch (Throwable t) {
        process(t, data.annotator, data.psiRoot);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void doApply() {
    for (MyData data : myAnnotationData) {
      if (data.annotationResult != null && data.psiRoot != null && data.psiRoot.isValid()) {
        try {
          data.annotator.apply(data.psiRoot, data.annotationResult, myAnnotationHolder);
        }
        catch (Throwable t) {
          process(t, data.annotator, data.psiRoot);
        }
      }
    }
  }

  private List<HighlightInfo> getHighlights() {
    List<HighlightInfo> infos = new ArrayList<>(myAnnotationHolder.size());
    for (Annotation annotation : myAnnotationHolder) {
      infos.add(HighlightInfo.fromAnnotation(annotation));
    }
    return infos;
  }

  private void doFinish(List<? extends HighlightInfo> highlights, long modificationStampBefore) {
    Editor editor = getEditor();
    assert editor != null;
    ApplicationManager.getApplication().invokeLater(() -> {
      if (!documentChanged(modificationStampBefore) && !myProject.isDisposed()) {
        int start = myRestrictRange.getStartOffset();
        int end = myRestrictRange.getEndOffset();
        UpdateHighlightersUtil.setHighlightersToEditor(myProject, myDocument, start, end, highlights, getColorsScheme(), getId());
        DaemonCodeAnalyzerEx.getInstanceEx(myProject).getFileStatusMap().markFileUpToDate(myDocument, getId());
      }
    }, ModalityState.stateForComponent(editor.getComponent()));
  }

  private static void process(Throwable t, ExternalAnnotator annotator, PsiFile root) {
    if (t instanceof ProcessCanceledException) throw (ProcessCanceledException)t;

    VirtualFile file = root.getVirtualFile();
    String path = file != null ? file.getPath() : root.getName();

    String message = "annotator: " + annotator + " (" + annotator.getClass() + ")";
    PluginException pe = PluginException.createByClass(message, t, annotator.getClass());
    LOG.error("ExternalToolPass: ", pe, new Attachment("root_path.txt", path));
  }
}