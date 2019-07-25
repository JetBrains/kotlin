// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.ant;

import com.intellij.compiler.ant.taskdefs.Include;
import com.intellij.compiler.ant.taskdefs.PatternSet;
import com.intellij.ide.highlighter.ArchiveFileType;
import com.intellij.openapi.fileTypes.*;
import com.intellij.openapi.project.Project;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Library pattern generation (based on {@link ArchiveFileType#INSTANCE} file type).
 */
public class LibraryPatterns extends Generator {
  /**
   * A pattern set to use
   */
  private final PatternSet myPatternSet;

  /**
   * A constructor
   *
   * @param project    a context project
   * @param genOptions a generation options
   */
  public LibraryPatterns(Project project, GenerationOptions genOptions) {
    myPatternSet = new PatternSet(BuildProperties.PROPERTY_LIBRARIES_PATTERNS);
    final FileType type = ArchiveFileType.INSTANCE;
    final List<FileNameMatcher> matchers = FileTypeManager.getInstance().getAssociations(type);
    for (FileNameMatcher m : matchers) {
      if (m instanceof ExactFileNameMatcher) {
        final String path = GenerationUtils
          .toRelativePath(m.getPresentableString(), BuildProperties.getProjectBaseDir(project), BuildProperties.getProjectBaseDirProperty(),
                          genOptions);
        myPatternSet.add(new Include(path));
      }
      else {
        myPatternSet.add(new Include(m.getPresentableString()));
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void generate(final PrintWriter out) throws IOException {
    myPatternSet.generate(out);
  }
}
