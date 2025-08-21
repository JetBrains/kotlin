#ifndef MYLIB_H
#define MYLIB_H

#ifdef _WIN32
  #define EXPORT __declspec(dllexport)
#else
  #define EXPORT
#endif

EXPORT int add(int a, int b);
EXPORT const char* hello();

#endif
