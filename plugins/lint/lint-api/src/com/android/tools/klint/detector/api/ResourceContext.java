/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.tools.lint.detector.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.client.api.LintDriver;
import com.google.common.annotations.Beta;

import java.io.File;

/**
 * A {@link com.android.tools.lint.detector.api.Context} used when checking resource files
 * (both bitmaps and XML files; for XML files a subclass of this context is used:
 * {@link com.android.tools.lint.detector.api.XmlContext}.)
 * <p/>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public class ResourceContext extends Context {
    private final ResourceFolderType mFolderType;

    /**
     * Construct a new {@link com.android.tools.lint.detector.api.ResourceContext}
     *
     * @param driver the driver running through the checks
     * @param project the project containing the file being checked
     * @param main the main project if this project is a library project, or
     *            null if this is not a library project. The main project is
     *            the root project of all library projects, not necessarily the
     *            directly including project.
     * @param file the file being checked
     * @param folderType the {@link com.android.resources.ResourceFolderType} of this file, if any
     */
    public ResourceContext(
            @NonNull LintDriver driver,
            @NonNull Project project,
            @Nullable Project main,
            @NonNull File file,
            @Nullable ResourceFolderType folderType) {
        super(driver, project, main, file);
        mFolderType = folderType;
    }

    /**
     * Returns the resource folder type of this XML file, if any.
     *
     * @return the resource folder type or null
     */
    @Nullable
    public ResourceFolderType getResourceFolderType() {
        return mFolderType;
    }

    /**
     * Returns the folder version. For example, for the file values-v14/foo.xml,
     * it returns 14.
     *
     * @return the folder version, or -1 if no specific version was specified
     */
    public int getFolderVersion() {
        return mDriver.getResourceFolderVersion(file);
    }
}
