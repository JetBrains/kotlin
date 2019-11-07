// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.server.impl;

import com.intellij.compiler.server.BuildProcessParametersProvider;
import com.intellij.compiler.server.CompileServerPlugin;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.PluginPathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtil;
import com.intellij.util.io.URLUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;

/**
 * @author nik
 */
public class BuildProcessClasspathManager {
  private static final Logger LOG = Logger.getInstance(BuildProcessClasspathManager.class);

  private List<String> myCompileServerPluginsClasspath;

  public List<String> getBuildProcessPluginsClasspath(Project project) {
    List<String> staticClasspath = getStaticClasspath();
    List<String> dynamicClasspath = getDynamicClasspath(project);

    if (dynamicClasspath.isEmpty()) {
      return staticClasspath;
    }
    else {
      dynamicClasspath.addAll(staticClasspath);
      return dynamicClasspath;
    }
  }

  private List<String> getStaticClasspath() {
    if (myCompileServerPluginsClasspath == null) {
      myCompileServerPluginsClasspath = computeCompileServerPluginsClasspath();
    }
    return myCompileServerPluginsClasspath;
  }

  private static List<String> computeCompileServerPluginsClasspath() {
    final List<String> classpath = new ArrayList<>();

    for (CompileServerPlugin serverPlugin : CompileServerPlugin.EP_NAME.getExtensions()) {
      final PluginId pluginId = serverPlugin.getPluginDescriptor().getPluginId();
      final IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(pluginId);
      LOG.assertTrue(plugin != null, pluginId);

      final File baseFile = plugin.getPath();
      if (baseFile.isFile()) {
        classpath.add(baseFile.getPath());
      }
      else if (baseFile.isDirectory()) {
        outer:
        for (String relativePath : StringUtil.split(serverPlugin.getClasspath(), ";")) {
          File jarFile = new File(baseFile, "lib/" + relativePath);
          if (jarFile.exists()) {
            classpath.add(jarFile.getPath());
            continue;
          }

          // ... 'plugin run configuration': all module output are copied to 'classes' folder
          File classesDir = new File(baseFile, "classes");
          if (classesDir.isDirectory()) {
            classpath.add(classesDir.getPath());
            continue;
          }

          // development mode
          if (PluginManagerCore.isRunningFromSources()) {
            // ... try "out/classes/production/<jar-name>", assuming that <jar-name> means module name
            String moduleName = FileUtilRt.getNameWithoutExtension(PathUtil.getFileName(relativePath));
            if (OLD_TO_NEW_MODULE_NAME.containsKey(moduleName)) {
              moduleName = OLD_TO_NEW_MODULE_NAME.get(moduleName);
            }
            File baseOutputDir = baseFile.getParentFile();
            if (baseOutputDir.getName().equals("test")) {
              baseOutputDir = new File(baseOutputDir.getParentFile(), "production");
            }
            File moduleDir = new File(baseOutputDir, moduleName);
            if (moduleDir.isDirectory()) {
              classpath.add(moduleDir.getPath());
              continue;
            }
            // ... try "<plugin-dir>/lib/<jar-name>", assuming that <jar-name> is a module library committed to VCS
            File pluginDir = getPluginDir(plugin);
            if (pluginDir != null) {
              File libraryFile = new File(pluginDir, "lib/" + PathUtil.getFileName(relativePath));
              if (libraryFile.exists()) {
                classpath.add(libraryFile.getPath());
                continue;
              }
            }
            // ... look for <jar-name> on the classpath, assuming that <jar-name> is an external (read: Maven) library
            try {
              Enumeration<URL> urls = BuildProcessClasspathManager.class.getClassLoader().getResources(JarFile.MANIFEST_NAME);
              while (urls.hasMoreElements()) {
                Pair<String, String> parts = URLUtil.splitJarUrl(urls.nextElement().getFile());
                if (parts != null && relativePath.equals(PathUtil.getFileName(parts.first))) {
                  classpath.add(parts.first);
                  continue outer;
                }
              }
            }
            catch (IOException ignored) { }
          }

          LOG.error("Cannot add '" + relativePath + "' from '" + plugin.getName() + ' ' + plugin.getVersion() + "'" + " to compiler classpath");
        }
      }
    }

    return classpath;
  }

  @Nullable
  private static File getPluginDir(IdeaPluginDescriptor plugin) {
    String pluginDirName = StringUtil.getShortName(plugin.getPluginId().getIdString());
    String extraDir = System.getProperty("idea.external.build.development.plugins.dir");
    if (extraDir != null) {
      File extraDirFile = new File(extraDir, pluginDirName);
      if (extraDirFile.isDirectory()) {
        return extraDirFile;
      }
    }
    File pluginHome = PluginPathManager.getPluginHome(pluginDirName);
    if (!pluginHome.isDirectory() && StringUtil.isCapitalized(pluginDirName)) {
      pluginHome = PluginPathManager.getPluginHome(StringUtil.decapitalize(pluginDirName));
    }
    return pluginHome.isDirectory() ? pluginHome : null;
  }

  private static List<String> getDynamicClasspath(Project project) {
    final List<String> classpath = new ArrayList<>();
    for (BuildProcessParametersProvider provider : BuildProcessParametersProvider.EP_NAME.getExtensionList(project)) {
      classpath.addAll(provider.getClassPath());
    }
    return classpath;
  }

  public static List<String> getLauncherClasspath(Project project) {
    final List<String> classpath = new ArrayList<>();
    for (BuildProcessParametersProvider provider : BuildProcessParametersProvider.EP_NAME.getExtensionList(project)) {
      classpath.addAll(provider.getLauncherClassPath());
    }
    return classpath;
  }

  //todo[nik] this is a temporary compatibility fix; we should update plugin layout so JAR names correspond to module names instead.
  private static final Map<String, String> OLD_TO_NEW_MODULE_NAME;
  static {
    OLD_TO_NEW_MODULE_NAME = new LinkedHashMap<>();
    OLD_TO_NEW_MODULE_NAME.put("android-jps-plugin", "intellij.android.jps");
    OLD_TO_NEW_MODULE_NAME.put("ant-jps-plugin", "intellij.ant.jps");
    OLD_TO_NEW_MODULE_NAME.put("aspectj-jps-plugin", "intellij.aspectj.jps");
    OLD_TO_NEW_MODULE_NAME.put("devkit-jps-plugin", "intellij.devkit.jps");
    OLD_TO_NEW_MODULE_NAME.put("eclipse-jps-plugin", "intellij.eclipse.jps");
    OLD_TO_NEW_MODULE_NAME.put("error-prone-jps-plugin", "intellij.errorProne.jps");
    OLD_TO_NEW_MODULE_NAME.put("flex-jps-plugin", "intellij.flex.jps");
    OLD_TO_NEW_MODULE_NAME.put("gradle-jps-plugin", "intellij.gradle.jps");
    OLD_TO_NEW_MODULE_NAME.put("grails-jps-plugin", "intellij.groovy.grails.jps");
    OLD_TO_NEW_MODULE_NAME.put("groovy-jps-plugin", "intellij.groovy.jps");
    OLD_TO_NEW_MODULE_NAME.put("gwt-jps-plugin", "intellij.gwt.jps");
    OLD_TO_NEW_MODULE_NAME.put("google-app-engine-jps-plugin", "intellij.java.googleAppEngine.jps");
    OLD_TO_NEW_MODULE_NAME.put("ui-designer-jps-plugin", "intellij.java.guiForms.jps");
    OLD_TO_NEW_MODULE_NAME.put("intellilang-jps-plugin", "intellij.java.langInjection.jps");
    OLD_TO_NEW_MODULE_NAME.put("dmServer-jps-plugin", "intellij.javaee.appServers.dmServer.jps");
    OLD_TO_NEW_MODULE_NAME.put("weblogic-jps-plugin", "intellij.javaee.appServers.weblogic.jps");
    OLD_TO_NEW_MODULE_NAME.put("webSphere-jps-plugin", "intellij.javaee.appServers.websphere.jps");
    OLD_TO_NEW_MODULE_NAME.put("jpa-jps-plugin", "intellij.javaee.jpa.jps");
    OLD_TO_NEW_MODULE_NAME.put("javaee-jps-plugin", "intellij.javaee.jps");
    OLD_TO_NEW_MODULE_NAME.put("javaFX-jps-plugin", "intellij.javaFX.jps");
    OLD_TO_NEW_MODULE_NAME.put("maven-jps-plugin", "intellij.maven.jps");
    OLD_TO_NEW_MODULE_NAME.put("osmorc-jps-plugin", "intellij.osgi.jps");
    OLD_TO_NEW_MODULE_NAME.put("ruby-chef-jps-plugin", "intellij.ruby.chef.jps");
    OLD_TO_NEW_MODULE_NAME.put("android-common", "intellij.android.common");
    OLD_TO_NEW_MODULE_NAME.put("build-common", "intellij.android.buildCommon");
    OLD_TO_NEW_MODULE_NAME.put("android-rt", "intellij.android.rt");
    OLD_TO_NEW_MODULE_NAME.put("sdk-common", "android.sdktools.sdk-common");
    OLD_TO_NEW_MODULE_NAME.put("sdklib", "android.sdktools.sdklib");
    OLD_TO_NEW_MODULE_NAME.put("layoutlib-api", "android.sdktools.layoutlib-api");
    OLD_TO_NEW_MODULE_NAME.put("repository", "android.sdktools.repository");
    OLD_TO_NEW_MODULE_NAME.put("manifest-merger", "android.sdktools.manifest-merger");
    OLD_TO_NEW_MODULE_NAME.put("common-eclipse-util", "intellij.eclipse.common");
    OLD_TO_NEW_MODULE_NAME.put("flex-shared", "intellij.flex.shared");
    OLD_TO_NEW_MODULE_NAME.put("groovy-rt-constants", "intellij.groovy.constants.rt");
    OLD_TO_NEW_MODULE_NAME.put("grails-compiler-patch", "intellij.groovy.grails.compilerPatch");
    OLD_TO_NEW_MODULE_NAME.put("appEngine-runtime", "intellij.java.googleAppEngine.runtime");
    OLD_TO_NEW_MODULE_NAME.put("common-javaFX-plugin", "intellij.javaFX.common");
  }
}
