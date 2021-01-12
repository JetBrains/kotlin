// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.settings;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.config.GradleSettingsListenerAdapter;
import org.jetbrains.plugins.gradle.model.ExternalTask;
import org.jetbrains.plugins.gradle.model.GradleExtensions;
import org.jetbrains.plugins.gradle.model.GradleProperty;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil;
import org.jetbrains.plugins.gradle.service.project.data.GradleExtensionsDataService;
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public class GradleExtensionsSettings {

  private static final Logger LOG = Logger.getInstance(GradleExtensionsSettings.class);
  private final Settings myState = new Settings();

  public GradleExtensionsSettings(Project project) {
    ExternalSystemApiUtil.subscribe(project, GradleConstants.SYSTEM_ID, new GradleSettingsListenerAdapter() {
      @Override
      public void onProjectsUnlinked(@NotNull Set<String> linkedProjectPaths) {
        myState.remove(linkedProjectPaths);
      }
    });
  }

  @NotNull
  public static Settings getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, GradleExtensionsSettings.class).myState;
  }

  public static void load(Project project) {
    final Collection<ExternalProjectInfo> projectsData =
      ProjectDataManager.getInstance().getExternalProjectsData(project, GradleConstants.SYSTEM_ID);
    for (ExternalProjectInfo projectInfo : projectsData) {
      DataNode<ProjectData> projectDataNode = projectInfo.getExternalProjectStructure();
      if (projectDataNode == null) continue;

      String projectPath = projectInfo.getExternalProjectPath();
      try {
        Collection<DataNode<GradleExtensions>> nodes = new SmartList<>();
        for (DataNode<ModuleData> moduleNode : ExternalSystemApiUtil.findAll(projectDataNode, ProjectKeys.MODULE)) {
          ContainerUtil.addIfNotNull(nodes, ExternalSystemApiUtil.find(moduleNode, GradleExtensionsDataService.KEY));
        }
        getInstance(project).add(projectPath, nodes);
      }
      catch (ClassCastException e) {
        // catch deserialization issue caused by fast serializer
        LOG.debug(e);
        ExternalProjectsManager.getInstance(project).getExternalProjectsWatcher().markDirty(projectPath);
      }
    }
  }

  public static class Settings {
    public Map<String, GradleProject> projects = new HashMap<>();

    public void add(@NotNull String rootPath,
                    @NotNull Collection<DataNode<GradleExtensions>> extensionsData) {
      Map<String, GradleExtensions> extensionMap = new HashMap<>();
      for (DataNode<GradleExtensions> node : extensionsData) {
        DataNode<?> parent = node.getParent();
        if (parent == null) continue;
        if (!(parent.getData() instanceof ModuleData)) continue;
        String gradlePath = GradleProjectResolverUtil.getGradlePath((ModuleData)parent.getData());
        extensionMap.put(gradlePath, node.getData());
      }

      add(rootPath, extensionMap);
    }

    public void add(@NotNull String rootPath, @NotNull Map<String, GradleExtensions> extensions) {
      GradleProject gradleProject = new GradleProject();
      for (Map.Entry<String, GradleExtensions> entry : extensions.entrySet()) {
        GradleExtensionsData extensionsData = new GradleExtensionsData();
        GradleExtensions gradleExtensions = entry.getValue();
        extensionsData.parent = gradleExtensions.getParentProjectPath();

        for (org.jetbrains.plugins.gradle.model.GradleExtension extension : gradleExtensions.getExtensions()) {
          GradleExtension gradleExtension = new GradleExtension();
          gradleExtension.name = extension.getName();
          gradleExtension.rootTypeFqn = extension.getTypeFqn();
          extensionsData.extensions.put(extension.getName(), gradleExtension);
        }
        for (org.jetbrains.plugins.gradle.model.GradleConvention convention : gradleExtensions.getConventions()) {
          GradleConvention gradleConvention = new GradleConvention();
          gradleConvention.name = convention.getName();
          gradleConvention.typeFqn = convention.getTypeFqn();
          extensionsData.conventions.add(gradleConvention);
        }
        for (GradleProperty property : gradleExtensions.getGradleProperties()) {
          GradleProp gradleProp = new GradleProp();
          gradleProp.name = property.getName();
          gradleProp.typeFqn = property.getTypeFqn();
          extensionsData.properties.put(gradleProp.name, gradleProp);
        }
        for (ExternalTask task : gradleExtensions.getTasks()) {
          GradleTask gradleTask = new GradleTask();
          gradleTask.name = task.getName();
          String type = task.getType();
          if (type != null) {
            gradleTask.typeFqn = type;
          }

          StringBuilder description = new StringBuilder();
          if (task.getDescription() != null) {
            description.append(task.getDescription());
            if (task.getGroup() != null) {
              description.append("<p>");
            }
          }
          if (task.getGroup() != null) {
            description.append("<i>Task group: ").append(task.getGroup()).append("<i>");
          }

          gradleTask.description = description.toString();
          extensionsData.tasksMap.put(gradleTask.name, gradleTask);
        }
        extensionsData.tasks = new SmartList<>(extensionsData.tasksMap.values());
        for (org.jetbrains.plugins.gradle.model.GradleConfiguration configuration : gradleExtensions.getConfigurations()) {
          GradleConfiguration gradleConfiguration = new GradleConfiguration();
          gradleConfiguration.name = configuration.getName();
          gradleConfiguration.description = configuration.getDescription();
          gradleConfiguration.visible = configuration.isVisible();
          gradleConfiguration.scriptClasspath = configuration.isScriptClasspathConfiguration();
          if (gradleConfiguration.scriptClasspath) {
            extensionsData.buildScriptConfigurations.put(configuration.getName(), gradleConfiguration);
          }
          else {
            extensionsData.configurations.put(configuration.getName(), gradleConfiguration);
          }
        }
        gradleProject.extensions.put(entry.getKey(), extensionsData);
        extensionsData.myGradleProject = gradleProject;
      }

      Map<String, GradleProject> projects = new HashMap<>(this.projects);
      projects.put(rootPath, gradleProject);
      this.projects = projects;
    }

    public void remove(Set<String> rootPaths) {
      Map<String, GradleProject> projects = new HashMap<>(this.projects);
      for (String path : rootPaths) {
        projects.remove(path);
      }
      this.projects = projects;
    }

    /**
     * Returns extensions available in the context of the gradle project related to the IDE module.
     */
    @Nullable
    public GradleExtensionsData getExtensionsFor(@Nullable Module module) {
      if (module == null) return null;
      return getExtensionsFor(ExternalSystemApiUtil.getExternalRootProjectPath(module),
                              GradleProjectResolverUtil.getGradlePath(module));
    }

    /**
     * Returns extensions available in the context of the specified (using gradle path notation, e.g. `:sub-project`) gradle project.
     *
     * @param rootProjectPath file path of the root gradle project
     * @param gradlePath      gradle project path notation
     * @return gradle extensions
     */
    @Nullable
    public GradleExtensionsData getExtensionsFor(@Nullable String rootProjectPath, @Nullable String gradlePath) {
      GradleProject gradleProject = getRootGradleProject(rootProjectPath);
      if (gradleProject == null) return null;
      return gradleProject.extensions.get(gradlePath);
    }

    @Contract("null -> null")
    @Nullable
    public GradleProject getRootGradleProject(@Nullable String rootProjectPath) {
      if (rootProjectPath == null) return null;
      return projects.get(rootProjectPath);
    }
  }

  public static class GradleProject {
    public Map<String, GradleExtensionsData> extensions = new HashMap<>();
  }

  public static class GradleExtensionsData {
    private GradleProject myGradleProject;
    public String parent;
    @NotNull
    public final Map<String, GradleExtension> extensions = new HashMap<>();
    @NotNull
    public final List<GradleConvention> conventions = new SmartList<>();
    @NotNull
    public final Map<String, GradleProp> properties = new HashMap<>();
    @NotNull
    public final Map<String, GradleTask> tasksMap = new LinkedHashMap<>();
    /**
     * @deprecated to be removed, use {@link GradleExtensionsData#tasksMap} instead
     */
    @Deprecated
    public List<GradleTask> tasks = Collections.emptyList();
    @NotNull
    public final Map<String, GradleConfiguration> configurations = new HashMap<>();
    @NotNull
    public final Map<String, GradleConfiguration> buildScriptConfigurations = new HashMap<>();

    @Nullable
    public GradleExtensionsData getParent() {
      if (myGradleProject == null) return null;
      return myGradleProject.extensions.get(parent);
    }

    @Nullable
    public GradleProp findProperty(@Nullable String name) {
      return findProperty(this, name);
    }

    @NotNull
    public Collection<GradleProp> findAllProperties() {
      return findAllProperties(this, new HashMap<>());
    }

    @NotNull
    private static Collection<GradleProp> findAllProperties(@NotNull GradleExtensionsData extensionsData,
                                                            @NotNull Map<String, GradleProp> result) {
      for (GradleProp property : extensionsData.properties.values()) {
        result.putIfAbsent(property.name, property);
      }
      if (extensionsData.getParent() != null) {
        findAllProperties(extensionsData.getParent(), result);
      }
      return result.values();
    }

    @Nullable
    private static GradleProp findProperty(@NotNull GradleExtensionsData extensionsData, String propName) {
      GradleProp prop = extensionsData.properties.get(propName);
      if (prop != null) return prop;
      if (extensionsData.parent != null && extensionsData.myGradleProject != null) {
        GradleExtensionsData parentData = extensionsData.myGradleProject.extensions.get(extensionsData.parent);
        if (parentData != null) {
          return findProperty(parentData, propName);
        }
      }
      return null;
    }
  }

  public interface TypeAware {
    String getTypeFqn();
  }

  public static class GradleExtension implements TypeAware {
    public String name;
    public String rootTypeFqn = CommonClassNames.JAVA_LANG_OBJECT_SHORT;

    @Override
    public String getTypeFqn() {
      return rootTypeFqn;
    }
  }

  public static class GradleConvention implements TypeAware {
    public String name;
    public String typeFqn = CommonClassNames.JAVA_LANG_OBJECT_SHORT;

    @Override
    public String getTypeFqn() {
      return typeFqn;
    }
  }

  public static class GradleProp implements TypeAware {
    public String name;
    public String typeFqn = CommonClassNames.JAVA_LANG_STRING;
    @Nullable
    public String value;

    @Override
    public String getTypeFqn() {
      return typeFqn;
    }
  }

  public static class GradleTask implements TypeAware {
    public String name;
    public String typeFqn = GradleCommonClassNames.GRADLE_API_DEFAULT_TASK;
    @Nullable
    public String description;

    @Override
    public String getTypeFqn() {
      return typeFqn;
    }
  }

  public static class GradleConfiguration {
    public String name;
    public boolean visible = true;
    public boolean scriptClasspath;
    public String description;
  }

  @Nullable
  public static GradleProject getRootProject(@NotNull PsiElement element) {
    final PsiFile containingFile = element.getContainingFile().getOriginalFile();
    final Project project = containingFile.getProject();
    return getInstance(project).getRootGradleProject(getRootProjectPath(element));
  }

  @Nullable
  public static String getRootProjectPath(@NotNull PsiElement element) {
    final PsiFile containingFile = element.getContainingFile().getOriginalFile();
    final Module module = ModuleUtilCore.findModuleForFile(containingFile);
    return ExternalSystemApiUtil.getExternalRootProjectPath(module);
  }
}
