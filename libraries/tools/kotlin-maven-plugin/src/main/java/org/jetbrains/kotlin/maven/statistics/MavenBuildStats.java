/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.statistics;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.config.KotlinCompilerVersion;
import org.jetbrains.kotlin.maven.Util;
import org.jetbrains.kotlin.statistics.BuildSessionLogger;
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics;
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics;
import org.jetbrains.kotlin.statistics.metrics.StringListMetrics;
import org.jetbrains.kotlin.statistics.metrics.StringMetrics;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * PoC of FUS analytics for the Maven plugin.
 * <p>
 * Every {@link MavenSession} owns its own {@link BuildSessionLogger}, so parallel builds (several Maven processes,
 * {@code -T} module parallelism, or several sessions in one JVM like mvnd) each produce their own profile file.
 * <p>
 * Reported events: environment ({@code executed_from_idea}, {@code os_type}, {@code os_version},
 * {@code cpu_number_of_cores}, {@code maven_jvm_heap_size}), versions ({@code maven_version},
 * {@code kotlin_maven_plugin_version}, {@code kotlin_compiler_version}), {@code project_path}, project walk
 * ({@code number_of_modules}, {@code number_of_subprojects}, {@code statistics_visit_all_projects_overhead}),
 * compilation ({@code compilation_started}, {@code compilations_count}, {@code kotlin_bta_used},
 * {@code kotlin_compiler_execution_policy}) and build finish ({@code build_failed}, {@code build_finish_time},
 * {@code maven_build_duration}, {@code maven_execution_duration}).
 * <p>
 * Location follows the Kotlin Gradle plugin convention: {@code <kotlin.user.home>/kotlin-profile}, where the
 * {@code kotlin.user.home} property (relative values are resolved against the root project directory) defaults to
 * {@code ~/.kotlin}.
 */
public final class MavenBuildStats {
    public static final String KOTLIN_USER_HOME_PROPERTY = "kotlin.user.home";
    public static final String ENABLE_PROPERTY = "enable_kotlin_performance_profile";

    @NotNull
    private static final Map<MavenSession, SessionStats> SESSIONS = Collections.synchronizedMap(new WeakHashMap<>());

    private MavenBuildStats() {
    }

    private static final class SessionStats {
        @NotNull
        final BuildSessionLogger logger;
        // timestamp of afterProjectsRead
        final long executionStartTime = System.currentTimeMillis();

        SessionStats(@NotNull BuildSessionLogger logger) {
            this.logger = logger;
        }
    }

    public static void startSession(@NotNull MavenSession session) {
        try {
            if (SESSIONS.containsKey(session) || !isEnabled(session)) return;

            BuildSessionLogger logger = new BuildSessionLogger(
                    new File(resolveKotlinUserHome(session), BuildSessionLogger.STATISTICS_FOLDER_NAME),
                    1000,
                    30L * 24 * 3600 * 1000,
                    false
            );
            logger.startBuildSession(UUID.randomUUID().toString());
            SessionStats stats = new SessionStats(logger);
            SESSIONS.put(session, stats);

            synchronized (stats.logger) {
                String name = subprojectName(session.getTopLevelProject());
                reportEnvironment(logger, name);
                reportVersions(logger, session, name);
                reportProjectInfo(logger, session, name);
            }
        } catch (Throwable ignored) {
            // analytics must never break the build
        }
    }

    private static void reportEnvironment(@NotNull BuildSessionLogger logger, @NotNull String name) {
        String osName = System.getProperty("os.name");
        if (osName != null) logger.report(StringMetrics.OS_TYPE, osName, name, null);
        String osVersion = System.getProperty("os.version");
        if (osVersion != null) logger.report(StringMetrics.OS_VERSION, osVersion, name, null);
        logger.report(NumericalMetrics.CPU_NUMBER_OF_CORES, (long) Runtime.getRuntime().availableProcessors(), name, null);
        logger.report(BooleanMetrics.EXECUTED_FROM_IDEA, System.getProperty("idea.active") != null, name, null);
        logger.report(NumericalMetrics.MAVEN_JVM_HEAP_SIZE, Runtime.getRuntime().maxMemory(), name, null);
    }

    private static void reportVersions(@NotNull BuildSessionLogger logger, @NotNull MavenSession session, @NotNull String name) {
        logger.report(StringMetrics.KOTLIN_COMPILER_VERSION, KotlinCompilerVersion.VERSION, name, null);
        String mavenVersion = session.getSystemProperties().getProperty("maven.version");
        if (mavenVersion != null) logger.report(StringMetrics.MAVEN_VERSION, mavenVersion, name, null);
        try {
            logger.report(StringMetrics.KOTLIN_MAVEN_PLUGIN_VERSION, Util.getMavenPluginVersion(), name, null);
        } catch (Exception ignored) {
            // plugin version is not available
        }
    }

    private static void reportProjectInfo(@NotNull BuildSessionLogger logger, @NotNull MavenSession session, @NotNull String name) {
        long walkStart = System.nanoTime();
        long modules = 0;
        for (MavenProject ignored : session.getProjects()) modules++;
        MavenProject root = session.getTopLevelProject();
        if (root != null && root.getBasedir() != null) {
            logger.report(StringMetrics.PROJECT_PATH, sha256(root.getBasedir().getAbsolutePath()), name, null);
        }
        logger.report(NumericalMetrics.NUMBER_OF_MODULES, modules, name, null);
        logger.report(NumericalMetrics.NUMBER_OF_SUBPROJECTS, modules, name, null);
        logger.report(NumericalMetrics.STATISTICS_VISIT_ALL_PROJECTS_OVERHEAD, (System.nanoTime() - walkStart) / 1_000_000, name, null);
    }

    /**
     * Reports how the compiler is executed. Called by compile mojos right before the compilation starts.
     */
    public static void reportCompilerExecution(@NotNull MavenSession session, @NotNull MavenProject project, boolean useDaemon) {
        try {
            SessionStats stats = SESSIONS.get(session);
            if (stats == null) return;
            synchronized (stats.logger) {
                String name = subprojectName(project);
                stats.logger.report(BooleanMetrics.KOTLIN_BTA_USED, true, name, null);
                stats.logger.report(StringListMetrics.KOTLIN_COMPILER_EXECUTION_POLICY, useDaemon ? "daemon" : "in-process", name, null);
            }
        } catch (Throwable ignored) {
            // analytics must never break the build
        }
    }

    public static void reportCompilationStarted(
            @NotNull MavenSession session,
            @NotNull MavenProject project,
            @NotNull MojoExecution execution
    ) {
        try {
            SessionStats stats = SESSIONS.get(session);
            if (stats == null) return;
            synchronized (stats.logger) {
                String name = subprojectName(project);
                stats.logger.report(BooleanMetrics.COMPILATION_STARTED, true, name, null);
                stats.logger.report(NumericalMetrics.COMPILATIONS_COUNT, 1L, name, null);
            }
        } catch (Throwable ignored) {
            // analytics must never break the build
        }
    }

    public static void finishSession(@NotNull MavenSession session) {
        try {
            SessionStats stats = SESSIONS.remove(session);
            if (stats == null) return;
            synchronized (stats.logger) {
                boolean failed = session.getResult() != null &&
                                 (session.getResult().hasExceptions());
                String name = subprojectName(session.getTopLevelProject());
                long finish = System.currentTimeMillis();
                if (session.getStartTime() != null) {
                    stats.logger.report(NumericalMetrics.MAVEN_BUILD_DURATION, finish - session.getStartTime().getTime(), name, null);
                }
                stats.logger.report(NumericalMetrics.MAVEN_EXECUTION_DURATION, finish - stats.executionStartTime, name, null);
                stats.logger.report(NumericalMetrics.BUILD_FINISH_TIME, finish, name, null);
                stats.logger.report(BooleanMetrics.BUILD_FAILED, failed, name, null);
                stats.logger.finishBuildSession();
            }
        } catch (Throwable ignored) {
            // analytics must never break the build
        }
    }

    @NotNull
    private static String subprojectName(@Nullable MavenProject project) {
        return sha256(project != null ? project.getArtifactId() : "unknown");
    }

    @NotNull
    private static String sha256(@NotNull String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Nullable
    private static String property(@NotNull MavenSession session, @NotNull String name) {
        String value = session.getUserProperties().getProperty(name);
        if (value == null) value = session.getSystemProperties().getProperty(name);
        if (value == null) value = System.getProperty(name);
        return value;
    }

    private static boolean isEnabled(@NotNull MavenSession session) {
        String value = property(session, ENABLE_PROPERTY);
        return value == null || Boolean.parseBoolean(value);
    }

    @NotNull
    static File resolveKotlinUserHome(@NotNull MavenSession session) {
        String configured = property(session, KOTLIN_USER_HOME_PROPERTY);
        if (configured != null && !configured.trim().isEmpty()) {
            File file = new File(configured);
            if (file.isAbsolute()) return file;
            MavenProject root = session.getTopLevelProject();
            File base = root != null && root.getBasedir() != null ? root.getBasedir() : new File(".");
            return new File(base, configured);
        }
        return new File(System.getProperty("user.home"), ".kotlin");
    }
}
