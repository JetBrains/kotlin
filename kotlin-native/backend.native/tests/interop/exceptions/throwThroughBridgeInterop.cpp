#include <stdexcept>

extern "C" __attribute__((always_inline)) void throwCppException() {
    throw std::runtime_error("Error");
}