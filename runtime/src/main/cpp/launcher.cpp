#include "Memory.h"

extern "C" void kotlinNativeMain();

int main() {
  InitMemory();
  kotlinNativeMain();
  return 0;
}
