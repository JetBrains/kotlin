/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl;

import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice;
import org.jetbrains.kotlin.util.slicedMap.MutableSlicedMap;
import org.jetbrains.kotlin.util.slicedMap.SlicedMapImpl;
import org.jetbrains.kotlin.util.slicedMap.Slices;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.Method;

public final class K1JvmSerializationBindings {
    public static final SerializationMappingSlice<FunctionDescriptor, Method> METHOD_FOR_FUNCTION =
            SerializationMappingSlice.create();
    public static final SerializationMappingSlice<PropertyDescriptor, Pair<Type, String>> FIELD_FOR_PROPERTY =
            SerializationMappingSlice.create();
    public static final SerializationMappingSlice<PropertyDescriptor, Method> SYNTHETIC_METHOD_FOR_PROPERTY =
            SerializationMappingSlice.create();
    public static final SerializationMappingSlice<PropertyDescriptor, Method> DELEGATE_METHOD_FOR_PROPERTY =
            SerializationMappingSlice.create();

    public static final class SerializationMappingSlice<K, V> extends BasicWritableSlice<K, V> {
        public SerializationMappingSlice() {
            super(Slices.ONLY_REWRITE_TO_EQUAL, false);
        }

        @NotNull
        public static <K, V> SerializationMappingSlice<K, V> create() {
            return new SerializationMappingSlice<>();
        }
    }

    static {
        BasicWritableSlice.initSliceDebugNames(K1JvmSerializationBindings.class);
    }

    private final MutableSlicedMap map = new SlicedMapImpl(false);

    public <K, V> void put(@NotNull SerializationMappingSlice<K, V> slice, @NotNull K key, @NotNull V value) {
        map.put(slice, key, value);
    }

    @Nullable
    public <K, V> V get(@NotNull SerializationMappingSlice<K, V> slice, @NotNull K key) {
        return map.get(slice, key);
    }
}
