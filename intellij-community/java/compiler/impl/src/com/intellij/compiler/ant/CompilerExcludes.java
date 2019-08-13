/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.compiler.ant;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.compiler.CompilerConfigurationImpl;
import com.intellij.compiler.ant.taskdefs.Exclude;
import com.intellij.compiler.ant.taskdefs.PatternSet;
import com.intellij.openapi.compiler.options.ExcludeEntryDescription;
import com.intellij.openapi.project.Project;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Eugene Zhuravlev
 */
public class CompilerExcludes extends Generator {
  private final PatternSet myPatternSet;

  public CompilerExcludes(Project project, GenerationOptions genOptions) {
    final CompilerConfigurationImpl compilerConfiguration = (CompilerConfigurationImpl)CompilerConfiguration.getInstance(project);
    final ExcludeEntryDescription[] excludeEntryDescriptions =
      compilerConfiguration.getExcludedEntriesConfiguration().getExcludeEntryDescriptions();
    myPatternSet = new PatternSet(BuildProperties.PROPERTY_COMPILER_EXCLUDES);
    for (final ExcludeEntryDescription entry : excludeEntryDescriptions) {
      final String path = GenerationUtils
        .toRelativePath(entry.getVirtualFile(), BuildProperties.getProjectBaseDir(project), BuildProperties.getProjectBaseDirProperty(),
                        genOptions);
      if (path == null) {
        // entry is invalid, skip it
        continue;
      }
      if (entry.isFile()) {
        myPatternSet.add(new Exclude(path));
      }
      else {
        if (entry.isIncludeSubdirectories()) {
          myPatternSet.add(new Exclude(path + "/**"));
        }
        else {
          myPatternSet.add(new Exclude(path + "/*"));
        }
      }
    }
  }


  @Override
  public void generate(PrintWriter out) throws IOException {
    myPatternSet.generate(out);
  }

  public static boolean isAvailable(Project project) {
    final CompilerConfigurationImpl compilerConfiguration = (CompilerConfigurationImpl)CompilerConfiguration.getInstance(project);
    final ExcludeEntryDescription[] excludeEntryDescriptions =
      compilerConfiguration.getExcludedEntriesConfiguration().getExcludeEntryDescriptions();
    return excludeEntryDescriptions.length > 0;
  }

}
