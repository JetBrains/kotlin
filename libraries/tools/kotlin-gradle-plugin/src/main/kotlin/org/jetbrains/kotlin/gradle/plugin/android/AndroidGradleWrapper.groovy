package org.jetbrains.kotlin.gradle.plugin.android

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.tasks.util.PatternFilterable
import org.jetbrains.annotations.NotNull

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

  @NotNull
  static def Set<File> getJavaSrcDirs(Object androidSourceSet) {
    return androidSourceSet.getJava().getSrcDirs()
  }

  @NotNull
  static def Set<File> getResourceDirs(Object androidSourceSet) {
    return androidSourceSet.getRes().getSrcDirs()
  }

  @NotNull
  static def File getManifestFile(Object androidSourceSet) {
    return androidSourceSet.getManifest().getSrcFile()
  }

  @NotNull
  static def List<String> getProductFlavorsNames(ApkVariant variant) {
      return variant.getProductFlavors().iterator().collect { it.getName() }
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
}
