/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.text.harmony_regex

import kotlin.text.*
import kotlin.test.*

class PatternErrorTest {

    @Test fun testCompileErrors() {
        // empty regex string - no exception should be thrown
        Regex("")

        // note: invalid regex syntax checked in PatternSyntaxExceptionTest

        // flags = 0 should raise no exception
        Regex("foo", emptySet())

        // check that all valid flags accepted without exception
        val options = setOf(
                RegexOption.UNIX_LINES,
                RegexOption.IGNORE_CASE,
                RegexOption.MULTILINE,
                RegexOption.CANON_EQ,
                RegexOption.COMMENTS,
                RegexOption.DOT_MATCHES_ALL)
        Regex("foo", options)
    }

}
