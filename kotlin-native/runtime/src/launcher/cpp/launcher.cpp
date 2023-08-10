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

#include <cstdlib>

#include "Memory.h"
#include "Natives.h"
#include "Runtime.h"
#include "KString.h"
#include "Types.h"
#include "Worker.h"

#include "launcher.h"

using namespace kotlin;

//--- Setup args --------------------------------------------------------------//

OBJ_GETTER(setupArgs, int argc, const char** argv) {
  // The count is one less, because we skip argv[0] which is the binary name.
  ObjHeader* result = AllocArrayInstance(theArrayTypeInfo, argc - 1, OBJ_RESULT);
  ArrayHeader* array = result->array();
  for (int index = 1; index < argc; index++) {
    ObjHolder result;
    CreateStringFromCString(argv[index], result.slot());
    UpdateHeapRef(ArrayAddressOfElementAt(array, index - 1), result.obj());
  }
  return result;
}

//--- main --------------------------------------------------------------------//
extern "C" KInt Konan_run_start(int argc, const char** argv) {
    ObjHolder args;
    setupArgs(argc, argv, args.slot());
    return Konan_start(args.obj());
}

extern "C" RUNTIME_USED int Init_and_run_start(int argc, const char** argv, int memoryDeInit) {
  Kotlin_initRuntimeIfNeeded();
  Kotlin_mm_switchThreadStateRunnable();

  KInt exitStatus = Konan_run_start(argc, argv);

  if (memoryDeInit) {
      Kotlin_shutdownRuntime();
  }

  return exitStatus;
}

#ifndef KONAN_ANDROID
extern "C" RUNTIME_USED int Konan_main(int argc, const char** argv) {
#else
extern "C" RUNTIME_USED int Konan_main_standalone(int argc, const char** argv) {
#endif
    return Init_and_run_start(argc, argv, 1);
}
