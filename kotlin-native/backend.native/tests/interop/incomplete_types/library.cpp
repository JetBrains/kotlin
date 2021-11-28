#include <string>
#include "library.h"

extern "C" {

struct S {
    std::string name;
};

S s = {
    .name = "initial"
};

void setContent(struct S* s, const char* name) {
    // Note that copy here is intentional: we use it as a workaround
    // for short lifetime of copy of the passed Kotlin string.
    s->name = name;
}

const char* getContent(struct S* s) {
    return s->name.c_str();
}

union U {
    float f;
    double d;
};

void setDouble(union U* u, double value) {
    u->d = value;
}

double getDouble(union U* u) {
    return u->d;
}

union U u = {
    .d = 0.0
};

char array[5] = { 0, 0, 0, 0, 0 };

void setArrayValue(char* array, char value) {
    for (int i = 0; i < 5; ++i) {
        array[i] = value;
    }
}

int arrayLength() {
    return sizeof(array) / sizeof(char);
}

}