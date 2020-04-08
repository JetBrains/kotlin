/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.diagnostics.Severity;
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo;
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice;
import org.jetbrains.kotlin.util.slicedMap.SlicedMap;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;

import java.util.Collection;
import java.util.Collections;

public class DummyTraces {
    public static final BindingTrace DUMMY_TRACE = new BindingTrace() {
        @NotNull
        @Override
        public BindingContext getBindingContext() {
            return new BindingContext() {

                @NotNull
                @Override
                public Diagnostics getDiagnostics() {
                    return Diagnostics.Companion.getEMPTY();
                }

                @Override
                public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
                    return DUMMY_TRACE.get(slice, key);
                }

                @NotNull
                @Override
                public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
                    return DUMMY_TRACE.getKeys(slice);
                }

                @NotNull
                @TestOnly
                @Override
                public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice) {
                    return ImmutableMap.of();
                }

                @Nullable
                @Override
                public KotlinType getType(@NotNull KtExpression expression) {
                    return DUMMY_TRACE.getType(expression);
                }

                @Override
                public void addOwnDataTo(@NotNull BindingTrace trace, boolean commitDiagnostics) {
                    // do nothing
                }
            };
        }

        @Override
        public <K, V> void record(WritableSlice<K, V> slice, K key, V value) {
        }

        @Override
        public <K> void record(WritableSlice<K, Boolean> slice, K key) {
        }

        @Override
        @SuppressWarnings("unchecked")
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            if (slice == BindingContext.PROCESSED) return (V) Boolean.FALSE;
            return SlicedMap.DO_NOTHING.get(slice, key);
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            assert slice.isCollective();
            return Collections.emptySet();
        }

        @Nullable
        @Override
        public KotlinType getType(@NotNull KtExpression expression) {
            KotlinTypeInfo typeInfo = get(BindingContext.EXPRESSION_TYPE_INFO, expression);
            return typeInfo != null ? typeInfo.getType() : null;
        }

        @Override
        public void recordType(@NotNull KtExpression expression, @Nullable KotlinType type) {
        }

        @Override
        public void report(@NotNull Diagnostic diagnostic) {
            if (Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS.contains(diagnostic.getFactory())) {
                throw new IllegalStateException("Unresolved: " + diagnostic.getPsiElement().getText());
            }
        }

        @Override
        public boolean wantsDiagnostics() {
            return false;
        }
    };

    public static BindingTrace DUMMY_EXCEPTION_ON_ERROR_TRACE = new BindingTrace() {
        @NotNull
        @Override
        public BindingContext getBindingContext() {
            return new BindingContext() {
                @NotNull
                @Override
                public Diagnostics getDiagnostics() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
                    return DUMMY_EXCEPTION_ON_ERROR_TRACE.get(slice, key);
                }

                @NotNull
                @Override
                public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
                    return DUMMY_EXCEPTION_ON_ERROR_TRACE.getKeys(slice);
                }

                @NotNull
                @TestOnly
                @Override
                public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice) {
                    return ImmutableMap.of();
                }

                @Nullable
                @Override
                public KotlinType getType(@NotNull KtExpression expression) {
                    return DUMMY_EXCEPTION_ON_ERROR_TRACE.getType(expression);
                }

                @Override
                public void addOwnDataTo(@NotNull BindingTrace trace, boolean commitDiagnostics) {
                    // do nothing
                }
            };
        }

        @Override
        public <K, V> void record(WritableSlice<K, V> slice, K key, V value) {
        }

        @Override
        public <K> void record(WritableSlice<K, Boolean> slice, K key) {
        }

        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            return null;
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            assert slice.isCollective();
            return Collections.emptySet();
        }

        @Nullable
        @Override
        public KotlinType getType(@NotNull KtExpression expression) {
            return null;
        }

        @Override
        public void recordType(@NotNull KtExpression expression, @Nullable KotlinType type) {
        }

        @Override
        public void report(@NotNull Diagnostic diagnostic) {
            if (diagnostic.getSeverity() == Severity.ERROR) {
                throw new IllegalStateException(DefaultErrorMessages.render(diagnostic));
            }
        }

        @Override
        public boolean wantsDiagnostics() {
            return true;
        }
    };
}
