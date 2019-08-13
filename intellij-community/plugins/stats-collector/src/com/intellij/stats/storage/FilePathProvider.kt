/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.stats.storage

import java.io.File

abstract class FilePathProvider {

    /**
     * Provides unique file where data should be temporarily stored, until it is send to log service
     */
    abstract fun getUniqueFile(): File

    /**
     * Returns all files with data to send
     */
    abstract fun getDataFiles(): List<File>

    /**
     * Returns root directory where files with logs are stored
     */
    abstract fun getStatsDataDirectory(): File


    /**
     * If user was offline for a long time we don't want to store all 1000 files,
     * instead we will store only last 2Mb of data
     */
    abstract fun cleanupOldFiles()


}