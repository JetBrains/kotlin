#include <stdlib.h>
#include <stdio.h>

void assert_c( value)  __attribute__((weak)){
    #ifndef ARM
        if (!value) {
            printf("Exception in thread \"main\" java.lang.AssertionError: Assertion failed\n");
            abort();
        }
    #endif
}