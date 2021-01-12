// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler;

import com.intellij.compiler.impl.*;
import com.intellij.compiler.impl.javaCompiler.BackendCompiler;
import com.intellij.compiler.server.BuildManager;
import com.intellij.execution.process.ProcessIOExecutorService;
import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.Compiler;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.compiler.util.InspectionValidator;
import com.intellij.openapi.compiler.util.InspectionValidatorWrapper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.ProjectExtensionPointName;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.serviceContainer.NonInjectable;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.net.NetUtils;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jps.api.CanceledStatus;
import org.jetbrains.jps.builders.impl.java.JavacCompilerTool;
import org.jetbrains.jps.incremental.BinaryContent;
import org.jetbrains.jps.javac.*;
import org.jetbrains.jps.javac.ast.api.JavacFileData;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// cannot be final - extended by Bazel plugin
public class CompilerManagerImpl extends CompilerManager {
  private static final ProjectExtensionPointName<CompilerFactory> COMPILER_FACTORY_EP = new ProjectExtensionPointName<>("com.intellij.compilerFactory");
  private static final ProjectExtensionPointName<CompileTaskBean> COMPILER_TASK_EP = new ProjectExtensionPointName<>("com.intellij.compiler.task");
  private static final ProjectExtensionPointName<CompilableFileTypesProvider> COMPILABLE_TYPE_EP = new ProjectExtensionPointName<>("com.intellij.compilableFileTypesProvider");

  private static final Logger LOG = Logger.getInstance(CompilerManagerImpl.class);

  private final Project myProject;

  private final Map<Compiler, String> myCompilers = new HashMap<>();
  private static final String NO_FACTORY_ID = "";

  private final List<CompileTask> myBeforeTasks = new ArrayList<>();
  private final List<CompileTask> myAfterTasks = new ArrayList<>();
  private final Set<FileType> myCompilableTypes = new HashSet<>();
  private volatile Set<FileType> myCachedCompilableTypes;
  private final CompilationStatusListener myEventPublisher;
  private final Semaphore myCompilationSemaphore = new Semaphore(1, true);
  private final Set<ModuleType<?>> myValidationDisabledModuleTypes = new HashSet<>();
  private final Set<LocalFileSystem.WatchRequest> myWatchRoots;
  private volatile ExternalJavacManager myExternalJavacManager;

  @SuppressWarnings("MissingDeprecatedAnnotation")
  @NonInjectable
  @Deprecated
  public CompilerManagerImpl(@NotNull Project project, @SuppressWarnings("unused") @NotNull MessageBus messageBus) {
    this(project);
  }

  public CompilerManagerImpl(@NotNull Project project) {
    myProject = project;
    myEventPublisher = project.getMessageBus().syncPublisher(CompilerTopics.COMPILATION_STATUS);
    // predefined compilers
    for (ProjectExtensionPointName<?> ep : Arrays.asList(COMPILABLE_TYPE_EP, BackendCompiler.EP_NAME)) {
      ep.addChangeListener(project, () -> {myCachedCompilableTypes = null;}, project);
    }
    COMPILER_FACTORY_EP.getPoint(project).addExtensionPointListener(new ExtensionPointListener<CompilerFactory>() {
      @Override
      public void extensionAdded(@NotNull CompilerFactory factory, @NotNull PluginDescriptor pluginDescriptor) {
        Compiler[] compilers = factory.createCompilers(CompilerManagerImpl.this);
        if (compilers != null) {
          String factoryId = getFactoryId(factory);
          for (Compiler compiler : compilers) {
            addCompiler(compiler, factoryId);
          }
        }
      }

      @Override
      public void extensionRemoved(@NotNull CompilerFactory factory, @NotNull PluginDescriptor pluginDescriptor) {
        List<Compiler> compilersToRemove = new ArrayList<>();
        String factoryId = getFactoryId(factory);
        for (Map.Entry<Compiler, String> entry : myCompilers.entrySet()) {
          if (factoryId.equals(entry.getValue())) {
            compilersToRemove.add(entry.getKey());
          }
        }
        for (Compiler compiler : compilersToRemove) {
          removeCompiler(compiler);
        }
      }
    }, true, project);

    final File projectGeneratedSrcRoot = CompilerPaths.getGeneratedDataDirectory(project);
    projectGeneratedSrcRoot.mkdirs();
    final LocalFileSystem lfs = LocalFileSystem.getInstance();
    myWatchRoots = lfs.addRootsToWatch(Collections.singletonList(FileUtil.toCanonicalPath(projectGeneratedSrcRoot.getPath())), true);
    Disposer.register(project, () -> {
      final ExternalJavacManager manager = myExternalJavacManager;
      myExternalJavacManager = null;
      if (manager != null) {
        manager.stop();
      }
      lfs.removeWatchedRoots(myWatchRoots);
      if (ApplicationManager.getApplication().isUnitTestMode()) {    // force cleanup for created compiler system directory with generated sources
        FileUtil.delete(CompilerPaths.getCompilerSystemDirectory(project));
      }
    });
  }

  // returns true if all javacs terminated
  @TestOnly
  public boolean waitForExternalJavacToTerminate(long time, @NotNull TimeUnit unit) {
    ExternalJavacManager externalJavacManager = myExternalJavacManager;
    return externalJavacManager == null || externalJavacManager.waitForAllProcessHandlers(time, unit);
  }
  @TestOnly
  public boolean awaitNettyThreadPoolTermination(long time, @NotNull TimeUnit unit) {
    ExternalJavacManager externalJavacManager = myExternalJavacManager;
    return externalJavacManager == null || externalJavacManager.awaitNettyThreadPoolTermination(time, unit);
  }

  public Semaphore getCompilationSemaphore() {
    return myCompilationSemaphore;
  }

  @Override
  public boolean isCompilationActive() {
    return myCompilationSemaphore.availablePermits() == 0;
  }

  @Override
  public final void addCompiler(@NotNull Compiler compiler) {
    addCompiler(compiler, NO_FACTORY_ID);
  }

  private void addCompiler(@NotNull Compiler compiler, @NotNull final String factoryId) {
    myCompilers.put(compiler, factoryId);
    // supporting file instrumenting compilers and validators for external build
    // Since these compilers are IDE-specific and use PSI, it is ok to run them before and after the build in the IDE
    if (compiler instanceof SourceInstrumentingCompiler) {
      addBeforeTask(new FileProcessingCompilerAdapterTask((FileProcessingCompiler)compiler));
    }
    else if (compiler instanceof Validator) {
      addAfterTask(new FileProcessingCompilerAdapterTask((FileProcessingCompiler)compiler));
    }
  }

  private static String getFactoryId(@Nullable CompilerFactory factory) {
    return factory == null? NO_FACTORY_ID : factory.getClass().getName();
  }

  @Override
  @Deprecated
  public void addTranslatingCompiler(@NotNull TranslatingCompiler compiler, Set<FileType> inputTypes, Set<FileType> outputTypes) {
    // empty
  }

  @Override
  public final void removeCompiler(@NotNull Compiler compiler) {
    if (myCompilers.remove(compiler) != null) {
      for (List<CompileTask> tasks : Arrays.asList(myBeforeTasks, myAfterTasks)) {
        tasks.removeIf(
          task -> task instanceof FileProcessingCompilerAdapterTask && ((FileProcessingCompilerAdapterTask)task).getCompiler() == compiler
        );
      }
    }
  }

  @Override
  public <T  extends Compiler> T @NotNull [] getCompilers(@NotNull Class<T> compilerClass) {
    final List<T> compilers = new ArrayList<>(myCompilers.size());
    for (final Compiler item : ContainerUtil.concat(myCompilers.keySet(), Compiler.EP_NAME.getExtensions(myProject))) {
      T concreteCompiler = ObjectUtils.tryCast(item, compilerClass);
      if (concreteCompiler != null) {
        compilers.add(concreteCompiler);
      }
    }
    if (compilerClass.isAssignableFrom(InspectionValidatorWrapper.class)) {
      InspectionValidator.EP_NAME.extensions(myProject).forEach(
        validator -> compilers.add(compilerClass.cast(InspectionValidatorWrapper.create(myProject, validator)))
      );
    }
    final T[] array = ArrayUtil.newArray(compilerClass, compilers.size());
    return compilers.toArray(array);
  }

  @Override
  public void addCompilableFileType(@NotNull FileType type) {
    myCompilableTypes.add(type);
  }

  @Override
  public void removeCompilableFileType(@NotNull FileType type) {
    myCompilableTypes.remove(type);
  }

  @Override
  public boolean isCompilableFileType(@NotNull FileType type) {
    if (myCompilableTypes.contains(type)) {
      return true;
    }
    Set<FileType> types = myCachedCompilableTypes;
    if (types == null) {
      types = new HashSet<>();
      for (CompilableFileTypesProvider extension : COMPILABLE_TYPE_EP.getExtensions(myProject)) {
        types.addAll(extension.getCompilableFileTypes());
      }
      for (BackendCompiler compiler : BackendCompiler.EP_NAME.getExtensions(myProject)) {
        types.addAll(compiler.getCompilableFileTypes());
      }
      myCachedCompilableTypes = types;
    }
    return types.contains(type);
  }

  @Override
  public final void addBeforeTask(@NotNull CompileTask task) {
    myBeforeTasks.add(task);
  }

  @Override
  public final void addAfterTask(@NotNull CompileTask task) {
    myAfterTasks.add(task);
  }

  @Override
  public @NotNull List<CompileTask> getBeforeTasks() {
    final List<Compiler> extCompilers = Compiler.EP_NAME.getExtensions(myProject);
    return ContainerUtil.concat(
      myBeforeTasks,
      extCompilers.stream().filter(compiler -> compiler instanceof SourceInstrumentingCompiler).map(compiler -> new FileProcessingCompilerAdapterTask((SourceInstrumentingCompiler)compiler)).collect(Collectors.toList()),
      getExtensionsTasks(CompileTaskBean.CompileTaskExecutionPhase.BEFORE)
    );
  }

  @Override
  public @NotNull List<CompileTask> getAfterTaskList() {
    final List<Compiler> extCompilers = Compiler.EP_NAME.getExtensions(myProject);
    return ContainerUtil.concat(
      myAfterTasks,
      extCompilers.stream().filter(compiler -> compiler instanceof Validator).map(compiler -> new FileProcessingCompilerAdapterTask((Validator)compiler)).collect(Collectors.toList()),
      InspectionValidator.EP_NAME.extensions(myProject).map(validator -> new FileProcessingCompilerAdapterTask(InspectionValidatorWrapper.create(myProject, validator))).collect(Collectors.toList()),
      getExtensionsTasks(CompileTaskBean.CompileTaskExecutionPhase.AFTER)
    );
  }

  private @NotNull List<CompileTask> getExtensionsTasks(@NotNull CompileTaskBean.CompileTaskExecutionPhase phase) {
    List<CompileTask> list = new ArrayList<>();
    COMPILER_TASK_EP.processWithPluginDescriptor(myProject, (ext, pluginDescriptor) -> {
      if (ext.executionPhase == phase) {
        list.add(ext.getInstance(myProject, pluginDescriptor));
      }
    });
    return list;
  }

  @Override
  public void compile(VirtualFile @NotNull [] files, CompileStatusNotification callback) {
    compile(createFilesCompileScope(files), callback);
  }

  @Override
  public void compile(@NotNull Module module, CompileStatusNotification callback) {
    new CompileDriver(myProject).compile(createModuleCompileScope(module, false), new ListenerNotificator(callback));
  }

  @Override
  public void compile(@NotNull CompileScope scope, CompileStatusNotification callback) {
    new CompileDriver(myProject).compile(scope, new ListenerNotificator(callback));
  }

  @Override
  public void make(CompileStatusNotification callback) {
    new CompileDriver(myProject).make(createProjectCompileScope(myProject), new ListenerNotificator(callback));
  }

  @Override
  public void make(@NotNull Module module, CompileStatusNotification callback) {
    new CompileDriver(myProject).make(createModuleCompileScope(module, true), new ListenerNotificator(callback));
  }

  @Override
  public void make(@NotNull Project project, Module @NotNull [] modules, CompileStatusNotification callback) {
    new CompileDriver(myProject).make(createModuleGroupCompileScope(project, modules, true), new ListenerNotificator(callback));
  }

  @Override
  public void make(@NotNull CompileScope scope, CompileStatusNotification callback) {
    new CompileDriver(myProject).make(scope, new ListenerNotificator(callback));
  }

  @Override
  public void makeWithModalProgress(@NotNull CompileScope scope, @Nullable CompileStatusNotification callback) {
    new CompileDriver(myProject).make(scope, true, new ListenerNotificator(callback));
  }

  @Override
  public boolean isUpToDate(@NotNull CompileScope scope) {
    return new CompileDriver(myProject).isUpToDate(scope);
  }

  @Override
  public void rebuild(CompileStatusNotification callback) {
    new CompileDriver(myProject).rebuild(new ListenerNotificator(callback));
  }

  @Override
  public void executeTask(@NotNull CompileTask task, @NotNull CompileScope scope, String contentName, Runnable onTaskFinished) {
    final CompileDriver compileDriver = new CompileDriver(myProject);
    compileDriver.executeCompileTask(task, scope, contentName, onTaskFinished);
  }

  private final Map<CompilationStatusListener, MessageBusConnection> myListenerAdapters = new HashMap<>();

  @Override
  public void addCompilationStatusListener(@NotNull CompilationStatusListener listener) {
    final MessageBusConnection connection = myProject.getMessageBus().connect();
    myListenerAdapters.put(listener, connection);
    connection.subscribe(CompilerTopics.COMPILATION_STATUS, listener);
  }

  @Override
  public void addCompilationStatusListener(@NotNull CompilationStatusListener listener, @NotNull Disposable parentDisposable) {
    final MessageBusConnection connection = myProject.getMessageBus().connect(parentDisposable);
    connection.subscribe(CompilerTopics.COMPILATION_STATUS, listener);
  }

  @Override
  public void removeCompilationStatusListener(final @NotNull CompilationStatusListener listener) {
    final MessageBusConnection connection = myListenerAdapters.remove(listener);
    if (connection != null) {
      connection.disconnect();
    }
  }

  @Override
  public boolean isExcludedFromCompilation(@NotNull VirtualFile file) {
    return CompilerConfiguration.getInstance(myProject).isExcludedFromCompilation(file);
  }

  @Override
  public @NotNull CompileScope createFilesCompileScope(final VirtualFile @NotNull [] files) {
    CompileScope[] scopes = new CompileScope[files.length];
    for(int i = 0; i < files.length; i++){
      scopes[i] = new OneProjectItemCompileScope(myProject, files[i]);
    }
    return new CompositeScope(scopes);
  }

  @Override
  public @NotNull CompileScope createModuleCompileScope(final @NotNull Module module, final boolean includeDependentModules) {
    return createModulesCompileScope(new Module[] {module}, includeDependentModules);
  }

  @Override
  public @NotNull CompileScope createModulesCompileScope(final Module @NotNull [] modules, final boolean includeDependentModules) {
    return createModulesCompileScope(modules, includeDependentModules, false);
  }

  @Override
  public @NotNull CompileScope createModulesCompileScope(Module @NotNull [] modules, boolean includeDependentModules, boolean includeRuntimeDependencies) {
    return new ModuleCompileScope(myProject, modules, includeDependentModules, includeRuntimeDependencies);
  }

  @Override
  public @NotNull CompileScope createModuleGroupCompileScope(final @NotNull Project project, final Module @NotNull [] modules, final boolean includeDependentModules) {
    return new ModuleCompileScope(project, modules, includeDependentModules);
  }

  @Override
  public @NotNull CompileScope createProjectCompileScope(final @NotNull Project project) {
    return new ProjectCompileScope(project);
  }

  @Override
  public void setValidationEnabled(ModuleType<?> moduleType, boolean enabled) {
    if (enabled) {
      myValidationDisabledModuleTypes.remove(moduleType);
    }
    else {
      myValidationDisabledModuleTypes.add(moduleType);
    }
  }

  @Override
  public boolean isValidationEnabled(Module module) {
    if (myValidationDisabledModuleTypes.isEmpty()) {
      return true; // optimization
    }
    return !myValidationDisabledModuleTypes.contains(ModuleType.get(module));
  }

  @Override
  public Collection<ClassObject> compileJavaCode(List<String> options,
                                                 Collection<? extends File> platformCp,
                                                 Collection<? extends File> classpath,
                                                 Collection<? extends File> upgradeModulePath,
                                                 Collection<? extends File> modulePath,
                                                 Collection<? extends File> sourcePath,
                                                 Collection<? extends File> files,
                                                 File outputDir) throws IOException, CompilationException {
    final Pair<Sdk, JavaSdkVersion> runtime = BuildManager.getJavacRuntimeSdk(myProject);

    final Sdk sdk = runtime.getFirst();
    final SdkTypeId type = sdk.getSdkType();
    String javaHome = null;
    if (type instanceof JavaSdkType) {
      javaHome = sdk.getHomePath();
      if (!isJdkOrJre(javaHome)) {
        // this can be a java-dependent SDK, implementing JavaSdkType
        // hack, because there is no direct way to obtain the java sdk, this sdk depends on
        final String binPath = ((JavaSdkType)type).getBinPath(sdk);
        javaHome = binPath != null? new File(binPath).getParent() : null;
        if (!isJdkOrJre(javaHome)) {
          javaHome = null;
        }
      }
    }
    if (javaHome == null) {
      throw new IOException("Was not able to determine JDK for project " + myProject.getName());
    }

    final OutputCollector outputCollector = new OutputCollector();
    DiagnosticCollector diagnostic = new DiagnosticCollector();

    final Set<File> sourceRoots = new THashSet<>(FileUtil.FILE_HASHING_STRATEGY);
    if (!sourcePath.isEmpty()) {
      sourceRoots.addAll(sourcePath);
    }
    else {
      for (File file : files) {
        final File parentFile = file.getParentFile();
        if (parentFile != null) {
          sourceRoots.add(parentFile);
        }
      }
    }
    final Map<File, Set<File>> outs = Collections.singletonMap(outputDir, sourceRoots);

    final ExternalJavacManager javacManager = getJavacManager();
    final CompilationPaths paths = CompilationPaths.create(platformCp, classpath, upgradeModulePath, ModulePath.create(modulePath), sourcePath);
    // do not keep process alive in tests since every test expects all spawned processes to terminate in teardown
    boolean compiledOk = javacManager != null && javacManager.forkJavac(
      javaHome, -1, Collections.emptyList(), options, paths, files, outs, diagnostic, outputCollector,
      new JavacCompilerTool(), CanceledStatus.NULL, !ApplicationManager.getApplication().isUnitTestMode()
    ).get();

    if (!compiledOk) {
      final List<CompilationException.Message> messages = new SmartList<>();
      for (Diagnostic<? extends JavaFileObject> d : diagnostic.getDiagnostics()) {
        final JavaFileObject source = d.getSource();
        final URI uri = source != null ? source.toUri() : null;
        messages.add(new CompilationException.Message(
          kindToCategory(d.getKind()), d.getMessage(Locale.US), uri != null? uri.toURL().toString() : null, (int)d.getLineNumber(), (int)d.getColumnNumber()
        ));
      }
      throw new CompilationException("Compilation failed", messages);
    }

    final List<ClassObject> result = new ArrayList<>();
    for (OutputFileObject fileObject : outputCollector.getCompiledClasses()) {
      final BinaryContent content = fileObject.getContent();
      result.add(new CompiledClass(fileObject.getName(), fileObject.getClassName(), content != null ? content.toByteArray() : null));
    }
    return result;
  }

  private static boolean isJdkOrJre(@Nullable String path) {
    return path != null && (JdkUtil.checkForJre(path) || JdkUtil.checkForJdk(path));
  }

  private static CompilerMessageCategory kindToCategory(Diagnostic.Kind kind) {
    switch (kind) {
      case ERROR: return CompilerMessageCategory.ERROR;
      case MANDATORY_WARNING:
      case WARNING: return CompilerMessageCategory.WARNING;
      case NOTE:
      default: return CompilerMessageCategory.INFORMATION;
    }
  }


  private @Nullable ExternalJavacManager getJavacManager() throws IOException {
    ExternalJavacManager manager = myExternalJavacManager;
    if (manager == null) {
      synchronized (this) {
        manager = myExternalJavacManager;
        if (manager == null) {
          final File compilerWorkingDir = getJavacCompilerWorkingDir();
          if (compilerWorkingDir == null) {
            return null; // should not happen for real projects
          }
          final int listenPort = NetUtils.findAvailableSocketPort();
          manager = new ExternalJavacManager(
            compilerWorkingDir, ProcessIOExecutorService.INSTANCE, Registry.intValue("compiler.external.javac.keep.alive.timeout", 5*60*1000)
          );
          manager.start(listenPort);
          myExternalJavacManager = manager;
          IdeEventQueue.getInstance().addIdleListener(new IdleTask(manager), IdleTask.CHECK_PERIOD);
        }
      }
    }
    return manager;
  }

  @Override
  public @Nullable File getJavacCompilerWorkingDir() {
    final File projectBuildDir = BuildManager.getInstance().getProjectSystemDirectory(myProject);
    if (projectBuildDir == null) {
      return null;
    }
    projectBuildDir.mkdirs();
    return projectBuildDir;
  }

  private static class CompiledClass implements ClassObject {
    private final String myPath;
    private final String myClassName;
    private final byte[] myBytes;

    CompiledClass(String path, String className, byte[] bytes) {
      myPath = path;
      myClassName = className;
      myBytes = bytes;
    }

    @Override
    public String getPath() {
      return myPath;
    }

    @Override
    public String getClassName() {
      return myClassName;
    }

    @Override
    public byte @Nullable [] getContent() {
      return myBytes;
    }

    @Override
    public String toString() {
      return getClassName();
    }
  }

  private class ListenerNotificator implements CompileStatusNotification {
    private final @Nullable CompileStatusNotification myDelegate;

    private ListenerNotificator(@Nullable CompileStatusNotification delegate) {
      myDelegate = delegate;
    }

    @Override
    public void finished(boolean aborted, int errors, int warnings, final @NotNull CompileContext compileContext) {
      if (!myProject.isDisposed()) {
        myEventPublisher.compilationFinished(aborted, errors, warnings, compileContext);
      }
      if (myDelegate != null) {
        myDelegate.finished(aborted, errors, warnings, compileContext);
      }
    }
  }

  private static class DiagnosticCollector implements DiagnosticOutputConsumer {
    private final List<Diagnostic<? extends JavaFileObject>> myDiagnostics = new ArrayList<>();
    @Override
    public void outputLineAvailable(String line) {
      // for debugging purposes uncomment this line
      //System.out.println(line);
      if (line != null && line.startsWith(ExternalJavacManager.STDERR_LINE_PREFIX)) {
        LOG.info(line.trim());
      }
    }

    @Override
    public void registerJavacFileData(JavacFileData data) {
      // ignore
    }

    @Override
    public void javaFileLoaded(File file) {
      // ignore
    }

    @Override
    public void customOutputData(String pluginId, String dataName, byte[] data) {
    }

    @Override
    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
      myDiagnostics.add(diagnostic);
    }

    public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
      return myDiagnostics;
    }
  }


  private static class OutputCollector implements OutputFileConsumer {
    private final List<OutputFileObject> myClasses = new ArrayList<>();

    @Override
    public void save(@NotNull OutputFileObject fileObject) {
      myClasses.add(fileObject);
    }

    List<OutputFileObject> getCompiledClasses() {
      return myClasses;
    }
  }

  private static class IdleTask implements Runnable {
    private static final int CHECK_PERIOD = 10000; // check idle javac processes every 10 second when IDE is idle
    private final ExternalJavacManager myManager;

    IdleTask(@NotNull ExternalJavacManager manager) {
      myManager = manager;
    }

    @Override
    public void run() {
      if (myManager.isRunning()) {
        myManager.shutdownIdleProcesses();
      }
      else {
        IdeEventQueue.getInstance().removeIdleListener(this);
      }
    }
  }
}