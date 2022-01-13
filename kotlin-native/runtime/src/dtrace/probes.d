/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

 // DTrace probes for K/N runtime

 provider kotlin_native_runtime {
    probe GCStart(uint32_t, uint32_t); // (count, reason)
 };