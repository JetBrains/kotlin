/*
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  *  http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied.  See the License for the
  * specific language governing permissions and limitations
  * under the License.
  */
package org.jetbrains.kotlin.tools.projectWizard.settings.version.maven

/**
 * Describes an artifact version in terms of its components, converts it to/from a string and
 * compares two versions.
 *
 * @author [Brett Porter](mailto:brett@apache.org)
 */
interface ArtifactVersion : Comparable<ArtifactVersion?> {
    val majorVersion: Int
    val minorVersion: Int
    val incrementalVersion: Int
    val buildNumber: Int
    val qualifier: String?
    fun parseVersion(version: String)
}