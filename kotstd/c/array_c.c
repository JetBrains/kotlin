extern char* malloc_heap(int size) __attribute__((weak));

int malloc_array(int x) __attribute__((weak)){
    return (int) malloc_heap(x);
}

char kotlinclib_byte_array_get_ix(int data, int index) __attribute__((weak)){
    return *((char *) data + index);
}

void kotlinclib_byte_array_set_ix(int data, int index, char value) __attribute__((weak)){
    char *ptr = (char *) data;
    *(ptr + index) = value;
}

char kotlinclib_boolean_array_get_ix(int data, int index) __attribute__((weak)){
    return kotlinclib_byte_array_get_ix(data, index);
}

void kotlinclib_boolean_array_set_ix(int data, int index, char value) __attribute__((weak)){
    kotlinclib_byte_array_set_ix(data, index, value);
}

char kotlinclib_char_array_get_ix(int data, int index) __attribute__((weak)){
    return kotlinclib_char_array_get_ix(data, index);
}

void kotlinclib_char_array_set_ix(int data, int index, char value) __attribute__((weak)){
    kotlinclib_char_array_set_ix(data, index, value);
}

int kotlinclib_int_array_get_ix(int data, int index) __attribute__((weak)){
    return *((int *) data + index);
}

void kotlinclib_int_array_set_ix(int data, int index, int value) __attribute__((weak)){
    int *ptr = (int *) data;
    *(ptr + index) = value;
}

short kotlinclib_short_array_get_ix(int data, int index) __attribute__((weak)){
    return *((short *) data + index);
}

void kotlinclib_short_array_set_ix(int data, int index, short value) __attribute__((weak)){
    short *ptr = (short *) data;
    *(ptr + index) = value;
}

long long kotlinclib_long_array_get_ix(int data, int index) __attribute__((weak)){
    return *((long long *) data + index);
}

void kotlinclib_long_array_set_ix(int data, int index, long long value) __attribute__((weak)){
    long long *ptr = (long long *) data;
    *(ptr + index) = value;
}
