// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.model.java.impl.compiler;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.compiler.JpsJavaCompilerConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Eugene Zhuravlev
 */
public class ResourcePatterns {
  private static final Logger LOG = Logger.getInstance("#org.jetbrains.jps.model.java.impl.compiler.ResourcePatterns");

  private final List<CompiledPattern> myCompiledPatterns = new ArrayList<>();
  private final List<CompiledPattern> myNegatedCompiledPatterns = new ArrayList<>();

  public ResourcePatterns(final JpsJavaCompilerConfiguration configuration) {
    final List<String> patterns = configuration.getResourcePatterns();
    for (String pattern : patterns) {
      final CompiledPattern regexp = convertToRegexp(pattern);
      if (isPatternNegated(pattern)) {
        myNegatedCompiledPatterns.add(regexp);
      }
      else {
        myCompiledPatterns.add(regexp);
      }
    }
  }

  public boolean isResourceFile(File file, @NotNull final File srcRoot) {
    final String name = file.getName();
    final String relativePathToParent;
    final String parentPath = file.getParent();
    if (parentPath != null) {
      relativePathToParent = "/" + FileUtilRt.getRelativePath(FileUtilRt.toSystemIndependentName(srcRoot.getAbsolutePath()), FileUtilRt.toSystemIndependentName(parentPath), '/', SystemInfo.isFileSystemCaseSensitive);
    }
    else {
      relativePathToParent = null;
    }
    for (CompiledPattern pair : myCompiledPatterns) {
      if (matches(name, relativePathToParent, srcRoot, pair)) {
        return true;
      }
    }

    if (myNegatedCompiledPatterns.isEmpty()) {
      return false;
    }

    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myNegatedCompiledPatterns.size(); i++) {
      if (matches(name, relativePathToParent, srcRoot, myNegatedCompiledPatterns.get(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean matches(String name, String parentRelativePath, @NotNull File srcRoot, CompiledPattern pattern) {
    if (!matches(name, pattern.fileName)) {
      return false;
    }
    if (parentRelativePath != null) {
      if (pattern.dir != null && !matches(parentRelativePath, pattern.dir)) {
        return false;
      }
      if (pattern.srcRoot != null && !matches(srcRoot.getName(), pattern.srcRoot)) {
        return false;
      }
    }
    return true;
  }

  private static boolean matches(String s, Pattern p) {
    try {
      return p.matcher(s).matches();
    }
    catch (Exception e) {
      LOG.error("Exception matching file name \"" + s + "\" against the pattern \"" + p + "\"", e);
      return false;
    }
  }

  private static CompiledPattern convertToRegexp(String wildcardPattern) {
    if (isPatternNegated(wildcardPattern)) {
      wildcardPattern = wildcardPattern.substring(1);
    }

    wildcardPattern = FileUtilRt.toSystemIndependentName(wildcardPattern);

    String srcRoot = null;
    int colon = wildcardPattern.indexOf(":");
    if (colon > 0) {
      srcRoot = wildcardPattern.substring(0, colon);
      wildcardPattern = wildcardPattern.substring(colon + 1);
    }
    
    String dirPattern = null;
    int slash = wildcardPattern.lastIndexOf('/');
    if (slash >= 0) {
      dirPattern = wildcardPattern.substring(0, slash + 1);
      wildcardPattern = wildcardPattern.substring(slash + 1);
      dirPattern = optimizeDirPattern(dirPattern);
    }

    wildcardPattern = normalizeWildcards(wildcardPattern);
    wildcardPattern = optimize(wildcardPattern);

    final Pattern dirCompiled = dirPattern == null ? null : compilePattern(dirPattern);
    final Pattern srcCompiled = srcRoot == null ? null : compilePattern(optimize(normalizeWildcards(srcRoot)));
    return new CompiledPattern(compilePattern(wildcardPattern), dirCompiled, srcCompiled);
  }

  public static String optimizeDirPattern(String dirPattern) {
    if (!dirPattern.startsWith("/")) {
      dirPattern = "/" + dirPattern;
    }
    //now dirPattern starts and ends with '/'

    dirPattern = normalizeWildcards(dirPattern);

    dirPattern = StringUtil.replace(dirPattern, "/.*.*/", "(/.*)?/");
    dirPattern = StringUtil.trimEnd(dirPattern, "/");

    dirPattern = optimize(dirPattern);
    return dirPattern;
  }

  private static String optimize(String wildcardPattern) {
    return wildcardPattern.replaceAll("(?:\\.\\*)+", ".*");
  }

  public static String normalizeWildcards(String wildcardPattern) {
    wildcardPattern = StringUtil.replace(wildcardPattern, "\\!", "!");
    wildcardPattern = StringUtil.replace(wildcardPattern, ".", "\\.");
    wildcardPattern = StringUtil.replace(wildcardPattern, "*?", ".+");
    wildcardPattern = StringUtil.replace(wildcardPattern, "?*", ".+");
    wildcardPattern = StringUtil.replace(wildcardPattern, "*", ".*");
    wildcardPattern = StringUtil.replace(wildcardPattern, "?", ".");
    return wildcardPattern;
  }

  private static boolean isPatternNegated(String wildcardPattern) {
    return wildcardPattern.length() > 1 && wildcardPattern.charAt(0) == '!';
  }

  private static Pattern compilePattern(@NonNls String s) {
    return Pattern.compile(s, SystemInfo.isFileSystemCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
  }
  
  private static class CompiledPattern {
    @NotNull final Pattern fileName;
    @Nullable final Pattern dir;
    @Nullable final Pattern srcRoot;

    CompiledPattern(@NotNull Pattern fileName, @Nullable Pattern dir, @Nullable Pattern srcRoot) {
      this.fileName = fileName;
      this.dir = dir;
      this.srcRoot = srcRoot;
    }
  }
  
}
