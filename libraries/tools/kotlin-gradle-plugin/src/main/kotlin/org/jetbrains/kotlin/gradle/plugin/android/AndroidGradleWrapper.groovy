package org.jetbrains.kotlin.gradle.plugin.android

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.internal.VariantManager
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
    /*before 0.11 gradle android plugin there was:
      androidSourceSet.getAllJava().source(kotlinDirSet)
      androidSourceSet.getAllSource().source(kotlinDirSet)
      but those methods were removed so as temporary hack next code was suggested*/
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
}
