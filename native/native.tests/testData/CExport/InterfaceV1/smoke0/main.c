#include "smoke0_api.h"

int main() {
    if (foo() != 42) {
        return -1;
    }
}