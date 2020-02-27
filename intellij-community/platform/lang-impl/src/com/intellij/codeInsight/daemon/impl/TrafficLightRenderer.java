// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeInsight.daemon.DaemonBundle;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightLevelUtil;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingLevelManager;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingSettingsPerFile;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.icons.AllIcons;
import com.intellij.ide.PowerSaveMode;
import com.intellij.lang.Language;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.impl.EditorMarkupModelImpl;
import com.intellij.openapi.editor.impl.event.MarkupModelListener;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.options.ex.ConfigurableExtensionPointUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.profile.codeInspection.ui.ErrorsConfigurableProvider;
import com.intellij.psi.*;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.TextIcon;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.io.storage.HeavyProcessLatch;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.JBValue;
import com.intellij.util.ui.UIUtil;
import com.intellij.xml.util.XmlStringUtil;
import gnu.trove.TIntArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class TrafficLightRenderer implements ErrorStripeRenderer, Disposable {
  private final Project myProject;
  private final Document myDocument;
  private final DaemonCodeAnalyzerImpl myDaemonCodeAnalyzer;
  private final SeverityRegistrar mySeverityRegistrar;
  private final PsiManager myPsiManager;
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

  private static final JBValue ICON_TEXT_GAP = new JBValue.Float(6);

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
    myPsiManager = myProject != null ? PsiManager.getInstance(myProject) : null;

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
      StringBuilder s = new StringBuilder("DS: finished=" + errorAnalyzingFinished);
      s.append("; pass statuses: ").append(passStati.size()).append("; ");
      for (ProgressableTextEditorHighlightingPass passStatus : passStati) {
        s.append(
          String.format("(%s %2.0f%% %b)", passStatus.getPresentableName(), passStatus.getProgress() * 100, passStatus.isFinished()));
      }
      s.append("; error count: ").append(errorCount.length).append(": ").append(new TIntArrayList(errorCount));
      return s.toString();
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
      icon = AllIcons.General.InspectionsPowerSaveMode;
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
    StringBuilder text = new StringBuilder();
    for (int i = lastNotNullIndex; i >= 0; i--) {
      int count = status.errorCount[i];
      if (count > 0) {
        final HighlightSeverity severity = mySeverityRegistrar.getSeverityByIndex(i);
        String name = count > 1 ? StringUtil.pluralize(StringUtil.toLowerCase(severity.getName())) : StringUtil.toLowerCase(severity.getName());
        text.append(status.errorAnalyzingFinished
                ? DaemonBundle.message("errors.found", count, name)
                : DaemonBundle.message("errors.found.so.far", count, name)).append("<br/>");
        currentSeverityErrors += count;
      }
    }
    if (currentSeverityErrors == 0) {
      text.append(status.errorAnalyzingFinished
              ? DaemonBundle.message("no.errors.or.warnings.found")
              : DaemonBundle.message("no.errors.or.warnings.found.so.far")).append("<br/>");
    }
    statistics = XmlStringUtil.wrapInHtml(text.toString());

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

  @Override
  public AnalyzerStatus getStatus(Editor editor) {
    if (PowerSaveMode.isEnabled()) {
      return new AnalyzerStatus(AllIcons.General.InspectionsPowerSaveMode,
                                  "Code analysis is disabled in power save mode", this::getUIController);
    }
    else {
      DaemonCodeAnalyzerStatus status = getDaemonCodeAnalyzerStatus(mySeverityRegistrar);
      List<Icon> statusIcons = new ArrayList<>();
      Font editorFont = editor.getComponent().getFont();
      Font font = editorFont.deriveFont(Font.PLAIN, editorFont.getSize() - JBUIScale.scale(2));

      StringBuilder statusTextBuilder = new StringBuilder();
      int currentSeverityErrors = 0;

      int lastNotNullIndex = ArrayUtil.lastIndexOfNot(status.errorCount, 0);
      Icon mainIcon = lastNotNullIndex == -1 ? AllIcons.General.InspectionsOK : mySeverityRegistrar.getRendererIconByIndex(lastNotNullIndex);

      for (int i = lastNotNullIndex; i >= 0; i--) {
        int count = status.errorCount[i];
        if (count > 0) {
          HighlightSeverity severity = mySeverityRegistrar.getSeverityByIndex(i);
          String name = StringUtil.toLowerCase(severity.getName());
          if (count > 1) {
            name = StringUtil.pluralize(name);
          }

          if (currentSeverityErrors > 0) statusTextBuilder.append(", ");
          statusTextBuilder.append(count).append(" ").append(name);
          currentSeverityErrors += count;

          statusIcons.add(mySeverityRegistrar.getRendererIconByIndex(i));
          TextIcon icon = new TextIcon(Integer.toString(status.errorCount[i]),
                                       editor.getColorsScheme().getDefaultForeground(), null, 0);
          icon.setFont(font);
          statusIcons.add(icon);
        }
      }

      if (statusIcons.size() > 0) {
        LayeredIcon statusIcon = new LayeredIcon(statusIcons.size());
        int maxIconHeight = statusIcons.stream().mapToInt(i -> i.getIconHeight()).max().orElse(0);
        for (int i = 0, xShift = 0; i < statusIcons.size(); i += 2) {
          Icon icon = statusIcons.get(i);
          int yShift = (maxIconHeight - icon.getIconHeight()) / 2;
          statusIcon.setIcon(icon, i, xShift, yShift);
          //noinspection AssignmentToForLoopParameter
          xShift += icon.getIconWidth();

          icon = statusIcons.get(i + 1);
          yShift = (maxIconHeight - icon.getIconHeight()) / 2;
          statusIcon.setIcon(icon, i + 1, xShift, yShift);
          //noinspection AssignmentToForLoopParameter
          xShift += icon.getIconWidth() + ICON_TEXT_GAP.get();
        }

        if (!status.errorAnalyzingFinished) statusTextBuilder.append(" found so far");

        return new AnalyzerStatus(mainIcon, statusTextBuilder.toString(), this::getUIController).
          withNavigation().
          withExpandedIcon(statusIcon);
      }
      else {
        if (StringUtil.isNotEmpty(status.reasonWhyDisabled)) {
          TextIcon offIcon = new TextIcon("OFF", editor.getColorsScheme().getDefaultForeground(), null, 0);
          offIcon.setFont(font);

          statusTextBuilder.append("No analysis has been performed:<br/>").append(status.reasonWhyDisabled);
          return new AnalyzerStatus(AllIcons.General.InspectionsTrafficOff, statusTextBuilder.toString(), this::getUIController).
            withExpandedIcon(new LayeredIcon(offIcon));
        }
        else if (StringUtil.isNotEmpty(status.reasonWhySuspended)) {
          TextIcon icon = new TextIcon("Indexing...", editor.getColorsScheme().getDefaultForeground(), null, 0);
          icon.setFont(font);
          statusTextBuilder.append("Code analysis has been suspended:<br/>").append(status.reasonWhySuspended);
          return new AnalyzerStatus(AllIcons.General.InspectionsPause, statusTextBuilder.toString(), this::getUIController).
            withExpandedIcon(new LayeredIcon(icon));
        }
        else if (status.errorAnalyzingFinished) {
          return new AnalyzerStatus(AllIcons.General.InspectionsOK, "No problems found", this::getUIController);
        }
        else {
          TextIcon icon = new TextIcon("Analyzing...", editor.getColorsScheme().getDefaultForeground(), null, 0);
          icon.setFont(font);
          return new AnalyzerStatus(AllIcons.General.InspectionsEye, "No problems found so far", this::getUIController).
            withExpandedIcon(new LayeredIcon(icon));
        }
      }
    }
  }

  private UIController myCurrentController; // This is cache. Don't access directly. Only via the method below.

  private AnalyzerController getUIController() {
    PsiFile psiFile = getPsiFile();
    if (myCurrentController == null || myPsiManager != null && !myPsiManager.areElementsEquivalent(myCurrentController.myPsiFile, psiFile)) {
      myCurrentController = new UIController(psiFile);
    }

    return myCurrentController;
  }

  private class UIController implements AnalyzerController {
    private final PsiFile myPsiFile;
    private final boolean notInLibrary;
    private final AnAction myMenuAction;
    private final List<LanguageHighlightLevel> myLevelsList;
    private final List<HectorComponentPanel> myAdditionalPanels;

    private UIController(PsiFile psiFile) {
      myPsiFile = psiFile;

      if (psiFile != null) {
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
        VirtualFile virtualFile = psiFile.getVirtualFile();
        assert virtualFile != null;
        notInLibrary = !fileIndex.isInLibrary(virtualFile) || fileIndex.isInContent(virtualFile);
        myAdditionalPanels = HectorComponentPanelsProvider.EP_NAME.extensions(myProject).
          map(hp -> hp.createConfigurable(myPsiFile)).filter(p -> p != null).collect(Collectors.toList());
      }
      else {
        notInLibrary = true;
        myAdditionalPanels = Collections.emptyList();
      }

      myMenuAction = initActions();
      myLevelsList = initLevels();
    }

    private AnAction initActions() {
      MenuAction result = new MenuAction();
      if (myProject != null) { // Configure inspections
        result.add(new DumbAwareAction(EditorBundle.message("iw.configure.inspections")) {
          @Override
          public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(myDaemonCodeAnalyzer.isHighlightingAvailable(getPsiFile()));
          }

          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            if (!myProject.isDisposed()) {
              Configurable projectConfigurable = ConfigurableExtensionPointUtil.createProjectConfigurableForProvider(myProject, ErrorsConfigurableProvider.class);
              if (projectConfigurable != null) {
                ShowSettingsUtil.getInstance().editConfigurable(getProject(), projectConfigurable);
              }
            }
          }
        });
      }

      result.add(DaemonEditorPopup.createGotoGroup());

      if (myProject != null) { // Import popup
        result.addAll(Separator.create(), new ToggleAction(EditorBundle.message("iw.show.import.tooltip")) {
          @Override
          public boolean isSelected(@NotNull AnActionEvent e) {
            return myDaemonCodeAnalyzer.isImportHintsEnabled(myPsiFile);
          }

          @Override
          public void setSelected(@NotNull AnActionEvent e, boolean state) {
            myDaemonCodeAnalyzer.setImportHintsEnabled(myPsiFile, state);
          }

          @Override
          public void update(@NotNull AnActionEvent e) {
            super.update(e);
            e.getPresentation().setEnabled(myDaemonCodeAnalyzer.isAutohintsAvailable(myPsiFile));
          }
        });
      }
      return result;
    }

    private List<LanguageHighlightLevel> initLevels() {
      List<LanguageHighlightLevel> result = new ArrayList<>();
      if (myProject != null && !myProject.isDisposed()) {
        FileViewProvider viewProvider = myPsiFile.getViewProvider();
        HighlightingLevelManager hlManager = HighlightingLevelManager.getInstance(myProject);
        for (Language language : viewProvider.getLanguages()) {
          PsiFile psiRoot = viewProvider.getPsi(language);
          result.add(new LanguageHighlightLevel(language, getAHLevel(hlManager.shouldHighlight(psiRoot), hlManager.shouldInspect(psiRoot))));
        }
      }
      return result;
    }

    @NotNull
    @Override
    public AnAction getActionMenu() {
      return myMenuAction;
    }

    @Override
    @NotNull
    public List<AHLevel> getAvailableLevels() {
      return notInLibrary ? Arrays.asList(AHLevel.values()) : Arrays.asList(AHLevel.NONE, AHLevel.ERRORS);
    }

    @NotNull
    @Override
    public List<LanguageHighlightLevel> getHighlightLevels() {
      return Collections.unmodifiableList(myLevelsList);
    }

    @Override
    public void setHighLightLevel(@NotNull LanguageHighlightLevel level) {
      if (myProject != null && !myProject.isDisposed() && !myLevelsList.contains(level)) {
        FileViewProvider viewProvider = myPsiFile.getViewProvider();

        PsiElement root = viewProvider.getPsi(level.getLanguage());
        if (level.getLevel() == AHLevel.NONE) {
          HighlightLevelUtil.forceRootHighlighting(root, FileHighlightingSetting.SKIP_HIGHLIGHTING);
        }
        else if (level.getLevel() == AHLevel.ERRORS) {
          HighlightLevelUtil.forceRootHighlighting(root, FileHighlightingSetting.SKIP_INSPECTION);
        }
        else {
          HighlightLevelUtil.forceRootHighlighting(root, FileHighlightingSetting.FORCE_HIGHLIGHTING);
        }

        myLevelsList.replaceAll(l -> l.getLanguage().equals(level.getLanguage()) ? level : l);

        InjectedLanguageManager.getInstance(myProject).dropFileCaches(myPsiFile);
        myDaemonCodeAnalyzer.restart();
      }
    }

    @Override
    public void fillHectorPanels(@NotNull Container container, @NotNull GridBag gc) {
      myAdditionalPanels.stream().peek(p -> p.reset()).map(p -> p.createComponent()).filter(c -> c != null).
        forEach(c -> container.add(c, gc.nextLine().next().fillCellHorizontally().coverLine().weightx(1.0)));
    }

    @Override
    public boolean onClose() {
      if (myAdditionalPanels.stream().allMatch(p -> p.canClose())) {
        if(myAdditionalPanels.stream().filter(p -> p.isModified()).peek(TrafficLightRenderer::applyPanel).count() > 0) {
          InjectedLanguageManager.getInstance(myProject).dropFileCaches(myPsiFile);
          myDaemonCodeAnalyzer.restart();
        }

        myAdditionalPanels.forEach(p -> p.disposeUIResources());
        return true;
      }
      else return false;
    }
  }

  private static void applyPanel(@NotNull HectorComponentPanel panel) {
    try {
      panel.apply();
    }
    catch (ConfigurationException ignored) {}
  }

  private static class MenuAction extends DefaultActionGroup implements HintManagerImpl.ActionToIgnore {
    private MenuAction() {
      setPopup(true);
    }
  }

  private static AHLevel getAHLevel(boolean highlight, boolean inspect) {
    if (!highlight && !inspect) return AHLevel.NONE;
    else if (highlight && !inspect) return AHLevel.ERRORS;
    else return AHLevel.ALL;
  }
}
