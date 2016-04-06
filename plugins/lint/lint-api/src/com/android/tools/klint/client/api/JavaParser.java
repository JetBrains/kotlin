/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.tools.klint.client.api;

import com.android.annotations.NonNull;
import com.android.tools.klint.detector.api.JavaContext;
import com.google.common.annotations.Beta;
import org.jetbrains.uast.UFile;

/**
 * A wrapper for a Java parser. This allows tools integrating lint to map directly
 * to builtin services, such as already-parsed data structures in Java editors.
 * <p/>
 * <b>NOTE: This is not public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public abstract class JavaParser {
    public static final String TYPE_OBJECT = "java.lang.Object";        //$NON-NLS-1$
    public static final String TYPE_STRING = "java.lang.String";        //$NON-NLS-1$
    public static final String TYPE_INT = "int";                        //$NON-NLS-1$
    public static final String TYPE_LONG = "long";                      //$NON-NLS-1$
    public static final String TYPE_CHAR = "char";                      //$NON-NLS-1$
    public static final String TYPE_FLOAT = "float";                    //$NON-NLS-1$
    public static final String TYPE_DOUBLE = "double";                  //$NON-NLS-1$
    public static final String TYPE_BOOLEAN = "boolean";                //$NON-NLS-1$
    public static final String TYPE_SHORT = "short";                    //$NON-NLS-1$
    public static final String TYPE_BYTE = "byte";                      //$NON-NLS-1$
    public static final String TYPE_NULL = "null";                      //$NON-NLS-1$

    /**
     * Dispose any data structures held for the given context.
     * @param context information about the file previously parsed
     * @param compilationUnit the compilation unit being disposed
     */
    public void dispose(@NonNull JavaContext context, @NonNull UFile compilationUnit) {
    }
}
