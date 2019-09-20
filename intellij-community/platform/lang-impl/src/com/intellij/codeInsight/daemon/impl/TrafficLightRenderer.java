// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeInsight.daemon.DaemonBundle;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingSettingsPerFile;
import com.intellij.icons.AllIcons;
import com.intellij.ide.PowerSaveMode;
import com.intellij.lang.Language;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.impl.EditorMarkupModelImpl;
import com.intellij.openapi.editor.impl.event.MarkupModelListener;
import com.intellij.openapi.editor.markup.ErrorStripeRenderer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.io.storage.HeavyProcessLatch;
import com.intellij.util.ui.UIUtil;
import com.intellij.xml.util.XmlStringUtil;
import gnu.trove.TIntArrayList;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class TrafficLightRenderer implements ErrorStripeRenderer, Disposable {
  private final Project myProject;
  private final Document myDocument;
  private final DaemonCodeAnalyzerImpl myDaemonCodeAnalyzer;
  private final SeverityRegistrar mySeverityRegistrar;
  private Icon icon;
  String statistics;
  String statusLabel;
  String statusExtraLine;
  boolean passStatusesVisible;
  final Map<ProgressableTextEditorHighlightingPass, Pair<JProgressBar, JLabel>> passes = new LinkedHashMap<>();
  static final int MAX = 100;
  boolean progressBarsEnabled;
  Boolean progressBarsCompleted;

  /**
   * array filled with number of highlighters with a given severity.
   * errorCount[idx] == number of highlighters of severity with index idx in this markup model.
   * severity index can be obtained via com.intellij.codeInsight.daemon.impl.SeverityRegistrar#getSeverityIdx(com.intellij.lang.annotation.HighlightSeverity)
   */
  protected int[] errorCount;

  /**
   * @deprecated Please use the constructor not taking PsiFile parameter: {@link #TrafficLightRenderer(Project, Document)}
   */
  @Deprecated
  public TrafficLightRenderer(@Nullable Project project, Document document, PsiFile psiFile) {
    this(project, document);
  }

  public TrafficLightRenderer(@Nullable Project project, @Nullable Document document) {
    myProject = project;
    myDaemonCodeAnalyzer = project == null ? null : (DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(project);
    myDocument = document;
    mySeverityRegistrar = SeverityRegistrar.getSeverityRegistrar(myProject);
    refresh(null);

    if (project != null && document != null) {
      final MarkupModelEx model = (MarkupModelEx)DocumentMarkupModel.forDocument(document, project, true);
      model.addMarkupModelListener(this, new MarkupModelListener() {
        @Override
        public void afterAdded(@NotNull RangeHighlighterEx highlighter) {
          incErrorCount(highlighter, 1);
        }

        @Override
        public void beforeRemoved(@NotNull RangeHighlighterEx highlighter) {
          incErrorCount(highlighter, -1);
        }
      });
      UIUtil.invokeLaterIfNeeded(() -> {
        for (RangeHighlighter rangeHighlighter : model.getAllHighlighters()) {
          incErrorCount(rangeHighlighter, 1);
        }
      });
    }
  }

  private PsiFile getPsiFile() {
    return myProject == null ? null : PsiDocumentManager.getInstance(myProject).getPsiFile(myDocument);
  }

  @NotNull
  public SeverityRegistrar getSeverityRegistrar() {
    return mySeverityRegistrar;
  }

  protected void refresh(@Nullable EditorMarkupModelImpl editorMarkupModel) {
    int maxIndex = mySeverityRegistrar.getSeverityMaxIndex();
    if (errorCount != null && maxIndex + 1 == errorCount.length) return;
    errorCount = new int[maxIndex + 1];
  }

  @Override
  public void dispose() {
  }

  private void incErrorCount(RangeHighlighter highlighter, int delta) {
    HighlightInfo info = HighlightInfo.fromRangeHighlighter(highlighter);
    if (info == null) return;
    HighlightSeverity infoSeverity = info.getSeverity();
    if (infoSeverity.myVal <= HighlightSeverity.INFORMATION.myVal) return;
    final int severityIdx = mySeverityRegistrar.getSeverityIdx(infoSeverity);
    if (severityIdx != -1) {
      errorCount[severityIdx] += delta;
    }
  }

  public boolean isValid() {
    return myProject == null || myDocument == null || getPsiFile() != null;
  }

  protected static class DaemonCodeAnalyzerStatus {
    public boolean errorAnalyzingFinished; // all passes done
    List<ProgressableTextEditorHighlightingPass> passStati = Collections.emptyList();
    public int[] errorCount = ArrayUtilRt.EMPTY_INT_ARRAY;
    // Used in Rider
    public String reasonWhyDisabled;
    // Used in Rider
    public String reasonWhySuspended;

    public DaemonCodeAnalyzerStatus() {
    }

    @Override
    public String toString() {
      @NonNls String s = "DS: finished=" + errorAnalyzingFinished;
      s += "; pass statuses: " + passStati.size() + "; ";
      for (ProgressableTextEditorHighlightingPass passStatus : passStati) {
        s += String.format("(%s %2.0f%% %b)", passStatus.getPresentableName(), passStatus.getProgress() *100, passStatus.isFinished());
      }
      s += "; error count: "+errorCount.length + ": "+new TIntArrayList(errorCount);
      return s;
    }
  }

  @NotNull
  protected DaemonCodeAnalyzerStatus getDaemonCodeAnalyzerStatus(@NotNull SeverityRegistrar severityRegistrar) {
    DaemonCodeAnalyzerStatus status = new DaemonCodeAnalyzerStatus();
    PsiFile psiFile = getPsiFile();
    if (psiFile == null) {
      status.reasonWhyDisabled = "No file";
      status.errorAnalyzingFinished = true;
      return status;
    }
    if (myProject != null && myProject.isDisposed()) {
      status.reasonWhyDisabled = "Project is disposed";
      status.errorAnalyzingFinished = true;
      return status;
    }
    if (!myDaemonCodeAnalyzer.isHighlightingAvailable(psiFile)) {
      if (!psiFile.isPhysical()) {
        status.reasonWhyDisabled = "File is generated";
        status.errorAnalyzingFinished = true;
        return status;
      }
      if (psiFile instanceof PsiCompiledElement) {
        status.reasonWhyDisabled = "File is decompiled";
        status.errorAnalyzingFinished = true;
        return status;
      }
      final FileType fileType = psiFile.getFileType();
      if (fileType.isBinary()) {
        status.reasonWhyDisabled = "File is binary";
        status.errorAnalyzingFinished = true;
        return status;
      }
      status.reasonWhyDisabled = "Highlighting is disabled for this file";
      status.errorAnalyzingFinished = true;
      return status;
    }

    FileViewProvider provider = psiFile.getViewProvider();
    Set<Language> languages = provider.getLanguages();
    HighlightingSettingsPerFile levelSettings = HighlightingSettingsPerFile.getInstance(myProject);
    boolean shouldHighlight = languages.isEmpty();
    for (Language language : languages) {
      PsiFile root = provider.getPsi(language);
      FileHighlightingSetting level = levelSettings.getHighlightingSettingForRoot(root);
      shouldHighlight |= level != FileHighlightingSetting.SKIP_HIGHLIGHTING;
    }
    if (!shouldHighlight) {
      status.reasonWhyDisabled = "Highlighting level is None";
      status.errorAnalyzingFinished = true;
      return status;
    }

    if (HeavyProcessLatch.INSTANCE.isRunning()) {
      status.reasonWhySuspended = StringUtil.defaultIfEmpty(HeavyProcessLatch.INSTANCE.getRunningOperationName(), "Heavy operation is running");
      status.errorAnalyzingFinished = true;
      return status;
    }

    status.errorCount = errorCount.clone();
    List<TextEditorHighlightingPass> passes = myDaemonCodeAnalyzer.getPassesToShowProgressFor(myDocument);
    status.passStati = passes.isEmpty() ? Collections.emptyList() :
                       new ArrayList<>(passes.size());
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < passes.size(); i++) {
      TextEditorHighlightingPass tepass = passes.get(i);
      if (!(tepass instanceof ProgressableTextEditorHighlightingPass) ||
          StringUtil.isEmpty(((ProgressableTextEditorHighlightingPass)tepass).getPresentableName())) {
        continue;
      }
      ProgressableTextEditorHighlightingPass pass = (ProgressableTextEditorHighlightingPass)tepass;

      if (pass.getProgress() < 0) continue;
      status.passStati.add(pass);
    }
    status.errorAnalyzingFinished = myDaemonCodeAnalyzer.isAllAnalysisFinished(psiFile);
    status.reasonWhySuspended = myDaemonCodeAnalyzer.isUpdateByTimerEnabled() ? null : "Highlighting is paused temporarily";
    fillDaemonCodeAnalyzerErrorsStatus(status, severityRegistrar);

    return status;
  }

  protected void fillDaemonCodeAnalyzerErrorsStatus(@NotNull DaemonCodeAnalyzerStatus status,
                                                    @NotNull SeverityRegistrar severityRegistrar) {
  }

  protected final Project getProject() {
    return myProject;
  }

  @Override
  public void paint(Component c, Graphics g, Rectangle r) {
    DaemonCodeAnalyzerStatus status = getDaemonCodeAnalyzerStatus(mySeverityRegistrar);
    Icon icon = getIcon(status);
    icon.paintIcon(c, g, r.x, r.y);
  }

  @NotNull
  private Icon getIcon(@NotNull DaemonCodeAnalyzerStatus status) {
    updatePanel(status);
    Icon icon = this.icon;
    if (PowerSaveMode.isEnabled() || status.reasonWhySuspended != null || status.reasonWhyDisabled != null || status.errorAnalyzingFinished) {
      return icon;
    }
    return AllIcons.General.InspectionsEye;
  }

  // return true if panel needs to be rebuilt
  boolean updatePanel(@NotNull DaemonCodeAnalyzerStatus status) {
    progressBarsEnabled = false;
    progressBarsCompleted = null;
    statistics = "";
    passStatusesVisible = false;
    statusLabel = null;
    statusExtraLine = null;

    boolean result = false;
    if (!status.passStati.equals(new ArrayList<>(passes.keySet()))) {
      // passes set has changed
      rebuildPassesMap(status);
      result = true;
    }

    if (PowerSaveMode.isEnabled()) {
      statusLabel = "Code analysis is disabled in power save mode";
      status.errorAnalyzingFinished = true;
      icon = AllIcons.General.InspectionsTrafficOff;
      return result;
    }
    if (status.reasonWhyDisabled != null) {
      statusLabel = "No analysis has been performed";
      statusExtraLine = "(" + status.reasonWhyDisabled + ")";
      passStatusesVisible = true;
      progressBarsCompleted = Boolean.FALSE;
      icon = AllIcons.General.InspectionsTrafficOff;
      return result;
    }
    if (status.reasonWhySuspended != null) {
      statusLabel = "Code analysis has been suspended";
      statusExtraLine = "(" + status.reasonWhySuspended + ")";
      passStatusesVisible = true;
      progressBarsCompleted = Boolean.FALSE;
      icon = AllIcons.General.InspectionsPause;
      return result;
    }

    int lastNotNullIndex = ArrayUtil.lastIndexOfNot(status.errorCount, 0);
    Icon icon = lastNotNullIndex == -1 ? AllIcons.General.InspectionsOK : mySeverityRegistrar.getRendererIconByIndex(lastNotNullIndex);

    if (status.errorAnalyzingFinished) {
      boolean isDumb = myProject != null && DumbService.isDumb(myProject);
      if (isDumb) {
        statusLabel = "Shallow analysis completed";
        statusExtraLine = "Complete results will be available after indexing";
      }
      else {
        statusLabel = DaemonBundle.message("analysis.completed");
      }
      progressBarsCompleted = Boolean.TRUE;
    }
    else {
      statusLabel = DaemonBundle.message("performing.code.analysis");
      passStatusesVisible = true;
      progressBarsEnabled = true;
      progressBarsCompleted = null;
    }

    int currentSeverityErrors = 0;
    @org.intellij.lang.annotations.Language("HTML")
    String text = "";
    for (int i = lastNotNullIndex; i >= 0; i--) {
      int count = status.errorCount[i];
      if (count > 0) {
        final HighlightSeverity severity = mySeverityRegistrar.getSeverityByIndex(i);
        String name = count > 1 ? StringUtil.pluralize(StringUtil.toLowerCase(severity.getName())) : StringUtil.toLowerCase(severity.getName());
        text += status.errorAnalyzingFinished
                ? DaemonBundle.message("errors.found", count, name)
                : DaemonBundle.message("errors.found.so.far", count, name);
        text += "<br>";
        currentSeverityErrors += count;
      }
    }
    if (currentSeverityErrors == 0) {
      text += (status.errorAnalyzingFinished
              ? DaemonBundle.message("no.errors.or.warnings.found")
              : DaemonBundle.message("no.errors.or.warnings.found.so.far")) + "<br>";
    }
    statistics = XmlStringUtil.wrapInHtml(text);

    this.icon = icon;
    return result;
  }

  private void rebuildPassesMap(@NotNull DaemonCodeAnalyzerStatus status) {
    passes.clear();
    for (ProgressableTextEditorHighlightingPass pass : status.passStati) {
      JProgressBar progressBar = new JProgressBar(0, MAX);
      progressBar.setMaximum(MAX);
      UIUtil.applyStyle(UIUtil.ComponentStyle.MINI, progressBar);
      JLabel percentLabel = new JLabel();
      percentLabel.setText(TrafficProgressPanel.MAX_TEXT);
      passes.put(pass, Pair.create(progressBar, percentLabel));
    }
  }
}
