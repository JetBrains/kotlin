#include <string.h>

#include "Memory.h"
#include "Natives.h"
#include "Types.h"

//--- Setup args --------------------------------------------------------------//

OBJ_GETTER(setupArgs, int argc, char** argv) {
  // The count is one less, because we skip argv[0] which is the binary name.
  AllocArrayInstance(theArrayTypeInfo, SCOPE_GLOBAL, argc - 1, OBJ_RESULT);
  ArrayHeader* array = (*OBJ_RESULT)->array();
  for (int index = 1; index < argc; index++) {
    AllocStringInstance(SCOPE_GLOBAL, argv[index], strlen(argv[index]),
                        ArrayAddressOfElementAt(array, index - 1));
  }
  RETURN_OBJ_RESULT();
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
