#include <chrono>
#include "Types.h"
#include "Natives.h"

using namespace std::chrono;

extern "C" {

KLong Kotlin_system_getTimeMillis() {
  return duration_cast<milliseconds>(high_resolution_clock::now().time_since_epoch()).count();
}

KLong Kotlin_system_getTimeNanos() {
  return duration_cast<nanoseconds>(high_resolution_clock::now().time_since_epoch()).count();
}

KLong Kotlin_system_getTimeMicros() {
  return duration_cast<microseconds>(high_resolution_clock::now().time_since_epoch()).count();
}

}  // extern "C"