// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.project;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ContentRootData;
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.util.containers.ContainerUtil;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.Task;
import org.gradle.tooling.model.cpp.CppExecutable;
import org.gradle.tooling.model.cpp.CppSharedLibrary;
import org.gradle.tooling.model.cpp.CppStaticLibrary;
import org.gradle.tooling.model.cpp.LinkageDetails;
import org.gradle.tooling.model.idea.IdeaModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.DefaultExternalTask;
import org.jetbrains.plugins.gradle.model.ExternalTask;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.builder.CppModelBuilder;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.*;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl.*;
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemConstants.UNORDERED)
public class GradleNativeProjectResolver extends AbstractProjectResolverExtension {
  @NotNull public static final Key<CppProject> CPP_PROJECT = Key.create(CppProject.class, ProjectKeys.MODULE.getProcessingWeight() + 1);

  @Override
  public void populateModuleContentRoots(@NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule) {
    CppProjectImpl cppProject = appendCppProject(gradleModule, ideModule);
    if (cppProject != null) {
      CppComponent mainComponent = cppProject.getMainComponent();
      Set<File> sourceFolders = mainComponent == null ? new HashSet<>() : getSourceFolders(mainComponent);
      CppTestSuite testComponent = cppProject.getTestComponent();
      Set<File> testSourceFolders = testComponent == null ? new HashSet<>() : getSourceFolders(testComponent);
      testSourceFolders.removeAll(sourceFolders);
      for (File folder : sourceFolders) {
        ContentRootData ideContentRoot = new ContentRootData(GradleConstants.SYSTEM_ID, folder.getAbsolutePath());
        ideContentRoot.storePath(ExternalSystemSourceType.SOURCE, folder.getAbsolutePath());
        ideModule.createChild(ProjectKeys.CONTENT_ROOT, ideContentRoot);
      }
      for (File folder : testSourceFolders) {
        ContentRootData ideContentRoot = new ContentRootData(GradleConstants.SYSTEM_ID, folder.getAbsolutePath());
        ideContentRoot.storePath(ExternalSystemSourceType.TEST, folder.getAbsolutePath());
        ideModule.createChild(ProjectKeys.CONTENT_ROOT, ideContentRoot);
      }
    }

    nextResolver.populateModuleContentRoots(gradleModule, ideModule);
  }

  @NotNull
  @Override
  public Set<Class> getExtraProjectModelClasses() {
    return ContainerUtil.set(org.gradle.tooling.model.cpp.CppProject.class, CppProject.class);
  }

  @NotNull
  @Override
  public Set<Class> getToolingExtensionsClasses() {
    return ContainerUtil.set(
      // native-gradle-tooling jar
      CppModelBuilder.class
    );
  }

  @Override
  public Set<Class> getTargetTypes() {
    return ContainerUtil.set(
      org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CppExecutable.class,
      org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CppSharedLibrary.class,
      org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CppStaticLibrary.class
    );
  }

  @Nullable
  private CppProjectImpl appendCppProject(@NotNull IdeaModule gradleModule,
                                          @NotNull DataNode<ModuleData> ideModule) {
    CppProjectImpl cppProject = null;
    org.gradle.tooling.model.cpp.CppProject gradleCppProject =
      resolverCtx.getExtraProject(gradleModule, org.gradle.tooling.model.cpp.CppProject.class);
    if (gradleCppProject != null) {
      cppProject = convert(gradleCppProject);
    }
    else {
      // todo[Vlad] old experimental support for Gradle 4.1 - 4.9, to be removed in 2019.1
      CppProject customGradleCppProject = resolverCtx.getExtraProject(gradleModule, CppProject.class);
      if (customGradleCppProject != null) {
        // store a local process copy of the object to get rid of proxy types for further serialization
        cppProject = new CppProjectImpl(customGradleCppProject);
      }
    }
    if (cppProject != null) {
      ideModule.createChild(CPP_PROJECT, cppProject);
    }
    return cppProject;
  }

  private static CppProjectImpl convert(@NotNull org.gradle.tooling.model.cpp.CppProject gradleCppProject) {
    CppProjectImpl cppProject = new CppProjectImpl();
    cppProject.setMainComponent(convert(gradleCppProject.getMainComponent()));
    cppProject.setTestComponent((CppTestSuite)convert(gradleCppProject.getTestComponent()));
    return cppProject;
  }

  private static CppComponent convert(@Nullable org.gradle.tooling.model.cpp.CppComponent component) {
    if (component == null) return null;
    CppComponentImpl cppComponent;
    if (component instanceof org.gradle.tooling.model.cpp.CppTestSuite) {
      cppComponent = new CppTestSuiteImpl(component.getName(), component.getBaseName());
    }
    else {
      cppComponent = new CppComponentImpl(component.getName(), component.getBaseName());
    }
    for (org.gradle.tooling.model.cpp.CppBinary binary : component.getBinaries()) {
      cppComponent.addBinary(convert(binary));
    }
    return cppComponent;
  }

  @Nullable
  private static CppBinaryImpl convert(@Nullable org.gradle.tooling.model.cpp.CppBinary binary) {
    CppBinaryImpl cppBinary;
    if (binary instanceof CppExecutable) {
      cppBinary = new CppExecutableImpl(binary.getName(), binary.getBaseName(), binary.getVariantName());
    }
    else if (binary instanceof CppSharedLibrary) {
      cppBinary = new CppSharedLibraryImpl(binary.getName(), binary.getBaseName(), binary.getVariantName());
    }
    else if (binary instanceof CppStaticLibrary) {
      cppBinary = new CppStaticLibraryImpl(binary.getName(), binary.getBaseName(), binary.getVariantName());
    }
    else {
      return null;
    }
    cppBinary.setCompilationDetails(convert(binary.getCompilationDetails()));
    cppBinary.setLinkageDetails(convert(binary.getLinkageDetails()));
    return cppBinary;
  }

  private static LinkageDetailsImpl convert(LinkageDetails details) {
    LinkageDetailsImpl linkageDetails = new LinkageDetailsImpl();
    linkageDetails.setAdditionalArgs(new ArrayList<>(details.getAdditionalArgs()));
    linkageDetails.setLinkTask(convert(details.getLinkTask()));
    linkageDetails.setOutputLocation(details.getOutputLocation());
    return linkageDetails;
  }

  private static ExternalTask convert(Task task) {
    if (task == null) return null;
    DefaultExternalTask externalTask = new DefaultExternalTask();
    externalTask.setDescription(task.getDescription());
    externalTask.setGroup(task.getGroup());
    externalTask.setName(task.getName());
    externalTask.setQName(task.getPath());
    //externalTask.setType(task.);
    return externalTask;
  }

  private static CompilationDetailsImpl convert(org.gradle.tooling.model.cpp.CompilationDetails details) {
    CompilationDetailsImpl compilationDetails = new CompilationDetailsImpl();
    compilationDetails.setCompileTask(convert(details.getCompileTask()));
    compilationDetails.setAdditionalArgs(new ArrayList<>(details.getAdditionalArgs()));
    compilationDetails.setCompilerExecutable(details.getCompilerExecutable());
    compilationDetails.setCompileWorkingDir(details.getCompileWorkingDir());
    compilationDetails.setFrameworkSearchPaths(new ArrayList<>(details.getFrameworkSearchPaths()));
    compilationDetails.setHeaderDirs(new LinkedHashSet<>(details.getHeaderDirs()));
    compilationDetails.setMacroDefines(convert(details.getMacroDefines()));
    compilationDetails.setMacroUndefines(new LinkedHashSet<>(details.getMacroUndefines()));
    compilationDetails.setSources(convertSources(details.getSources()));
    compilationDetails.setSystemHeaderSearchPaths(new ArrayList<>(details.getSystemHeaderSearchPaths()));
    compilationDetails.setUserHeaderSearchPaths(new ArrayList<>(details.getUserHeaderSearchPaths()));
    return compilationDetails;
  }

  private static Set<SourceFile> convertSources(DomainObjectSet<? extends org.gradle.tooling.model.cpp.SourceFile> sources) {
    Set<SourceFile> sourceFiles = new LinkedHashSet<>(sources.size());
    for (org.gradle.tooling.model.cpp.SourceFile sourceFile : sources) {
      sourceFiles.add(convert(sourceFile));
    }
    return sourceFiles;
  }

  private static SourceFile convert(org.gradle.tooling.model.cpp.SourceFile sourceFile) {
    return new SourceFileImpl(sourceFile.getSourceFile(), sourceFile.getObjectFile());
  }

  private static Set<MacroDirective> convert(DomainObjectSet<? extends org.gradle.tooling.model.cpp.MacroDirective> directives) {
    Set<MacroDirective> macroDirectives = new LinkedHashSet<>(directives.size());
    for (org.gradle.tooling.model.cpp.MacroDirective directive : directives) {
      macroDirectives.add(convert(directive));
    }
    return macroDirectives;
  }

  private static MacroDirective convert(org.gradle.tooling.model.cpp.MacroDirective directive) {
    return new MacroDirectiveImpl(directive.getName(), directive.getValue());
  }

  private static Set<File> getSourceFolders(CppComponent component) {
    Set<File> result = new LinkedHashSet<>();
    for (CppBinary binary : component.getBinaries()) {
      CompilationDetails compilationDetails = binary.getCompilationDetails();
      result.addAll(compilationDetails.getHeaderDirs());
      compilationDetails.getSources().forEach(sourceFile -> result.add(sourceFile.getSourceFile().getParentFile()));
    }
    return result;
  }
}
