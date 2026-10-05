/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Detects whether the build runs on a CI server by checking if any of the {@link #CI_MARKERS} are present
 * either as an environment variable or as a JVM system property.
 */
// TODO KT-89996: Extract and reuse the CI detection logic
final class CiEnvironment {
    private static final List<String> CI_MARKERS = Arrays.asList(
            "CI",
            "JENKINS_URL",
            "HUDSON_URL",
            "TEAMCITY_VERSION",
            "CIRCLE_BUILD_URL",
            "bamboo_resultsUrl",
            "GITHUB_ACTIONS",
            "GITLAB_CI",
            "TRAVIS_JOB_ID",
            "BITRISE_BUILD_URL",
            "GO_SERVER_URL",
            "TF_BUILD",
            "BUILDKITE"
    );

    private CiEnvironment() {
    }

    static boolean isCiBuild() {
        return detectedCiMarker(System::getenv, System::getProperty) != null;
    }

    @Nullable
    static String detectedCiMarker(@NotNull Function<String, String> env, @NotNull Function<String, String> sysProps) {
        for (String marker : CI_MARKERS) {
            if (env.apply(marker) != null || sysProps.apply(marker) != null) {
                return marker;
            }
        }
        return null;
    }
}
