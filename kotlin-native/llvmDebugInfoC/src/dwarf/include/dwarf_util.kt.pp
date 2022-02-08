/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.kotlin.backend.konan.llvm

/**
 * This code was generated  with following command:
 * $ clang -xc -E -Idist/dependencies/clang-llvm-3.9.0-darwin-macos/include/ llvmDebugInfoC/src/dwarf/include/dwarf_util.kt.pp -Wp,-P -Wp,-CC -o - | sed -e '/^$/d' -e '/^\ *\/\/.*$/d' > backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/llvm/Dwarf.kt
 *
 */

internal enum class DwarfTag(val value:Int) {
#define HANDLE_DW_TAG(ID, NAME) DW_TAG_##NAME(ID),
#include "llvm/Support/Dwarf.def"
#undef HANDLE_DW_TAG
}

internal enum class DwarfTypeKind(val value:Byte) {
#define HANDLE_DW_ATE(ID, NAME) DW_ATE_##NAME(ID),
#include "llvm/Support/Dwarf.def"
#undef HANDLE_DW_ATE
}

internal enum class DwarfOp(val value:Long) {
#define HANDLE_DW_OP(ID, NAME) DW_OP_##NAME(ID),
#include "llvm/Support/Dwarf.def"
#undef HANDLE_DW_OP
}

internal enum class DwarfLanguage(val value:Int) {
#define HANDLE_DW_LANG(ID, NAME) DW_LANG_##NAME(ID),
#include "llvm/Support/Dwarf.def"
#undef HANDLE_DW_LANG
  DW_LANG_Kotlin(0x0001) /* manually added, should be changed someday. */
}