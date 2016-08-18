
#define STATIC_AREA_SIZE 30000
#define DYNAMIC_AREA_SIZE 30000

#define STATIC_HEAP 0
#define DYNAMIC_HEAP 1

#ifdef ARM

char static_area[STATIC_AREA_SIZE];
char dynamic_area[DYNAMIC_AREA_SIZE];

char* heaps[2] = {
    (char*) static_area,
    (char*) dynamic_area
};

int heap_tails[2] = {0, 0};
int active_heap = STATIC_HEAP;

int dynamic_heap_consume = 0;
int dynamic_heap_max = 0;

#else
char* malloc(int);
#endif

#ifdef DBG
    static int total = 0;
    int printf(const char * restrict format, ... );
#endif

char* malloc_heap(int size) {
#ifdef ARM
    char* ptr = heaps[active_heap] + heap_tails[active_heap];
    heap_tails[active_heap] += size;

    return ptr;

#else
    #ifdef DBG
        total = total + size;
        printf("Alloc [%d] TOTAL: [%d]\n", size, total);
    #endif
    return malloc(size);
#endif
}

void set_active_heap(int heap) {
#ifdef ARM
    active_heap = heap;
#endif
}

void clean_dynamic_heap() {
#ifdef ARM
    dynamic_heap_consume += heap_tails[DYNAMIC_HEAP];
    if (heap_tails[DYNAMIC_HEAP] > dynamic_heap_max) {
        dynamic_heap_max = heap_tails[DYNAMIC_HEAP];
    }

    heap_tails[DYNAMIC_HEAP] = 0;
#endif
}
