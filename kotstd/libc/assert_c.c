#include <stdlib.h>
#include <stdio.h>

void assert_c(int value) {
    if (!value) {
        printf("Exception in thread \"main\" java.lang.AssertionError: Assertion failed\n");
        abort();
    }
}