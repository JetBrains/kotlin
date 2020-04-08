/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.codeInsight.gradle;

import org.gradle.util.GradleVersion;
import org.hamcrest.CustomMatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.tooling.annotation.PluginTargetVersions;
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions;
import org.jetbrains.plugins.gradle.tooling.util.VersionMatcher;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import java.lang.annotation.Annotation;

// modified copy of org.jetbrains.plugins.gradle.tooling.VersionMatcherRule
public class PluginTargetVersionsRule extends TestWatcher {

    private class TargetVersionsImpl implements TargetVersions {
        private final String value;

        TargetVersionsImpl(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public boolean checkBaseVersions() {
            return true;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }
    }


    @Nullable
    private CustomMatcher gradleVersionMatcher;

    @Nullable
    private CustomMatcher pluginVersionMatcher;


    @NotNull
    public boolean matches(String gradleVersion, String pluginVersion, boolean isLatestPluginVersion) {
        boolean matchGradleVersion = gradleVersionMatcher == null || gradleVersionMatcher.matches(gradleVersion);
        if (isLatestPluginVersion) {
            return matchGradleVersion;
        }
        boolean pluginVersionMatches = pluginVersionMatcher == null || pluginVersionMatcher.matches(pluginVersion);
        return matchGradleVersion && pluginVersionMatches;
    }

    @Override
    protected void starting(Description d) {
        final PluginTargetVersions pluginTargetVersions = d.getAnnotation(PluginTargetVersions.class);
        if (d.getAnnotation(TargetVersions.class) != null && pluginTargetVersions != null) {
            throw new IllegalArgumentException(String.format("Annotations %s and %s could not be used together. ",
                                                             TargetVersions.class.getName(), PluginTargetVersions.class.getName()));
        }
        if (pluginTargetVersions == null) return;

        gradleVersionMatcher = VersionMatcherRule.produceMatcher("Gradle", new TargetVersionsImpl(pluginTargetVersions.gradleVersion()));
        pluginVersionMatcher = VersionMatcherRule.produceMatcher("Plugin", new TargetVersionsImpl(pluginTargetVersions.pluginVersion()));
    }
}
