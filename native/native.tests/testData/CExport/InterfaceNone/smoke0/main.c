#include <stdint.h>

int32_t my_function(int32_t);

int main() {
    if (my_function(5) != 5) {
        return -1;
    }
}