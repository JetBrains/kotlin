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
package com.android.tools.klint.detector.api;

import com.android.annotations.NonNull;
import com.google.common.annotations.Beta;

import java.util.EnumSet;

/**
 * An {@linkplain Implementation} of an {@link Issue} maps to the {@link Detector}
 * class responsible for analyzing the issue, as well as the {@link Scope} required
 * by the detector to perform its analysis.
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public class Implementation {
    private final Class<? extends Detector> mClass;
    private final EnumSet<Scope> mScope;
    private EnumSet<Scope>[] mAnalysisScopes;

    @SuppressWarnings("unchecked")
    private static final EnumSet<Scope>[] EMPTY = new EnumSet[0];

    /**
     * Creates a new implementation for analyzing one or more issues
     *
     * @param detectorClass the class of the detector to find this issue
     * @param scope the scope of files required to analyze this issue
     */
    @SuppressWarnings("unchecked")
    public Implementation(
            @NonNull Class<? extends Detector> detectorClass,
            @NonNull EnumSet<Scope> scope) {
        this(detectorClass, scope, EMPTY);
    }

    /**
     * Creates a new implementation for analyzing one or more issues
     *
     * @param detectorClass the class of the detector to find this issue
     * @param scope the scope of files required to analyze this issue
     * @param analysisScopes optional set of extra scopes the detector is capable of working in
     */
    public Implementation(
            @NonNull Class<? extends Detector> detectorClass,
            @NonNull EnumSet<Scope> scope,
            @NonNull EnumSet<Scope>... analysisScopes) {
        mClass = detectorClass;
        mScope = scope;
        mAnalysisScopes = analysisScopes;
    }

    /**
     * Returns the class of the detector to use to find this issue
     *
     * @return the class of the detector to use to find this issue
     */
    @NonNull
    public Class<? extends Detector> getDetectorClass() {
        return mClass;
    }

    @Override
    public String toString() {
        return mClass.toString();
    }

    /**
     * Returns the scope required to analyze the code to detect this issue.
     * This is determined by the detectors which reports the issue.
     *
     * @return the required scope
     */
    @NonNull
    public EnumSet<Scope> getScope() {
        return mScope;
    }

    /**
     * Returns the sets of scopes required to analyze this issue, or null if all
     * scopes named by {@link #getScope()} are necessary. Note that only
     * <b>one</b> match out of this collection is required, not all, and that
     * the scope set returned by {@link #getScope()} does not have to be returned
     * by this method, but is always implied to be included.
     * <p>
     * The scopes returned by {@link #getScope()} list all the various
     * scopes that are <b>affected</b> by this issue, meaning the detector
     * should consider it. Frequently, the detector must analyze all these
     * scopes in order to properly decide whether an issue is found. For
     * example, the unused resource detector needs to consider both the XML
     * resource files and the Java source files in order to decide if a resource
     * is unused. If it analyzes just the Java files for example, it might
     * incorrectly conclude that a resource is unused because it did not
     * discover a resource reference in an XML file.
     * <p>
     * However, there are other issues where the issue can occur in a variety of
     * files, but the detector can consider each in isolation. For example, the
     * API checker is affected by both XML files and Java class files (detecting
     * both layout constructor references in XML layout files as well as code
     * references in .class files). It doesn't have to analyze both; it is
     * capable of incrementally analyzing just an XML file, or just a class
     * file, without considering the other.
     * <p>
     * The required scope list provides a list of scope sets that can be used to
     * analyze this issue. For each scope set, all the scopes must be matched by
     * the incremental analysis, but any one of the scope sets can be analyzed
     * in isolation.
     * <p>
     * The required scope list is not required to include the full scope set
     * returned by {@link #getScope()}; that set is always assumed to be
     * included.
     * <p>
     * NOTE: You would normally call {@link #isAdequate(EnumSet)} rather
     * than calling this method directly.
     *
     * @return a list of required scopes, or null.
     */
    @NonNull
    public EnumSet<Scope>[] getAnalysisScopes() {
        return mAnalysisScopes;
    }

    /**
     * Returns true if the given scope is adequate for analyzing this issue.
     * This looks through the analysis scopes (see
     * {@link #getAnalysisScopes()}) and if the scope passed in fully
     * covers at least one of them, or if it covers the scope of the issue
     * itself (see {@link #getScope()}, which should be a superset of all the
     * analysis scopes) returns true.
     * <p>
     * The scope set returned by {@link #getScope()} lists all the various
     * scopes that are <b>affected</b> by this issue, meaning the detector
     * should consider it. Frequently, the detector must analyze all these
     * scopes in order to properly decide whether an issue is found. For
     * example, the unused resource detector needs to consider both the XML
     * resource files and the Java source files in order to decide if a resource
     * is unused. If it analyzes just the Java files for example, it might
     * incorrectly conclude that a resource is unused because it did not
     * discover a resource reference in an XML file.
     * <p>
     * However, there are other issues where the issue can occur in a variety of
     * files, but the detector can consider each in isolation. For example, the
     * API checker is affected by both XML files and Java class files (detecting
     * both layout constructor references in XML layout files as well as code
     * references in .class files). It doesn't have to analyze both; it is
     * capable of incrementally analyzing just an XML file, or just a class
     * file, without considering the other.
     * <p>
     * An issue can register additional scope sets that can are adequate
     * for analyzing the issue, by supplying it to
     * {@link #Implementation(Class, java.util.EnumSet, java.util.EnumSet[])}.
     * This method returns true if the given scope matches one or more analysis
     * scope, or the overall scope.
     *
     * @param scope the scope available for analysis
     * @return true if this issue can be analyzed with the given available scope
     */
    public boolean isAdequate(@NonNull EnumSet<Scope> scope) {
        if (scope.containsAll(mScope)) {
            return true;
        }

        if (mAnalysisScopes != null) {
            for (EnumSet<Scope> analysisScope : mAnalysisScopes) {
                if (scope.containsAll(analysisScope)) {
                    return true;
                }
            }
        }

        return false;
    }
}
