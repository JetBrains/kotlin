package org.jetbrains.android.inspections.klint;

import com.android.annotations.NonNull;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.LintOptions;
import com.android.ide.common.repository.ResourceVisibilityLookup;
import com.android.ide.common.res2.AbstractResourceRepository;
import com.android.ide.common.res2.ResourceFile;
import com.android.ide.common.res2.ResourceItem;
import com.android.tools.idea.res.AppResourceRepository;
import com.android.tools.idea.res.LocalResourceRepository;
import com.android.tools.idea.sdk.IdeSdks;
import com.android.tools.klint.client.api.*;
import com.android.tools.klint.checks.ApiLookup;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.DefaultPosition;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Position;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.TextFormat;
import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.HashMap;
import com.intellij.util.net.HttpConfigurable;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.facet.AndroidRootUtil;
import org.jetbrains.android.sdk.AndroidSdkType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UastLanguagePlugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.android.tools.klint.detector.api.TextFormat.RAW;

/**
 * Implementation of the {@linkplain LintClient} API for executing lint within the IDE:
 * reading files, reporting issues, logging errors, etc.
 */
public class IntellijLintClient extends LintClient implements Disposable {
  protected static final Logger LOG = Logger.getInstance("#org.jetbrains.android.inspections.IntellijLintClient");

  @NonNull protected Project myProject;
  @Nullable protected Map<com.android.tools.klint.detector.api.Project, Module> myModuleMap;

  @NonNull
  private List<UastLanguagePlugin> myPlugins;

  protected IntellijLintClient(@NonNull Project project) {
    myProject = project;
    myPlugins = LintLanguageExtension.getPlugins(project);
  }

  /** Creates a lint client for batch inspections */
  public static IntellijLintClient forBatch(@NotNull Project project,
                                            @NotNull Map<Issue, Map<File, List<ProblemData>>> problemMap,
                                            @NotNull AnalysisScope scope,
                                            @NotNull List<Issue> issues) {
    return new BatchLintClient(project, problemMap, scope, issues);
  }

  /**
   * Returns an {@link ApiLookup} service.
   *
   * @param project the project to use for locating the Android SDK
   * @return an API lookup if one can be found
   */
  @Nullable
  public static ApiLookup getApiLookup(@NotNull Project project) {
    return ApiLookup.get(new IntellijLintClient(project));
  }

  /**
   * Creates a lint client used for in-editor single file lint analysis (e.g. background checking while user is editing.)
   */
  public static IntellijLintClient forEditor(@NotNull State state) {
    return new EditorLintClient(state);
  }

  @Nullable
  protected Module findModuleForLintProject(@NotNull Project project,
                                            @NotNull com.android.tools.klint.detector.api.Project lintProject) {
    if (myModuleMap != null) {
      Module module = myModuleMap.get(lintProject);
      if (module != null) {
        return module;
      }
    }
    final File dir = lintProject.getDir();
    final VirtualFile vDir = LocalFileSystem.getInstance().findFileByIoFile(dir);
    return vDir != null ? ModuleUtilCore.findModuleForFile(vDir, project) : null;
  }

  void setModuleMap(@Nullable Map<com.android.tools.klint.detector.api.Project, Module> moduleMap) {
    myModuleMap = moduleMap;
  }

  @Override
  public Configuration getConfiguration(@NonNull com.android.tools.klint.detector.api.Project project) {
    if (project.isGradleProject() && project.isAndroidProject() && !project.isLibrary()) {
      AndroidProject model = project.getGradleProjectModel();
      if (model != null) {
        try {
          LintOptions lintOptions = model.getLintOptions();
          final Map<String, Integer> overrides = lintOptions.getSeverityOverrides();
          if (overrides != null && !overrides.isEmpty()) {
            return new DefaultConfiguration(this, project, null) {
              @NonNull
              @Override
              public Severity getSeverity(@NonNull Issue issue) {
                Integer severity = overrides.get(issue.getId());
                if (severity != null) {
                  switch (severity.intValue()) {
                    case LintOptions.SEVERITY_FATAL:
                      return Severity.FATAL;
                    case LintOptions.SEVERITY_ERROR:
                      return Severity.ERROR;
                    case LintOptions.SEVERITY_WARNING:
                      return Severity.WARNING;
                    case LintOptions.SEVERITY_INFORMATIONAL:
                      return Severity.INFORMATIONAL;
                    case LintOptions.SEVERITY_IGNORE:
                    default:
                      return Severity.IGNORE;
                  }
                }

                // This is a LIST lookup. I should make this faster!
                if (!getIssues().contains(issue)) {
                  return Severity.IGNORE;
                }

                return super.getSeverity(issue);
              }
            };
          }
        } catch (Exception e) {
          LOG.error(e);
        }
      }
    }
    return new DefaultConfiguration(this, project, null) {
      @Override
      public boolean isEnabled(@NonNull Issue issue) {
        return getIssues().contains(issue) && super.isEnabled(issue);
      }
    };
  }

  @Override
  public void report(@NonNull Context context,
                     @NonNull Issue issue,
                     @NonNull Severity severity,
                     @Nullable Location location,
                     @NonNull String message,
                     @NonNull TextFormat format) {
    assert false : message;
  }

  @NonNull protected List<Issue> getIssues() {
    return Collections.emptyList();
  }

  @Nullable
  protected Module getModule() {
    return null;
  }

  /**
   * Recursively calls {@link #report} on the secondary location of this error, if any, which in turn may call it on a third
   * linked location, and so on.This is necessary since IntelliJ problems don't have secondary locations; instead, we create one
   * problem for each location associated with the lint error.
   */
  protected void reportSecondary(@NonNull Context context, @NonNull Issue issue, @NonNull Severity severity, @NonNull Location location,
                                 @NonNull String message, @NonNull TextFormat format) {
    Location secondary = location.getSecondary();
    if (secondary != null) {
      if (secondary.getMessage() != null) {
        message = message + " (" + secondary.getMessage() + ")";
      }
      report(context, issue, severity, secondary, message, format);
    }
  }

  @Override
  public void log(@NonNull Severity severity, @Nullable Throwable exception, @Nullable String format, @Nullable Object... args) {
    if (severity == Severity.ERROR || severity == Severity.FATAL) {
      if (format != null) {
        LOG.error(String.format(format, args), exception);
      } else if (exception != null) {
        LOG.error(exception);
      }
    } else if (severity == Severity.WARNING) {
      if (format != null) {
        LOG.warn(String.format(format, args), exception);
      } else if (exception != null) {
        LOG.warn(exception);
      }
    } else {
      if (format != null) {
        LOG.info(String.format(format, args), exception);
      } else if (exception != null) {
        LOG.info(exception);
      }
    }
  }

  @Override
  public XmlParser getXmlParser() {
    return new DomPsiParser(this);
  }

  @NonNull
  @Override
  public List<File> getJavaClassFolders(@NonNull com.android.tools.klint.detector.api.Project project) {
    // todo: implement when class files checking detectors will be available
    return Collections.emptyList();
  }

  @NonNull
  @Override
  public List<File> getJavaLibraries(@NonNull com.android.tools.klint.detector.api.Project project) {
    // todo: implement
    return Collections.emptyList();
  }

  @Override
  @NonNull
  public String readFile(@NonNull final File file) {
    final VirtualFile vFile = LocalFileSystem.getInstance().findFileByIoFile(file);
    if (vFile == null) {
      LOG.debug("Cannot find file " + file.getPath() + " in the VFS");
      return "";
    }

    return ApplicationManager.getApplication().runReadAction(new Computable<String>() {
      @Nullable
      @Override
      public String compute() {
        final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(vFile);
        if (psiFile == null) {
          LOG.info("Cannot find file " + file.getPath() + " in the PSI");
          return null;
        }
        else {
          return psiFile.getText();
        }
      }
    });
  }

  @Override
  public void dispose() {
  }

  @Nullable
  @Override
  public File getSdkHome() {
    Module module = getModule();
    if (module != null) {
      Sdk moduleSdk = ModuleRootManager.getInstance(module).getSdk();
      if (moduleSdk != null) {
        String path = moduleSdk.getHomePath();
        if (path != null) {
          File home = new File(path);
          if (home.exists()) {
            return home;
          }
        }
      }
    }

    File sdkHome = super.getSdkHome();
    if (sdkHome != null) {
      return sdkHome;
    }

    for (Module m : ModuleManager.getInstance(myProject).getModules()) {
      Sdk moduleSdk = ModuleRootManager.getInstance(m).getSdk();
      if (moduleSdk != null) {
        if (moduleSdk.getSdkType() instanceof AndroidSdkType) {
          String path = moduleSdk.getHomePath();
          if (path != null) {
            File home = new File(path);
            if (home.exists()) {
              return home;
            }
          }
        }
      }
    }

    return IdeSdks.getAndroidSdkPath();
  }

  @Nullable
  @Override
  public SdkWrapper getSdk() {
    if (mSdk == null) {
      Module module = getModule();
      SdkWrapper sdk = getLocalSdk(module);
      if (sdk != null) {
        mSdk = sdk;
      } else {
        for (Module m : ModuleManager.getInstance(myProject).getModules()) {
          sdk = getLocalSdk(m);
          if (sdk != null) {
            mSdk = sdk;
            break;
          }
        }

        if (mSdk == null) {
          mSdk = super.getSdk();
        }
      }
    }

    return mSdk;
  }

  @Nullable
  private static SdkWrapper getLocalSdk(@Nullable Module module) {
    if (module != null) {
      AndroidFacet facet = AndroidFacet.getInstance(module);
      if (facet != null) {
        return IntellijLintUtils.getModelFacade(facet).getLocalSdk();
      }
    }

    return null;
  }

  @Override
  public boolean isGradleProject(com.android.tools.klint.detector.api.Project project) {
    Module module = getModule();
    if (module != null) {
      AndroidFacet facet = AndroidFacet.getInstance(module);
      return facet != null && IntellijLintUtils.isGradleModule(facet);
    }
    return false;
  }

  @Override
  public Project getProject() {
    return myProject;
  }

  @Override
  public List<UastLanguagePlugin> getLanguagePlugins() {
    return myPlugins;
  }

  // Overridden such that lint doesn't complain about missing a bin dir property in the event
  // that no SDK is configured
  @Override
  @Nullable
  public File findResource(@NonNull String relativePath) {
    File top = getSdkHome();
    if (top != null) {
      File file = new File(top, relativePath);
      if (file.exists()) {
        return file;
      }
    }

    return null;
  }

  @Nullable private static volatile String ourSystemPath;

  @Override
  @Nullable
  public File getCacheDir(boolean create) {
    final String path = ourSystemPath != null ? ourSystemPath : (ourSystemPath = PathUtil.getCanonicalPath(PathManager.getSystemPath()));
    File lint = new File(path, "lint");
    if (create && !lint.exists()) {
      lint.mkdirs();
    }
    return lint;
  }

  @Override
  public boolean isProjectDirectory(@NonNull File dir) {
    return new File(dir, Project.DIRECTORY_STORE_FOLDER).exists();
  }

  /**
   * A lint client used for in-editor single file lint analysis (e.g. background checking while user is editing.)
   * <p>
   * Since this applies only to a given file and module, it can take some shortcuts over what the general
   * {@link BatchLintClient} has to do.
   * */
  private static class EditorLintClient extends IntellijLintClient {
    private final State myState;

    public EditorLintClient(@NotNull State state) {
      super(state.getModule().getProject());
      myState = state;
    }

    @Nullable
    @Override
    protected Module getModule() {
      return myState.getModule();
    }

    @NonNull
    @Override
    protected List<Issue> getIssues() {
      return myState.getIssues();
    }

    @Override
    public void report(@NonNull Context context,
                       @NonNull Issue issue,
                       @NonNull Severity severity,
                       @Nullable Location location,
                       @NonNull String message,
                       @NonNull TextFormat format) {
      if (location != null) {
        final File file = location.getFile();
        final VirtualFile vFile = LocalFileSystem.getInstance().findFileByIoFile(file);

        if (myState.getMainFile().equals(vFile)) {
          final Position start = location.getStart();
          final Position end = location.getEnd();

          final TextRange textRange = start != null && end != null && start.getOffset() <= end.getOffset()
                                      ? new TextRange(start.getOffset(), end.getOffset())
                                      : TextRange.EMPTY_RANGE;

          Severity configuredSeverity = severity != issue.getDefaultSeverity() ? severity : null;
          message = format.convertTo(message, RAW);
          myState.getProblems().add(new ProblemData(issue, message, textRange, configuredSeverity));
        }

        Location secondary = location.getSecondary();
        if (secondary != null && myState.getMainFile().equals(LocalFileSystem.getInstance().findFileByIoFile(secondary.getFile()))) {
          reportSecondary(context, issue, severity, location, message, format);
        }
      }
    }

    @Override
    @NotNull
    public String readFile(@NonNull File file) {
      final VirtualFile vFile = LocalFileSystem.getInstance().findFileByIoFile(file);

      if (vFile == null) {
        LOG.debug("Cannot find file " + file.getPath() + " in the VFS");
        return "";
      }
      final String content = getFileContent(vFile);

      if (content == null) {
        LOG.info("Cannot find file " + file.getPath() + " in the PSI");
        return "";
      }
      return content;
    }

    @Nullable
    private String getFileContent(final VirtualFile vFile) {
      if (Comparing.equal(myState.getMainFile(), vFile)) {
        return myState.getMainFileContent();
      }

      return ApplicationManager.getApplication().runReadAction(new Computable<String>() {
        @Nullable
        @Override
        public String compute() {
          final Module module = myState.getModule();
          final Project project = module.getProject();
          final PsiFile psiFile = PsiManager.getInstance(project).findFile(vFile);

          if (psiFile == null) {
            return null;
          }
          final Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);

          if (document != null) {
            final DocumentListener listener = new DocumentListener() {
              @Override
              public void beforeDocumentChange(DocumentEvent event) {
              }

              @Override
              public void documentChanged(DocumentEvent event) {
                myState.markDirty();
              }
            };
            document.addDocumentListener(listener, EditorLintClient.this);
          }
          return psiFile.getText();
        }
      });
    }

    @NonNull
    @Override
    public List<File> getJavaSourceFolders(@NonNull com.android.tools.klint.detector.api.Project project) {
      final VirtualFile[] sourceRoots = ModuleRootManager.getInstance(myState.getModule()).getSourceRoots(false);
      final List<File> result = new ArrayList<File>(sourceRoots.length);

      for (VirtualFile root : sourceRoots) {
        result.add(new File(root.getPath()));
      }
      return result;
    }

    @NonNull
    @Override
    public List<File> getResourceFolders(@NonNull com.android.tools.klint.detector.api.Project project) {
      AndroidFacet facet = AndroidFacet.getInstance(myState.getModule());
      if (facet != null) {
        return IntellijLintUtils.getResourceDirectories(facet);
      }
      return super.getResourceFolders(project);
    }
  }

  /** Lint client used for batch operations */
  private static class BatchLintClient extends IntellijLintClient {
    private final Map<Issue, Map<File, List<ProblemData>>> myProblemMap;
    private final AnalysisScope myScope;
    private final List<Issue> myIssues;

    public BatchLintClient(@NotNull Project project,
                           @NotNull Map<Issue, Map<File, List<ProblemData>>> problemMap,
                           @NotNull AnalysisScope scope,
                           @NotNull List<Issue> issues) {
      super(project);
      myProblemMap = problemMap;
      myScope = scope;
      myIssues = issues;
    }

    @Nullable
    @Override
    protected Module getModule() {
      // No default module
      return null;
    }

    @NonNull
    @Override
    protected List<Issue> getIssues() {
      return myIssues;
    }

    @Override
    public void report(@NonNull Context context,
                       @NonNull Issue issue,
                       @NonNull Severity severity,
                       @Nullable Location location,
                       @NonNull String message,
                       @NonNull TextFormat format) {
      VirtualFile vFile = null;
      File file = null;

      if (location != null) {
        file = location.getFile();
        vFile = LocalFileSystem.getInstance().findFileByIoFile(file);
      }
      else if (context.getProject() != null) {
        final Module module = findModuleForLintProject(myProject, context.getProject());

        if (module != null) {
          final AndroidFacet facet = AndroidFacet.getInstance(module);
          vFile = facet != null ? AndroidRootUtil.getPrimaryManifestFile(facet) : null;

          if (vFile != null) {
            file = new File(vFile.getPath());
          }
        }
      }

      boolean inScope = vFile != null && myScope.contains(vFile);
      // In analysis batch mode, the AnalysisScope contains a specific set of virtual
      // files, not directories, so any errors reported against a directory will not
      // be considered part of the scope and therefore won't be reported. Correct
      // for this.
      if (!inScope && vFile != null && vFile.isDirectory()) {
        if (myScope.getScopeType() == AnalysisScope.PROJECT) {
          inScope = true;
        } else if (myScope.getScopeType() == AnalysisScope.MODULE ||
                   myScope.getScopeType() == AnalysisScope.MODULES) {
          final Module module = findModuleForLintProject(myProject, context.getProject());
          if (module != null && myScope.containsModule(module)) {
            inScope = true;
          }
        }
      }

      if (inScope) {
        file = new File(PathUtil.getCanonicalPath(file.getPath()));

        Map<File, List<ProblemData>> file2ProblemList = myProblemMap.get(issue);
        if (file2ProblemList == null) {
          file2ProblemList = new HashMap<File, List<ProblemData>>();
          myProblemMap.put(issue, file2ProblemList);
        }

        List<ProblemData> problemList = file2ProblemList.get(file);
        if (problemList == null) {
          problemList = new ArrayList<ProblemData>();
          file2ProblemList.put(file, problemList);
        }

        TextRange textRange = TextRange.EMPTY_RANGE;

        if (location != null) {
          final Position start = location.getStart();
          final Position end = location.getEnd();

          if (start != null && end != null && start.getOffset() <= end.getOffset()) {
            textRange = new TextRange(start.getOffset(), end.getOffset());
          }
        }
        Severity configuredSeverity = severity != issue.getDefaultSeverity() ? severity : null;
        message = format.convertTo(message, RAW);
        problemList.add(new ProblemData(issue, message, textRange, configuredSeverity));

        if (location != null && location.getSecondary() != null) {
          reportSecondary(context, issue, severity, location, message, format);
        }
      }
    }

    @NonNull
    @Override
    public List<File> getJavaSourceFolders(@NonNull com.android.tools.klint.detector.api.Project project) {
      final Module module = findModuleForLintProject(myProject, project);
      if (module == null) {
        return Collections.emptyList();
      }
      final VirtualFile[] sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots(false);
      final List<File> result = new ArrayList<File>(sourceRoots.length);

      for (VirtualFile root : sourceRoots) {
        result.add(new File(root.getPath()));
      }
      return result;
    }

    @NonNull
    @Override
    public List<File> getResourceFolders(@NonNull com.android.tools.klint.detector.api.Project project) {
      final Module module = findModuleForLintProject(myProject, project);
      if (module != null) {
        AndroidFacet facet = AndroidFacet.getInstance(module);
        if (facet != null) {
          return IntellijLintUtils.getResourceDirectories(facet);
        }
      }
      return super.getResourceFolders(project);
    }
  }

  @Override
  public boolean checkForSuppressComments() {
    return false;
  }

  @Override
  public boolean supportsProjectResources() {
    return true;
  }

  @Nullable
  @Override
  public AbstractResourceRepository getProjectResources(com.android.tools.klint.detector.api.Project project, boolean includeDependencies) {
    final Module module = findModuleForLintProject(myProject, project);
    if (module != null) {
      AndroidFacet facet = AndroidFacet.getInstance(module);
      if (facet != null) {
        return includeDependencies ? facet.getProjectResources(true) : facet.getModuleResources(true);
      }
    }

    return null;
  }

  @Nullable
  @Override
  public URLConnection openConnection(@NonNull URL url) throws IOException {
    return HttpConfigurable.getInstance().openConnection(url.toExternalForm());
  }

  @NonNull
  @Override
  public Location.Handle createResourceItemHandle(@NonNull ResourceItem item) {
    XmlTag tag = LocalResourceRepository.getItemTag(myProject, item);
    if (tag != null) {
      ResourceFile source = item.getSource();
      assert source != null : item;
      return new LocationHandle(source.getFile(), tag);
    }
    return super.createResourceItemHandle(item);
  }

  @NonNull
  @Override
  public ResourceVisibilityLookup.Provider getResourceVisibilityProvider() {
    Module module = getModule();
    if (module != null) {
      AppResourceRepository appResources = AppResourceRepository.getAppResources(module, true);
      if (appResources != null) {
        ResourceVisibilityLookup.Provider provider = appResources.getResourceVisibilityProvider();
        if (provider != null) {
          return provider;
        }
      }
    }
    return super.getResourceVisibilityProvider();
  }

  private static class LocationHandle implements Location.Handle, Computable<Location> {
    private final File myFile;
    private final XmlElement myNode;
    private Object myClientData;

    public LocationHandle(File file, XmlElement node) {
      myFile = file;
      myNode = node;
    }

    @NonNull
    @Override
    public Location resolve() {
      if (!ApplicationManager.getApplication().isReadAccessAllowed()) {
        return ApplicationManager.getApplication().runReadAction(this);
      }
      TextRange textRange = myNode.getTextRange();

      // For elements, don't highlight the entire element range; instead, just
      // highlight the element name
      if (myNode instanceof XmlTag) {
        String tag = ((XmlTag)myNode).getName();
        int index = myNode.getText().indexOf(tag);
        if (index != -1) {
          int start = textRange.getStartOffset() + index;
          textRange = new TextRange(start, start + tag.length());
        }
      }

      Position start = new DefaultPosition(-1, -1, textRange.getStartOffset());
      Position end = new DefaultPosition(-1, -1, textRange.getEndOffset());
      return Location.create(myFile, start, end);
    }

    @Override
    public Location compute() {
      return resolve();
    }

    @Override
    public void setClientData(@Nullable Object clientData) {
      myClientData = clientData;
    }

    @Override
    @Nullable
    public Object getClientData() {
      return myClientData;
    }
  }
}
