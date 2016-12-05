#include <string.h>
#include "Memory.h"
#include "Natives.h"
#include "Types.h"

ObjHeader* setupArgs(int argc, char** argv) {
    // The count is one less, because we skip argv[0] which is the binary name.
    ObjHeader* args = AllocArrayInstance(theArrayTypeInfo, SCOPE_GLOBAL, argc-1);

    for (int i = 0; i < argc-1; i++) {
      Kotlin_Array_set(args, i, AllocStringInstance(
          SCOPE_GLOBAL, argv[i+1], strlen(argv[i+1]) ));
    }

    return args;
}

extern "C" void Konan_start(ObjHeader* );

int main(int argc, char** argv) {

    InitMemory();

    ObjHeader* args = setupArgs(argc, argv);
    Konan_start(args);

    // Yes, we have to follow Java convention and return zero.
    return 0;
}
