// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.gradle.model.impl;

import com.intellij.openapi.util.io.FileUtil;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.file.pattern.PatternMatcherFactory;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Vladislav.Soroka
 */
public class GradleResourceFileFilter implements FileFilter {
  private final FilePattern myFilePattern;
  private final File myRoot;
  private final Spec<RelativePath> myFileFilterSpec;

  public GradleResourceFileFilter(@NotNull File rootFile, @NotNull FilePattern filePattern) {
    myFilePattern = filePattern;
    myRoot = rootFile;
    myFileFilterSpec = getAsSpec();
  }

  @Override
  public boolean accept(@NotNull File file) {
    final String relPath = FileUtil.getRelativePath(myRoot, file);
    return relPath != null && isIncluded(relPath);
  }

  private boolean isIncluded(@NotNull String relativePath) {
    RelativePath path = new RelativePath(true, relativePath.split(Pattern.quote(File.separator)));
    return myFileFilterSpec.isSatisfiedBy(path);
  }

  private Spec<RelativePath> getAsSpec() {
    return Specs.intersect(getAsIncludeSpec(true), Specs.negate(getAsExcludeSpec(true)));
  }

  private Spec<RelativePath> getAsExcludeSpec(boolean caseSensitive) {
    Collection<String> allExcludes = new LinkedHashSet<>(myFilePattern.excludes);
    List<Spec<RelativePath>> matchers = new ArrayList<>();
    for (String exclude : allExcludes) {
      Spec<RelativePath> patternMatcher = PatternMatcherFactory.getPatternMatcher(false, caseSensitive, exclude);
      matchers.add(patternMatcher);
    }
    if (matchers.isEmpty()) {
      return Specs.satisfyNone();
    }
    return Specs.union(matchers);
  }

  private Spec<RelativePath> getAsIncludeSpec(boolean caseSensitive) {
    List<Spec<RelativePath>> matchers = new ArrayList<>();
    for (String include : myFilePattern.includes) {
      Spec<RelativePath> patternMatcher = PatternMatcherFactory.getPatternMatcher(true, caseSensitive, include);
      matchers.add(patternMatcher);
    }
    return Specs.union(matchers);
  }
}