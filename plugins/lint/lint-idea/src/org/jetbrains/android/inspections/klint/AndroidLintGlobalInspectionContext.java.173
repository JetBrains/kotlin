package org.jetbrains.android.inspections.klint;

import com.android.tools.klint.client.api.LintDriver;
import com.android.tools.klint.client.api.LintRequest;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Scope;
import com.google.common.collect.Lists;
import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInspection.GlobalInspectionContext;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.Tools;
import com.intellij.codeInspection.lang.GlobalInspectionContextExtension;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.module.impl.scopes.ModuleWithDependenciesScope;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.containers.HashMap;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

class AndroidLintGlobalInspectionContext implements GlobalInspectionContextExtension<AndroidLintGlobalInspectionContext> {
  static final Key<AndroidLintGlobalInspectionContext> ID = Key.create("AndroidLintGlobalInspectionContext");
  private Map<Issue, Map<File, List<ProblemData>>> myResults;

  @NotNull
  @Override
  public Key<AndroidLintGlobalInspectionContext> getID() {
    return ID;
  }

  @Override
  public void performPreRunActivities(@NotNull List<Tools> globalTools, @NotNull List<Tools> localTools, @NotNull final GlobalInspectionContext context) {
    final Project project = context.getProject();

    if (!ProjectFacetManager.getInstance(project).hasFacets(AndroidFacet.ID)) {
      return;
    }

    final List<Issue> issues = AndroidLintExternalAnnotator.getIssuesFromInspections(project, null);
    if (issues.size() == 0) {
      return;
    }

    final Map<Issue, Map<File, List<ProblemData>>> problemMap = new HashMap<Issue, Map<File, List<ProblemData>>>();
    final AnalysisScope scope = context.getRefManager().getScope();
    if (scope == null) {
      return;
    }

    final IntellijLintClient client = IntellijLintClient.forBatch(project, problemMap, scope, issues);
    final LintDriver lint = new LintDriver(new IntellijLintIssueRegistry(), client);

    final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator != null) {
      ProgressWrapper.unwrap(indicator).setText("Running Android Lint");
    }

    EnumSet<Scope> lintScope;
    //noinspection ConstantConditions
    if (!IntellijLintProject.SUPPORT_CLASS_FILES) {
      lintScope = EnumSet.copyOf(Scope.ALL);
      // Can't run class file based checks
      lintScope.remove(Scope.CLASS_FILE);
      lintScope.remove(Scope.ALL_CLASS_FILES);
      lintScope.remove(Scope.JAVA_LIBRARIES);
    } else {
      lintScope = Scope.ALL;
    }

    List<VirtualFile> files = null;
    final List<Module> modules = Lists.newArrayList();

    int scopeType = scope.getScopeType();
    switch (scopeType) {
      case AnalysisScope.MODULE: {
        SearchScope searchScope = scope.toSearchScope();
        if (searchScope instanceof ModuleWithDependenciesScope) {
          ModuleWithDependenciesScope s = (ModuleWithDependenciesScope)searchScope;
          if (!s.isSearchInLibraries()) {
            modules.add(s.getModule());
          }
        }
        break;
      }
      case AnalysisScope.FILE:
      case AnalysisScope.VIRTUAL_FILES:
      case AnalysisScope.UNCOMMITTED_FILES: {
        files = Lists.newArrayList();
        SearchScope searchScope = ApplicationManager
                .getApplication()
                .runReadAction((Computable<SearchScope>) scope::toSearchScope);
        if (searchScope instanceof LocalSearchScope) {
          final LocalSearchScope localSearchScope = (LocalSearchScope)searchScope;
          final PsiElement[] elements = localSearchScope.getScope();
          final List<VirtualFile> finalFiles = files;

          ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
              for (PsiElement element : elements) {
                if (element instanceof PsiFile) { // should be the case since scope type is FILE
                  Module module = ModuleUtilCore.findModuleForPsiElement(element);
                  if (module != null && !modules.contains(module)) {
                    modules.add(module);
                  }
                  VirtualFile virtualFile = ((PsiFile)element).getVirtualFile();
                  if (virtualFile != null) {
                    finalFiles.add(virtualFile);
                  }
                }
              }
            }
          });
        } else {
          final List<VirtualFile> finalList = files;
          scope.accept(new PsiElementVisitor() {
            @Override
            public void visitFile(PsiFile file) {
              VirtualFile virtualFile = file.getVirtualFile();
              if (virtualFile != null) {
                finalList.add(virtualFile);
              }
            }
          });
        }
        if (files.isEmpty()) {
          files = null;
        } else {
          // Lint will compute it lazily based on actual files in the request
          lintScope = null;
        }
        break;
      }
      case AnalysisScope.PROJECT: {
        modules.addAll(Arrays.asList(ModuleManager.getInstance(project).getModules()));
        break;
      }
      case AnalysisScope.CUSTOM:
      case AnalysisScope.MODULES:
      case AnalysisScope.DIRECTORY: {
        // Handled by the getNarrowedComplementaryScope case below
        break;
      }

      case AnalysisScope.INVALID:
        break;
      default:
        Logger.getInstance(this.getClass()).warn("Unexpected inspection scope " + scope + ", " + scopeType);
    }

    if (modules.isEmpty()) {
      for (Module module : ModuleManager.getInstance(project).getModules()) {
        if (scope.containsModule(module)) {
          modules.add(module);
        }
      }

      if (modules.isEmpty() && files != null) {
        for (VirtualFile file : files) {
          Module module = ModuleUtilCore.findModuleForFile(file, project);
          if (module != null && !modules.contains(module)) {
            modules.add(module);
          }
        }
      }

      if (modules.isEmpty()) {
        AnalysisScope narrowed = scope.getNarrowedComplementaryScope(project);
        for (Module module : ModuleManager.getInstance(project).getModules()) {
          if (narrowed.containsModule(module)) {
            modules.add(module);
          }
        }
      }
    }

    LintRequest request = new IntellijLintRequest(client, project, files, modules, false);
    request.setScope(lintScope);

    lint.analyze(request);

    myResults = problemMap;
  }

  @Nullable
  public Map<Issue, Map<File, List<ProblemData>>> getResults() {
    return myResults;
  }

  @Override
  public void performPostRunActivities(@NotNull List<InspectionToolWrapper> inspections, @NotNull final GlobalInspectionContext context) {
  }

  @Override
  public void cleanup() {
  }
}
