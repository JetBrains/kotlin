// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.integrations.javaee;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashMap;
import org.gradle.tooling.model.idea.IdeaModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.model.ExternalDependency;
import org.jetbrains.plugins.gradle.model.ExternalProject;
import org.jetbrains.plugins.gradle.model.ExternalSourceSet;
import org.jetbrains.plugins.gradle.model.data.*;
import org.jetbrains.plugins.gradle.model.ear.EarConfiguration;
import org.jetbrains.plugins.gradle.model.web.WebConfiguration;
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension;
import org.jetbrains.plugins.gradle.service.project.LibraryDataNodeSubstitutor;
import org.jetbrains.plugins.gradle.service.project.ProjectResolverContext;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.util.*;

import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.findAllRecursively;
import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.toCanonicalPath;
import static org.jetbrains.plugins.gradle.service.project.GradleProjectResolver.*;
import static org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil.buildDependencies;

/**
 * {@link JavaEEGradleProjectResolverExtension} provides JavaEE project info based on gradle tooling API models.
 *
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemConstants.UNORDERED)
public class JavaEEGradleProjectResolverExtension extends AbstractProjectResolverExtension {
  private static final Key<Map<String/* artifact path */, String /* module id*/>> ARCHIVES_ARTIFACTS = Key.create("archivesArtifactsMap");
  private static final Key<List<Pair<DataNode<? extends ModuleData>, EarConfiguration>>> EAR_CONFIGURATIONS =
    Key.create("earConfigurations");

  @Override
  public void populateModuleExtraModels(@NotNull IdeaModule gradleModule, @NotNull final DataNode<ModuleData> ideModule) {
    DataNode<ProjectData> projectDataNode = ideModule.getParent(ProjectData.class);
    NotNullLazyValue<DataNode<? extends ModuleData>> findTargetModuleNode = new NotNullLazyValue<DataNode<? extends ModuleData>>() {
      @NotNull
      @Override
      protected DataNode<? extends ModuleData> compute() {
        final String mainSourceSetModuleId = ideModule.getData().getId() + ":main";
        DataNode<? extends ModuleData> targetModuleNode =
          ExternalSystemApiUtil.find(ideModule, GradleSourceSetData.KEY, node -> mainSourceSetModuleId.equals(node.getData().getId()));
        if (targetModuleNode == null) {
          targetModuleNode = ideModule;
        }
        return targetModuleNode;
      }
    };

    Map<String/* artifact path */, String /* module id*/> archivesMap;
    if (projectDataNode != null) {
      archivesMap = projectDataNode.getUserData(ARCHIVES_ARTIFACTS);
      if (archivesMap == null) {
        archivesMap = new THashMap<>(FileUtil.PATH_HASHING_STRATEGY);
        projectDataNode.putUserData(ARCHIVES_ARTIFACTS, archivesMap);
      }

      ExternalProject externalProject = resolverCtx.getExtraProject(gradleModule, ExternalProject.class);
      if (externalProject != null) {
        if (externalProject.getArtifactsByConfiguration().get("archives") != null) {
          final Set<File> archivesArtifacts = new HashSet<>(externalProject.getArtifactsByConfiguration().get("archives"));
          final Set<File> testsArtifacts = externalProject.getArtifactsByConfiguration().get("tests");
          if (testsArtifacts != null) {
            archivesArtifacts.removeAll(testsArtifacts);
          }

          for (File artifactFile : archivesArtifacts) {
            archivesMap.put(toCanonicalPath(artifactFile.getAbsolutePath()), findTargetModuleNode.getValue().getData().getId());
          }
        }
      }
    }

    final WebConfiguration webConfiguration = resolverCtx.getExtraProject(gradleModule, WebConfiguration.class);

    if (webConfiguration != null) {
      final List<War> warModels = ContainerUtil.map(webConfiguration.getWarModels(), (Function<WebConfiguration.WarModel, War>)model -> {
        War war = new War(model.getWarName(), model.getWebAppDirName(), model.getWebAppDir());
        war.setWebXml(model.getWebXml());
        war.setWebResources(mapWebResources(model.getWebResources()));
        war.setClasspath(model.getClasspath());
        war.setManifestContent(model.getManifestContent());
        war.setArchivePath(model.getArchivePath());
        return war;
      });
      findTargetModuleNode.getValue().createChild(
        WebConfigurationModelData.KEY, new WebConfigurationModelData(GradleConstants.SYSTEM_ID, warModels));
    }

    final EarConfiguration earConfiguration = resolverCtx.getExtraProject(gradleModule, EarConfiguration.class);
    if (earConfiguration != null) {
      if (projectDataNode != null) {
        List<Pair<DataNode<? extends ModuleData>, EarConfiguration>> ears = projectDataNode.getUserData(EAR_CONFIGURATIONS);
        if (ears == null) {
          ears = new ArrayList<>();
          projectDataNode.putUserData(EAR_CONFIGURATIONS, ears);
        }
        ears.add(Pair.create(findTargetModuleNode.getValue(), earConfiguration));
      }
    }
    nextResolver.populateModuleExtraModels(gradleModule, ideModule);
  }

  @NotNull
  @Override
  public Set<Class> getExtraProjectModelClasses() {
    return ContainerUtil.set(WebConfiguration.class, EarConfiguration.class);
  }

  @Override
  public void resolveFinished(@NotNull DataNode<ProjectData> projectDataNode) {
    List<Pair<DataNode<? extends ModuleData>, EarConfiguration>> earConfigurations = projectDataNode.getUserData(EAR_CONFIGURATIONS);
    if (earConfigurations == null) return;
    for (Pair<DataNode<? extends ModuleData>, EarConfiguration> pair : earConfigurations) {
      DataNode<? extends ModuleData> moduleNode = pair.first;
      EarConfiguration earConfiguration = pair.second;
      final List<Ear> warModels = ContainerUtil.map(earConfiguration.getEarModels(), (Function<EarConfiguration.EarModel, Ear>)model -> {
        Ear ear = new Ear(model.getEarName(), model.getAppDirName(), model.getLibDirName());
        ear.setManifestContent(model.getManifestContent());
        ear.setDeploymentDescriptor(model.getDeploymentDescriptor());
        ear.setResources(mapEarResources(model.getResources()));
        ear.setArchivePath(model.getArchivePath());
        return ear;
      });

      final Collection<DependencyData> deployDependencies = getDependencies(
        resolverCtx, projectDataNode, moduleNode, earConfiguration.getDeployDependencies());
      final Collection<DependencyData> earlibDependencies = getDependencies(
        resolverCtx, projectDataNode, moduleNode, earConfiguration.getEarlibDependencies());
      moduleNode.createChild(EarConfigurationModelData.KEY,
                             new EarConfigurationModelData(GradleConstants.SYSTEM_ID, warModels, deployDependencies, earlibDependencies));
    }
  }

  private static List<WebResource> mapWebResources(List<WebConfiguration.WebResource> webResources) {
    return ContainerUtil.mapNotNull(webResources, resource -> {
      if (resource == null) return null;

      final WarDirectory warDirectory = WarDirectory.fromPath(resource.getWarDirectory());
      return new WebResource(warDirectory, resource.getRelativePath(), resource.getFile());
    });
  }

  private static List<EarResource> mapEarResources(List<EarConfiguration.EarResource> resources) {
    return ContainerUtil.mapNotNull(resources, resource -> {
      if (resource == null) return null;

      return new EarResource(resource.getEarDirectory(), resource.getRelativePath(), resource.getFile());
    });
  }


  @SuppressWarnings("unchecked")
  private static Collection<DependencyData> getDependencies(@NotNull ProjectResolverContext resolverCtx,
                                                            @NotNull DataNode<ProjectData> ideProject,
                                                            @NotNull DataNode<? extends ModuleData> moduleDataNode,
                                                            @NotNull Collection<ExternalDependency> dependencies)
    throws IllegalStateException {

    final Map<String, Pair<String, ExternalSystemSourceType>> moduleOutputsMap =
      ideProject.getUserData(MODULES_OUTPUTS);
    assert moduleOutputsMap != null;

    final Map<String, Pair<DataNode<GradleSourceSetData>, ExternalSourceSet>> sourceSetMap =
      ideProject.getUserData(RESOLVED_SOURCE_SETS);
    assert sourceSetMap != null;

    final Map<String, String> artifactsMap = ideProject.getUserData(CONFIGURATION_ARTIFACTS);
    assert artifactsMap != null;

    final Map<String, String> allArtifactsMap;
    final Map<String, String> archivesMap = ideProject.getUserData(ARCHIVES_ARTIFACTS);
    if (archivesMap != null && !archivesMap.isEmpty()) {
      allArtifactsMap = new THashMap<>(FileUtil.PATH_HASHING_STRATEGY);
      allArtifactsMap.putAll(artifactsMap);
      allArtifactsMap.putAll(archivesMap);
    }
    else {
      allArtifactsMap = artifactsMap;
    }

    DataNode fakeNode = new DataNode(moduleDataNode.getKey(), moduleDataNode.getData(), null);
    buildDependencies(resolverCtx, sourceSetMap, allArtifactsMap, fakeNode, dependencies, null);

    LibraryDataNodeSubstitutor librarySubstitutor =
      new LibraryDataNodeSubstitutor(resolverCtx, null, null, null, sourceSetMap, moduleOutputsMap, artifactsMap);
    final Collection<DataNode<LibraryDependencyData>> libraryDependencies = findAllRecursively(fakeNode, ProjectKeys.LIBRARY_DEPENDENCY);
    for (DataNode<LibraryDependencyData> libraryDependencyDataNode : libraryDependencies) {
      librarySubstitutor.run(libraryDependencyDataNode);
    }

    final Collection<DataNode<?>> dataNodes = findAllRecursively(fakeNode, node -> node.getData() instanceof DependencyData);
    return ContainerUtil.map(dataNodes, node -> (DependencyData)node.getData());
  }
}
