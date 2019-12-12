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
 * Describes a restriction in versioning.
 *
 * @author [Brett Porter](mailto:brett@apache.org)
 */
class Restriction(
    val lowerBound: ArtifactVersion?,
    val isLowerBoundInclusive: Boolean,
    val upperBound: ArtifactVersion?,
    val isUpperBoundInclusive: Boolean
) {

    fun containsVersion(version: ArtifactVersion): Boolean {
        if (lowerBound != null) {
            val comparison = lowerBound.compareTo(version)
            if (comparison == 0 && !isLowerBoundInclusive) {
                return false
            }
            if (comparison > 0) {
                return false
            }
        }
        if (upperBound != null) {
            val comparison = upperBound.compareTo(version)
            if (comparison == 0 && !isUpperBoundInclusive) {
                return false
            }
            if (comparison < 0) {
                return false
            }
        }
        return true
    }

    override fun hashCode(): Int {
        var result = 13
        result += lowerBound?.hashCode() ?: 1
        result *= if (isLowerBoundInclusive) 1 else 2
        result -= upperBound?.hashCode() ?: 3
        result *= if (isUpperBoundInclusive) 2 else 3
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Restriction) {
            return false
        }
        if (lowerBound != null) {
            if (lowerBound != other.lowerBound) {
                return false
            }
        } else if (other.lowerBound != null) {
            return false
        }
        if (isLowerBoundInclusive != other.isLowerBoundInclusive) {
            return false
        }
        if (upperBound != null) {
            if (upperBound != other.upperBound) {
                return false
            }
        } else if (other.upperBound != null) {
            return false
        }
        return isUpperBoundInclusive == other.isUpperBoundInclusive
    }

    override fun toString(): String {
        val buf = StringBuilder()
        buf.append(if (isLowerBoundInclusive) '[' else '(')
        if (lowerBound != null) {
            buf.append(lowerBound.toString())
        }
        buf.append(',')
        if (upperBound != null) {
            buf.append(upperBound.toString())
        }
        buf.append(if (isUpperBoundInclusive) ']' else ')')
        return buf.toString()
    }

    companion object {
        val EVERYTHING = Restriction(null, false, null, false)
    }
}