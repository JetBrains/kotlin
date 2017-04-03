#include <stddef.h>

// Those types are needed but not defined or used in libcurl headers.
typedef size_t header_callback(char *buffer,   size_t size,   size_t nitems, void *userdata);
typedef size_t write_data_callback(void *buffer, size_t size, size_t nmemb, void *userp);
