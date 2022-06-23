#include "kt49034.h"

struct JSContext {
    int field;
};

struct JSContext global = { 15 };

extern "C" struct JSContext* bar() {
    return &global;
}