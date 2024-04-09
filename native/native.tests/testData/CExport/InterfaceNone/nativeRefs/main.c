#include <stdlib.h>
#include <stdint.h>

uintptr_t get_null();
uintptr_t get_singleton_object();
uintptr_t get_static_object();
uintptr_t get_array();
uintptr_t get_local_object();
uintptr_t dispose_object(uintptr_t);

_Bool compare_identities(uintptr_t, uintptr_t);
_Bool compare_objects(uintptr_t, uintptr_t);
_Bool compare_arrays(uintptr_t, uintptr_t);

void retain_object(uintptr_t);
void release_object(uintptr_t);

void test(uintptr_t obj1, uintptr_t obj2, _Bool (*comparator)(uintptr_t, uintptr_t), int code) {
        if (!comparator(obj1, obj2)) {
            exit(code);
        }

        release_object(obj1);
        dispose_object(obj1);

        release_object(obj2);
        dispose_object(obj2);
}

int main() {
    test(get_singleton_object(), get_singleton_object(), compare_identities, -1);
    test(get_static_object(), get_static_object(), compare_identities, -2);
    test(get_local_object(), get_local_object(), compare_objects, -3);
    test(get_null(), get_null(), compare_identities, -4);
    test(get_array(), get_array(), compare_arrays, -5);

    return 0;
}