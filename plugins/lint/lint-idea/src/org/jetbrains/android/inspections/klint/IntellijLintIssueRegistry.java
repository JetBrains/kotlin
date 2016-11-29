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
package org.jetbrains.android.inspections.klint;

import com.android.annotations.NonNull;
import com.android.tools.klint.checks.*;
import com.android.tools.klint.detector.api.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.jetbrains.android.inspections.klint.IntellijLintProject.*;

/**
 * Custom version of the {@link BuiltinIssueRegistry}. This
 * variation will filter the default issues and remove
 * any issues that aren't usable inside IDEA (e.g. they
 * rely on class files), and it will also replace the implementation
 * of some issues with IDEA specific ones.
 */
public class IntellijLintIssueRegistry extends BuiltinIssueRegistry {
  private static final Implementation DUMMY_IMPLEMENTATION = new Implementation(Detector.class,
                                                                                EnumSet.noneOf(Scope.class));

  private static final String CUSTOM_EXPLANATION =
    "When custom (third-party) lint rules are integrated in the IDE, they are not available as native IDE inspections, " +
    "so the explanation text (which must be statically registered by a plugin) is not available. As a workaround, run the " +
    "lint target in Gradle instead; the HTML report will include full explanations.";

  /**
   * Issue reported by a custom rule (3rd party detector). We need a placeholder issue to reference for inspections, which
   * have to be registered statically (can't load these on the fly from custom jars the way lint does)
   */
  @NonNull
  public static final Issue CUSTOM_WARNING = Issue.create(
    "CustomWarning", "Warning from Custom Rule", CUSTOM_EXPLANATION, Category.CORRECTNESS, 5, Severity.WARNING, DUMMY_IMPLEMENTATION);

  @NonNull
  public static final Issue CUSTOM_ERROR = Issue.create(
    "CustomError", "Error from Custom Rule", CUSTOM_EXPLANATION, Category.CORRECTNESS, 5, Severity.ERROR, DUMMY_IMPLEMENTATION);

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
          //issue.setImplementation(IntellijApiDetector.IMPLEMENTATION);
        } else if (detectorClass == ViewTypeDetector.class) {
          issue.setImplementation(IntellijViewTypeDetector.IMPLEMENTATION);
        }
        if (detectorClass == SupportAnnotationDetector.class) {
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
