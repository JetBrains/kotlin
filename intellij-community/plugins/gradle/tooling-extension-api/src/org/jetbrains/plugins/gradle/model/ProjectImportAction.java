// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.util.Pair;
import com.intellij.util.ExceptionUtilRt;
import gnu.trove.THashSet;
import org.gradle.api.Action;
import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.UnknownModelException;
import org.gradle.tooling.UnsupportedVersionException;
import org.gradle.tooling.internal.adapter.ProtocolToModelAdapter;
import org.gradle.tooling.internal.adapter.TargetTypeProvider;
import org.gradle.tooling.internal.gradle.DefaultBuildIdentifier;
import org.gradle.tooling.internal.gradle.DefaultProjectIdentifier;
import org.gradle.tooling.model.*;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.tooling.model.gradle.BasicGradleProject;
import org.gradle.tooling.model.gradle.GradleBuild;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.ProjectImportModelProvider.BuildModelConsumer;
import org.jetbrains.plugins.gradle.model.ProjectImportModelProvider.ProjectModelConsumer;
import org.jetbrains.plugins.gradle.model.internal.DummyModel;
import org.jetbrains.plugins.gradle.model.internal.TurnOffDefaultTasks;
import org.jetbrains.plugins.gradle.tooling.Exceptions;
import org.jetbrains.plugins.gradle.tooling.serialization.SerializationService;
import org.jetbrains.plugins.gradle.tooling.serialization.ToolingSerializer;
import org.jetbrains.plugins.gradle.tooling.serialization.internal.IdeaProjectSerializationService;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public class ProjectImportAction implements BuildAction<ProjectImportAction.AllModels>, Serializable {
  private final Set<ProjectImportModelProvider> myProjectsLoadedModelProviders = new THashSet<ProjectImportModelProvider>();
  private final Set<ProjectImportModelProvider> myBuildFinishedModelProviders = new THashSet<ProjectImportModelProvider>();
  private final Set<Class<?>> myTargetTypes = new THashSet<Class<?>>();
  private final boolean myIsPreviewMode;
  private final boolean myIsCompositeBuildsSupported;
  private final boolean myUseCustomSerialization;
  private boolean myUseProjectsLoadedPhase;
  private AllModels myAllModels = null;
  @Nullable
  private transient GradleBuild myGradleBuild;
  private ToolingSerializerAdapter mySerializer;

  public ProjectImportAction(boolean isPreviewMode) {
    this(isPreviewMode, false, false);
  }

  public ProjectImportAction(boolean isPreviewMode,
                             boolean isCompositeBuildsSupported,
                             boolean useCustomSerialization) {
    myIsPreviewMode = isPreviewMode;
    myIsCompositeBuildsSupported = isCompositeBuildsSupported;
    myUseCustomSerialization = useCustomSerialization;
  }

  public void addProjectImportModelProvider(@NotNull ProjectImportModelProvider provider) {
    addProjectImportModelProvider(provider, false);
  }

  public void addProjectImportModelProvider(@NotNull ProjectImportModelProvider provider, boolean isProjectLoadedProvider) {
    if (isProjectLoadedProvider) {
      myProjectsLoadedModelProviders.add(provider);
    }
    else {
      myBuildFinishedModelProviders.add(provider);
    }
  }

  public void addTargetTypes(@NotNull Set<Class<?>> targetTypes) {
    myTargetTypes.addAll(targetTypes);
  }

  public void prepareForPhasedExecuter() {
    myUseProjectsLoadedPhase = true;
  }

  public void prepareForNonPhasedExecuter() {
    myUseProjectsLoadedPhase = false;
  }

  @Nullable
  @Override
  public AllModels execute(BuildController controller) {
    configureAdditionalTypes(controller);
    boolean isProjectsLoadedAction = myAllModels == null && myUseProjectsLoadedPhase;
    if (isProjectsLoadedAction || !myUseProjectsLoadedPhase) {
      long startTime = System.currentTimeMillis();
      myGradleBuild = controller.getBuildModel();
      AllModels allModels = new AllModels(convert(myGradleBuild));
      allModels.logPerformance("Get model GradleBuild", System.currentTimeMillis() - startTime);
      long startTimeBuildEnv = System.currentTimeMillis();
      BuildEnvironment buildEnvironment = controller.findModel(BuildEnvironment.class);
      allModels.setBuildEnvironment(buildEnvironment);
      allModels.logPerformance("Get model BuildEnvironment", System.currentTimeMillis() - startTimeBuildEnv);
      myAllModels = allModels;
      mySerializer = new ToolingSerializerAdapter(controller);
    }

    assert myGradleBuild != null;
    assert mySerializer != null;
    controller = new MyBuildController(controller, myGradleBuild);
    for (BasicGradleProject gradleProject : myGradleBuild.getProjects()) {
      addProjectModels(mySerializer, controller, myAllModels, gradleProject, isProjectsLoadedAction);
    }
    addBuildModels(mySerializer, controller, myAllModels, myGradleBuild, isProjectsLoadedAction);

    if (myIsCompositeBuildsSupported) {
      for (GradleBuild includedBuild : myGradleBuild.getIncludedBuilds()) {
        if (!isProjectsLoadedAction) {
          myAllModels.getIncludedBuilds().add(convert(includedBuild));
        }
        for (BasicGradleProject project : includedBuild.getProjects()) {
          addProjectModels(mySerializer, controller, myAllModels, project, isProjectsLoadedAction);
        }
        addBuildModels(mySerializer, controller, myAllModels, includedBuild, isProjectsLoadedAction);
      }
    }
    if (isProjectsLoadedAction) {
      controller.getModel(TurnOffDefaultTasks.class);
    }
    return isProjectsLoadedAction && !myAllModels.hasModels() ? null : myAllModels;
  }

  @NotNull
  private static Build convert(@NotNull GradleBuild build) {
    DefaultBuild rootProject = new DefaultBuild(build.getRootProject().getName(), build.getBuildIdentifier().getRootDir());
    for (BasicGradleProject project : build.getProjects()) {
      rootProject.addProject(project.getName(), project.getProjectIdentifier());
    }
    return rootProject;
  }

  private void configureAdditionalTypes(BuildController controller) {
    if (myTargetTypes.isEmpty()) return;

    try {
      Field adapterField;
      try {
        adapterField = controller.getClass().getDeclaredField("adapter");
      }
      catch (NoSuchFieldException e) {
        // since v.4.4 there is a BuildControllerWithoutParameterSupport can be used
        Field delegate = controller.getClass().getDeclaredField("delegate");
        delegate.setAccessible(true);
        Object wrappedController = delegate.get(controller);
        adapterField = wrappedController.getClass().getDeclaredField("adapter");
        controller = (BuildController)wrappedController;
      }
      adapterField.setAccessible(true);
      ProtocolToModelAdapter adapter = (ProtocolToModelAdapter)adapterField.get(controller);

      Field typeProviderField = adapter.getClass().getDeclaredField("targetTypeProvider");
      typeProviderField.setAccessible(true);
      TargetTypeProvider typeProvider = (TargetTypeProvider)typeProviderField.get(adapter);

      Field targetTypesField = typeProvider.getClass().getDeclaredField("configuredTargetTypes");
      targetTypesField.setAccessible(true);
      //noinspection unchecked
      Map<String, Class<?>> targetTypes = (Map<String, Class<?>>)targetTypesField.get(typeProvider);

      for (Class<?> targetType : myTargetTypes) {
        targetTypes.put(targetType.getCanonicalName(), targetType);
      }
    }
    catch (Exception ignore) {
      // TODO handle error
    }
  }

  private void addProjectModels(@NotNull final ToolingSerializerAdapter serializerAdapter,
                                @NotNull BuildController controller,
                                @NotNull final AllModels allModels,
                                @NotNull final BasicGradleProject project,
                                boolean isProjectsLoadedAction) {
    try {
      Set<ProjectImportModelProvider> modelProviders = getModelProviders(isProjectsLoadedAction);
      for (ProjectImportModelProvider extension : modelProviders) {
        final Set<String> obtainedModels = new THashSet<String>();
        long startTime = System.currentTimeMillis();
        ProjectModelConsumer modelConsumer = new ProjectModelConsumer() {
          @Override
          public void consume(@NotNull Object object, @NotNull Class clazz) {
            if (myUseCustomSerialization) {
              object = serializerAdapter.serialize(object);
            }
            allModels.addModel(object, clazz, project);
            obtainedModels.add(clazz.getName());
          }
        };
        extension.populateProjectModels(controller, project, modelConsumer);
        allModels.logPerformance(
          "Ran extension " + extension.getClass().getName() +
          " for project " + project.getProjectIdentifier().getProjectPath() +
          " obtained " + obtainedModels.size() + " model(s): " + joinClassNamesToString(obtainedModels),
          System.currentTimeMillis() - startTime);
      }
    }
    catch (Exception e) {
      // do not fail project import in a preview mode
      if (!myIsPreviewMode) {
        throw new ExternalSystemException(e);
      }
    }
  }

  private void addBuildModels(@NotNull final ToolingSerializerAdapter serializerAdapter,
                              @NotNull BuildController controller,
                              @NotNull final AllModels allModels,
                              @NotNull final GradleBuild buildModel,
                              boolean isProjectsLoadedAction) {
    try {
      Set<ProjectImportModelProvider> modelProviders = getModelProviders(isProjectsLoadedAction);
      for (ProjectImportModelProvider extension : modelProviders) {
        final Set<String> obtainedModels = new HashSet<String>();
        long startTime = System.currentTimeMillis();
        BuildModelConsumer modelConsumer = new BuildModelConsumer() {
          @Override
          public void consumeProjectModel(@NotNull ProjectModel projectModel, @NotNull Object object, @NotNull Class clazz) {
            if (myUseCustomSerialization) {
              object = serializerAdapter.serialize(object);
            }
            allModels.addModel(object, clazz, projectModel);
            obtainedModels.add(clazz.getName());
          }

          @Override
          public void consume(@NotNull BuildModel buildModel, @NotNull Object object, @NotNull Class clazz) {
            if (myUseCustomSerialization) {
              object = serializerAdapter.serialize(object);
            }
            allModels.addModel(object, clazz, buildModel);
            obtainedModels.add(clazz.getName());
          }
        };
        extension.populateBuildModels(controller, buildModel, modelConsumer);
        allModels.logPerformance(
          "Ran extension " +
          extension.getClass().getName() +
          " for build " + buildModel.getBuildIdentifier().getRootDir().getPath() +
          " obtained " + obtainedModels.size() + " model(s): " + joinClassNamesToString(obtainedModels),
          System.currentTimeMillis() - startTime);
      }
    }
    catch (Exception e) {
      // do not fail project import in a preview mode
      if (!myIsPreviewMode) {
        throw new ExternalSystemException(e);
      }
    }
  }

  private Set<ProjectImportModelProvider> getModelProviders(boolean isProjectsLoadedAction) {
    Set<ProjectImportModelProvider> modelProviders = new LinkedHashSet<ProjectImportModelProvider>();
    if (!myUseProjectsLoadedPhase) {
      modelProviders.addAll(myProjectsLoadedModelProviders);
      modelProviders.addAll(myBuildFinishedModelProviders);
    }
    else {
      modelProviders = isProjectsLoadedAction ? myProjectsLoadedModelProviders : myBuildFinishedModelProviders;
    }
    return modelProviders;
  }

  @NotNull
  private static String joinClassNamesToString(@NotNull Set<String> names) {
    StringBuilder sb = new StringBuilder();
    for (Iterator<String> it = names.iterator(); it.hasNext();) {
      sb.append(it.next());
      if (it.hasNext()) {
        sb.append(", ");
      }
    }

    return sb.toString();
  }

  public static class AllModels extends ModelsHolder<BuildModel, ProjectModel> {
    @NotNull private final List<Build> includedBuilds = new ArrayList<Build>();
    private final Map<String, Long> performanceTrace = new LinkedHashMap<String, Long>();

    public AllModels(@NotNull Build mainBuild) {
      super(mainBuild);
    }

    public AllModels(@NotNull IdeaProject ideaProject) {
      super(new LegacyIdeaProjectModelAdapter(ideaProject));
      addModel(ideaProject, IdeaProject.class);
    }

    /**
     * @deprecated use {@link #getModel(Class<IdeaProject>)}
     */
    @NotNull
    @Deprecated
    public IdeaProject getIdeaProject() {
      IdeaProject ideaProject = getModel(IdeaProject.class);
      assert ideaProject != null;
      return ideaProject;
    }

    @NotNull
    public Build getMainBuild() {
      return (Build)getRootModel();
    }

    @NotNull
    public List<Build> getIncludedBuilds() {
      return includedBuilds;
    }

    @Nullable
    public BuildEnvironment getBuildEnvironment() {
      return getModel(BuildEnvironment.class);
    }

    public void setBuildEnvironment(@Nullable BuildEnvironment buildEnvironment) {
      if (buildEnvironment != null) {
        addModel(buildEnvironment, BuildEnvironment.class);
      }
    }

    public void logPerformance(@NotNull final String description, long millis) {
      performanceTrace.put(description, millis);
    }

    public Map<String, Long> getPerformanceTrace() {
      return performanceTrace;
    }
  }

  private static class ToolingSerializerAdapter {
    private final Object mySerializer;
    private final Method mySerializerWriteMethod;
    private final ClassLoader myModelBuildersClassLoader;

    private ToolingSerializerAdapter(@NotNull BuildController controller) {
      Object unpacked = new ProtocolToModelAdapter().unpack(controller.getModel(DummyModel.class));
      myModelBuildersClassLoader = unpacked.getClass().getClassLoader();
      try {
        Class<?> toolingSerializerClass = myModelBuildersClassLoader.loadClass(ToolingSerializer.class.getName());
        mySerializer = toolingSerializerClass.newInstance();
        mySerializerWriteMethod = toolingSerializerClass.getMethod("write", Object.class, Class.class);
        registerIdeaProjectSerializationService(toolingSerializerClass);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    // support custom serialization of the gradle built-in IdeaProject model
    private void registerIdeaProjectSerializationService(Class<?> toolingSerializerClass)
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
      Class<?> serializationServiceClass = myModelBuildersClassLoader.loadClass(SerializationService.class.getName());
      final IdeaProjectSerializationService ideaProjectService = new IdeaProjectSerializationService(getBuildGradleVersion());
      Method register = toolingSerializerClass.getMethod("register", serializationServiceClass);
      Object proxyInstance =
        Proxy.newProxyInstance(myModelBuildersClassLoader, new Class<?>[]{serializationServiceClass}, new InvocationHandler() {
          @Override
          public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return ideaProjectService.getClass().getMethod(method.getName(), method.getParameterTypes()).invoke(ideaProjectService, args);
          }
        });
      register.invoke(mySerializer, proxyInstance);
    }

    @NotNull
    private GradleVersion getBuildGradleVersion() {
      try {
        Class<?> gradleVersionClass = myModelBuildersClassLoader.loadClass("org.gradle.util.GradleVersion");
        Object buildGradleVersion = gradleVersionClass.getMethod("current").invoke(gradleVersionClass);
        return GradleVersion.version(gradleVersionClass.getMethod("getVersion").invoke(buildGradleVersion).toString());
      }
      catch (Exception e) {
        ExceptionUtilRt.rethrowUnchecked(e);
        throw new RuntimeException(e);
      }
    }

    @Nullable
    private Pair<Object, ? extends Class<?>> prepare(Object object) {
      Class<?> modelClazz;
      if (object instanceof IdeaProject) { // support custom serialization of the gradle built-in IdeaProject model
        modelClazz = IdeaProject.class;
      }
      else {
        try {
          object = new ProtocolToModelAdapter().unpack(object);
        }
        catch (IllegalArgumentException ignore) {
        }
        modelClazz = object.getClass();
        if (modelClazz.getClassLoader() != myModelBuildersClassLoader) {
          //The object has not been created by custom model builders
          return null;
        }
      }
      return Pair.pair(object, modelClazz);
    }

    private Object serialize(Object object) {
      try {
        Pair<Object, ? extends Class<?>> preparedObject = prepare(object);
        if (preparedObject != null) {
          return mySerializerWriteMethod.invoke(mySerializer, preparedObject.first, preparedObject.second);
        }
      }
      catch (Exception e) {
        Throwable unwrap = Exceptions.unwrap(e);
        if (object instanceof IdeaProject) {
          ExceptionUtilRt.rethrowUnchecked(unwrap);
          throw new RuntimeException(unwrap);
        }
        //noinspection UseOfSystemOutOrSystemErr
        System.err.println(ExceptionUtilRt.getThrowableText(unwrap, "org.jetbrains."));
      }
      return object;
    }
  }

  private final static class DefaultBuild implements Build, Serializable {
    private final String myName;
    private final DefaultBuildIdentifier myBuildIdentifier;
    private final Collection<Project> myProjects = new ArrayList<Project>(0);

    private DefaultBuild(String name, File rootDir) {
      myName = name;
      myBuildIdentifier = new DefaultBuildIdentifier(rootDir);
    }

    @Override
    public String getName() {
      return myName;
    }

    @Override
    public BuildIdentifier getBuildIdentifier() {
      return myBuildIdentifier;
    }

    @Override
    public Collection<Project> getProjects() {
      return myProjects;
    }

    private void addProject(String name, final ProjectIdentifier projectIdentifier) {
      final String projectPath = projectIdentifier.getProjectPath();
      File rootDir = myBuildIdentifier.getRootDir();
      assert rootDir.getPath().equals(projectIdentifier.getBuildIdentifier().getRootDir().getPath());
      myProjects.add(new DefaultProjectModel(name, rootDir, projectPath));
    }

    private final static class DefaultProjectModel implements Project, Serializable {
      private final String myName;
      private final DefaultProjectIdentifier myProjectIdentifier;

      private DefaultProjectModel(@NotNull String name, @NotNull File rootDir, @NotNull String projectPath) {
        myName = name;
        myProjectIdentifier = new DefaultProjectIdentifier(rootDir, projectPath);
      }

      @Override
      public String getName() {
        return myName;
      }

      @Override
      public ProjectIdentifier getProjectIdentifier() {
        return myProjectIdentifier;
      }

      @Override
      public String toString() {
        return "ProjectModel{" +
               "name='" + myName + '\'' +
               ", id=" + myProjectIdentifier +
               '}';
      }
    }
  }

  private final static class MyBuildController implements BuildController {
    private final BuildController myDelegate;
    private final GradleBuild myMainGradleBuild;
    private final Model myMyMainGradleBuildRootProject;

    private MyBuildController(@NotNull BuildController buildController, @NotNull GradleBuild mainGradleBuild) {
      myDelegate = buildController;
      myMainGradleBuild = mainGradleBuild;
      myMyMainGradleBuildRootProject = myMainGradleBuild.getRootProject();
    }

    @Override
    public <T> T getModel(Class<T> aClass) throws UnknownModelException {
      if (aClass == GradleBuild.class) {
        //noinspection unchecked
        return (T)myMainGradleBuild;
      }
      return myDelegate.getModel(myMyMainGradleBuildRootProject, aClass);
    }

    @Override
    public <T> T findModel(Class<T> aClass) {
      if (aClass == GradleBuild.class) {
        //noinspection unchecked
        return (T)myMainGradleBuild;
      }
      return myDelegate.findModel(myMyMainGradleBuildRootProject, aClass);
    }

    @Override
    public GradleBuild getBuildModel() {
      return myMainGradleBuild;
    }

    @Override
    public <T> T getModel(Model model, Class<T> aClass) throws UnknownModelException {
      if (isMainBuild(model)) {
        return getModel(aClass);
      }
      else {
        return myDelegate.getModel(model, aClass);
      }
    }

    @Override
    public <T> T findModel(Model model, Class<T> aClass) {
      if (isMainBuild(model)) {
        return findModel(aClass);
      }
      else {
        return myDelegate.findModel(model, aClass);
      }
    }

    @Override
    public <T, P> T getModel(Class<T> aClass, Class<P> aClass1, Action<? super P> action)
      throws UnsupportedVersionException {
      return myDelegate.getModel(myMyMainGradleBuildRootProject, aClass, aClass1, action);
    }

    @Override
    public <T, P> T findModel(Class<T> aClass, Class<P> aClass1, Action<? super P> action) {
      return myDelegate.findModel(myMyMainGradleBuildRootProject, aClass, aClass1, action);
    }

    @Override
    public <T, P> T getModel(Model model, Class<T> aClass, Class<P> aClass1, Action<? super P> action)
      throws UnsupportedVersionException {
      if (isMainBuild(model)) {
        return getModel(aClass, aClass1, action);
      }
      else {
        return myDelegate.getModel(model, aClass, aClass1, action);
      }
    }

    @Override
    public <T, P> T findModel(Model model, Class<T> aClass, Class<P> aClass1, Action<? super P> action) {
      if (isMainBuild(model)) {
        return findModel(aClass, aClass1, action);
      }
      else {
        return myDelegate.findModel(model, aClass, aClass1, action);
      }
    }

    private boolean isMainBuild(Model model) {
      return model == null || model == myMainGradleBuild;
    }
  }
}
