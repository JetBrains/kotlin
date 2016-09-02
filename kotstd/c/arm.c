#ifdef ARM


int printf(const char *__fmt, ...) __attribute__((weak)){
    /*
    * We dont have support output on ARM.
    */
    return 0;
}

#endif