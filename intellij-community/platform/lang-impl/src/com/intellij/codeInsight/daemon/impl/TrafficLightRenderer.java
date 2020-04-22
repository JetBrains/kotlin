// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.UtilBundle;
import com.intellij.analysis.problemsView.inspection.ProblemsViewToolWindowFactory;
import com.intellij.codeInsight.daemon.DaemonBundle;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightLevelUtil;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingLevelManager;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingSettingsPerFile;
import com.intellij.diff.util.DiffUserDataKeys;
import com.intellij.icons.AllIcons;
import com.intellij.ide.PowerSaveMode;
import com.intellij.lang.Language;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.diagnostic.Logger;
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
import com.intellij.openapi.progress.ProcessCanceledException;
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
import com.intellij.util.ArrayUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.DeprecatedMethodException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.storage.HeavyProcessLatch;
import com.intellij.util.ui.GridBag;
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
  @NotNull
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
   * @deprecated Please use {@link #TrafficLightRenderer(Project, Document)} instead
   */
  @Deprecated
  public TrafficLightRenderer(Project project, Document document, PsiFile psiFile) {
    this(project, document);
    DeprecatedMethodException.report("Please use TrafficLightRenderer(Project, Document) instead");
  }

  public TrafficLightRenderer(@NotNull Project project, @Nullable Document document) {
    myProject = project;
    myDaemonCodeAnalyzer = (DaemonCodeAnalyzerImpl)DaemonCodeAnalyzer.getInstance(project);
    myDocument = document;
    mySeverityRegistrar = SeverityRegistrar.getSeverityRegistrar(myProject);

    refresh(null);

    if (document != null) {
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
    return PsiDocumentManager.getInstance(myProject).getPsiFile(myDocument);
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
    return myDocument == null || getPsiFile() != null;
  }

  protected static final class DaemonCodeAnalyzerStatus {
    public boolean errorAnalyzingFinished; // all passes done
    List<ProgressableTextEditorHighlightingPass> passes = Collections.emptyList();
    public int[] errorCount = ArrayUtilRt.EMPTY_INT_ARRAY;
    // Used in Rider
    public String reasonWhyDisabled;
    // Used in Rider
    public String reasonWhySuspended;

    private HeavyProcessLatch.Type heavyProcessType;

    public DaemonCodeAnalyzerStatus() {
    }

    @Override
    public String toString() {
      StringBuilder s = new StringBuilder("DS: finished=" + errorAnalyzingFinished);
      s.append("; pass statuses: ").append(passes.size()).append("; ");
      for (ProgressableTextEditorHighlightingPass passStatus : passes) {
        s.append(String.format("(%s %2.0f%% %b)", passStatus.getPresentableName(), passStatus.getProgress() * 100, passStatus.isFinished()));
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
    if (myProject.isDisposed()) {
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
      Map.Entry<String, HeavyProcessLatch.Type> processEntry = HeavyProcessLatch.INSTANCE.getRunningOperation();
      if (processEntry != null) {
        status.reasonWhySuspended = processEntry.getKey();
        status.heavyProcessType = processEntry.getValue();
      }
      else {
        status.reasonWhySuspended = "Heavy operation is running";
        status.heavyProcessType = HeavyProcessLatch.Type.Paused;
      }
      status.errorAnalyzingFinished = true;
      return status;
    }

    status.errorCount = errorCount.clone();

    status.passes = ContainerUtil.mapNotNull(myDaemonCodeAnalyzer.getPassesToShowProgressFor(myDocument), pass-> {
      if (!(pass instanceof ProgressableTextEditorHighlightingPass)) return null;
      ProgressableTextEditorHighlightingPass p = (ProgressableTextEditorHighlightingPass)pass;
      if (StringUtil.isEmpty(p.getPresentableName()) || p.getProgress() < 0) return null;
      return p;
    });

    status.errorAnalyzingFinished = myDaemonCodeAnalyzer.isAllAnalysisFinished(psiFile);
    status.reasonWhySuspended = myDaemonCodeAnalyzer.isUpdateByTimerEnabled() ? null : "Highlighting is paused temporarily";
    fillDaemonCodeAnalyzerErrorsStatus(status, severityRegistrar);

    return status;
  }

  protected void fillDaemonCodeAnalyzerErrorsStatus(@NotNull DaemonCodeAnalyzerStatus status,
                                                    @NotNull SeverityRegistrar severityRegistrar) {
  }

  protected final @NotNull Project getProject() {
    return myProject;
  }

  @Override
  public void paint(@NotNull Component c, Graphics g, @NotNull Rectangle r) {
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
    if (!status.passes.equals(new ArrayList<>(passes.keySet()))) {
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
      boolean isDumb = DumbService.isDumb(myProject);
      if (isDumb) {
        statusLabel = "Shallow analysis completed";
        statusExtraLine = "Complete results will be available after indexing";
      }
      else {
        statusLabel = "";
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
    for (ProgressableTextEditorHighlightingPass pass : status.passes) {
      JProgressBar progressBar = new JProgressBar(0, MAX);
      progressBar.setMaximum(MAX);
      UIUtil.applyStyle(UIUtil.ComponentStyle.MINI, progressBar);
      JLabel percentLabel = new JLabel();
      percentLabel.setText(TrafficProgressPanel.MAX_TEXT);
      passes.put(pass, Pair.create(progressBar, percentLabel));
    }
  }

  @Override
  @NotNull
  public AnalyzerStatus getStatus(@NotNull Editor editor) {
    if (PowerSaveMode.isEnabled()) {
      return new AnalyzerStatus(AllIcons.General.InspectionsPowerSaveMode,
                                  "Code analysis is disabled in power save mode", "", () -> createUIController(editor));
    }
    else {
      DaemonCodeAnalyzerStatus status = getDaemonCodeAnalyzerStatus(mySeverityRegistrar);
      List<StatusItem> statusItems = new ArrayList<>();
      Icon mainIcon = null;

      String title = "";
      String details = "";
      boolean isDumb = DumbService.isDumb(myProject);
      if (status.errorAnalyzingFinished) {
        if (isDumb) {
          title = DaemonBundle.message("shallow.analysis.completed");
          details = DaemonBundle.message("shallow.analysis.completed.details");
        }
      }
      else {
        title = DaemonBundle.message("performing.code.analysis");
      }

      int[] errorCount = status.errorCount;
      for (int i = errorCount.length - 1; i >= 0; i--) {
        int count = errorCount[i];
        if (count > 0) {
          HighlightSeverity severity = mySeverityRegistrar.getSeverityByIndex(i);
          String name = StringUtil.toLowerCase(severity.getName());
          if (count > 1) {
            name = StringUtil.pluralize(name);
          }

          Icon icon = mySeverityRegistrar.getRendererIconByIndex(i);
          statusItems.add(new StatusItem(Integer.toString(count), icon, name));

          if (mainIcon == null) {
            mainIcon = icon;
          }
        }
      }

      if (statusItems.size() > 0) {
        if (mainIcon == null) {
          mainIcon = AllIcons.General.InspectionsOK;
        }
        AnalyzerStatus result = new AnalyzerStatus(mainIcon, title, "", () -> createUIController(editor)).
          withNavigation().
          withExpandedStatus(statusItems);

        //noinspection ConstantConditions
        return status.errorAnalyzingFinished ? result :
               result.withAnalyzingType(AnalyzingType.PARTIAL).
               withPasses(ContainerUtil.map(status.passes, p -> new PassWrapper(p.getPresentableName(), p.getProgress(), p.isFinished())));
      }
      if (StringUtil.isNotEmpty(status.reasonWhyDisabled)) {
        return new AnalyzerStatus(AllIcons.General.InspectionsTrafficOff,
                                  DaemonBundle.message("no.analysis.performed"),
                                  status.reasonWhyDisabled, () -> createUIController(editor)).
          withExpandedStatus(new StatusItem(DaemonBundle.message("iw.status.off")));
      }
      if (StringUtil.isNotEmpty(status.reasonWhySuspended)) {
        return new AnalyzerStatus(AllIcons.General.InspectionsPause,
                                  DaemonBundle.message("analysis.suspended"),
                                  status.reasonWhySuspended, () -> createUIController(editor)).
          withExpandedStatus(new StatusItem(status.heavyProcessType != null ?
                                            status.heavyProcessType.toString() :
                                            DaemonBundle.message("iw.status.paused")));
      }
      if (status.errorAnalyzingFinished) {
        return isDumb ?
          new AnalyzerStatus(AllIcons.General.InspectionsPause, title, details, () -> createUIController(editor)).
            withExpandedStatus(new StatusItem(UtilBundle.message("heavyProcess.type.indexing"))) :
          new AnalyzerStatus(AllIcons.General.InspectionsOK, DaemonBundle.message("no.errors.or.warnings.found"),
                                  details, () -> createUIController(editor));
      }

      //noinspection ConstantConditions
      return new AnalyzerStatus(AllIcons.General.InspectionsEye, title,
                                details, () -> createUIController(editor)).
        withExpandedStatus(new StatusItem(DaemonBundle.message("iw.status.analyzing"))).
        withAnalyzingType(AnalyzingType.EMPTY).
        withPasses(ContainerUtil.map(status.passes, p -> new PassWrapper(p.getPresentableName(), p.getProgress(), p.isFinished())));
    }
  }

  @NotNull
  protected UIController createUIController(@NotNull Editor editor) {
    boolean mergeEditor = editor.getUserData(DiffUserDataKeys.MERGE_EDITOR_FLAG) == Boolean.TRUE;
    return editor.getEditorKind() == EditorKind.DIFF && !mergeEditor ? new SimplifiedUIController() : new DefaultUIController();
  }

  protected abstract class AbstractUIController implements UIController {
    private final boolean inLibrary;
    private final List<LanguageHighlightLevel> myLevelsList;
    private final List<HectorComponentPanel> myAdditionalPanels;

    protected AbstractUIController() {
      PsiFile psiFile = getPsiFile();
      if (psiFile != null) {
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
        VirtualFile virtualFile = psiFile.getVirtualFile();
        assert virtualFile != null;
        inLibrary = fileIndex.isInLibrary(virtualFile) && !fileIndex.isInContent(virtualFile);
        myAdditionalPanels = HectorComponentPanelsProvider.EP_NAME.extensions(getProject()).
          map(hp -> hp.createConfigurable(psiFile)).filter(p -> p != null).collect(Collectors.toList());
      }
      else {
        inLibrary = false;
        myAdditionalPanels = Collections.emptyList();
      }

      myLevelsList = initLevels();
    }

    private @NotNull List<LanguageHighlightLevel> initLevels() {
      List<LanguageHighlightLevel> result = new ArrayList<>();
      PsiFile psiFile = getPsiFile();
      if (psiFile != null && !getProject().isDisposed()) {
        FileViewProvider viewProvider = psiFile.getViewProvider();
        HighlightingLevelManager hlManager = HighlightingLevelManager.getInstance(getProject());
        for (Language language : viewProvider.getLanguages()) {
          PsiFile psiRoot = viewProvider.getPsi(language);
          result.add(new LanguageHighlightLevel(language.getID(), getHighlightLevel(hlManager.shouldHighlight(psiRoot), hlManager.shouldInspect(psiRoot))));
        }
      }
      return result;
    }

    @Override
    @NotNull
    public List<InspectionsLevel> getAvailableLevels() {
      return inLibrary ? Arrays.asList(InspectionsLevel.NONE, InspectionsLevel.ERRORS): Arrays.asList(InspectionsLevel.values());
    }

    @NotNull
    @Override
    public List<LanguageHighlightLevel> getHighlightLevels() {
      return Collections.unmodifiableList(myLevelsList);
    }

    @Override
    public void setHighLightLevel(@NotNull LanguageHighlightLevel level) {
      PsiFile psiFile = getPsiFile();
      if (psiFile != null && !getProject().isDisposed() && !myLevelsList.contains(level)) {
        FileViewProvider viewProvider = psiFile.getViewProvider();

        Language language = Language.findLanguageByID(level.getLangID());
        if (language != null) {
          PsiElement root = viewProvider.getPsi(language);
          if (level.getLevel() == InspectionsLevel.NONE) {
            HighlightLevelUtil.forceRootHighlighting(root, FileHighlightingSetting.SKIP_HIGHLIGHTING);
          }
          else if (level.getLevel() == InspectionsLevel.ERRORS) {
            HighlightLevelUtil.forceRootHighlighting(root, FileHighlightingSetting.SKIP_INSPECTION);
          }
          else {
            HighlightLevelUtil.forceRootHighlighting(root, FileHighlightingSetting.FORCE_HIGHLIGHTING);
          }

          myLevelsList.replaceAll(l -> l.getLangID().equals(level.getLangID()) ? level : l);

          InjectedLanguageManager.getInstance(getProject()).dropFileCaches(psiFile);
          myDaemonCodeAnalyzer.restart();
        }
      }
    }

    @Override
    public void fillHectorPanels(@NotNull Container container, @NotNull GridBag gc) {
      for (HectorComponentPanel p : myAdditionalPanels) {
        JComponent c;
        try {
          p.reset();
          c = p.createComponent();
        }
        catch (ProcessCanceledException e) {
          throw e;
        }
        catch (Throwable e) {
          Logger.getInstance(TrafficLightRenderer.class).error(e);
          continue;
        }

        if (c != null) {
          container.add(c, gc.nextLine().next().fillCellHorizontally().coverLine().weightx(1.0));
        }
      }
    }

    @Override
    public boolean canClosePopup() {
      if (myAdditionalPanels.isEmpty()) {
        return true;
      }
      if (myAdditionalPanels.stream().allMatch(p -> p.canClose())) {
        PsiFile psiFile = getPsiFile();
        if (myAdditionalPanels.stream().filter(p -> p.isModified()).peek(TrafficLightRenderer::applyPanel).count() > 0) {
          if (psiFile != null) {
            InjectedLanguageManager.getInstance(getProject()).dropFileCaches(psiFile);
          }
          myDaemonCodeAnalyzer.restart();
        }
        return true;
      }
      return false;
    }

    @Override
    public void onClosePopup() {
      myAdditionalPanels.forEach(p -> p.disposeUIResources());
    }

    @Override
    public void openProblemsView() {
      ProblemsViewToolWindowFactory.wakeup(getProject(), true);
    }
  }

  private static void applyPanel(@NotNull HectorComponentPanel panel) {
    try {
      panel.apply();
    }
    catch (ConfigurationException ignored) {}
  }

  @NotNull
  private static InspectionsLevel getHighlightLevel(boolean highlight, boolean inspect) {
    if (!highlight && !inspect) return InspectionsLevel.NONE;
    else if (highlight && !inspect) return InspectionsLevel.ERRORS;
    else return InspectionsLevel.ALL;
  }

  public class DefaultUIController extends AbstractUIController {
    private final List<AnAction> myMenuActions = initActions();

    private @NotNull List<AnAction> initActions() {
        List<AnAction> result = new ArrayList<>();
        result.add(new DumbAwareAction(EditorBundle.message("iw.configure.inspections")) {
          @Override
          public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(myDaemonCodeAnalyzer.isHighlightingAvailable(getPsiFile()));
          }

          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            if (!getProject().isDisposed()) {
              Configurable projectConfigurable = ConfigurableExtensionPointUtil.createProjectConfigurableForProvider(getProject(),
                                                                                                         ErrorsConfigurableProvider.class);
              if (projectConfigurable != null) {
                ShowSettingsUtil.getInstance().editConfigurable(getProject(), projectConfigurable);
              }
            }
          }
        });

        result.add(DaemonEditorPopup.createGotoGroup());

        result.add(Separator.create());
        result.add(new ToggleAction(EditorBundle.message("iw.show.import.tooltip")) {
          @Override
          public boolean isSelected(@NotNull AnActionEvent e) {
            PsiFile psiFile = getPsiFile();
            return psiFile != null && myDaemonCodeAnalyzer.isImportHintsEnabled(psiFile);
          }

          @Override
          public void setSelected(@NotNull AnActionEvent e, boolean state) {
            PsiFile psiFile = getPsiFile();
            if (psiFile != null) {
              myDaemonCodeAnalyzer.setImportHintsEnabled(psiFile, state);
            }
          }

          @Override
          public void update(@NotNull AnActionEvent e) {
            super.update(e);
            e.getPresentation().setEnabled(myDaemonCodeAnalyzer.isAutohintsAvailable(getPsiFile()));
          }

          @Override
          public boolean isDumbAware() {
            return true;
          }
        });

        return result;
    }

    @Override
    public @NotNull List<AnAction> getActions() {
      return myMenuActions;
    }

    @Override
    public boolean enableToolbar() {
      return true;
    }
  }

  public class SimplifiedUIController extends AbstractUIController {
    @Override
    public boolean enableToolbar() {
      return false;
    }

    @NotNull
    @Override
    public List<AnAction> getActions() {
      return Collections.emptyList();
    }
  }
}
