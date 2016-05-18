package org.jetbrains.android.inspections.klint;

import com.android.annotations.concurrency.GuardedBy;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.intellij.analysis.AnalysisScope;
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.lang.annotation.ProblemGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.HashMap;
import org.jetbrains.android.util.AndroidBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.util.*;

import static com.android.tools.klint.detector.api.TextFormat.HTML;
import static com.android.tools.klint.detector.api.TextFormat.RAW;
import static com.intellij.xml.CommonXmlStrings.HTML_END;
import static com.intellij.xml.CommonXmlStrings.HTML_START;

public abstract class AndroidLintInspectionBase extends GlobalInspectionTool {
  private static final Logger LOG = Logger.getInstance("#org.jetbrains.android.inspections.klint.AndroidLintInspectionBase");

  private static final Object ISSUE_MAP_LOCK = new Object();

  @GuardedBy("ISSUE_MAP_LOCK")
  private static volatile Map<Issue, String> ourIssue2InspectionShortName;

  protected final Issue myIssue;
  private final String[] myGroupPath;
  private final String myDisplayName;

  protected AndroidLintInspectionBase(@NotNull String displayName, @NotNull Issue issue) {
    myIssue = issue;

    final Category category = issue.getCategory();
    final String[] categoryNames = category != null
                                   ? computeAllNames(category)
                                   : ArrayUtil.EMPTY_STRING_ARRAY;

    myGroupPath = ArrayUtil.mergeArrays(new String[]{AndroidBundle.message("android.inspections.group.name"),
      AndroidBundle.message("android.lint.inspections.subgroup.name")}, categoryNames);
    myDisplayName = displayName;
  }

  @NotNull
  public AndroidLintQuickFix[] getQuickFixes(@NotNull PsiElement startElement, @NotNull PsiElement endElement, @NotNull String message) {
    return getQuickFixes(message);
  }

  @NotNull
  public AndroidLintQuickFix[] getQuickFixes(@NotNull String message) {
    return AndroidLintQuickFix.EMPTY_ARRAY;
  }

  @NotNull
  public IntentionAction[] getIntentions(@NotNull PsiElement startElement, @NotNull PsiElement endElement) {
    return IntentionAction.EMPTY_ARRAY;
  }

  @Override
  public boolean isGraphNeeded() {
    return false;
  }

  @NotNull
  private LocalQuickFix[] getLocalQuickFixes(@NotNull PsiElement startElement, @NotNull PsiElement endElement, @NotNull String message) {
    final AndroidLintQuickFix[] fixes = getQuickFixes(startElement, endElement, message);
    final LocalQuickFix[] result = new LocalQuickFix[fixes.length];

    for (int i = 0; i < fixes.length; i++) {
      if (fixes[i].isApplicable(startElement, endElement, AndroidQuickfixContexts.BatchContext.TYPE)) {
        result[i] = new MyLocalQuickFix(fixes[i]);
      }
    }
    return result;
  }

  @Override
  public void runInspection(@NotNull AnalysisScope scope,
                            @NotNull final InspectionManager manager,
                            @NotNull final GlobalInspectionContext globalContext,
                            @NotNull final ProblemDescriptionsProcessor problemDescriptionsProcessor) {
    final AndroidLintGlobalInspectionContext androidLintContext = globalContext.getExtension(AndroidLintGlobalInspectionContext.ID);
    if (androidLintContext == null) {
      return;
    }

    final Map<Issue, Map<File, List<ProblemData>>> problemMap = androidLintContext.getResults();
    if (problemMap == null) {
      return;
    }

    final Map<File, List<ProblemData>> file2ProblemList = problemMap.get(myIssue);
    if (file2ProblemList == null) {
      return;
    }

    for (final Map.Entry<File, List<ProblemData>> entry : file2ProblemList.entrySet()) {
      final File file = entry.getKey();
      final VirtualFile vFile = LocalFileSystem.getInstance().findFileByIoFile(file);

      if (vFile == null) {
        continue;
      }
      ApplicationManager.getApplication().runReadAction(new Runnable() {
        @Override
        public void run() {
          final PsiManager psiManager = PsiManager.getInstance(globalContext.getProject());
          final PsiFile psiFile = psiManager.findFile(vFile);

          if (psiFile != null) {
            final ProblemDescriptor[] descriptors = computeProblemDescriptors(psiFile, manager, entry.getValue());

            if (descriptors.length > 0) {
              problemDescriptionsProcessor.addProblemElement(globalContext.getRefManager().getReference(psiFile), descriptors);
            }
          } else if (vFile.isDirectory()) {
            final PsiDirectory psiDirectory = psiManager.findDirectory(vFile);

            if (psiDirectory != null) {
              final ProblemDescriptor[] descriptors = computeProblemDescriptors(psiDirectory, manager, entry.getValue());

              if (descriptors.length > 0) {
                problemDescriptionsProcessor.addProblemElement(globalContext.getRefManager().getReference(psiDirectory), descriptors);
              }
            }
          }
        }
      });
    }
  }

  @NotNull
  private ProblemDescriptor[] computeProblemDescriptors(@NotNull PsiElement psiFile,
                                                        @NotNull InspectionManager manager,
                                                        @NotNull List<ProblemData> problems) {
    ProgressManager.checkCanceled();

    final List<ProblemDescriptor> result = new ArrayList<ProblemDescriptor>();

    for (ProblemData problemData : problems) {
      final String originalMessage = problemData.getMessage();

      // We need to have explicit <html> and </html> tags around the text; inspection infrastructure
      // such as the {@link com.intellij.codeInspection.ex.DescriptorComposer} will call
      // {@link com.intellij.xml.util.XmlStringUtil.isWrappedInHtml}. See issue 177283 for uses.
      final String formattedMessage = HTML_START + RAW.convertTo(originalMessage, HTML) + HTML_END;
      final TextRange range = problemData.getTextRange();

      if (range.getStartOffset() == range.getEndOffset()) {

        if (psiFile instanceof PsiBinaryFile || psiFile instanceof PsiDirectory) {
          final LocalQuickFix[] fixes = getLocalQuickFixes(psiFile, psiFile, originalMessage);
          result.add(new NonTextFileProblemDescriptor((PsiFileSystemItem)psiFile, formattedMessage, fixes));
        } else if (!isSuppressedFor(psiFile)) {
          result.add(manager.createProblemDescriptor(psiFile, formattedMessage, false,
                                                     getLocalQuickFixes(psiFile, psiFile, originalMessage),
                                                     ProblemHighlightType.GENERIC_ERROR_OR_WARNING));
        }
      }
      else {
        final PsiElement startElement = psiFile.findElementAt(range.getStartOffset());
        final PsiElement endElement = psiFile.findElementAt(range.getEndOffset() - 1);

        if (startElement != null && endElement != null && !isSuppressedFor(startElement)) {
          result.add(manager.createProblemDescriptor(startElement, endElement, formattedMessage,
                                                     ProblemHighlightType.GENERIC_ERROR_OR_WARNING, false,
                                                     getLocalQuickFixes(startElement, endElement, originalMessage)));
        }
      }
    }
    return result.toArray(new ProblemDescriptor[result.size()]);
  }

  @NotNull
  @Override
  public SuppressQuickFix[] getBatchSuppressActions(@Nullable PsiElement element) {
    SuppressLintQuickFix suppressLintQuickFix = new SuppressLintQuickFix(myIssue);
    if (AndroidLintExternalAnnotator.INCLUDE_IDEA_SUPPRESS_ACTIONS) {
      final List<SuppressQuickFix> result = new ArrayList<SuppressQuickFix>();
      result.add(suppressLintQuickFix);
      result.addAll(Arrays.asList(BatchSuppressManager.SERVICE.getInstance().createBatchSuppressActions(HighlightDisplayKey.find(getShortName()))));
      result.addAll(Arrays.asList(new XmlSuppressableInspectionTool.SuppressTagStatic(getShortName()),
                                  new XmlSuppressableInspectionTool.SuppressForFile(getShortName())));
      return result.toArray(new SuppressQuickFix[result.size()]);
    } else {
      return new SuppressQuickFix[] { suppressLintQuickFix };
    }
  }

  private static class SuppressLintQuickFix implements SuppressQuickFix {
    private Issue myIssue;

    private SuppressLintQuickFix(Issue issue) {
      myIssue = issue;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, @NotNull PsiElement context) {
      return true;
    }

    @NotNull
    @Override
    public String getName() {
      return "Suppress with @SuppressLint (Java) or tools:ignore (XML)";
    }

    @NotNull
    @Override
    public String getFamilyName() {
      return "Suppress";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {}
  }

  @TestOnly
  public static void invalidateInspectionShortName2IssueMap() {
    ourIssue2InspectionShortName = null;
  }

  public static String getInspectionShortNameByIssue(@NotNull Project project, @NotNull Issue issue) {
    synchronized (ISSUE_MAP_LOCK) {
      if (ourIssue2InspectionShortName == null) {
        ourIssue2InspectionShortName = new HashMap<Issue, String>();

        final InspectionProfile profile = InspectionProjectProfileManager.getInstance(project).getInspectionProfile();

        for (InspectionToolWrapper e : profile.getInspectionTools(null)) {
          final String shortName = e.getShortName();

          if (shortName.startsWith("AndroidKLint")) {
            final InspectionProfileEntry entry = e.getTool();
            if (entry instanceof AndroidLintInspectionBase) {
              final Issue s = ((AndroidLintInspectionBase)entry).getIssue();
              ourIssue2InspectionShortName.put(s, shortName);
            }
          }
        }
      }
      return ourIssue2InspectionShortName.get(issue);
    }
  }

  @NotNull
  private static String[] computeAllNames(@NotNull Category category) {
    final List<String> result = new ArrayList<String>();

    Category c = category;

    while (c != null) {
      final String name = c.getName();

      if (name == null) {
        return ArrayUtil.EMPTY_STRING_ARRAY;
      }
      result.add(name);
      c = c.getParent();
    }
    return ArrayUtil.reverseArray(ArrayUtil.toStringArray(result));
  }

  @Nls
  @NotNull
  @Override
  public String getGroupDisplayName() {
    return AndroidBundle.message("android.lint.inspections.group.name");
  }

  @NotNull
  @Override
  public String[] getGroupPath() {
    return myGroupPath;
  }

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return myDisplayName;
  }

  @SuppressWarnings("deprecation")
  @Override
  public String getStaticDescription() {
    StringBuilder sb = new StringBuilder(1000);
    sb.append("<html><body>");
    sb.append(myIssue.getBriefDescription(HTML));
    sb.append("<br><br>");
    sb.append(myIssue.getExplanation(HTML));
    List<String> urls = myIssue.getMoreInfo();
    if (!urls.isEmpty()) {
      boolean separated = false;
      for (String url : urls) {
        if (!myIssue.getExplanation(RAW).contains(url)) {
          if (!separated) {
            sb.append("<br><br>");
            separated = true;
          } else {
            sb.append("<br>");
          }
          sb.append("<a href=\"");
          sb.append(url);
          sb.append("\">");
          sb.append(url);
          sb.append("</a>");
        }
      }
    }
    sb.append("</body></html>");

    return sb.toString();
  }

  @Override
  public boolean isEnabledByDefault() {
    return myIssue.isEnabledByDefault();
  }

  @NotNull
  @Override
  public String getShortName() {
    return InspectionProfileEntry.getShortName(getClass().getSimpleName());
  }

  @NotNull
  @Override
  public HighlightDisplayLevel getDefaultLevel() {
    final Severity defaultSeverity = myIssue.getDefaultSeverity();
    if (defaultSeverity == null) {
      return HighlightDisplayLevel.WARNING;
    }
    final HighlightDisplayLevel displayLevel = toHighlightDisplayLevel(defaultSeverity);
    return displayLevel != null ? displayLevel : HighlightDisplayLevel.WARNING;
  }

  @Nullable
  static HighlightDisplayLevel toHighlightDisplayLevel(@NotNull Severity severity) {
    switch (severity) {
      case ERROR:
        return HighlightDisplayLevel.ERROR;
      case FATAL:
        return HighlightDisplayLevel.ERROR;
      case WARNING:
        return HighlightDisplayLevel.WARNING;
      case INFORMATIONAL:
        return HighlightDisplayLevel.WEAK_WARNING;
      case IGNORE:
        return null;
      default:
        LOG.error("Unknown severity " + severity);
        return null;
    }
  }

  /** Returns true if the given analysis scope is adequate for single-file analysis */
  private static boolean isSingleFileScope(EnumSet<Scope> scopes) {
    if (scopes.size() != 1) {
      return false;
    }
    final Scope scope = scopes.iterator().next();
    return scope == Scope.SOURCE_FILE || scope == Scope.RESOURCE_FILE || scope == Scope.MANIFEST
           || scope == Scope.PROGUARD_FILE || scope == Scope.OTHER;
  }

  @Override
  public boolean worksInBatchModeOnly() {
    Implementation implementation = myIssue.getImplementation();
    if (isSingleFileScope(implementation.getScope())) {
      return false;
    }
    for (EnumSet<Scope> scopes : implementation.getAnalysisScopes()) {
      if (isSingleFileScope(scopes)) {
        return false;
      }
    }

    return true;
  }

  @NotNull
  public Issue getIssue() {
    return myIssue;
  }

  static class MyLocalQuickFix implements LocalQuickFix {
    private final AndroidLintQuickFix myLintQuickFix;

    MyLocalQuickFix(@NotNull AndroidLintQuickFix lintQuickFix) {
      myLintQuickFix = lintQuickFix;
    }

    @NotNull
    @Override
    public String getName() {
      return myLintQuickFix.getName();
    }

    @NotNull
    @Override
    public String getFamilyName() {
      return AndroidBundle.message("android.lint.quickfixes.family");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      myLintQuickFix.apply(descriptor.getStartElement(), descriptor.getEndElement(), AndroidQuickfixContexts.BatchContext.getInstance());
    }
  }

  /**
   * A {@link com.intellij.codeInspection.ProblemDescriptor} for image and directory files. This is
   * necessary because the {@link InspectionManager}'s createProblemDescriptor methods
   * all use {@link com.intellij.codeInspection.ProblemDescriptorBase} where in the constructor
   * it insists that the start and end {@link PsiElement} instances must have a valid
   * <b>text</b> range, which does not apply for images.
   * <p>
   * This custom descriptor allows the batch lint analysis to correctly handle lint errors
   * associated with image files (such as the various {@link com.android.tools.lint.checks.IconDetector}
   * warnings), as well as directory errors (such as incorrect locale folders),
   * and clicking on them will navigate to the correct icon.
   */
  private static class NonTextFileProblemDescriptor implements ProblemDescriptor {
    private final PsiFileSystemItem myFile;
    private final String myMessage;
    private final LocalQuickFix[] myFixes;
    private ProblemGroup myGroup;

    public NonTextFileProblemDescriptor(@NotNull PsiFileSystemItem file, @NotNull String message, @NotNull LocalQuickFix[] fixes) {
      myFile = file;
      myMessage = message;
      myFixes = fixes;
    }

    @Override
    public PsiElement getPsiElement() {
      return myFile;
    }

    @Override
    public PsiElement getStartElement() {
      return myFile;
    }

    @Override
    public PsiElement getEndElement() {
      return myFile;
    }

    @Override
    public TextRange getTextRangeInElement() {
      return new TextRange(0, 0);
    }

    @Override
    public int getLineNumber() {
      return 0;
    }

    @NotNull
    @Override
    public ProblemHighlightType getHighlightType() {
      return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
    }

    @Override
    public boolean isAfterEndOfLine() {
      return false;
    }

    @Override
    public void setTextAttributes(TextAttributesKey key) {
    }

    @Nullable
    @Override
    public ProblemGroup getProblemGroup() {
      return myGroup;
    }

    @Override
    public void setProblemGroup(@Nullable ProblemGroup problemGroup) {
      myGroup = problemGroup;
    }

    @Override
    public boolean showTooltip() {
      return false;
    }

    @NotNull
    @Override
    public String getDescriptionTemplate() {
      return myMessage;
    }

    @Nullable
    @Override
    public QuickFix[] getFixes() {
      return myFixes;
    }
  }
}
