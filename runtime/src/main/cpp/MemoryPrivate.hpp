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

#ifndef RUNTIME_MEMORYPRIVATE_HPP
#define RUNTIME_MEMORYPRIVATE_HPP

#include "Memory.h"

extern "C" {

bool TryAddHeapRef(const ObjHeader* object);

MODEL_VARIANTS(void, ReleaseHeapRef, const ObjHeader* object);
MODEL_VARIANTS(void, ReleaseHeapRefNoCollect, const ObjHeader* object);

void Kotlin_ObjCExport_releaseAssociatedObject(void* associatedObject);

ForeignRefContext InitLocalForeignRef(ObjHeader* object);

ForeignRefContext InitForeignRef(ObjHeader* object);
void DeinitForeignRef(ObjHeader* object, ForeignRefContext context);

bool IsForeignRefAccessible(ObjHeader* object, ForeignRefContext context);

// Should be used when reference is read from a possibly shared variable,
// and there's nothing else keeping the object alive.
void AdoptReferenceFromSharedVariable(ObjHeader* object);

}  // extern "C"

#endif // RUNTIME_MEMORYPRIVATE_HPP
