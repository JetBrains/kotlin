/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Maven artifacts of the compiler plugin wrappers provided by kotlin-maven-plugin itself,
 * so users do not need to declare them as plugin dependencies.
 */
public final class CompilerPluginWrapperArtifacts {
    public static final String KOTLIN_GROUP_ID = "org.jetbrains.kotlin";

    public static final Set<String> WRAPPER_ARTIFACT_IDS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            "kotlin-maven-allopen",
            "kotlin-maven-noarg",
            "kotlin-maven-sam-with-receiver",
            "kotlin-maven-lombok",
            "kotlin-maven-power-assert",
            "kotlin-maven-serialization",
            "kotlin-maven-dataframe"
    )));

    private CompilerPluginWrapperArtifacts() {}

    public static boolean isWrapperArtifact(String groupId, String artifactId) {
        return KOTLIN_GROUP_ID.equals(groupId) && WRAPPER_ARTIFACT_IDS.contains(artifactId);
    }

    public static String syncWarning(String artifactId) {
        return redundantDependencyWarning(artifactId) + " (reported during project setup)";
    }

    public static String mojoWarning(String artifactId) {
        return redundantDependencyWarning(artifactId) + " (reported during compilation)";
    }

    private static String redundantDependencyWarning(String artifactId) {
        return "Redundant compiler plugin dependency: '" + artifactId +
                "' is already provided by the Kotlin Maven plugin and can be removed";
    }
}
