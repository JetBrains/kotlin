/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin.lower.decoys

import androidx.compose.compiler.plugins.kotlin.ComposeCallableIds
import androidx.compose.compiler.plugins.kotlin.ComposeClassIds
import androidx.compose.compiler.plugins.kotlin.ComposeFqNames

object DecoyClassIds {
    val Decoy = ComposeClassIds.internalClassIdFor("Decoy")
    val DecoyImplementation = ComposeClassIds.internalClassIdFor("DecoyImplementation")
    val DecoyImplementationDefaultsBitMask =
        ComposeClassIds.internalClassIdFor("DecoyImplementationDefaultsBitMask")
}

object DecoyCallableIds {
    val illegalDecoyCallException =
        ComposeCallableIds.internalTopLevelCallableId("illegalDecoyCallException")
}

object DecoyFqNames {
    val Decoy = DecoyClassIds.Decoy.asSingleFqName()
    val DecoyImplementation = DecoyClassIds.DecoyImplementation.asSingleFqName()
    val DecoyImplementationDefaultsBitMask =
        DecoyClassIds.DecoyImplementationDefaultsBitMask.asSingleFqName()

    val CurrentComposerIntrinsic = ComposeFqNames.fqNameFor("\$get-currentComposer\$\$composable")
    val key = ComposeFqNames.fqNameFor("key\$composable")
}
