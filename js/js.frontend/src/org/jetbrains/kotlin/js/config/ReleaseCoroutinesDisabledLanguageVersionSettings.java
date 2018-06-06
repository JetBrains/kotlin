/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.config.*;

public class ReleaseCoroutinesDisabledLanguageVersionSettings implements LanguageVersionSettings {
    @NotNull
    private final LanguageVersionSettings delegate;

    public ReleaseCoroutinesDisabledLanguageVersionSettings(@NotNull LanguageVersionSettings delegate) {
        this.delegate = delegate;
    }

    @NotNull
    @Override
    public LanguageFeature.State getFeatureSupport(@NotNull LanguageFeature feature) {
        if (feature.equals(LanguageFeature.ReleaseCoroutines)) {
            return LanguageFeature.State.DISABLED;
        }
        return delegate.getFeatureSupport(feature);
    }

    @Override
    public boolean isPreRelease() {
        return delegate.isPreRelease();
    }

    @Override
    public <T> T getFlag(@NotNull AnalysisFlag<? extends T> flag) {
        return delegate.getFlag(flag);
    }

    @NotNull
    @Override
    public ApiVersion getApiVersion() {
        return delegate.getApiVersion();
    }

    @NotNull
    @Override
    public LanguageVersion getLanguageVersion() {
        return delegate.getLanguageVersion();
    }

    @Override
    public boolean supportsFeature(@NotNull LanguageFeature feature) {
        if (feature.equals(LanguageFeature.ReleaseCoroutines)) {
            return false;
        }
        return delegate.supportsFeature(feature);
    }
}
