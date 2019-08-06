/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.gradle.model;

import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.internal.adapter.ProtocolToModelAdapter;
import org.gradle.tooling.internal.adapter.TargetTypeProvider;
import org.gradle.tooling.internal.gradle.DefaultBuildIdentifier;
import org.gradle.tooling.model.BuildIdentifier;
import org.gradle.tooling.model.BuildModel;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.ProjectModel;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.tooling.model.gradle.BasicGradleProject;
import org.gradle.tooling.model.gradle.GradleBuild;
import org.gradle.tooling.model.idea.BasicIdeaProject;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.ProjectImportModelProvider.BuildModelConsumer;
import org.jetbrains.plugins.gradle.model.ProjectImportModelProvider.ProjectModelConsumer;
import org.jetbrains.plugins.gradle.tooling.serialization.ToolingSerializer;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public class ProjectImportAction implements BuildAction<ProjectImportAction.AllModels>, Serializable {
  private final Set<ProjectImportModelProvider> myProjectsLoadedModelProviders = new LinkedHashSet<ProjectImportModelProvider>();
  private final Set<ProjectImportModelProvider> myBuildFinishedModelProviders = new LinkedHashSet<ProjectImportModelProvider>();
  private final Set<Class> myTargetTypes = new LinkedHashSet<Class>();
  private final boolean myIsPreviewMode;
  private final boolean myIsCompositeBuildsSupported;
  private final boolean myUseCustomSerialization;
  private boolean myUseProjectsLoadedPhase;
  private AllModels myAllModels = null;
  @Nullable
  private transient GradleBuild myGradleBuild;

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

  public void addTargetTypes(@NotNull Set<Class> targetTypes) {
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
  public AllModels execute(final BuildController controller) {
    configureAdditionalTypes(controller);
    boolean isProjectsLoadedAction = myAllModels == null && myUseProjectsLoadedPhase;
    if (isProjectsLoadedAction || !myUseProjectsLoadedPhase) {
      long startTime = System.currentTimeMillis();
      myGradleBuild = controller.getBuildModel();
      AllModels allModels = new AllModels(new DefaultBuildModel(myGradleBuild.getBuildIdentifier().getRootDir()));
      allModels.logPerformance("Get model GradleBuild", System.currentTimeMillis() - startTime);
      long startTimeBuildEnv = System.currentTimeMillis();
      BuildEnvironment buildEnvironment = controller.findModel(BuildEnvironment.class);
      allModels.setBuildEnvironment(buildEnvironment);
      allModels.logPerformance("Get model BuildEnvironment", System.currentTimeMillis() - startTimeBuildEnv);
      myAllModels = allModels;
    }
    if (!isProjectsLoadedAction) {
      long startTimeIdeaProject = System.currentTimeMillis();
      final IdeaProject ideaProject = myIsPreviewMode ?
                                      controller.getModel(BasicIdeaProject.class) : controller.getModel(IdeaProject.class);
      assert ideaProject != null && !ideaProject.getModules().isEmpty();
      myAllModels.logPerformance("Get model IdeaProject" + (myIsPreviewMode ? " (preview mode)" : ""),
                                 System.currentTimeMillis() - startTimeIdeaProject);
      myAllModels.setIdeaProject(ideaProject);
    }

    assert myGradleBuild != null;
    ToolingSerializerAdapter serializerHolder = new ToolingSerializerAdapter();
    for (BasicGradleProject gradleProject : myGradleBuild.getProjects()) {
      addProjectModels(serializerHolder, controller, myAllModels, gradleProject, isProjectsLoadedAction);
    }
    addBuildModels(serializerHolder, controller, myAllModels, myGradleBuild, isProjectsLoadedAction);

    if (myIsCompositeBuildsSupported) {
      for (GradleBuild includedBuild : myGradleBuild.getIncludedBuilds()) {
        if (!isProjectsLoadedAction) {
          IdeaProject ideaIncludedProject = myIsPreviewMode
                                            ? controller.findModel(includedBuild, BasicIdeaProject.class)
                                            : controller.findModel(includedBuild, IdeaProject.class);
          myAllModels.getIncludedBuilds().add(ideaIncludedProject);
        }
        for (BasicGradleProject project : includedBuild.getProjects()) {
          addProjectModels(serializerHolder, controller, myAllModels, project, isProjectsLoadedAction);
        }
        addBuildModels(serializerHolder, controller, myAllModels, includedBuild, isProjectsLoadedAction);
      }
    }

    return isProjectsLoadedAction && myAllModels.hasModels() ? null : myAllModels;
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
        final Set<String> obtainedModels = new HashSet<String>();
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
    private final List<IdeaProject> includedBuilds = new ArrayList<IdeaProject>();
    private final Map<String, Long> performanceTrace = new LinkedHashMap<String, Long>();
    private IdeaProject myIdeaProject;

    public AllModels(@NotNull BuildModel rootProjectModel) {
      super(rootProjectModel);
    }

    public AllModels(@NotNull IdeaProject ideaProject) {
      super(new IdeaProjectBuildModelAdapter(ideaProject));
      setIdeaProject(ideaProject);
    }

    @NotNull
    public IdeaProject getIdeaProject() {
      return myIdeaProject;
    }

    private void setIdeaProject(@NotNull IdeaProject ideaProject) {
      myIdeaProject = ideaProject;
    }

    public List<IdeaProject> getIncludedBuilds() {
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

    private static class IdeaProjectBuildModelAdapter implements BuildModel {
      private final BuildIdentifier myBuildIdentifier;

      private IdeaProjectBuildModelAdapter(@NotNull IdeaProject ideaProject) {
        DomainObjectSet<? extends IdeaModule> ideaModules = ideaProject.getChildren();
        assert !ideaModules.isEmpty();
        IdeaModule ideaModule = ideaModules.getAt(0);
        myBuildIdentifier = ideaModule.getGradleProject().getProjectIdentifier().getBuildIdentifier();
      }

      @Override
      public BuildIdentifier getBuildIdentifier() {
        return myBuildIdentifier;
      }
    }
  }

  private static class ToolingSerializerAdapter {
    private ClassLoader myModelBuildersClassLoader;
    private Object mySerializer;
    private Method mySerializerWriteMethod;

    private Object prepare(Object object) throws IOException {
      try {
        object = new ProtocolToModelAdapter().unpack(object);
      }
      catch (IllegalArgumentException ignore) {
      }

      try {
        ClassLoader objectClassLoader = object.getClass().getClassLoader();
        if (myModelBuildersClassLoader == null) {
          Class<?> toolingSerializerClass = objectClassLoader.loadClass(ToolingSerializer.class.getName());
          mySerializer = toolingSerializerClass.newInstance();
          mySerializerWriteMethod = toolingSerializerClass.getMethod("write", Object.class, Class.class);
          myModelBuildersClassLoader = objectClassLoader;
        }
        else if (objectClassLoader != myModelBuildersClassLoader) {
          //The object has not been created by custom model builders
          return null;
        }
      }
      catch (ClassNotFoundException e) {
        // The object has not been created by custom model builders
        return null;
      }
      catch (Exception e) {
        throw new IOException(e);
      }

      return object;
    }

    private Object serialize(Object object) {
      try {
        Object preparedObject = prepare(object);
        if (preparedObject != null) {
          return mySerializerWriteMethod.invoke(mySerializer, preparedObject, preparedObject.getClass());
        }
      }
      catch (Exception e) {
        //noinspection UseOfSystemOutOrSystemErr
        System.err.println(e.getMessage());
      }
      return object;
    }
  }

  private final static class DefaultBuildModel implements BuildModel, Serializable {
    private final File myRootDir;

    private DefaultBuildModel(File rootDir) {myRootDir = rootDir;}

    @Override
    public BuildIdentifier getBuildIdentifier() {
      return new DefaultBuildIdentifier(myRootDir);
    }
  }
}
