#include <Foundation/NSObjCRuntime.h>

typedef NS_ENUM (int, ForwardEnumPOD) { // There was forward definition of this enum in another module `forwardEnum1`
    Value1POD,
    Value2POD
};
