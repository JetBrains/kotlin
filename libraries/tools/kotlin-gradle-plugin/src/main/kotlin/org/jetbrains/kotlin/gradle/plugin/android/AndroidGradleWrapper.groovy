package org.jetbrains.kotlin.gradle.plugin.android

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.internal.VariantManager
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.builder.dependency.DependencyContainer
import com.android.builder.dependency.LibraryDependency
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

  static def PatternFilterable getResourceFilter(Object androidSourceSet) {
    def resources = androidSourceSet.getResources()
    if (resources != null) {
      return resources.getFilter()
    }
    return null
  }

  @NotNull
  static def String getVariantName(Object variant) {
    return variant.getBuildType().getName()
  }

  @Nullable
  static def AbstractCompile getJavaCompile(Object baseVariantData) {
    if (baseVariantData.getMetaClass().getMetaProperty("javaCompileTask")) {
      return baseVariantData.javaCompileTask
    }
    else if (baseVariantData.getMetaClass().getMetaProperty("javaCompilerTask")) {
      return baseVariantData.javaCompilerTask
    }
    return null
  }

  @NotNull
  static def Set<File> getJavaSrcDirs(Object androidSourceSet) {
    return androidSourceSet.getJava().getSrcDirs()
  }

  static def setNoJdk(Object kotlinOptionsExtension) {
    kotlinOptionsExtension.noJdk = true
  }

  @NotNull
  static def List<String> getProductFlavorsNames(ApkVariant variant) {
      return variant.getProductFlavors().iterator().collect { it.getName() }
  }

  @NotNull
  static def List<AndroidSourceSet> getProductFlavorsSourceSets(BaseExtension extension) {
      return extension.productFlavors.iterator().collect { extension.sourceSets.findByName(it.name) }
  }

  @NotNull
  static def DefaultDomainObjectSet<TestVariant> getTestVariants(BaseExtension extension) {
    if (extension.getMetaClass().getMetaMethod("getTestVariants")) {
      return extension.getTestVariants()
    }
    return Collections.emptyList()
  }

  @NotNull
  static def List<File> getRClassFolder(BaseVariant variant) {
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

  static def VariantManager getVariantDataManager(BasePlugin plugin) {
    return plugin.getVariantManager()
  }

  static def List<File> getJavaSources(BaseVariantData variantData) {
    def result = new LinkedHashSet<File>()

    // user sources
    List<SourceProvider> providers = variantData.variantConfiguration.getSortedSourceProviders();
    for (SourceProvider provider : providers) {
      result.addAll((provider as AndroidSourceSet).getJava().getSrcDirs());
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
        result.add(variantData.scope.getRClassSourceOutputDir());
      }

      if (variantData.scope.getGenerateBuildConfigTask() != null) {
        result.add(variantData.scope.getBuildConfigSourceOutputDir());
      }

      if (variantData.scope.getAidlCompileTask() != null) {
        result.add(variantData.scope.getAidlSourceOutputDir());
      }

      if (variantData.scope.getGlobalScope().getExtension().getDataBinding().isEnabled()) {
        result.add(variantData.scope.getClassOutputForDataBinding());
      }

      if (!variantData.variantConfiguration.getRenderscriptNdkModeEnabled()
              && variantData.scope.getRenderscriptCompileTask() != null) {
        result.add(variantData.scope.getRenderscriptSourceOutputDir());
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
  static def Map<File, File> getJarToAarMapping(BaseVariantData variantData) {
    def jarToLibraryArtifactMap = new HashMap<File, File>()

    def libraries = getVariantLibraryDependencies(variantData)
    if (libraries == null) return jarToLibraryArtifactMap

    for (lib in libraries) {
      jarToLibraryArtifactMap[lib.jarFile] = lib.bundle

      // local dependencies are detected as changed by gradle, because they are seem to be
      // rewritten every time when bundle changes
      // when local dep will actually change, record for bundle will be removed from registry
      for (localDep in lib.localJars) {
        if (localDep instanceof File) {
          // android tools 2.2
          jarToLibraryArtifactMap[localDep] = lib.bundle
        }
        else if (localDep.metaClass.getMetaMethod("jarFile") != null) {
          // android tools < 2.2
          jarToLibraryArtifactMap[localDep.jarFile] = lib.bundle
        }
      }
    }

    return jarToLibraryArtifactMap
  }

  @Nullable
  private static def Iterable<LibraryDependency> getVariantLibraryDependencies(BaseVariantData variantData) {
    def variantDependency = variantData.variantDependency
    if (variantDependency instanceof DependencyContainer) {
      // android tools < 2.2
      return variantDependency.getAndroidDependencies()
    }

    def variantDependencyMeta = variantData.variantDependency.getMetaClass()
    def getCompileDependencies = variantDependencyMeta.getMetaMethod("getCompileDependencies")
    if (getCompileDependencies != null && getCompileDependencies.returnType.metaClass == DependencyContainer.metaClass) {
      // android tools 2.2
      return variantDependency.getCompileDependencies().getAndroidDependencies()
    }

    return null
  }
}
