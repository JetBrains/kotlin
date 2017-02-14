#include <string.h>

#include "Memory.h"
#include "Natives.h"
#include "String.h"
#include "Types.h"

//--- Setup args --------------------------------------------------------------//

OBJ_GETTER(setupArgs, int argc, char** argv) {
  // The count is one less, because we skip argv[0] which is the binary name.
  ObjHeader* result = AllocArrayInstance(theArrayTypeInfo, argc - 1, OBJ_RESULT);
  ArrayHeader* array = result->array();
  for (int index = 1; index < argc; index++) {
    CreateStringFromCString(
      argv[index], ArrayAddressOfElementAt(array, index - 1));
  }
  return result;
}

//--- main --------------------------------------------------------------------//
extern "C" void Konan_start(const ObjHeader* );

int main(int argc, char** argv) {

    InitMemory();
    InitGlobalVariables();

    {
      ObjHolder args;
      setupArgs(argc, argv, args.slot());
      Konan_start(args.obj());
    }

    DeinitMemory();

    // Yes, we have to follow Java convention and return zero.
    return 0;
}
