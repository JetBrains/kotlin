#define MACRO_CONST_GLOBAL 1

int getValue(void);
#define MACRO_GLOBAL getValue()

extern int global;