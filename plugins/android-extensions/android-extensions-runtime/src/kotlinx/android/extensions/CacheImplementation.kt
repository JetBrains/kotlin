/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.android.extensions

/**
 * Caching mechanism for [LayoutContainer] implementations, and also for the types directly supported by Android Extensions,
 * such as [android.app.Activity] or [android.app.Fragment].
 */
public enum class CacheImplementation {
    /** Use [android.util.SparseArray] as a backing store for the resolved views. */
    SPARSE_ARRAY,
    /** Use [HashMap] as a backing store for the resolved views (default). */
    HASH_MAP,
    /** Do not cache views for this layout. */
    NO_CACHE;

    companion object {
        /** The default cache implementation is [HASH_MAP]. */
        val DEFAULT = HASH_MAP
    }
}