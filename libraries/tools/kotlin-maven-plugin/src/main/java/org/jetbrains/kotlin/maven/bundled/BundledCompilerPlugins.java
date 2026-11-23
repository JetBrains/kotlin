/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.bundled;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Compiler plugins that kotlin-maven-plugin can enable by name alone (KT-91851):
 * their Maven extension wrappers are bundled into this plugin's JAR, and the corresponding
 * compiler plugin JARs are resolved at runtime from the same repositories, so the user
 * does not have to declare extra {@code kotlin-maven-*} artifacts in the plugin's
 * {@code <dependencies>} block.
 */
public final class BundledCompilerPlugins {
    public static final String KOTLIN_GROUP_ID = "org.jetbrains.kotlin";

    public static final String NOARG_COMPILER_PLUGIN_ID = "org.jetbrains.kotlin.noarg";
    public static final String ALLOPEN_COMPILER_PLUGIN_ID = "org.jetbrains.kotlin.allopen";
    public static final String SAM_WITH_RECEIVER_COMPILER_PLUGIN_ID = "org.jetbrains.kotlin.samWithReceiver";
    public static final String LOMBOK_COMPILER_PLUGIN_ID = "org.jetbrains.kotlin.lombok";
    public static final String POWER_ASSERT_COMPILER_PLUGIN_ID = "org.jetbrains.kotlin.powerassert";
    public static final String SERIALIZATION_COMPILER_PLUGIN_ID = "org.jetbrains.kotlinx.serialization";
    public static final String DATAFRAME_COMPILER_PLUGIN_ID = "org.jetbrains.kotlin.dataframe";

    /**
     * Maven plugin name -> the wrapper artifact that used to be required as a plugin {@code <dependency>}.
     * Used to detect poms that still provide the compiler plugin the old way (backward compatibility).
     */
    private static final Map<String, String> WRAPPER_ARTIFACTS = new LinkedHashMap<>();

    /**
     * Maven plugin name -> the compiler plugin artifact (group {@link #KOTLIN_GROUP_ID}, version of
     * kotlin-maven-plugin itself) that is resolved at runtime and passed to the compiler via {@code -Xplugin}.
     */
    private static final Map<String, String> COMPILER_PLUGIN_ARTIFACTS = new LinkedHashMap<>();

    static {
        WRAPPER_ARTIFACTS.put("no-arg", "kotlin-maven-noarg");
        WRAPPER_ARTIFACTS.put("jpa", "kotlin-maven-noarg");
        WRAPPER_ARTIFACTS.put("all-open", "kotlin-maven-allopen");
        WRAPPER_ARTIFACTS.put("spring", "kotlin-maven-allopen");
        WRAPPER_ARTIFACTS.put("sam-with-receiver", "kotlin-maven-sam-with-receiver");
        WRAPPER_ARTIFACTS.put("lombok", "kotlin-maven-lombok");
        WRAPPER_ARTIFACTS.put("power-assert", "kotlin-maven-power-assert");
        WRAPPER_ARTIFACTS.put("kotlinx-serialization", "kotlin-maven-serialization");
        WRAPPER_ARTIFACTS.put("kotlin-dataframe", "kotlin-maven-dataframe");

        COMPILER_PLUGIN_ARTIFACTS.put("no-arg", "kotlin-noarg-compiler-plugin-embeddable");
        COMPILER_PLUGIN_ARTIFACTS.put("jpa", "kotlin-noarg-compiler-plugin-embeddable");
        COMPILER_PLUGIN_ARTIFACTS.put("all-open", "kotlin-allopen-compiler-plugin-embeddable");
        COMPILER_PLUGIN_ARTIFACTS.put("spring", "kotlin-allopen-compiler-plugin-embeddable");
        COMPILER_PLUGIN_ARTIFACTS.put("sam-with-receiver", "kotlin-sam-with-receiver-compiler-plugin-embeddable");
        COMPILER_PLUGIN_ARTIFACTS.put("lombok", "kotlin-lombok-compiler-plugin-embeddable");
        COMPILER_PLUGIN_ARTIFACTS.put("power-assert", "kotlin-power-assert-compiler-plugin-embeddable");
        COMPILER_PLUGIN_ARTIFACTS.put("kotlinx-serialization", "kotlin-serialization-compiler-plugin-embeddable");
        COMPILER_PLUGIN_ARTIFACTS.put("kotlin-dataframe", "kotlin-dataframe-compiler-plugin-embeddable");
    }

    private BundledCompilerPlugins() {}

    public static boolean isBundledPluginName(@NotNull String pluginName) {
        return COMPILER_PLUGIN_ARTIFACTS.containsKey(pluginName);
    }

    /** The wrapper artifact that used to provide this plugin as a plugin {@code <dependency>}, or {@code null}. */
    @Nullable
    public static String getWrapperArtifactId(@NotNull String pluginName) {
        return WRAPPER_ARTIFACTS.get(pluginName);
    }

    /** The compiler plugin artifact to resolve at runtime for this plugin name, or {@code null} for custom extensions. */
    @Nullable
    public static String getCompilerPluginArtifactId(@NotNull String pluginName) {
        return COMPILER_PLUGIN_ARTIFACTS.get(pluginName);
    }

    /**
     * The effective set of plugin names: the names from {@code <compilerPlugins>} plus the plugins they imply
     * (the "jpa" compiler plugin also applies "all-open", as in {@link org.jetbrains.kotlin.maven.KotlinCompileMojoBase}).
     */
    @NotNull
    public static Set<String> expandPluginNames(@Nullable Collection<String> pluginNames) {
        if (pluginNames == null || pluginNames.isEmpty()) return Collections.emptySet();

        Set<String> result = new LinkedHashSet<>(pluginNames);
        // "jpa" compiler plugin should also apply 'all-open' plugin
        if (result.contains("jpa")) {
            result.add("all-open");
        }
        return result;
    }
}
