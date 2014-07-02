package org.jetbrains.kotlin.gradle.plugin.android

import com.android.build.gradle.BasePlugin
import com.android.build.gradle.api.ApkVariant
import org.gradle.api.tasks.util.PatternFilterable
import org.jetbrains.annotations.NotNull

class AndroidGradleWrapper {
  static def getRuntimeJars(BasePlugin basePlugin) {
    if (basePlugin.getMetaClass().getMetaMethod("getRuntimeJarList")) {
      return basePlugin.getRuntimeJarList()
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
  static def List<String> getProductFlavorsNames(ApkVariant variant) {
      return variant.getProductFlavors().iterator().collect { it.getName() }
  }
}
