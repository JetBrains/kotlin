/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.android.inspections.lint;

import com.android.annotations.NonNull;
import com.android.tools.lint.checks.*;
import com.android.tools.lint.detector.api.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.jetbrains.android.inspections.lint.IntellijLintProject.*;

/**
 * Custom version of the {@link BuiltinIssueRegistry}. This
 * variation will filter the default issues and remove
 * any issues that aren't usable inside IDEA (e.g. they
 * rely on class files), and it will also replace the implementation
 * of some issues with IDEA specific ones.
 */
public class IntellijLintIssueRegistry extends BuiltinIssueRegistry {
  private static List<Issue> ourFilteredIssues;

  public IntellijLintIssueRegistry() {
  }

  @NonNull
  @Override
  public List<Issue> getIssues() {
    if (ourFilteredIssues == null) {
      List<Issue> sIssues = super.getIssues();
      List<Issue> result = new ArrayList<Issue>(sIssues.size());
      for (Issue issue : sIssues) {
        Implementation implementation = issue.getImplementation();
        EnumSet<Scope> scope = implementation.getScope();
        Class<? extends Detector> detectorClass = implementation.getDetectorClass();
        if (detectorClass == ApiDetector.class) {
          issue.setImplementation(IntellijApiDetector.IMPLEMENTATION);
        } else if (detectorClass == RegistrationDetector.class) {
          issue.setImplementation(IntellijRegistrationDetector.IMPLEMENTATION);
        } else if (detectorClass == GradleDetector.class) {
          issue.setImplementation(IntellijGradleDetector.IMPLEMENTATION);
        } else if (detectorClass == ViewTypeDetector.class) {
          issue.setImplementation(IntellijViewTypeDetector.IMPLEMENTATION);
        } else if (detectorClass == SupportAnnotationDetector.class) {
          // Handled by the ResourceTypeInspection
          continue;
        } else if (scope.contains(Scope.CLASS_FILE) ||
            scope.contains(Scope.ALL_CLASS_FILES) ||
            scope.contains(Scope.JAVA_LIBRARIES)) {
          //noinspection ConstantConditions
          assert !SUPPORT_CLASS_FILES; // When enabled, adjust this to include class detector based issues

          boolean isOk = false;
          for (EnumSet<Scope> analysisScope : implementation.getAnalysisScopes()) {
            if (!analysisScope.contains(Scope.CLASS_FILE) &&
                !analysisScope.contains(Scope.ALL_CLASS_FILES) &&
                !analysisScope.contains(Scope.JAVA_LIBRARIES)) {
              isOk = true;
              break;
            }
          }
          if (!isOk) {
            // Skip issue: not included inside the IDE
            continue;
          }
        }
        result.add(issue);
      }
      //noinspection AssignmentToStaticFieldFromInstanceMethod
      ourFilteredIssues = result;
    }

    return ourFilteredIssues;
  }
}
