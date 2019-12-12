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

import java.util.*


/**
 * Default implementation of artifact versioning.
 *
 * @author [Brett Porter](mailto:brett@apache.org)
 */
class DefaultArtifactVersion(version: String) : ArtifactVersion {
    private var _majorVersion: Int? = null
    private var _minorVersion: Int? = null
    private var _incrementalVersion: Int? = null
    private var _buildNumber: Int? = null

    override val majorVersion: Int
        get() = _majorVersion ?: 0
    override val minorVersion: Int
        get() = _minorVersion ?: 0
    override val incrementalVersion: Int
        get() = _incrementalVersion ?: 0
    override val buildNumber: Int
        get() = _buildNumber ?: 0
    override var qualifier: String? = null
        private set


    private var comparable: ComparableVersion? = null
    override fun hashCode(): Int {
        return 11 + comparable.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return if (other !is ArtifactVersion) {
            false
        } else compareTo(other) == 0
    }

    override fun compareTo(other: ArtifactVersion?): Int {
        return if (other is DefaultArtifactVersion) {
            comparable!!.compareTo(other.comparable)
        } else {
            compareTo(DefaultArtifactVersion(other.toString()))
        }
    }


    override fun parseVersion(version: String) {
        comparable = ComparableVersion(version)
        val index = version.indexOf('-')
        val part1: String
        var part2: String? = null
        if (index < 0) {
            part1 = version
        } else {
            part1 = version.substring(0, index)
            part2 = version.substring(index + 1)
        }
        if (part2 != null) {
            if (part2.length == 1 || !part2.startsWith("0")) {
                _buildNumber = tryParseInt(part2)
                if (_buildNumber == null) {
                    qualifier = part2
                }
            } else {
                qualifier = part2
            }
        }
        if (!part1.contains(".") && !part1.startsWith("0")) {
            _majorVersion = tryParseInt(part1)
            if (_majorVersion == null) {
                // qualifier is the whole version, including "-"

                qualifier = version
                _buildNumber = null
            }
        } else {
            var fallback = false
            val tok = StringTokenizer(part1, ".")
            if (tok.hasMoreTokens()) {
                _majorVersion = getNextIntegerToken(tok)
                if (_majorVersion == null) {
                    fallback = true
                }
            } else {
                fallback = true
            }
            if (tok.hasMoreTokens()) {
                _minorVersion = getNextIntegerToken(tok)
                if (_minorVersion == null) {
                    fallback = true
                }
            }
            if (tok.hasMoreTokens()) {
                _incrementalVersion = getNextIntegerToken(tok)
                if (_incrementalVersion == null) {
                    fallback = true
                }
            }
            if (tok.hasMoreTokens()) {
                qualifier = tok.nextToken()
                fallback = isDigits(qualifier)
            }

            // string tokenizer won't detect these and ignores them


            if (part1.contains("..") || part1.startsWith(".") || part1.endsWith(".")) {
                fallback = true
            }
            if (fallback) {
                // qualifier is the whole version, including "-"

                qualifier = version
                _majorVersion = null
                _minorVersion = null
                _incrementalVersion = null
                _buildNumber = null
            }
        }
    }

    override fun toString(): String {
        return comparable.toString()
    }

    companion object {
        private fun getNextIntegerToken(tok: StringTokenizer): Int? {
            val s = tok.nextToken()
            return if (s.length > 1 && s.startsWith("0")) {
                null
            } else tryParseInt(s)
        }

        private fun tryParseInt(s: String): Int? {
            // for performance, check digits instead of relying later on catching NumberFormatException
            return if (!isDigits(s)) {
                null
            } else try {
                val longValue = s.toLong()
                if (longValue > Integer.MAX_VALUE) {
                    null
                } else longValue.toInt()
            } catch (e: NumberFormatException) {
                // should never happen since checked isDigits(s) before

                null
            }
        }

        fun isDigits(string: String?) = string?.all { it.isDigit() } ?: false
    }

    init {
        parseVersion(version)
    }
}