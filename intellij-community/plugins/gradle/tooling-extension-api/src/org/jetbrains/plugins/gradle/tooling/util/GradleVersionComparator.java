// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.util;

import gnu.trove.THashMap;
import gnu.trove.TObjectIntHashMap;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;

public class GradleVersionComparator implements Comparable<GradleVersion> {
  private final GradleVersion myVersion;
  private final TObjectIntHashMap<String> myResults = new TObjectIntHashMap<String>();
  private final THashMap<String, GradleVersion> myVersionsMap = new THashMap<String, GradleVersion>();

  public GradleVersionComparator(@NotNull GradleVersion gradleVersion) {
    myVersion = gradleVersion;
  }

  @Override
  public int compareTo(@NotNull GradleVersion gradleVersion) {
    if (myVersion == gradleVersion) return 0;
    String version = gradleVersion.getVersion();
    if (myVersion.getVersion().equals(version)) return 0;
    int result = myResults.get(version);
    if (result != 0) return result;

    result = myVersion.compareTo(gradleVersion);
    myResults.put(version, result);
    return result;
  }

  public boolean lessThan(@NotNull GradleVersion gradleVersion) {
    return compareTo(gradleVersion) < 0;
  }

  public boolean lessThan(@NotNull String gradleVersion) {
    return lessThan(getGradleVersion(gradleVersion));
  }

  public boolean isOrGreaterThan(@NotNull GradleVersion gradleVersion) {
    return compareTo(gradleVersion) >= 0;
  }

  public boolean isOrGreaterThan(@NotNull String gradleVersion) {
    return isOrGreaterThan(getGradleVersion(gradleVersion));
  }

  public boolean is(@NotNull GradleVersion gradleVersion) {
    return compareTo(gradleVersion) == 0;
  }

  public boolean is(@NotNull String gradleVersion) {
    return is(getGradleVersion(gradleVersion));
  }

  @NotNull
  private GradleVersion getGradleVersion(@NotNull String gradleVersion) {
    GradleVersion version = myVersionsMap.get(gradleVersion);
    if (version == null) {
      version = GradleVersion.version(gradleVersion);
      myVersionsMap.put(gradleVersion, version);
    }
    return version;
  }
}
