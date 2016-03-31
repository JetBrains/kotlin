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

package com.android.tools.lint.client.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Scope;
import com.google.common.annotations.Beta;

import java.io.File;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * Information about a request to run lint
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public class LintRequest {
    @NonNull
    protected final LintClient mClient;

    @NonNull
    protected final List<File> mFiles;

    @Nullable
    protected EnumSet<Scope> mScope;

    @Nullable
    protected Boolean mReleaseMode;

    @Nullable
    protected Collection<Project> mProjects;

    /**
     * Creates a new {@linkplain LintRequest}, to be passed to a {@link LintDriver}
     *
     * @param client the tool wrapping the analyzer, such as an IDE or a CLI
     * @param files the set of files to check with lint. This can reference Android projects,
     *          or directories containing Android projects, or individual XML or Java files
     *          (typically for incremental IDE analysis).
     */
    public LintRequest(@NonNull LintClient client, @NonNull List<File> files) {
        mClient = client;
        mFiles = files;
    }

    /**
     * Returns the lint client requesting the lint check
     *
     * @return the client, never null
     */
    @NonNull
    public LintClient getClient() {
        return mClient;
    }

    /**
     * Returns the set of files to check with lint. This can reference Android projects,
     * or directories containing Android projects, or individual XML or Java files
     * (typically for incremental IDE analysis).
     *
     * @return the set of files to check, should not be empty
     */
    @NonNull
    public List<File> getFiles() {
        return mFiles;
    }

    /**
     * Sets the scope to use; lint checks which require a wider scope set
     * will be ignored
     *
     * @return the scope to use, or null to use the default
     */
    @Nullable
    public EnumSet<Scope> getScope() {
        return mScope;
    }

    /**
     * Sets the scope to use; lint checks which require a wider scope set
     * will be ignored
     *
     * @param scope the scope
     * @return this, for constructor chaining
     */
    @NonNull
    public LintRequest setScope(@Nullable EnumSet<Scope> scope) {
        mScope = scope;
        return this;
    }

    /**
     * Returns {@code true} if lint is invoked as part of a release mode build,
     * {@code false}  if it is part of a debug mode build, and {@code null} if
     * the release mode is not known
     *
     * @return true if this lint is running in release mode, null if not known
     */
    @Nullable
    public Boolean isReleaseMode() {
        return mReleaseMode;
    }

    /**
     * Sets the release mode. Use {@code true} if lint is invoked as part of a
     * release mode build, {@code false} if it is part of a debug mode build,
     * and {@code null} if the release mode is not known
     *
     * @param releaseMode true if this lint is running in release mode, null if not known
     * @return this, for constructor chaining
     */
    @NonNull
    public LintRequest setReleaseMode(@Nullable Boolean releaseMode) {
        mReleaseMode = releaseMode;
        return this;
    }

    /**
     * Gets the projects for the lint requests. This is optional; if not provided lint will search
     * the {@link #getFiles()} directories and look for projects via {@link
     * LintClient#isProjectDirectory(java.io.File)}. However, this method allows a lint client to
     * set up all the projects ahead of time, and associate those projects with native resources
     * (in an IDE for example, each lint project can be associated with the corresponding IDE
     * project).
     *
     * @return a collection of projects, or null
     */
    @Nullable
    public Collection<Project> getProjects() {
        return mProjects;
    }

    /**
     * Sets the projects for the lint requests. This is optional; if not provided lint will search
     * the {@link #getFiles()} directories and look for projects via {@link
     * LintClient#isProjectDirectory(java.io.File)}. However, this method allows a lint client to
     * set up all the projects ahead of time, and associate those projects with native resources
     * (in an IDE for example, each lint project can be associated with the corresponding IDE
     * project).
     *
     * @param projects a collection of projects, or null
     */
    public void setProjects(@Nullable Collection<Project> projects) {
        mProjects = projects;
    }

    /**
     * Returns the project to be used as the main project during analysis. This is
     * usually the project itself, but when you are for example analyzing a library project,
     * it can be the app project using the library.
     *
     * @param project the project to look up the main project for
     * @return the main project
     */
    @NonNull
    public Project getMainProject(@NonNull Project project) {
        return project;
    }
}
