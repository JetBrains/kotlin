// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.builder;

import org.gradle.api.Project;
import org.gradle.api.component.SoftwareComponent;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.project.DefaultProject;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.provider.Provider;
import org.gradle.internal.impldep.org.apache.commons.lang.StringUtils;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.language.cpp.*;
import org.gradle.language.cpp.plugins.CppBasePlugin;
import org.gradle.language.cpp.plugins.CppPlugin;
import org.gradle.language.cpp.tasks.CppCompile;
import org.gradle.language.nativeplatform.ComponentWithExecutable;
import org.gradle.language.nativeplatform.internal.ConfigurableComponentWithExecutable;
import org.gradle.nativeplatform.internal.CompilerOutputFileNamingScheme;
import org.gradle.nativeplatform.internal.CompilerOutputFileNamingSchemeFactory;
import org.gradle.nativeplatform.tasks.LinkExecutable;
import org.gradle.nativeplatform.test.cpp.CppTestSuite;
import org.gradle.nativeplatform.toolchain.Clang;
import org.gradle.nativeplatform.toolchain.Gcc;
import org.gradle.nativeplatform.toolchain.NativeToolChain;
import org.gradle.nativeplatform.toolchain.VisualCpp;
import org.gradle.nativeplatform.toolchain.internal.NativeLanguageTools;
import org.gradle.nativeplatform.toolchain.internal.PlatformToolProvider;
import org.gradle.nativeplatform.toolchain.internal.ToolType;
import org.gradle.nativeplatform.toolchain.internal.tools.CommandLineToolSearchResult;
import org.gradle.nativeplatform.toolchain.internal.tools.ToolSearchPath;
import org.gradle.platform.base.internal.toolchain.ToolSearchResult;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.DefaultExternalTask;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.CppProject;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.SourceFile;
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl.*;
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;
import org.jetbrains.plugins.gradle.tooling.util.ReflectionUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * The prototype of the C++ project gradle tooling model builder.
 * This implementation should be moved or replaced with the similar model builder from the Gradle distribution.
 *
 * @author Vladislav.Soroka
 * @deprecated to be removed in 2019.1, use built-in 'org.gradle.tooling.model.cpp.CppComponent' available since Gradle 4.10
 */
@Deprecated
public class CppModelBuilder implements ModelBuilderService {
  private static final boolean IS_51_OR_BETTER = GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("5.1")) >= 0;
  private static final boolean IS_410_OR_BETTER = IS_51_OR_BETTER ||
                                                  GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("4.10")) >= 0;
  private static final boolean IS_48_OR_BETTER = IS_410_OR_BETTER ||
                                                 GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("4.8")) >= 0;
  private static final boolean IS_47_OR_BETTER = IS_48_OR_BETTER ||
                                                 GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("4.7")) >= 0;
  private static final boolean IS_41_OR_BETTER = IS_47_OR_BETTER ||
                                                 GradleVersion.current().getBaseVersion().compareTo(GradleVersion.version("4.1")) >= 0;

  @Override
  public boolean canBuild(String modelName) {
    return CppProject.class.getName().equals(modelName);
  }

  @Nullable
  @Override
  public Object buildAll(final String modelName, final Project project) {
    if (!IS_41_OR_BETTER || IS_410_OR_BETTER) {
      return null;
    }
    PluginContainer pluginContainer = project.getPlugins();
    if (!pluginContainer.hasPlugin(CppBasePlugin.class)) {
      if (pluginContainer.hasPlugin(CppPlugin.class)) {
        project.getLogger().error(
          "[sync warning] The IDE doesn't support 'cpp' gradle plugin. " +
          "Consider to use new gradle C++ plugins, see details at https://blog.gradle.org/introducing-the-new-cpp-plugins");
      }
      return null;
    }

    final CppProjectImpl cppProject = new CppProjectImpl();
    for (SoftwareComponent component : project.getComponents()) {
      if (component instanceof CppComponent) {
        CppComponent cppComponent = (CppComponent)component;
        String cppComponentName = cppComponent.getName();
        String cppComponentBaseName = cppComponent.getBaseName().get();

        Set<File> headerDirs = new LinkedHashSet<File>(cppComponent.getPrivateHeaderDirs().getFiles());

        CppComponentImpl ideCppComponent;
        if (cppComponent instanceof ProductionCppComponent) {
          ideCppComponent = new CppComponentImpl(cppComponentName, cppComponentBaseName);
          cppProject.setMainComponent(ideCppComponent);
        }
        else if (cppComponent instanceof CppTestSuite) {
          CppTestSuiteImpl cppTestSuite = new CppTestSuiteImpl(cppComponentName, cppComponentBaseName);
          ideCppComponent = cppTestSuite;
          cppProject.setTestComponent(cppTestSuite);
        }
        else {
          continue;
        }

        for (CppBinary cppBinary : cppComponent.getBinaries().get()) {
          String binaryName = cppBinary.getName();
          String baseName = cppBinary.getBaseName().getOrElse("");
          String variantName = StringUtils.uncapitalize(StringUtils.removeStart(binaryName, "main"));

          boolean isExecutable = cppBinary instanceof ComponentWithExecutable;
          CppBinaryImpl binary;
          if (cppBinary instanceof CppSharedLibrary) {
            binary = new CppSharedLibraryImpl(binaryName, baseName, variantName);
          }
          else if (cppBinary instanceof CppStaticLibrary) {
            binary = new CppStaticLibraryImpl(binaryName, baseName, variantName);
          }
          else if (isExecutable) {
            binary = new CppExecutableImpl(binaryName, baseName, variantName);
          }
          else {
            continue;
          }
          ideCppComponent.addBinary(binary);

          Provider<CppCompile> cppCompileProvider = cppBinary.getCompileTask();
          if (cppCompileProvider.isPresent()) {
            CppCompile cppCompile = cppCompileProvider.get();
            CompilationDetailsImpl compilationDetails = new CompilationDetailsImpl();
            binary.setCompilationDetails(compilationDetails);

            String compileTaskName = cppCompile.getPath();
            DefaultExternalTask compileTask = new DefaultExternalTask();
            compileTask.setName(compileTaskName);
            compileTask.setQName(compileTaskName);
            compilationDetails.setCompileTask(compileTask);

            Set<File> compileIncludePath = new LinkedHashSet<File>(cppBinary.getCompileIncludePath().getFiles());
            Set<File> systemIncludes = new LinkedHashSet<File>();
            //Since Gradle 4.8, system header include directories should be accessed separately via the systemIncludes property
            //see https://github.com/gradle/gradle-native/blob/master/docs/RELEASE-NOTES.md#better-control-over-system-include-path-for-native-compilation---583
            if (IS_48_OR_BETTER) {
              compileIncludePath.addAll(cppCompile.getIncludes().getFiles());
              systemIncludes.addAll(cppCompile.getSystemIncludes().getFiles());
            }
            else {
              systemIncludes.addAll(cppCompile.getIncludes().getFiles());
            }
            compilationDetails.setSystemHeaderSearchPaths(new ArrayList<File>(systemIncludes));
            compilationDetails.setUserHeaderSearchPaths(new ArrayList<File>(compileIncludePath));
            compilationDetails.setHeaderDirs(headerDirs);

            Set<SourceFile> sources = getSources(project, cppBinary, cppCompile);
            compilationDetails.setSources(sources);

            File compilerWorkingDir = cppCompile.getObjectFileDir().get().getAsFile();
            compilationDetails.setCompileWorkingDir(compilerWorkingDir);
            File cppCompilerExecutable = findCppCompilerExecutable(project, cppBinary);
            compilationDetails.setCompilerExecutable(cppCompilerExecutable);

            List<String> compilerArgs = new ArrayList<String>(cppCompile.getCompilerArgs().getOrElse(Collections.<String>emptyList()));
            compilationDetails.setAdditionalArgs(compilerArgs);
          }

          LinkageDetailsImpl linkageDetails = new LinkageDetailsImpl();
          binary.setLinkageDetails(linkageDetails);
          DefaultExternalTask linkTask = new DefaultExternalTask();
          linkageDetails.setLinkTask(linkTask);
          linkTask.setName("");
          linkTask.setQName("");
          if (isExecutable) {
            Provider<? extends LinkExecutable> fileProvider = ((ComponentWithExecutable)cppBinary).getLinkTask();
            if (fileProvider.isPresent()) {
              LinkExecutable linkExecutable = fileProvider.get();
              String linkTaskName = linkExecutable.getPath();
              linkTask.setName(linkTaskName);
              linkTask.setQName(linkTaskName);
              File executableFile = getExecutableFile(linkExecutable);
              linkageDetails.setOutputLocation(executableFile);
            }
          }
        }
      }
    }
    return cppProject;
  }

  private static Set<SourceFile> getSources(Project project, CppBinary cppBinary, CppCompile cppCompile) {
    Set<SourceFile> sources = new LinkedHashSet<SourceFile>();
    for (File file : cppBinary.getCppSource().getFiles()) {
      sources.add(new SourceFileImpl(file, null));
    }
    if (cppCompile.getObjectFileDir().isPresent()) {
      File objectFileDir = cppCompile.getObjectFileDir().get().getAsFile();
      Iterator<CompilerOutputFileNamingSchemeFactory> it =
        ((DefaultProject)project).getServices().getAll(CompilerOutputFileNamingSchemeFactory.class).iterator();
      if (it.hasNext()) {
        CompilerOutputFileNamingSchemeFactory outputFileNamingSchemeFactory = it.next();
        boolean isTargetWindows = IS_51_OR_BETTER
                                  ? cppBinary.getTargetPlatform().getTargetMachine().getOperatingSystemFamily().isWindows()
                                  : isWindowsOld(cppBinary.getTargetPlatform());
        String objectFileExtension = isTargetWindows ? ".obj" : ".o";
        CompilerOutputFileNamingScheme outputFileNamingScheme = outputFileNamingSchemeFactory.create();
        outputFileNamingScheme.withOutputBaseFolder(objectFileDir).withObjectFileNameSuffix(objectFileExtension);

        for (SourceFile sourceFile : sources) {
          File objectFile = outputFileNamingScheme.map(sourceFile.getSourceFile());
          ((SourceFileImpl)sourceFile).setObjectFile(objectFile);
        }
      }
    }
    return sources;
  }

  private static boolean isWindowsOld(CppPlatform platform) {
    Object operatingSystem = ReflectionUtil.callByReflection(platform, "getOperatingSystem");
    if (operatingSystem == null) {
      return false;
    }
    Object isWindowsNullable = ReflectionUtil.callByReflection(operatingSystem, "isWindows");
    return Boolean.TRUE.equals(isWindowsNullable);
  }

  @Nullable
  private static File getExecutableFile(LinkExecutable linkExecutable) {
    File executableFile;
    RegularFileProperty binaryFile = null;
    if (IS_47_OR_BETTER) {
      binaryFile = linkExecutable.getLinkedFile();
    }
    else {
      try {
        Object linkedFile = linkExecutable.getClass().getMethod("getBinaryFile").invoke(linkExecutable);
        if (linkedFile instanceof RegularFileProperty) {
          binaryFile = (RegularFileProperty)linkedFile;
        }
      }
      catch (Exception e) {
        //noinspection CallToPrintStackTrace
        e.printStackTrace();
      }
    }
    executableFile = binaryFile != null ? binaryFile.getAsFile().getOrNull() : null;
    return executableFile;
  }

  @Nullable
  private static File findCppCompilerExecutable(Project project, CppBinary cppBinary) {
    Throwable throwable = null;
    try {
      if (cppBinary instanceof ConfigurableComponentWithExecutable) {
        PlatformToolProvider toolProvider = ((ConfigurableComponentWithExecutable)cppBinary).getPlatformToolProvider();
        ToolSearchResult toolSearchResult;
        if (IS_410_OR_BETTER) {
          toolSearchResult = toolProvider.locateTool(ToolType.CPP_COMPILER);
        }
        else {
          Method isToolAvailableMethod = toolProvider.getClass().getDeclaredMethod("isToolAvailable", ToolType.class);
          isToolAvailableMethod.setAccessible(true);
          toolSearchResult = (ToolSearchResult)isToolAvailableMethod.invoke(toolProvider, ToolType.CPP_COMPILER);
        }
        if (toolSearchResult.isAvailable()) {
          if (toolSearchResult instanceof CommandLineToolSearchResult) {
            return ((CommandLineToolSearchResult)toolSearchResult).getTool();
          }
          // dirty hack for gradle versions <= 4.9 because of dummy implementation of org.gradle.nativeplatform.toolchain.internal.msvcpp.VisualCppPlatformToolProvider.isToolAvailable
          if (toolProvider.getClass().getSimpleName().equals("VisualCppPlatformToolProvider")) {
            Field visualCppField = toolProvider.getClass().getDeclaredField("visualCpp");
            visualCppField.setAccessible(true);
            Object visualCpp = visualCppField.get(toolProvider);

            if (visualCpp instanceof NativeLanguageTools) {
              return ((NativeLanguageTools)visualCpp).getCompilerExecutable();
            }
          }
        }
      }
    }
    catch (Throwable t) {
      throwable = t;
    }

    NativeToolChain toolChain = cppBinary.getToolChain();
    String exeName;
    if (toolChain instanceof Gcc) {
      exeName = "g++";
    }
    else if (toolChain instanceof Clang) {
      exeName = "clang++";
    }
    else if (toolChain instanceof VisualCpp) {
      exeName = "cl";
    }
    else {
      exeName = null;
    }

    if (exeName != null) {
      ToolSearchPath toolSearchPath = new ToolSearchPath(OperatingSystem.current());
      CommandLineToolSearchResult searchResult = toolSearchPath.locate(ToolType.CPP_COMPILER, exeName);
      if (searchResult.isAvailable()) {
        return searchResult.getTool();
      }
    }

    if (!IS_47_OR_BETTER) {
      project.getLogger().error(
        "[sync error] Unable to resolve compiler executable. " +
        "The project uses '" + GradleVersion.current() + "' try to update the gradle version");
    }
    else {
      project.getLogger().error("[sync error] Unable to resolve compiler executable", throwable);
    }
    return null;
  }

  @NotNull
  @Override
  public ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
    return ErrorMessageBuilder.create(
      project, e, "C++ project import errors"
    ).withDescription("Unable to import C++ project");
  }
}
