/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.tooling.util;

import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions;

/**
 * @author Vladislav.Soroka
 */
public class VersionMatcher {

  private static final String RANGE_TOKEN = " <=> ";

  @NotNull
  private final GradleVersion myGradleVersion;

  public VersionMatcher(@NotNull GradleVersion version) {
    myGradleVersion = version;
  }

  public boolean isVersionMatch(@Nullable TargetVersions targetVersions) {
    if (targetVersions == null) return true;
    return isVersionMatch(targetVersions.value(), targetVersions.checkBaseVersions());
  }

  public boolean isVersionMatch(@Nullable String targetVersions, boolean checkBaseVersions) {
    if (targetVersions == null || targetVersions.isEmpty()) return true;

    final GradleVersion current = adjust(myGradleVersion, checkBaseVersions);

    if (targetVersions.endsWith("+")) {
      String minVersion = targetVersions.substring(0, targetVersions.length() - 1);
      return compare(current, minVersion, checkBaseVersions) >= 0;
    }
    else if (targetVersions.startsWith("<")) {
      if (targetVersions.startsWith("<=")) {
        String maxVersion = targetVersions.substring(2);
        return compare(current, maxVersion, checkBaseVersions) <= 0;
      }
      else {
        String maxVersion = targetVersions.substring(1);
        return compare(current, maxVersion, checkBaseVersions) < 0;
      }
    }
    else {
      final int rangeIndex = targetVersions.indexOf(RANGE_TOKEN);
      if (rangeIndex != -1) {
        String minVersion = targetVersions.substring(0, rangeIndex);
        String maxVersion = targetVersions.substring(rangeIndex + RANGE_TOKEN.length());
        return compare(current, minVersion, checkBaseVersions) >= 0 &&
          compare(current, maxVersion, checkBaseVersions) <= 0;
      }
      else {
        return compare(current, targetVersions, checkBaseVersions) == 0;
      }
    }
  }


  private static int compare(@NotNull GradleVersion gradleVersion, @NotNull String otherGradleVersion, boolean checkBaseVersions) {
    return gradleVersion.compareTo(adjust(GradleVersion.version(otherGradleVersion), checkBaseVersions));
  }

  private static GradleVersion adjust(@NotNull GradleVersion version, boolean checkBaseVersions) {
    return checkBaseVersions ? version.getBaseVersion() : version;
  }
}
