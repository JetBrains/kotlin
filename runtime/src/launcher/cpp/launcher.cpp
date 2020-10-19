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

#include "Cleaner.h"
#include "Memory.h"
#include "Natives.h"
#include "Runtime.h"
#include "KString.h"
#include "Types.h"
#include "Worker.h"

#ifndef KONAN_ANDROID

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
extern "C" KInt Konan_start(const ObjHeader*);

extern "C" KInt Konan_run_start(int argc, const char** argv) {
    ObjHolder args;
    setupArgs(argc, argv, args.slot());
    return Konan_start(args.obj());
}

extern "C" RUNTIME_USED int Init_and_run_start(int argc, const char** argv, int memoryDeInit) {
#ifdef KONAN_NO_CTORS_SECTION
  extern void _Konan_constructors(void);
  _Konan_constructors();
#endif

  Kotlin_initRuntimeIfNeeded();

  KInt exitStatus = Konan_run_start(argc, argv);

  if (memoryDeInit) {
      if (Kotlin_cleanersLeakCheckerEnabled()) {
          // Make sure to collect any lingering cleaners.
          PerformFullGC();
          // Execute all the cleaner blocks and stop the Cleaner worker.
          ShutdownCleaners(true);
      } else {
          // Stop the cleaner worker without executing remaining cleaner blocks.
          ShutdownCleaners(false);
      }
      if (Kotlin_memoryLeakCheckerEnabled()) WaitNativeWorkersTermination();
      Kotlin_deinitRuntimeIfNeeded();
  }

  return exitStatus;
}

extern "C" RUNTIME_USED int Konan_main(int argc, const char** argv) {
    return Init_and_run_start(argc, argv, 1);
}

#ifdef KONAN_WASM
// Before we pass control to Konan_main, we need to obtain argv elements
// from the javascript world.
extern "C" int Konan_js_arg_size(int index);
extern "C" int Konan_js_fetch_arg(int index, char* ptr);

extern "C" RUNTIME_USED int Konan_js_main(int argc, int memoryDeInit) {
    char** argv = (char**)konan::calloc(1, argc);
    for (int i = 0; i< argc; ++i) {
        argv[i] = (char*)konan::calloc(1, Konan_js_arg_size(i));
        Konan_js_fetch_arg(i, argv[i]);
    }
    return Init_and_run_start(argc, (const char**)argv, memoryDeInit);
}

#endif 

#endif
