// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.performance;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.importing.GradleImportingTestCase;

import java.util.Map;

public abstract class GradleImportPerformanceTestCase extends GradleImportingTestCase {

  protected static void assertTracedTimePercentAtLeast(@NotNull final Map<String, Long> trace, long time, int threshold) {
    final long tracedTime = trace.get("Gradle data obtained")
                            + trace.get("Gradle project data processed")
                            + trace.get("Data import total");

    double percent = (double)tracedTime / time * 100;
    assertTrue(String.format("Test time [%d] traced time [%d], percentage [%.2f]", time, tracedTime, percent),
               percent > threshold && percent < 100);
  }

}
