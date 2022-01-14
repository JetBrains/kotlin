/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

 // DTrace probes for K/N runtime

// Values of reasons and description is taken from https://docs.microsoft.com/en-us/dotnet/framework/performance/garbage-collection-etw-events
 provider kotlin_native_runtime {
    probe GCStart_V1(uint32_t, uint32_t, uint32_t, uint32_t, uint16_t); // (count, depth, reason, type, clrInstance)
    probe GCSuspendEE_V1(uint16_t); // (reason)
    probe GCSuspendEEEnd_V1();
    probe GCEnd_V1(uint32_t, uint32_t, uint16_t); // (count, depth, clrInstance)
 };