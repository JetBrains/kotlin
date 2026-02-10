#ifndef KOTLIN_NATIVE_COMMON_HPP
#define KOTLIN_NATIVE_COMMON_HPP

#include "llvm/ExecutionEngine/Orc/Core.h"
#include "llvm/ExecutionEngine/Orc/ObjectLinkingLayer.h"
#include "llvm/ExecutionEngine/JITLink/JITLink.h"

namespace kotlin::hot {
inline constexpr auto kKotlinFunPrefix = "_kfun:";
inline constexpr auto kKotlinClassPrefix = "_kclass:";
inline constexpr auto kImplSymbolSuffix = "$knhr";
}

#endif // KOTLIN_NATIVE_COMMON_HPP
