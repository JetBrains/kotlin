// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project

import com.amazon.ion.IonType
import com.google.gson.GsonBuilder
import com.intellij.execution.configurations.SimpleJavaParameters
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.Consumer
import com.intellij.util.net.HttpConfigurable
import gnu.trove.THash
import org.codehaus.groovy.runtime.typehandling.ShortTypeHandling
import org.gradle.internal.impldep.com.google.common.collect.Multimap
import org.gradle.tooling.model.build.BuildEnvironment
import org.gradle.tooling.model.idea.IdeaModule
import org.gradle.tooling.model.idea.IdeaProject
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.gradle.model.ProjectImportAction
import org.jetbrains.plugins.gradle.tooling.internal.init.Init
import kotlin.Int.Companion.MAX_VALUE

@ApiStatus.Internal
@Order(MAX_VALUE)
internal class BaseResolverExtension : GradleProjectResolverExtension {
  override fun setProjectResolverContext(projectResolverContext: ProjectResolverContext) {}
  override fun getNext(): GradleProjectResolverExtension? = null
  override fun setNext(projectResolverExtension: GradleProjectResolverExtension) {}   // should be the last extension in the chain
  override fun populateProjectExtraModels(gradleProject: IdeaProject, ideProject: DataNode<ProjectData>) {}
  override fun createModule(gradleModule: IdeaModule, projectDataNode: DataNode<ProjectData>): DataNode<ModuleData>? = null
  override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {}
  override fun populateModuleContentRoots(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {}
  override fun populateModuleCompileOutputSettings(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {}
  override fun populateModuleDependencies(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>, ideProject: DataNode<ProjectData>) {}
  override fun populateModuleTasks(gradleModule: IdeaModule,
                                   ideModule: DataNode<ModuleData>,
                                   ideProject: DataNode<ProjectData>): Collection<TaskData> = emptyList()

  override fun getExtraProjectModelClasses(): Set<Class<*>> = emptySet()
  override fun getToolingExtensionsClasses(): Set<Class<*>> {
    return linkedSetOf(
      ExternalSystemSourceType::class.java, // external-system-rt.jar
      ProjectImportAction::class.java,  // gradle-tooling-extension-api jar
      Init::class.java,  // gradle-tooling-extension-impl jar
      Multimap::class.java, // repacked gradle guava
      GsonBuilder::class.java,
      ShortTypeHandling::class.java,
      THash::class.java,  // trove4j jar
      IonType::class.java,  // ion-java jar
      SystemInfoRt::class.java // util-rt jat, !!! do not replace it with SystemInfo.class from util module
    )
  }

  override fun getExtraJvmArgs(): List<Pair<String, String>> {
    val extraJvmArgs = mutableListOf<Pair<String, String>>()
    val httpConfigurable = HttpConfigurable.getInstance()
    if (!httpConfigurable.PROXY_EXCEPTIONS.isNullOrEmpty()) {
      val hosts = StringUtil.split(httpConfigurable.PROXY_EXCEPTIONS, ",")
      if (hosts.isNotEmpty()) {
        val nonProxyHosts = StringUtil.join(hosts, StringUtil.TRIMMER, "|")
        extraJvmArgs.add(Pair.pair("http.nonProxyHosts", nonProxyHosts))
        extraJvmArgs.add(Pair.pair("https.nonProxyHosts", nonProxyHosts))
      }
    }
    if (httpConfigurable.USE_HTTP_PROXY && StringUtil.isNotEmpty(httpConfigurable.proxyLogin)) {
      extraJvmArgs.add(
        Pair.pair("http.proxyUser", httpConfigurable.proxyLogin))
      extraJvmArgs.add(
        Pair.pair("https.proxyUser", httpConfigurable.proxyLogin))
      val plainProxyPassword = httpConfigurable.plainProxyPassword
      extraJvmArgs.add(Pair.pair("http.proxyPassword", plainProxyPassword))
      extraJvmArgs.add(Pair.pair("https.proxyPassword", plainProxyPassword))
    }
    extraJvmArgs.addAll(httpConfigurable.getJvmProperties(false, null))
    return extraJvmArgs
  }

  override fun getExtraCommandLineArgs(): List<String> = emptyList()
  override fun getUserFriendlyError(buildEnvironment: BuildEnvironment?,
                                    error: Throwable,
                                    projectPath: String,
                                    buildFilePath: String?): ExternalSystemException =
    BaseProjectImportErrorHandler().getUserFriendlyError(buildEnvironment, error, projectPath, buildFilePath)

  override fun preImportCheck() {}
  override fun enhanceTaskProcessing(taskNames: List<String>,
                                     jvmParametersSetup: String?,
                                     initScriptConsumer: Consumer<String>) {
  }

  override fun enhanceRemoteProcessing(parameters: SimpleJavaParameters) {}
}