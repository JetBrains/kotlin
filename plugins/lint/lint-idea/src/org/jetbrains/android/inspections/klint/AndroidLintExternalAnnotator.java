package org.jetbrains.android.inspections.klint;

import com.android.tools.klint.client.api.IssueRegistry;
import com.android.tools.klint.client.api.LintDriver;
import com.android.tools.klint.client.api.LintRequest;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Scope;
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.daemon.DaemonBundle;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.CustomEditInspectionToolsSettingsAction;
import com.intellij.codeInspection.ex.DisableInspectionToolAction;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ui.UIUtil;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.android.compiler.AndroidCompileUtil;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.android.util.AndroidCommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinFileType;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static com.android.SdkConstants.*;
import static com.android.tools.klint.detector.api.TextFormat.HTML;
import static com.android.tools.klint.detector.api.TextFormat.RAW;

public class AndroidLintExternalAnnotator extends ExternalAnnotator<State, State> {
  static final boolean INCLUDE_IDEA_SUPPRESS_ACTIONS = false;

  @Nullable
  @Override
  public State collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
    return collectInformation(file);
  }

  @Override
  public State collectInformation(@NotNull PsiFile file) {
    final Module module = ModuleUtilCore.findModuleForPsiElement(file);
    if (module == null) {
      return null;
    }

    final AndroidFacet facet = AndroidFacet.getInstance(module);
    if (facet == null && !IntellijLintProject.hasAndroidModule(module.getProject())) {
      return null;
    }

    final VirtualFile vFile = file.getVirtualFile();
    if (vFile == null) {
      return null;
    }

    final FileType fileType = file.getFileType();

    if (fileType == FileTypes.PLAIN_TEXT) {
      if (!AndroidCommonUtils.PROGUARD_CFG_FILE_NAME.equals(file.getName()) &&
          !AndroidCompileUtil.OLD_PROGUARD_CFG_FILE_NAME.equals(file.getName())) {
        return null;
      }
    }
    else if (fileType != KotlinFileType.INSTANCE && fileType != StdFileTypes.PROPERTIES) {
      return null;
    }

    final List<Issue> issues = getIssuesFromInspections(file.getProject(), file);
    if (issues.size() == 0) {
      return null;
    }
    return new State(module, vFile, file.getText(), issues);
  }

  @Override
  public State doAnnotate(final State state) {
    final IntellijLintClient client = IntellijLintClient.forEditor(state);
    try {
      final LintDriver lint = new LintDriver(new IntellijLintIssueRegistry(), client);

      EnumSet<Scope> scope;
      VirtualFile mainFile = state.getMainFile();
      final FileType fileType = mainFile.getFileType();
      String name = mainFile.getName();
      if (fileType == StdFileTypes.XML) {
        if (name.equals(ANDROID_MANIFEST_XML)) {
          scope = Scope.MANIFEST_SCOPE;
        } else {
          scope = Scope.RESOURCE_FILE_SCOPE;
        }
      } else if (fileType == KotlinFileType.INSTANCE) {
        scope = Scope.JAVA_FILE_SCOPE;
      } else if (name.equals(OLD_PROGUARD_FILE) || name.equals(FN_PROJECT_PROGUARD_FILE)) {
        scope = EnumSet.of(Scope.PROGUARD_FILE);
      } else if (fileType == StdFileTypes.PROPERTIES) {
        scope = Scope.PROPERTY_SCOPE;
      } else {
        // #collectionInformation above should have prevented this
        assert false;
        return state;
      }

      Project project = state.getModule().getProject();
      if (project.isDisposed()) {
        return state;
      }

      List<VirtualFile> files = Collections.singletonList(mainFile);
      final LintRequest request = new IntellijLintRequest(
              client, project, files, Collections.singletonList(state.getModule()), true /* incremental */);
      request.setScope(scope);

      ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(new Runnable() {
        @Override
        public void run() {
          lint.analyze(request);
        }
      });
    }
    finally {
      Disposer.dispose(client);
    }
    return state;
  }

  @NotNull
  static List<Issue> getIssuesFromInspections(@NotNull Project project, @Nullable PsiElement context) {
    final List<Issue> result = new ArrayList<Issue>();
    final IssueRegistry fullRegistry = new IntellijLintIssueRegistry();

    for (Issue issue : fullRegistry.getIssues()) {
      final String inspectionShortName = AndroidLintInspectionBase.getInspectionShortNameByIssue(project, issue);
      if (inspectionShortName == null) {
        continue;
      }

      final HighlightDisplayKey key = HighlightDisplayKey.find(inspectionShortName);
      if (key == null) {
        continue;
      }

      final InspectionProfile profile = InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
      final boolean enabled = context != null ? profile.isToolEnabled(key, context) : profile.isToolEnabled(key);

      if (!enabled) {
        continue;
      } else if (!issue.isEnabledByDefault()) {
        // If an issue is marked as not enabled by default, lint won't run it, even if it's in the set
        // of issues provided by an issue registry. Since in the IDE we're enforcing the enabled-state via
        // inspection profiles, mark the issue as enabled to allow users to turn on a lint check directly
        // via the inspections UI.
        issue.setEnabledByDefault(true);
      }
      result.add(issue);
    }
    return result;
  }

  @Override
  public void apply(@NotNull PsiFile file, State state, @NotNull AnnotationHolder holder) {
    if (state.isDirty()) {
      return;
    }
    final Project project = file.getProject();

    for (ProblemData problemData : state.getProblems()) {
      final Issue issue = problemData.getIssue();
      final String message = problemData.getMessage();
      final TextRange range = problemData.getTextRange();

      if (range.getStartOffset() == range.getEndOffset()) {
        continue;
      }

      final Pair<AndroidLintInspectionBase, HighlightDisplayLevel> pair =
        AndroidLintUtil.getHighlighLevelAndInspection(project, issue, file);
      if (pair == null) {
        continue;
      }
      final AndroidLintInspectionBase inspection = pair.getFirst();
      HighlightDisplayLevel displayLevel = pair.getSecond();

      if (inspection != null) {
        final HighlightDisplayKey key = HighlightDisplayKey.find(inspection.getShortName());

        if (key != null) {
          final PsiElement startElement = file.findElementAt(range.getStartOffset());
          final PsiElement endElement = file.findElementAt(range.getEndOffset() - 1);

          if (startElement != null && endElement != null && !inspection.isSuppressedFor(startElement)) {
            if (problemData.getConfiguredSeverity() != null) {
              HighlightDisplayLevel configuredLevel =
                AndroidLintInspectionBase.toHighlightDisplayLevel(problemData.getConfiguredSeverity());
              if (configuredLevel != null) {
                displayLevel = configuredLevel;
              }
            }
            final Annotation annotation = createAnnotation(holder, message, range, displayLevel, issue);

            for (AndroidLintQuickFix fix : inspection.getQuickFixes(startElement, endElement, message)) {
              if (fix.isApplicable(startElement, endElement, AndroidQuickfixContexts.EditorContext.TYPE)) {
                annotation.registerFix(new MyFixingIntention(fix, startElement, endElement));
              }
            }

            for (IntentionAction intention : inspection.getIntentions(startElement, endElement)) {
              annotation.registerFix(intention);
            }

            String id = key.getID();
            if (IntellijLintIssueRegistry.CUSTOM_ERROR == issue
                || IntellijLintIssueRegistry.CUSTOM_WARNING == issue) {
              Issue original = IntellijLintClient.findCustomIssue(message);
              if (original != null) {
                id = original.getId();
              }
            }

            annotation.registerFix(new SuppressLintIntentionAction(id, startElement));
            annotation.registerFix(new MyDisableInspectionFix(key));
            annotation.registerFix(new MyEditInspectionToolsSettingsAction(key, inspection));

            if (INCLUDE_IDEA_SUPPRESS_ACTIONS) {
              final SuppressQuickFix[] suppressActions = inspection.getBatchSuppressActions(startElement);
              for (SuppressQuickFix action : suppressActions) {
                if (action.isAvailable(project, startElement)) {
                  ProblemHighlightType type = annotation.getHighlightType();
                  annotation.registerFix(action, null, key, InspectionManager.getInstance(project).createProblemDescriptor(
                    startElement, endElement, message, type, true, LocalQuickFix.EMPTY_ARRAY));
                }
              }
            }
          }
        }
      }
    }
  }

  @SuppressWarnings("deprecation")
  @NotNull
  private Annotation createAnnotation(@NotNull AnnotationHolder holder,
                                      @NotNull String message,
                                      @NotNull TextRange range,
                                      @NotNull HighlightDisplayLevel displayLevel,
                                      @NotNull Issue issue) {
    // Convert from inspection severity to annotation severity
    HighlightSeverity severity;
    if (displayLevel == HighlightDisplayLevel.ERROR) {
      severity = HighlightSeverity.ERROR;
    } else if (displayLevel == HighlightDisplayLevel.WARNING) {
      severity = HighlightSeverity.WARNING;
    } else if (displayLevel == HighlightDisplayLevel.WEAK_WARNING) {
      severity = HighlightSeverity.WEAK_WARNING;
    } else if (displayLevel == HighlightDisplayLevel.INFO) {
      severity = HighlightSeverity.INFO;
    } else {
      severity = HighlightSeverity.WARNING;
    }

    String link = " <a "
        +"href=\"#lint/" + issue.getId() + "\""
        + (UIUtil.isUnderDarcula() ? " color=\"7AB4C9\" " : "")
        +">" + DaemonBundle.message("inspection.extended.description")
        +"</a> " + getShowMoreShortCut();
    String tooltip = XmlStringUtil.wrapInHtml(RAW.convertTo(message, HTML) + link);

    return holder.createAnnotation(severity, range, message, tooltip);
  }

  // Based on similar code in the LocalInspectionsPass constructor
  private String myShortcutText;
  private String getShowMoreShortCut() {
    if (myShortcutText == null) {
      final KeymapManager keymapManager = KeymapManager.getInstance();
      if (keymapManager != null) {
        final Keymap keymap = keymapManager.getActiveKeymap();
        myShortcutText =
          keymap == null ? "" : "(" + KeymapUtil.getShortcutsText(keymap.getShortcuts(IdeActions.ACTION_SHOW_ERROR_DESCRIPTION)) + ")";
      }
      else {
        myShortcutText = "";
      }
    }

    return myShortcutText;
  }

  private static class MyDisableInspectionFix implements IntentionAction, Iconable {
    private final DisableInspectionToolAction myDisableInspectionToolAction;

    private MyDisableInspectionFix(@NotNull HighlightDisplayKey key) {
      myDisableInspectionToolAction = new DisableInspectionToolAction(key);
    }

    @NotNull
    @Override
    public String getText() {
      return "Disable inspection";
    }

    @NotNull
    @Override
    public String getFamilyName() {
      return getText();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
      return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
      myDisableInspectionToolAction.invoke(project, editor, file);
    }

    @Override
    public boolean startInWriteAction() {
      return myDisableInspectionToolAction.startInWriteAction();
    }

    @Override
    public Icon getIcon(@IconFlags int flags) {
      return myDisableInspectionToolAction.getIcon(flags);
    }
  }

  public static class MyFixingIntention implements IntentionAction, HighPriorityAction {
    private final AndroidLintQuickFix myQuickFix;
    private final PsiElement myStartElement;
    private final PsiElement myEndElement;

    public MyFixingIntention(@NotNull AndroidLintQuickFix quickFix, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
      myQuickFix = quickFix;
      myStartElement = startElement;
      myEndElement = endElement;
    }

    @NotNull
    @Override
    public String getText() {
      return myQuickFix.getName();
    }

    @NotNull
    @Override
    public String getFamilyName() {
      return AndroidBundle.message("android.lint.quickfixes.family");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
      return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
      FileModificationService.getInstance().prepareFileForWrite(file);
      myQuickFix.apply(myStartElement, myEndElement, AndroidQuickfixContexts.EditorContext.getInstance(editor));
    }

    @Override
    public boolean startInWriteAction() {
      return true;
    }
  }

  private static class MyEditInspectionToolsSettingsAction extends CustomEditInspectionToolsSettingsAction {
    private MyEditInspectionToolsSettingsAction(@NotNull HighlightDisplayKey key, @NotNull final AndroidLintInspectionBase inspection) {
      super(key, new Computable<String>() {
        @Override
        public String compute() {
          return "Edit '" + inspection.getDisplayName() + "' inspection settings";
        }
      });
    }
  }
}
