/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.modules

import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.kotlin.jps.build.KotlinBuilderModuleScriptGenerator.getRelatedProductionModule

fun TargetId(moduleBuildTarget: ModuleBuildTarget): TargetId {
    // Since IDEA 2016 each gradle source root is imported as a separate module.
    // One gradle module X is imported as two JPS modules:
    // 1. X-production with one production target;
    // 2. X-test with one test target.
    // This breaks kotlin code since internal members' names are mangled using module name.
    // For example, a declaration of a function 'f' in 'X-production' becomes 'fXProduction', but a call 'f' in 'X-test' becomes 'fXTest()'.
    // The workaround is to replace a name of such test target with the name of corresponding production module.
    // See KT-11993.
    val name = getRelatedProductionModule(moduleBuildTarget.module)?.name ?: moduleBuildTarget.id
    return TargetId(name, moduleBuildTarget.targetType.typeId)
}
