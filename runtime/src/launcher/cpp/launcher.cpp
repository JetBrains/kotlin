#include <string.h>
#include "Memory.h"
#include "Natives.h"
#include "Types.h"

extern "C" ArrayHeader* konanArrayInit(ArrayHeader*, int) 
    asm("_kfun:kotlin.Array.<init>(Int)");

extern "C" const TypeInfo konanArrayType 
    asm("_ktype:kotlin.Array");

ArrayHeader* setupArgs(int argc, char** argv) {

    // The count is one less, because we skip argv[0] which is the binary name.
    ArrayHeader* array = AllocArrayInstance(&konanArrayType, SCOPE_GLOBAL, argc-1);
    ArrayHeader* args = konanArrayInit(array, argc-1);

    for (int i = 0; i < argc-1; i++) {
        Kotlin_Array_set(args, i, (ArrayHeader*) makeString( argv[i+1] ));
    }

    return args;
}
        
extern "C" void konanStart(void*) asm("_kfun:start(Array<String>)");

int main(int argc, char** argv) {

    InitMemory();

    ArrayHeader* args = setupArgs(argc, argv);
    konanStart(args);

    // Yes, we have to follow Java convention and return zero.
    return 0;
}

