/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public class FastTracingHelper {
    private static final MethodHandle uuidGetter;
    private static final MethodHandle currentPacketArrayGetter;
    private static final MethodHandle packetsGetter;
    private static final MethodHandle fillCountGetter;
    private static final MethodHandle typeGetter;
    private static final MethodHandle timestampGetter;

    static {
        MethodHandle uid = null;
        MethodHandle cpa = null;
        MethodHandle pk = null;
        MethodHandle fc = null;
        MethodHandle ty = null;
        MethodHandle tsg = null;

        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class<?> trackClass = Class.forName("androidx.tracing.Track");
            Class<?> traceEventClass = Class.forName("androidx.tracing.TraceEvent");
            Class<?> packetArrayClass = Class.forName("androidx.tracing.PooledTracePacketArray");

            Field uuidField = trackClass.getDeclaredField("uuid");
            uuidField.setAccessible(true);
            uid = lookup.unreflectGetter(uuidField);

            Field currentPacketArrayField = trackClass.getDeclaredField("currentPacketArray");
            currentPacketArrayField.setAccessible(true);
            cpa = lookup.unreflectGetter(currentPacketArrayField);

            Field packetsField = packetArrayClass.getDeclaredField("packets");
            packetsField.setAccessible(true);
            pk = lookup.unreflectGetter(packetsField);

            Field fillCountField = packetArrayClass.getDeclaredField("fillCount");
            fillCountField.setAccessible(true);
            fc = lookup.unreflectGetter(fillCountField);

            Field typeField = traceEventClass.getDeclaredField("type");
            typeField.setAccessible(true);
            ty = lookup.unreflectGetter(typeField);

            Field timestampField = traceEventClass.getDeclaredField("timestamp");
            timestampField.setAccessible(true);
            tsg = lookup.unreflectGetter(timestampField);
        } catch (Throwable t) {
            // Ignore if tracing is not available
        }

        uuidGetter = uid;
        currentPacketArrayGetter = cpa;
        packetsGetter = pk;
        fillCountGetter = fc;
        typeGetter = ty;
        timestampGetter = tsg;
    }

    public static long getTrackUuid(Object track) throws Throwable {
        if (uuidGetter == null) return 0L;
        return (long) uuidGetter.invoke(track);
    }

    public static Long getParentPhaseStartTimestamp(Object track) {
        try {
            if (currentPacketArrayGetter == null) return null;
            Object packetArray = currentPacketArrayGetter.invoke(track);
            if (packetArray == null) return null;
            Object[] packets = (Object[]) packetsGetter.invoke(packetArray);
            int fillCount = (int) fillCountGetter.invoke(packetArray);

            for (int i = fillCount - 1; i >= 0; i--) {
                Object event = packets[i];
                if (event == null) continue;
                int type = (int) typeGetter.invoke(event);
                if (type == 1) { // Begin Section
                    return (Long) timestampGetter.invoke(event);
                }
            }
        } catch (Throwable t) {
            // Ignore
        }
        return null;
    }
}
