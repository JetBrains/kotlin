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
import org.jetbrains.kotlin.statistics.BuildSessionLogger;
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics;
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics;
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
 * Reported events: {@code kotlin_compiler_version}, {@code number_of_subprojects}, {@code compilation_started},
 * {@code compilations_count}, {@code build_failed}.
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
        final long startTime = System.currentTimeMillis();

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
                logger.report(StringMetrics.KOTLIN_COMPILER_VERSION, KotlinCompilerVersion.VERSION, name, null);
                logger.report(NumericalMetrics.NUMBER_OF_SUBPROJECTS, session.getProjects().size(), name, null);
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
                stats.logger.report(BooleanMetrics.BUILD_FAILED, failed, subprojectName(session.getTopLevelProject()), null);
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
