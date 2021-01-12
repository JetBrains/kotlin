// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve;

import org.jetbrains.annotations.NonNls;

/**
 * @author Vladislav.Soroka
 */
public final class GradleCommonClassNames {
  @NonNls public static final String GRADLE_API_SCRIPT = "org.gradle.api.Script";
  @NonNls public static final String GRADLE_API_PROJECT = "org.gradle.api.Project";
  @NonNls public static final String GRADLE_API_BASE_PLUGIN_CONVENTION = "org.gradle.api.plugins.BasePluginConvention";
  @NonNls public static final String GRADLE_API_JAVA_PLUGIN_CONVENTION = "org.gradle.api.plugins.JavaPluginConvention";
  @NonNls public static final String GRADLE_API_APPLICATION_PLUGIN_CONVENTION = "org.gradle.api.plugins.ApplicationPluginConvention";
  @NonNls public static final String GRADLE_API_WAR_CONVENTION = "org.gradle.api.plugins.WarPluginConvention";
  @NonNls public static final String GRADLE_API_CONFIGURATION_CONTAINER = "org.gradle.api.artifacts.ConfigurationContainer";
  @NonNls public static final String GRADLE_API_DEPENDENCY_SUBSTITUTIONS = "org.gradle.api.artifacts.DependencySubstitutions";
  @NonNls public static final String GRADLE_API_RESOLUTION_STRATEGY = "org.gradle.api.artifacts.ResolutionStrategy";
  @NonNls public static final String GRADLE_API_CONFIGURATION = "org.gradle.api.artifacts.Configuration";
  @NonNls public static final String GRADLE_API_ARTIFACT_HANDLER = "org.gradle.api.artifacts.dsl.ArtifactHandler";
  @NonNls public static final String GRADLE_API_PUBLISH_ARTIFACT = "org.gradle.api.artifacts.PublishArtifact";
  @NonNls public static final String GRADLE_API_CONFIGURABLE_PUBLISH_ARTIFACT = "org.gradle.api.artifacts.ConfigurablePublishArtifact";
  @NonNls public static final String GRADLE_API_PUBLICATION_CONTAINER = "org.gradle.api.publish.PublicationContainer";
  @NonNls public static final String GRADLE_API_DEPENDENCY_HANDLER = "org.gradle.api.artifacts.dsl.DependencyHandler";
  @NonNls public static final String GRADLE_API_COMPONENT_METADATA_HANDLER = "org.gradle.api.artifacts.dsl.ComponentMetadataHandler";
  @NonNls public static final String GRADLE_API_COMPONENT_MODULE_METADATA_HANDLER = "org.gradle.api.artifacts.dsl.ComponentModuleMetadataHandler";
  @NonNls public static final String GRADLE_API_COMPONENT_MODULE_METADATA_DETAILS = "org.gradle.api.artifacts.ComponentModuleMetadataDetails";
  @NonNls public static final String GRADLE_API_ARTIFACTS_EXTERNAL_MODULE_DEPENDENCY = "org.gradle.api.artifacts.ExternalModuleDependency";
  @NonNls public static final String GRADLE_API_ARTIFACTS_PROJECT_DEPENDENCY = "org.gradle.api.artifacts.ProjectDependency";
  @NonNls public static final String GRADLE_API_ARTIFACTS_SELF_RESOLVING_DEPENDENCY = "org.gradle.api.artifacts.SelfResolvingDependency";
  @NonNls public static final String GRADLE_API_ARTIFACTS_CLIENT_MODULE_DEPENDENCY = "org.gradle.api.artifacts.ClientModule";
  @NonNls public static final String GRADLE_API_ARTIFACTS_MODULE_DEPENDENCY = "org.gradle.api.artifacts.ModuleDependency";
  @NonNls public static final String GRADLE_API_ARTIFACTS_DEPENDENCY = "org.gradle.api.artifacts.Dependency";
  @NonNls public static final String GRADLE_API_ARTIFACTS_DEPENDENCY_ARTIFACT = "org.gradle.api.artifacts.DependencyArtifact";
  @NonNls public static final String GRADLE_API_REPOSITORY_HANDLER = "org.gradle.api.artifacts.dsl.RepositoryHandler";
  @NonNls public static final String GRADLE_API_PUBLISHING_EXTENSION = "org.gradle.api.publish.PublishingExtension";
  @NonNls public static final String GRADLE_API_SOURCE_DIRECTORY_SET = "org.gradle.api.file.SourceDirectorySet";
  @NonNls public static final String GRADLE_API_SOURCE_SET = "org.gradle.api.tasks.SourceSet";
  @NonNls public static final String GRADLE_API_SOURCE_SET_CONTAINER = "org.gradle.api.tasks.SourceSetContainer";
  @NonNls public static final String GRADLE_API_DISTRIBUTION_CONTAINER = "org.gradle.api.distribution.DistributionContainer";
  @NonNls public static final String GRADLE_API_DISTRIBUTION = "org.gradle.api.distribution.Distribution";
  @NonNls public static final String GRADLE_API_FILE_COPY_SPEC = "org.gradle.api.file.CopySpec";
  @NonNls public static final String GRADLE_API_FILE_FILE_COLLECTION = "org.gradle.api.file.FileCollection";
  @NonNls public static final String GRADLE_API_FILE_CONFIGURABLE_FILE_TREE = "org.gradle.api.file.ConfigurableFileTree";
  @NonNls public static final String GRADLE_API_FILE_CONFIGURABLE_FILE_COLLECTION = "org.gradle.api.file.ConfigurableFileCollection";
  @NonNls public static final String GRADLE_API_SCRIPT_HANDLER = "org.gradle.api.initialization.dsl.ScriptHandler";
  @NonNls public static final String GRADLE_API_TASK = "org.gradle.api.Task";
  @NonNls public static final String GRADLE_API_DEFAULT_TASK = "org.gradle.api.DefaultTask";
  @NonNls public static final String GRADLE_API_TASKS_ACTION = "org.gradle.api.tasks.TaskAction";
  @NonNls public static final String GRADLE_API_TASKS_DELETE = "org.gradle.api.tasks.Delete";
  @NonNls public static final String GRADLE_JVM_TASKS_JAR = "org.gradle.jvm.tasks.Jar";
  @NonNls public static final String GRADLE_API_TASKS_BUNDLING_JAR = "org.gradle.api.tasks.bundling.Jar";
  @NonNls public static final String GRADLE_API_TASKS_BUNDLING_WAR = "org.gradle.api.tasks.bundling.War";
  @NonNls public static final String GRADLE_API_TASKS_COMPILE_JAVA_COMPILE = "org.gradle.api.tasks.compile.JavaCompile";
  @NonNls public static final String GRADLE_API_TASKS_WRAPPER_WRAPPER = "org.gradle.api.tasks.wrapper.Wrapper";
  @NonNls public static final String GRADLE_API_TASKS_JAVADOC_JAVADOC = "org.gradle.api.tasks.javadoc.Javadoc";
  @NonNls public static final String GRADLE_API_TASKS_DIAGNOSTICS_DEPENDENCY_REPORT_TASK = "org.gradle.api.tasks.diagnostics.DependencyReportTask";
  @NonNls public static final String GRADLE_API_TASKS_DIAGNOSTICS_DEPENDENCY_INSIGHT_REPORT_TASK = "org.gradle.api.tasks.diagnostics.DependencyInsightReportTask";
  @NonNls public static final String GRADLE_API_TASKS_DIAGNOSTICS_PROJECT_REPORT_TASK = "org.gradle.api.tasks.diagnostics.ProjectReportTask";
  @NonNls public static final String GRADLE_API_TASKS_DIAGNOSTICS_PROPERTY_REPORT_TASK = "org.gradle.api.tasks.diagnostics.PropertyReportTask";
  @NonNls public static final String GRADLE_API_TASKS_DIAGNOSTICS_TASK_REPORT_TASK = "org.gradle.api.tasks.diagnostics.TaskReportTask";
  @NonNls public static final String GRADLE_API_TASKS_TESTING_TEST = "org.gradle.api.tasks.testing.Test";
  @NonNls public static final String GRADLE_API_JUNIT_OPTIONS = "org.gradle.api.tasks.testing.junit.JUnitOptions";
  @NonNls public static final String GRADLE_API_TEST_LOGGING_CONTAINER = "org.gradle.api.tasks.testing.logging.TestLoggingContainer";
  @NonNls public static final String GRADLE_API_TASKS_UPLOAD = "org.gradle.api.tasks.Upload";
  @NonNls public static final String GRADLE_LANGUAGE_JVM_TASKS_PROCESS_RESOURCES = "org.gradle.language.jvm.tasks.ProcessResources";
  @NonNls public static final String GRADLE_BUILDSETUP_TASKS_SETUP_BUILD = "org.gradle.buildsetup.tasks.SetupBuild";
  @NonNls public static final String GRADLE_API_TASK_CONTAINER = "org.gradle.api.tasks.TaskContainer";
  @NonNls public static final String GRADLE_API_JAVA_ARCHIVES_MANIFEST = "org.gradle.api.java.archives.Manifest";
  @NonNls public static final String GRADLE_API_DOMAIN_OBJECT_COLLECTION = "org.gradle.api.DomainObjectCollection";
  @NonNls public static final String GRADLE_API_NAMED_DOMAIN_OBJECT_COLLECTION = "org.gradle.api.NamedDomainObjectCollection";
  @NonNls public static final String GRADLE_API_NAMED_DOMAIN_OBJECT_CONTAINER = "org.gradle.api.NamedDomainObjectContainer";
  @NonNls public static final String GRADLE_API_ARTIFACTS_REPOSITORIES_ARTIFACT_REPOSITORY = "org.gradle.api.artifacts.repositories.ArtifactRepository";
  @NonNls public static final String GRADLE_API_ARTIFACTS_REPOSITORIES_MAVEN_ARTIFACT_REPOSITORY = "org.gradle.api.artifacts.repositories.MavenArtifactRepository";
  @NonNls public static final String GRADLE_API_ARTIFACTS_REPOSITORIES_IVY_ARTIFACT_REPOSITORY = "org.gradle.api.artifacts.repositories.IvyArtifactRepository";
  @NonNls public static final String GRADLE_API_ARTIFACTS_REPOSITORIES_FLAT_DIRECTORY_ARTIFACT_REPOSITORY = "org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository";
  @NonNls public static final String GRADLE_API_ARTIFACTS_MAVEN_MAVEN_DEPLOYER = "org.gradle.api.artifacts.maven.MavenDeployer";
  @NonNls public static final String GRADLE_API_PLUGINS_MAVEN_REPOSITORY_HANDLER_CONVENTION = "org.gradle.api.plugins.MavenRepositoryHandlerConvention";
  @NonNls public static final String GRADLE_API_INITIALIZATION_SETTINGS = "org.gradle.api.initialization.Settings";
  @NonNls public static final String GRADLE_API_EXTRA_PROPERTIES_EXTENSION = "org.gradle.api.plugins.ExtraPropertiesExtension";
  @NonNls public static final String GRADLE_PROCESS_EXEC_SPEC = "org.gradle.process.ExecSpec";

  private GradleCommonClassNames() {
  }
}
