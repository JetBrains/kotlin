/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.plugin.android

import com.android.build.api.transform.*
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.internal.VariantManager
import com.android.build.gradle.internal.pipeline.OriginalStream
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.builder.model.SourceProvider
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.util.PatternFilterable
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

class AndroidGradleWrapper {
  static def getRuntimeJars(BasePlugin basePlugin, BaseExtension baseExtension) {
    if (basePlugin.getMetaClass().getMetaMethod("getRuntimeJarList")) {
      return basePlugin.getRuntimeJarList()
    }
    else if (baseExtension.getMetaClass().getMetaMethod("getBootClasspath")) {
        return baseExtension.getBootClasspath()
    }
    else {
      return basePlugin.getBootClasspath()
    }
  }

  static def srcDir(Object androidSourceSet, Object kotlinDirSet) {
    androidSourceSet.getJava().srcDir(kotlinDirSet)
  }

  static PatternFilterable getResourceFilter(Object androidSourceSet) {
    def resources = androidSourceSet.getResources()
    if (resources != null) {
      return resources.getFilter()
    }
    return null
  }

  @NotNull
  static String getVariantName(Object variant) {
    return variant.getBuildType().getName()
  }

  @Nullable
  private static getJackOptions(@NotNull Object variantData) {
    def variantConfiguration = variantData.variantConfiguration
    if (variantConfiguration.getMetaClass().getMetaMethod("getJackOptions")) {
      return variantConfiguration.getJackOptions()
    }
    return null
  }

  static boolean isJackEnabled(@NotNull Object variantData) {
    return getJackOptions(variantData)?.enabled ?: false
  }

  @Nullable
  static AbstractCompile getJavaTask(@NotNull Object variantData) {
      if (isJackEnabled(variantData)) {
          return getJavacTask(variantData)
      }
      else {
          return getJavaCompile(variantData)
      }
  }

  @Nullable
  private static AbstractCompile getJavacTask(@NotNull Object baseVariantData) {
    if (baseVariantData.getMetaClass().getMetaProperty("javacTask")) {
      return baseVariantData.javacTask
    }
    return null
  }

  @Nullable
  private static TransformTask getJackTask(@NotNull Object variantData) {
    def compilerTask = variantData.javaCompilerTask
    if (compilerTask instanceof TransformTask) {
      return compilerTask
    }
    return null
  }

  @Nullable
  private static getJackTransform(@NotNull Object variantData) {
    return getJackTask(variantData)?.transform
  }

  static addSourceToJack(@NotNull Object variantData, @NotNull File sourceFolder) {
    getJackTransform(variantData)?.addSource(sourceFolder)
  }

  static disableJackAnnotationProcessing(@NotNull Object variantData) {
    def jackOptions = getJackTransform(variantData)?.options
    jackOptions?.setAnnotationProcessorOutputDirectory(null)
    jackOptions?.setAnnotationProcessorNames([])
    jackOptions?.setAnnotationProcessorClassPath([])
    jackOptions?.setAnnotationProcessorOptions([:])
  }

  static configureJackTask(
          @NotNull Object variantData,
          @NotNull File jillOutputFile,
          @NotNull String kotlinJillTaskName) {
    def jackTask = getJackTask(variantData)
    // There is no Jack task for some variants
    if (jackTask == null) {
      return
    }

    def jillOutputStream = OriginalStream.builder()
            .addContentType(QualifiedContent.DefaultContentType.CLASSES)
            .addScope(QualifiedContent.Scope.PROJECT)
            .setJar(jillOutputFile)
            .setDependency(kotlinJillTaskName)
            .build()

    jackTask.consumedInputStreams.add(jillOutputStream)
    jackTask.dependsOn(kotlinJillTaskName)
  }

  @Nullable
  private static AbstractCompile getJavaCompile(@NotNull Object baseVariantData) {
    if (baseVariantData.getMetaClass().getMetaProperty("javaCompileTask")) {
      return baseVariantData.javaCompileTask
    }
    else if (baseVariantData.getMetaClass().getMetaProperty("javaCompilerTask")) {
      return baseVariantData.javaCompilerTask
    }
    return null
  }

  @NotNull
  static Set<File> getJavaSrcDirs(Object androidSourceSet) {
    return androidSourceSet.getJava().getSrcDirs()
  }

  static def setNoJdk(Object kotlinOptionsExtension) {
    kotlinOptionsExtension.noJdk = true
  }

  @NotNull
  static List<String> getProductFlavorsNames(ApkVariant variant) {
      return variant.getProductFlavors().iterator().collect { it.getName() }
  }

  @NotNull
  static List<AndroidSourceSet> getProductFlavorsSourceSets(BaseExtension extension) {
      return extension.productFlavors.iterator().collect { extension.sourceSets.findByName(it.name) }
  }

  @Nullable
  static Map<String, String> getAnnotationProcessorOptionsFromAndroidVariant(@Nullable Object variantData) {
    if (!(variantData instanceof BaseVariantData)) {
      throw new IllegalArgumentException("BaseVariantData instance expected")
    }

    def variantConfiguration = variantData.variantConfiguration
    if (variantConfiguration.getMetaClass().getMetaMethod("getJavaCompileOptions")) {
      def javaCompileOptions = variantConfiguration.getJavaCompileOptions()

      if (javaCompileOptions.getMetaClass().getMetaMethod("getAnnotationProcessorOptions")) {
        return javaCompileOptions.getAnnotationProcessorOptions().getArguments()
      }
    }

    return null
  }

  @NotNull
  static DefaultDomainObjectSet<TestVariant> getTestVariants(BaseExtension extension) {
    if (extension.getMetaClass().getMetaMethod("getTestVariants")) {
      return extension.getTestVariants()
    }
    return Collections.emptyList()
  }

  @NotNull
  static List<File> getRClassFolder(BaseVariant variant) {
    def list = new ArrayList<File>()
    if (variant.getMetaClass().getMetaMethod("getProcessResources")) {
      list.add(variant.getProcessResources().getSourceOutputDir())
    }
    else {
      for (Object variantOutput : variant.getOutputs()) {
        list.add(variantOutput.processResources.sourceOutputDir)
      }
    }
    return list
  }

  static VariantManager getVariantDataManager(BasePlugin plugin) {
    return plugin.getVariantManager()
  }

  static List<File> getJavaSources(BaseVariantData variantData) {
    def result = new LinkedHashSet<File>()

    // user sources
    List<SourceProvider> providers = variantData.variantConfiguration.getSortedSourceProviders()
    for (SourceProvider provider : providers) {
      result.addAll((provider as AndroidSourceSet).getJava().getSrcDirs())
    }

    // generated sources
    def getJavaSourcesMethod = variantData.getMetaClass().getMetaMethod("getJavaSources")
    if (getJavaSourcesMethod.returnType.metaClass == Object[].metaClass) {
      result.addAll(variantData.getJavaSources().findAll { it instanceof File })
    }
    else if (getJavaSourcesMethod.returnType.metaClass == List.metaClass) {
      def fileTrees = variantData.getJavaSources().findAll { it instanceof ConfigurableFileTree }
      result.addAll(fileTrees.collect { it.getDir() })
    }
    else {
      if (variantData.scope.getGenerateRClassTask() != null) {
        result.add(variantData.scope.getRClassSourceOutputDir())
      }

      if (variantData.scope.getGenerateBuildConfigTask() != null) {
        result.add(variantData.scope.getBuildConfigSourceOutputDir())
      }

      if (variantData.scope.getAidlCompileTask() != null) {
        result.add(variantData.scope.getAidlSourceOutputDir())
      }

      if (variantData.scope.getGlobalScope().getExtension().getDataBinding().isEnabled()) {
        result.add(variantData.scope.getClassOutputForDataBinding())
      }

      if (!variantData.variantConfiguration.getRenderscriptNdkModeEnabled()
              && variantData.scope.getRenderscriptCompileTask() != null) {
        result.add(variantData.scope.getRenderscriptSourceOutputDir())
      }
    }

    def getExtraSourcesMethod = variantData.getMetaClass().getMetaMethod("getExtraGeneratedSourceFolders")
    if (getExtraSourcesMethod.returnType.metaClass == List.metaClass) {
      def folders = variantData.getExtraGeneratedSourceFolders()
      if (folders != null) {
        result.addAll(folders)
      }
    }

    return result.toList()
  }

  @NotNull
  static Map<File, File> getJarToAarMapping(BaseVariantData variantData) {
    def jarToLibraryArtifactMap = new HashMap<File, File>()

    def libraries = getVariantLibraryDependencies(variantData)
    if (libraries == null) return jarToLibraryArtifactMap

    for (lib in libraries) {
      Object bundle = getLibraryArtifactFile(lib)
      jarToLibraryArtifactMap[lib.jarFile] = bundle

      // local dependencies are detected as changed by gradle, because they are seem to be
      // rewritten every time when bundle changes
      // when local dep will actually change, record for bundle will be removed from registry
      for (localDep in lib.localJars) {
        if (localDep instanceof File) {
          // android tools 2.2
          jarToLibraryArtifactMap[localDep] = bundle
        }
        else if (localDep.metaClass.getMetaMethod("jarFile") != null) {
          // android tools < 2.2
          jarToLibraryArtifactMap[localDep.jarFile] = bundle
        }
      }
    }

    return jarToLibraryArtifactMap
  }

  private static def getLibraryArtifactFile(Object lib) {
    if (lib.class.name == "com.android.builder.dependency.level2.AndroidDependency") {
      // android tools >= 2.3
      return lib.artifactFile
    } else {
      // android tools <= 2.2
      return lib.bundle
    }
  }

  @Nullable
  private static Iterable<Object> getVariantLibraryDependencies(BaseVariantData variantData) {
    def variantDependency = variantData.variantDependency

    if (variantDependency.metaClass.getMetaMethod("getAndroidDependencies") != null) {
      // android tools < 2.2
      return variantDependency.getAndroidDependencies() as Iterable<Object>
    }

    def getCompileDependencies = variantDependency.metaClass.getMetaMethod("getCompileDependencies")
    if (getCompileDependencies != null) {
      def compileDependencies = variantDependency.getCompileDependencies()
      if (compileDependencies.metaClass.getMetaMethod("getDirectAndroidDependencies") != null) {
        return compileDependencies.getDirectAndroidDependencies() // android >= 2.3
      } else if (compileDependencies.metaClass.getMetaMethod("getAndroidDependencies")) {
        return compileDependencies.getAndroidDependencies() // android 2.2
      }
    }

    return null
  }
}
