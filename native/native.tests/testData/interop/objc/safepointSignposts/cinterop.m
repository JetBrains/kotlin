#include "cinterop.h"

NSObject* __weak globalObj;

int runTest(NSObject* obj, int iterations) {
    globalObj = obj;
    int result = 0;
    for (int i = 0; i < iterations; ++i) {
        NSObject* tmp = globalObj;
        if (tmp) {
            result += tmp.hash;
        }
    }
    return result;
}