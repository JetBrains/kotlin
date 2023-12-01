#include <string>
#include "auxiliarySources.aux.h"

static std::string _name = "Hello from C++";

const char* name() {
    return _name.c_str();
}