// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.ant;

import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.util.ArrayUtilRt;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Generator for property files.
 *
 * @author Eugene Zhuravlev
 */
public class PropertyFileGeneratorImpl extends PropertyFileGenerator {
  /**
   * List of the properties
   */
  private final List<Couple<String>> myProperties = new ArrayList<>();

  /**
   * A constructor that extracts all need properties for ant build from the project.
   *
   * @param project    a project to examine
   * @param genOptions generation options
   */
  public PropertyFileGeneratorImpl(Project project, GenerationOptions genOptions) {
    // path variables
    final Map<String, String> pathMacros = PathMacros.getInstance().getUserMacros();
    if (pathMacros.size() > 0) {
      final String[] macroNames = ArrayUtilRt.toStringArray(pathMacros.keySet());
      Arrays.sort(macroNames);
      for (final String macroName : macroNames) {
        addProperty(BuildProperties.getPathMacroProperty(macroName), pathMacros.get(macroName));
      }
    }
    // jdk homes
    if (genOptions.forceTargetJdk) {
      final Sdk[] usedJdks = BuildProperties.getUsedJdks(project);
      for (Sdk jdk : usedJdks) {
        if (jdk.getHomeDirectory() == null) {
          continue;
        }
        final File homeDir = BuildProperties.toCanonicalFile(VfsUtilCore.virtualToIoFile(jdk.getHomeDirectory()));
        addProperty(BuildProperties.getJdkHomeProperty(jdk.getName()), homeDir.getPath().replace(File.separatorChar, '/'));
      }
    }
    // generate idea.home property
    if (genOptions.isIdeaHomeGenerated()) {
      addProperty(BuildProperties.PROPERTY_IDEA_HOME, PathManager.getHomePath());
    }

    if (genOptions.enableFormCompiler) {
      addProperty(BuildProperties.PROPERTY_INCLUDE_JAVA_RUNTIME_FOR_INSTRUMENTATION, genOptions.forceTargetJdk? "false" : "true");
    }

    ChunkBuildExtension.generateAllProperties(this, project, genOptions);
  }

  @Override
  public void addProperty(String name, String value) {
    myProperties.add(Couple.of(name, value));
  }

  @Override
  public void generate(PrintWriter out) throws IOException {
    boolean isFirst = true;
    for (final Couple<String> pair : myProperties) {
      if (!isFirst) {
        crlf(out);
      }
      else {
        isFirst = false;
      }
      out.print(StringUtil.escapeProperty(pair.getFirst(), true));
      out.print("=");
      out.print(StringUtil.escapeProperty(pair.getSecond(), false));
    }
  }
}
