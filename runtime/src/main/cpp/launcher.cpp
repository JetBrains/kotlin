#include "Memory.h"

extern "C" int kotlinNativeMain();

int main() {
  InitMemory();
  return kotlinNativeMain();
}
