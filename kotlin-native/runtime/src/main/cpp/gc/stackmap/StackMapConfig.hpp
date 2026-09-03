#pragma once

#include <cstdint>

// This scheme's version byte, written by DeltaMainStackMapEncoder::emit and
// checked by DeltaMainStackMapBuilder::verifyMagic. Bump this whenever the
// wire format changes in a way that isn't purely additive.
#define DELTA_MAIN_VERSION 3

// Whether callee-saved-register liveness is encoded in the stack map, in
// addition to stack-slot liveness. Must match the corresponding
// DeltaMainStackMapEncoder(EmitRegisters=...) setting used to build the
// running binary; kept as a compile-time constant (rather than a runtime
// flag) because it changes the wire format's magic byte, checked once at
// startup by DeltaMainStackMapBuilder::verifyMagic.
//
// Always 0 in this initial port: DeltaMainStackMapEncoder does not yet have
// a register-liveness data source on the LLVM side (see
// llvm/lib/CodeGen/DeltaMainStackMapEncoder.h's class comment).
#define ENABLE_REGISTERS 0

// Whether a function's stack map can be resolved lazily, from its stack
// map's address alone, without first walking the whole `__LLVM_StackMaps`
// section (DeltaMainStackMapBuilder::collectLazy). Not yet implemented on
// the LLVM emitting side in this port (see DeltaMainStackMapEncoder.h);
// reserved for a follow-up.
#define ENABLE_LAZY_STACKMAP 0

#if ENABLE_REGISTERS
#define REGISTER_IN_STACKMAP_MAGIC 0b1
#else
#define REGISTER_IN_STACKMAP_MAGIC 0b0
#endif

#if ENABLE_LAZY_STACKMAP
#define LAZY_ENABLED_MAGIC 0b10
#else
#define LAZY_ENABLED_MAGIC 0b0
#endif

// Mach-O prefixes C symbol names with an extra leading underscore; ELF/COFF
// do not. DeltaMainStackMapEncoder::emit (LLVM side) names the section
// symbol "__LLVM_StackMaps" at the IR/MC level, which the Mach-O backend
// then additionally underscore-prefixes to "___LLVM_StackMaps"; on
// ELF/COFF targets the symbol reaches the linker unprefixed, as
// "__LLVM_StackMaps". These `extern "C"` declarations name the symbol as
// the linker will see it on each object format, so `&kotlin::stackMap::
// deltaMainStackMapsSymbol()` resolves correctly on both.
#if defined(__APPLE__)
extern "C" __attribute__((weak_import)) uint8_t _LLVM_StackMaps;
#else
extern "C" __attribute__((weak)) uint8_t __LLVM_StackMaps;
#endif

namespace kotlin::stackMap {

/// Address of the start of the `__LLVM_StackMaps` section, i.e. the point
/// that every function offset recorded in the section (see
/// DeltaMainStackMapEncoder::emit) is relative to.
inline uint8_t* deltaMainStackMapsSymbol() noexcept {
#if defined(__APPLE__)
    return &_LLVM_StackMaps;
#else
    return &__LLVM_StackMaps;
#endif
}

} // namespace kotlin::stackMap
