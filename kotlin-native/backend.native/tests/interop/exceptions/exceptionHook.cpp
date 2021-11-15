#include "testlib_api.h"

#include <stdio.h>

int main(int argc, char** argv) {
    try {
        testlib_symbols()->kotlin.root.setHookAndThrow();
    } catch (...) {
        printf("FAIL. Native catch.\n");
    }
}