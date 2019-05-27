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

package org.jetbrains.kotlin.konan.properties

import org.jetbrains.kotlin.konan.target.*

fun Properties.hostString(name: String): String?
    = this.propertyString(name, HostManager.hostName)

fun Properties.hostList(name: String): List<String>
    = this.propertyList(name, HostManager.hostName)

fun Properties.targetString(name: String, target: KonanTarget): String?
    = this.propertyString(name, target.name)

fun Properties.targetList(name: String, target: KonanTarget): List<String>
    = this.propertyList(name, target.name)

fun Properties.hostTargetString(name: String, target: KonanTarget): String?
    = this.propertyString(name, hostTargetSuffix(HostManager.host, target))

fun Properties.hostTargetList(name: String, target: KonanTarget): List<String>
    = this.propertyList(name, hostTargetSuffix(HostManager.host, target))