/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package kotlin.text.regex

/**
 * Base class for quantifiers.
 */
internal abstract class QuantifierSet(open var innerSet: AbstractSet, override var next: AbstractSet, type: Int)
    : SimpleSet(type) {

    override fun first(set: AbstractSet): Boolean =
        innerSet.first(set) || next.first(set)

    override fun hasConsumed(matchResult: MatchResultImpl): Boolean = true

    override fun processSecondPassInternal(): AbstractSet {
        val innerSet = this.innerSet
        if (innerSet.secondPassVisited) {
            this.innerSet = innerSet.processSecondPass()
        }

        return super.processSecondPassInternal()
    }
}
