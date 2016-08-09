extern char *malloc(int size);

int malloc_array(int x) {
    return (int) malloc(x);
}

char kotlinclib_get_byte(int data, int index) {
    return *((char *) data + index);
}

void kotlinclib_set_byte(int data, int index, char value) {
    char *ptr = (char *) data;
    *(ptr + index) = value;
}

int kotlinclib_get_int(int data, int index) {
    return *((int *) data + index);
}

void kotlinclib_set_int(int data, int index, int value) {
    int *ptr = (int *) data;
    *(ptr + index) = value;
}

short kotlinclib_get_short(int data, int index) {
    return *((short *) data + index);
}

void kotlinclib_set_short(int data, int index, short value) {
    short *ptr = (short *) data;
    *(ptr + index) = value;
}