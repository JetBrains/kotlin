#include <cassert>
#include "TypeInfo.h"

extern "C" {
    int lookupField(TypeInfo* info, NameHash nameSignature) {
        assert(false); // not implemented yet
        return -1;
    }
}