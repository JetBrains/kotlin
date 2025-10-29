int function(int);
int functionWithAsm(int) __asm("funWithAsm");

extern int global;
extern const int globalWithAsm __asm("global2");

extern char globalArray[10];
extern char globalArrayWithAsm[10] __asm("globalArray2");

struct S { int x; };
extern struct S globalStruct;
extern struct S globalStructWithAsm __asm("globalStruct2");

#define wrappedMacro function(0)