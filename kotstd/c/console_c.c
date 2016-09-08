#include <stdio.h>

void kotlinclib_print_int(int message) {
    printf("%d", message);
}

void kotlinclib_print_long(long long message) {
    printf("%lld", message);
}

void kotlinclib_print_byte(char message) {
    printf("%d", message);
}

void kotlinclib_print_short(short message) {
    printf("%d", message);
}

void kotlinclib_print_char(char message) {
    printf("%c", message);
}

void kotlinclib_print_boolean(int message) {
    if (message == 0) {
        printf("false");
    }
    else {
        printf("true");
    }
}

void kotlinclib_print_float(float message) {
    printf("%f", message);
}

void kotlinclib_print_double(double message) {
    printf("%lf", message);
}

void kotlinclib_print_string(char *message) {
    printf("%s", message);
}


void kotlinclib_println() {
    kotlinclib_print_char('\n');
}

void kotlinclib_println_int(int message) {
    kotlinclib_print_int(message);
    kotlinclib_println();
}

void kotlinclib_println_long(long long message) {
    kotlinclib_print_long(message);
    kotlinclib_println();
}

void kotlinclib_println_byte(char message) {
    kotlinclib_print_byte(message);
    kotlinclib_println();
}

void kotlinclib_println_short(short message) {
    kotlinclib_print_short(message);
    kotlinclib_println();
}

void kotlinclib_println_char(char message) {
    kotlinclib_print_char(message);
    kotlinclib_println();
}

void kotlinclib_println_boolean(int message) {
    kotlinclib_print_boolean(message);
    kotlinclib_println();
}

void kotlinclib_println_float(float message) {
    kotlinclib_print_float(message);
    kotlinclib_println();
}

void kotlinclib_println_double(double message) {
    kotlinclib_print_double(message);
    kotlinclib_println();
}

void kotlinclib_println_string(char *message) {
    kotlinclib_print_string(message);
    kotlinclib_println();
}
