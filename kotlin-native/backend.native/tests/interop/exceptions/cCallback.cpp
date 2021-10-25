#include <stdio.h>

extern "C" void runAndCatch(void(*f)(void)) {
    try {
        f();
    } catch (...) {
        printf("CATCH IN C++!\n");
    }
}