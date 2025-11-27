/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

// original source copyright:
// Copyright 2011 Google LLC
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name of Google LLC nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

// dump_syms_tool.cc: Command line tool that uses the DumpSymbols class.
// TODO(waylonis): accept stdin

#ifdef HAVE_CONFIG_H
#include <config.h>  // Must come first
#endif

#include <mach-o/arch.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

#include <algorithm>
#include <fstream>
#include <iostream>
#include <limits>
#include <memory>
#include <sstream>
#include <string>
#include <utility>
#include <vector>

#include "common/mac/dump_syms.h"
#include "common/mac/arch_utilities.h"
#include "common/mac/macho_utilities.h"
#include "common/path_helper.h"
#include "common/scoped_ptr.h"
#include "common/using_std_string.h"
#include "google_breakpad/processor/basic_source_line_resolver.h"
#include "google_breakpad/processor/minidump.h"
#include "google_breakpad/processor/minidump_processor.h"
#include "google_breakpad/processor/process_state.h"
#include "google_breakpad/processor/call_stack.h"
#include "google_breakpad/processor/code_module.h"
#include "google_breakpad/processor/code_modules.h"
#include "google_breakpad/processor/process_state.h"
#include "google_breakpad/processor/source_line_resolver_interface.h"
#include "google_breakpad/processor/stack_frame_cpu.h"
#include "processor/logging.h"
#include "processor/simple_symbol_supplier.h"
#include "processor/stackwalk_common.h"
#include "processor/pathname_stripper.h"

using google_breakpad::BasicSourceLineResolver;
using google_breakpad::CallStack;
using google_breakpad::CodeModules;
using google_breakpad::DumpSymbols;
using google_breakpad::MemoryRegion;
using google_breakpad::Minidump;
using google_breakpad::MinidumpMemoryList;
using google_breakpad::MinidumpThreadList;
using google_breakpad::MinidumpProcessor;
using google_breakpad::Module;
using google_breakpad::PathnameStripper;
using google_breakpad::ProcessState;
using google_breakpad::SimpleSymbolSupplier;
using google_breakpad::SourceLineResolverInterface;
using google_breakpad::StackFrame;
using google_breakpad::StackFrameAMD64;
using google_breakpad::StackFrameARM;
using google_breakpad::StackFrameARM64;
using google_breakpad::StackFrameMIPS;
using google_breakpad::StackFramePPC;
using google_breakpad::StackFrameRISCV;
using google_breakpad::StackFrameRISCV64;
using google_breakpad::StackFrameSPARC;
using google_breakpad::StackFrameX86;
using google_breakpad::scoped_ptr;
using std::vector;
using std::unique_ptr;

static bool StackFrameEntryComparator(const Module::StackFrameEntry* a,
                                      const Module::StackFrameEntry* b) {
  return a->address < b->address;
}

// Copy the CFI data from |from_module| into |to_module|, for any non-
// overlapping ranges.
static void CopyCFIDataBetweenModules(Module* to_module,
                                      const Module* from_module) {
  typedef vector<Module::StackFrameEntry*>::const_iterator Iterator;

  // Get the CFI data from both the source and destination modules and ensure
  // it is sorted by start address.
  vector<Module::StackFrameEntry*> from_data;
  from_module->GetStackFrameEntries(&from_data);
  std::sort(from_data.begin(), from_data.end(), &StackFrameEntryComparator);

  vector<Module::StackFrameEntry*> to_data;
  to_module->GetStackFrameEntries(&to_data);
  std::sort(to_data.begin(), to_data.end(), &StackFrameEntryComparator);

  Iterator to_it = to_data.begin();

  for (Iterator it = from_data.begin(); it != from_data.end(); ++it) {
    Module::StackFrameEntry* from_entry = *it;
    Module::Address from_entry_end = from_entry->address + from_entry->size;

    // Find the first CFI record in the |to_module| that does not have an
    // address less than the entry to be copied.
    while (to_it != to_data.end()) {
      if (from_entry->address > (*to_it)->address)
        ++to_it;
      else
        break;
    }

    // If the entry does not overlap, then it is safe to copy to |to_module|.
    if (to_it == to_data.end() || (from_entry->address < (*to_it)->address &&
            from_entry_end < (*to_it)->address)) {
      to_module->AddStackFrameEntry(
          std::make_unique<Module::StackFrameEntry>(*from_entry));
    }
  }
}

static std::optional<std::string> CreateSyms(string srcPath) {
  string dsymPath = srcPath + ".dSYM";
  SymbolData symbol_data = CFI | SYMBOLS_AND_FILES;
  DumpSymbols dump_symbols(symbol_data, true, false, "", false);

  // For x86_64 binaries, the CFI data is in the __TEXT,__eh_frame of the
  // Mach-O file, which is not copied into the dSYM. Whereas in i386, the CFI
  // data is in the __DWARF,__debug_frame section, which is moved into the
  // dSYM. Therefore, to get x86_64 CFI data, dump_syms needs to look at both
  // the dSYM and the Mach-O file. If both paths are present and CFI was
  // requested, then consider the Module as "split" and dump all the debug data
  // from the primary debug info file, the dSYM, and then dump additional CFI
  // data from the source Mach-O file.
  bool split_module = !dsymPath.empty() && !srcPath.empty();
  const string& primary_file =
    split_module ? dsymPath : srcPath;

  dump_symbols.SetReportWarnings(false);

  if (!dump_symbols.Read(primary_file))
    return {};

  // Read the primary file into a Breakpad Module.
  Module* module = NULL;
  if (!dump_symbols.ReadSymbolData(&module))
    return {};
  scoped_ptr<Module> scoped_module(module);

  // If this is a split module, read the secondary Mach-O file, from which the
  // CFI data will be extracted.
  if (split_module && primary_file == dsymPath) {
    if (!dump_symbols.Read(srcPath))
      return {};

    Module* cfi_module = NULL;
    if (!dump_symbols.ReadSymbolData(&cfi_module))
      return {};
    scoped_ptr<Module> scoped_cfi_module(cfi_module);

    bool name_matches = cfi_module->name() == module->name();

    // Ensure that the modules are for the same debug code file.
    if (!name_matches || cfi_module->os() != module->os() ||
        cfi_module->architecture() != module->architecture() ||
        cfi_module->identifier() != module->identifier()) {
      fprintf(stderr, "Cannot generate a symbol file from split sources that do"
                      " not match.\n");
      if (!name_matches) {
        fprintf(stderr, "Name mismatch: binary=[%s], dSYM=[%s]\n",
                cfi_module->name().c_str(), module->name().c_str());
      }
      if (cfi_module->os() != module->os()) {
        fprintf(stderr, "OS mismatch: binary=[%s], dSYM=[%s]\n",
                cfi_module->os().c_str(), module->os().c_str());
      }
      if (cfi_module->architecture() != module->architecture()) {
        fprintf(stderr, "Architecture mismatch: binary=[%s], dSYM=[%s]\n",
                cfi_module->architecture().c_str(),
                module->architecture().c_str());
      }
      if (cfi_module->identifier() != module->identifier()) {
        fprintf(stderr, "Identifier mismatch: binary=[%s], dSYM=[%s]\n",
                cfi_module->identifier().c_str(), module->identifier().c_str());
      }
      return {};
    }

    CopyCFIDataBetweenModules(module, cfi_module);
  }

  std::stringstream out;
  if (!module->Write(out, symbol_data))
    return {};
  return out.str();
}

static bool createDir(std::string path) {
  if (mkdir(path.c_str(), 0700) != 0) {
    fprintf(stderr, "Failed to create directory at %s: %s\n", path.c_str(), strerror(errno));
    return false;
  }
  return true;
}

static bool SaveSyms(std::string syms, std::string symbol_path) {
  std::string slash = "/";
  if (!createDir(symbol_path))
    return false;
  std::string header = syms.substr(0, syms.find_first_of('\n'));
  auto keywordDelimeter = header.find_first_of(' ');
  if (header.substr(0, keywordDelimeter) != "MODULE") {
    fprintf(stderr, "Header does not start with MODULE: %s\n", header.c_str());
    return false;
  }
  auto platformDelimeter = header.find_first_of(' ', keywordDelimeter + 1);
  auto archDelimeter = header.find_first_of(' ', platformDelimeter + 1);
  auto hashDelimeter = header.find_first_of(' ', archDelimeter + 1);
  std::string hash = std::string(header.substr(archDelimeter + 1, hashDelimeter - archDelimeter - 1));
  std::string moduleName = std::string(header.substr(hashDelimeter + 1));
  std::string modulePath = std::string(symbol_path + slash + moduleName);
  if (!createDir(modulePath))
    return false;
  std::string hashPath = modulePath + slash + hash;
  if (!createDir(hashPath))
    return false;
  std::ofstream symFile(hashPath + slash + moduleName + std::string(".sym"));
  symFile << syms;
  return true;
}

// PrintRegister prints a register's name and value to stdout.  It will
// print four registers on a line.  For the first register in a set,
// pass 0 for |start_col|.  For registers in a set, pass the most recent
// return value of PrintRegister.
// The caller is responsible for printing the final newline after a set
// of registers is completely printed, regardless of the number of calls
// to PrintRegister.
static const int kMaxWidth = 80;  // optimize for an 80-column terminal
static int PrintRegister(const char* name, uint32_t value, int start_col) {
  char buffer[64];
  snprintf(buffer, sizeof(buffer), " %5s = 0x%08x", name, value);

  if (start_col + static_cast<ssize_t>(strlen(buffer)) > kMaxWidth) {
    start_col = 0;
    printf("\n ");
  }
  fputs(buffer, stdout);

  return start_col + strlen(buffer);
}

// PrintRegister64 does the same thing, but for 64-bit registers.
static int PrintRegister64(const char* name, uint64_t value, int start_col) {
  char buffer[64];
  snprintf(buffer, sizeof(buffer), " %5s = 0x%016" PRIx64 , name, value);

  if (start_col + static_cast<ssize_t>(strlen(buffer)) > kMaxWidth) {
    start_col = 0;
    printf("\n ");
  }
  fputs(buffer, stdout);

  return start_col + strlen(buffer);
}

// PrintStackContents prints the stack contents of the current frame to stdout.
static void PrintStackContents(const string& indent,
                               const StackFrame* frame,
                               const StackFrame* prev_frame,
                               const string& cpu,
                               const MemoryRegion* memory,
                               const CodeModules* modules,
                               SourceLineResolverInterface* resolver) {
  // Find stack range.
  int word_length = 0;
  uint64_t stack_begin = 0, stack_end = 0;
  if (cpu == "x86") {
    word_length = 4;
    const StackFrameX86* frame_x86 = static_cast<const StackFrameX86*>(frame);
    const StackFrameX86* prev_frame_x86 =
        static_cast<const StackFrameX86*>(prev_frame);
    if ((frame_x86->context_validity & StackFrameX86::CONTEXT_VALID_ESP) &&
        (prev_frame_x86->context_validity & StackFrameX86::CONTEXT_VALID_ESP)) {
      stack_begin = frame_x86->context.esp;
      stack_end = prev_frame_x86->context.esp;
    }
  } else if (cpu == "amd64") {
    word_length = 8;
    const StackFrameAMD64* frame_amd64 =
        static_cast<const StackFrameAMD64*>(frame);
    const StackFrameAMD64* prev_frame_amd64 =
        static_cast<const StackFrameAMD64*>(prev_frame);
    if ((frame_amd64->context_validity & StackFrameAMD64::CONTEXT_VALID_RSP) &&
        (prev_frame_amd64->context_validity &
         StackFrameAMD64::CONTEXT_VALID_RSP)) {
      stack_begin = frame_amd64->context.rsp;
      stack_end = prev_frame_amd64->context.rsp;
    }
  } else if (cpu == "arm") {
    word_length = 4;
    const StackFrameARM* frame_arm = static_cast<const StackFrameARM*>(frame);
    const StackFrameARM* prev_frame_arm =
        static_cast<const StackFrameARM*>(prev_frame);
    if ((frame_arm->context_validity &
         StackFrameARM::CONTEXT_VALID_SP) &&
        (prev_frame_arm->context_validity & StackFrameARM::CONTEXT_VALID_SP)) {
      stack_begin = frame_arm->context.iregs[13];
      stack_end = prev_frame_arm->context.iregs[13];
    }
  } else if (cpu == "arm64") {
    word_length = 8;
    const StackFrameARM64* frame_arm64 =
        static_cast<const StackFrameARM64*>(frame);
    const StackFrameARM64* prev_frame_arm64 =
        static_cast<const StackFrameARM64*>(prev_frame);
    if ((frame_arm64->context_validity &
         StackFrameARM64::CONTEXT_VALID_SP) &&
        (prev_frame_arm64->context_validity &
         StackFrameARM64::CONTEXT_VALID_SP)) {
      stack_begin = frame_arm64->context.iregs[31];
      stack_end = prev_frame_arm64->context.iregs[31];
    }
  } else if (cpu == "riscv") {
    word_length = 4;
    const StackFrameRISCV* frame_riscv =
        static_cast<const StackFrameRISCV*>(frame);
    const StackFrameRISCV* prev_frame_riscv =
        static_cast<const StackFrameRISCV*>(prev_frame);
    if ((frame_riscv->context_validity &
         StackFrameRISCV::CONTEXT_VALID_SP) &&
        (prev_frame_riscv->context_validity &
         StackFrameRISCV::CONTEXT_VALID_SP)) {
      stack_begin = frame_riscv->context.sp;
      stack_end = prev_frame_riscv->context.sp;
    }
  } else if (cpu == "riscv64") {
    word_length = 8;
    const StackFrameRISCV64* frame_riscv64 =
        static_cast<const StackFrameRISCV64*>(frame);
    const StackFrameRISCV64* prev_frame_riscv64 =
        static_cast<const StackFrameRISCV64*>(prev_frame);
    if ((frame_riscv64->context_validity &
         StackFrameRISCV64::CONTEXT_VALID_SP) &&
        (prev_frame_riscv64->context_validity &
         StackFrameRISCV64::CONTEXT_VALID_SP)) {
      stack_begin = frame_riscv64->context.sp;
      stack_end = prev_frame_riscv64->context.sp;
    }
  }
  if (!word_length || !stack_begin || !stack_end)
    return;

  // Print stack contents.
  printf("\n%sStack contents:", indent.c_str());
  for(uint64_t address = stack_begin; address < stack_end; ) {
    // Print the start address of this row.
    if (word_length == 4)
      printf("\n%s %08x", indent.c_str(), static_cast<uint32_t>(address));
    else
      printf("\n%s %016" PRIx64, indent.c_str(), address);

    // Print data in hex.
    const int kBytesPerRow = 16;
    string data_as_string;
    for (int i = 0; i < kBytesPerRow; ++i, ++address) {
      uint8_t value = 0;
      if (address < stack_end &&
          memory->GetMemoryAtAddress(address, &value)) {
        printf(" %02x", value);
        data_as_string.push_back(isprint(value) ? value : '.');
      } else {
        printf("   ");
        data_as_string.push_back(' ');
      }
    }
    // Print data as string.
    printf("  %s", data_as_string.c_str());
  }

  // Try to find instruction pointers from stack.
  printf("\n%sPossible instruction pointers:\n", indent.c_str());
  for (uint64_t address = stack_begin; address < stack_end;
       address += word_length) {
    StackFrame pointee_frame;

    // Read a word (possible instruction pointer) from stack.
    if (word_length == 4) {
      uint32_t data32 = 0;
      memory->GetMemoryAtAddress(address, &data32);
      pointee_frame.instruction = data32;
    } else {
      uint64_t data64 = 0;
      memory->GetMemoryAtAddress(address, &data64);
      pointee_frame.instruction = data64;
    }
    pointee_frame.module =
        modules->GetModuleForAddress(pointee_frame.instruction);

    // Try to look up the function name.
    std::deque<unique_ptr<StackFrame>> inlined_frames;
    if (pointee_frame.module)
      resolver->FillSourceLineInfo(&pointee_frame, &inlined_frames);

    // Print function name.
    auto print_function_name = [&](StackFrame* frame) {
      if (!frame->function_name.empty()) {
        if (word_length == 4) {
          printf("%s *(0x%08x) = 0x%08x", indent.c_str(),
                 static_cast<uint32_t>(address),
                 static_cast<uint32_t>(frame->instruction));
        } else {
          printf("%s *(0x%016" PRIx64 ") = 0x%016" PRIx64, indent.c_str(),
                 address, frame->instruction);
        }
        printf(
            " <%s> [%s : %d + 0x%" PRIx64 "]\n", frame->function_name.c_str(),
            PathnameStripper::File(frame->source_file_name).c_str(),
            frame->source_line, frame->instruction - frame->source_line_base);
      }
    };
    print_function_name(&pointee_frame);
    for (unique_ptr<StackFrame> &frame : inlined_frames)
      print_function_name(frame.get());
  }
  printf("\n");
}

static void PrintFrameHeader(const StackFrame* frame, int frame_index) {
  printf("%2d  ", frame_index);

  uint64_t instruction_address = frame->ReturnAddress();

  if (frame->module) {
    printf("%s", PathnameStripper::File(frame->module->code_file()).c_str());
    if (!frame->function_name.empty()) {
      printf("!%s", frame->function_name.c_str());
      if (!frame->source_file_name.empty()) {
        string source_file = PathnameStripper::File(frame->source_file_name);
        printf(" [%s : %d + 0x%" PRIx64 "]", source_file.c_str(),
               frame->source_line,
               instruction_address - frame->source_line_base);
      } else {
        printf(" + 0x%" PRIx64, instruction_address - frame->function_base);
      }
    } else {
      printf(" + 0x%" PRIx64,
             instruction_address - frame->module->base_address());
    }
  } else {
    printf("0x%" PRIx64, instruction_address);
  }
}

// PrintStack prints the call stack in |stack| to stdout, in a reasonably
// useful form.  Module, function, and source file names are displayed if
// they are available.  The code offset to the base code address of the
// source line, function, or module is printed, preferring them in that
// order.  If no source line, function, or module information is available,
// an absolute code offset is printed.
//
// If |cpu| is a recognized CPU name, relevant register state for each stack
// frame printed is also output, if available.
static void PrintStack(const CallStack* stack,
                       const string& cpu,
                       bool output_stack_contents,
                       const MemoryRegion* memory,
                       const CodeModules* modules,
                       SourceLineResolverInterface* resolver,
                       bool brief) {
  int frame_count = stack->frames()->size();
  if (frame_count == 0) {
    printf(" <no frames>\n");
  }
  for (int frame_index = 0; frame_index < frame_count; ++frame_index) {
    const StackFrame* frame = stack->frames()->at(frame_index);
    PrintFrameHeader(frame, frame_index);
    printf("\n ");

    if (brief) continue;

    // Inlined frames don't have registers info.
    if (frame->trust != StackFrameAMD64::FRAME_TRUST_INLINE) {
      int sequence = 0;
      if (cpu == "x86") {
        const StackFrameX86* frame_x86 =
            reinterpret_cast<const StackFrameX86*>(frame);

        if (frame_x86->context_validity & StackFrameX86::CONTEXT_VALID_EIP)
          sequence = PrintRegister("eip", frame_x86->context.eip, sequence);
        if (frame_x86->context_validity & StackFrameX86::CONTEXT_VALID_ESP)
          sequence = PrintRegister("esp", frame_x86->context.esp, sequence);
        if (frame_x86->context_validity & StackFrameX86::CONTEXT_VALID_EBP)
          sequence = PrintRegister("ebp", frame_x86->context.ebp, sequence);
        if (frame_x86->context_validity & StackFrameX86::CONTEXT_VALID_EBX)
          sequence = PrintRegister("ebx", frame_x86->context.ebx, sequence);
        if (frame_x86->context_validity & StackFrameX86::CONTEXT_VALID_ESI)
          sequence = PrintRegister("esi", frame_x86->context.esi, sequence);
        if (frame_x86->context_validity & StackFrameX86::CONTEXT_VALID_EDI)
          sequence = PrintRegister("edi", frame_x86->context.edi, sequence);
        if (frame_x86->context_validity == StackFrameX86::CONTEXT_VALID_ALL) {
          sequence = PrintRegister("eax", frame_x86->context.eax, sequence);
          sequence = PrintRegister("ecx", frame_x86->context.ecx, sequence);
          sequence = PrintRegister("edx", frame_x86->context.edx, sequence);
          sequence = PrintRegister("efl", frame_x86->context.eflags, sequence);
        }
      } else if (cpu == "ppc") {
        const StackFramePPC* frame_ppc =
            reinterpret_cast<const StackFramePPC*>(frame);

        if (frame_ppc->context_validity & StackFramePPC::CONTEXT_VALID_SRR0)
          sequence = PrintRegister("srr0", frame_ppc->context.srr0, sequence);
        if (frame_ppc->context_validity & StackFramePPC::CONTEXT_VALID_GPR1)
          sequence = PrintRegister("r1", frame_ppc->context.gpr[1], sequence);
      } else if (cpu == "amd64") {
        const StackFrameAMD64* frame_amd64 =
            reinterpret_cast<const StackFrameAMD64*>(frame);

        if (frame_amd64->context_validity & StackFrameAMD64::CONTEXT_VALID_RAX)
          sequence = PrintRegister64("rax", frame_amd64->context.rax, sequence);
        if (frame_amd64->context_validity & StackFrameAMD64::CONTEXT_VALID_RDX)
          sequence = PrintRegister64("rdx", frame_amd64->context.rdx, sequence);
        if (frame_amd64->context_validity & StackFrameAMD64::CONTEXT_VALID_RCX)
          sequence = PrintRegister64("rcx", frame_amd64->context.rcx, sequence);
        if (frame_amd64->context_validity & StackFrameAMD64::CONTEXT_VALID_RBX)
          sequence = PrintRegister64("rbx", frame_amd64->context.rbx, sequence);
        if (frame_amd64->context_validity & StackFrameAMD64::CONTEXT_VALID_RSI)
          sequence = PrintRegister64("rsi", frame_amd64->context.rsi, sequence);
        if (frame_amd64->context_validity & StackFrameAMD64::CONTEXT_VALID_RDI)
          sequence = PrintRegister64("rdi", frame_amd64->context.rdi, sequence);
        if (frame_amd64->context_validity & StackFrameAMD64::CONTEXT_VALID_RBP)
          sequence = PrintRegister64("rbp", frame_amd64->context.rbp, sequence);
        if (frame_amd64->context_validity & StackFrameAMD64::CONTEXT_VALID_RSP)
          sequence = PrintRegister64("rsp", frame_amd64->context.rsp, sequence);
        if (frame_amd64->context_validity & StackFrameAMD64::CONTEXT_VALID_R8)
          sequence = PrintRegister64("r8", frame_amd64->context.r8, sequence);
        if (frame_amd64->context_validity & StackFrameAMD64::CONTEXT_VALID_R9)
          sequence = PrintRegister64("r9", frame_amd64->context.r9, sequence);
        if (frame_amd64->context_validity & StackFrameAMD64::CONTEXT_VALID_R10)
          sequence = PrintRegister64("r10", frame_amd64->context.r10, sequence);
        if (frame_amd64->context_validity & StackFrameAMD64::CONTEXT_VALID_R11)
          sequence = PrintRegister64("r11", frame_amd64->context.r11, sequence);
        if (frame_amd64->context_validity & StackFrameAMD64::CONTEXT_VALID_R12)
          sequence = PrintRegister64("r12", frame_amd64->context.r12, sequence);
        if (frame_amd64->context_validity & StackFrameAMD64::CONTEXT_VALID_R13)
          sequence = PrintRegister64("r13", frame_amd64->context.r13, sequence);
        if (frame_amd64->context_validity & StackFrameAMD64::CONTEXT_VALID_R14)
          sequence = PrintRegister64("r14", frame_amd64->context.r14, sequence);
        if (frame_amd64->context_validity & StackFrameAMD64::CONTEXT_VALID_R15)
          sequence = PrintRegister64("r15", frame_amd64->context.r15, sequence);
        if (frame_amd64->context_validity & StackFrameAMD64::CONTEXT_VALID_RIP)
          sequence = PrintRegister64("rip", frame_amd64->context.rip, sequence);
      } else if (cpu == "sparc") {
        const StackFrameSPARC* frame_sparc =
            reinterpret_cast<const StackFrameSPARC*>(frame);

        if (frame_sparc->context_validity & StackFrameSPARC::CONTEXT_VALID_SP)
          sequence =
              PrintRegister("sp", frame_sparc->context.g_r[14], sequence);
        if (frame_sparc->context_validity & StackFrameSPARC::CONTEXT_VALID_FP)
          sequence =
              PrintRegister("fp", frame_sparc->context.g_r[30], sequence);
        if (frame_sparc->context_validity & StackFrameSPARC::CONTEXT_VALID_PC)
          sequence = PrintRegister("pc", frame_sparc->context.pc, sequence);
      } else if (cpu == "arm") {
        const StackFrameARM* frame_arm =
            reinterpret_cast<const StackFrameARM*>(frame);

        // Argument registers (caller-saves), which will likely only be valid
        // for the youngest frame.
        if (frame_arm->context_validity & StackFrameARM::CONTEXT_VALID_R0)
          sequence = PrintRegister("r0", frame_arm->context.iregs[0], sequence);
        if (frame_arm->context_validity & StackFrameARM::CONTEXT_VALID_R1)
          sequence = PrintRegister("r1", frame_arm->context.iregs[1], sequence);
        if (frame_arm->context_validity & StackFrameARM::CONTEXT_VALID_R2)
          sequence = PrintRegister("r2", frame_arm->context.iregs[2], sequence);
        if (frame_arm->context_validity & StackFrameARM::CONTEXT_VALID_R3)
          sequence = PrintRegister("r3", frame_arm->context.iregs[3], sequence);

        // General-purpose callee-saves registers.
        if (frame_arm->context_validity & StackFrameARM::CONTEXT_VALID_R4)
          sequence = PrintRegister("r4", frame_arm->context.iregs[4], sequence);
        if (frame_arm->context_validity & StackFrameARM::CONTEXT_VALID_R5)
          sequence = PrintRegister("r5", frame_arm->context.iregs[5], sequence);
        if (frame_arm->context_validity & StackFrameARM::CONTEXT_VALID_R6)
          sequence = PrintRegister("r6", frame_arm->context.iregs[6], sequence);
        if (frame_arm->context_validity & StackFrameARM::CONTEXT_VALID_R7)
          sequence = PrintRegister("r7", frame_arm->context.iregs[7], sequence);
        if (frame_arm->context_validity & StackFrameARM::CONTEXT_VALID_R8)
          sequence = PrintRegister("r8", frame_arm->context.iregs[8], sequence);
        if (frame_arm->context_validity & StackFrameARM::CONTEXT_VALID_R9)
          sequence = PrintRegister("r9", frame_arm->context.iregs[9], sequence);
        if (frame_arm->context_validity & StackFrameARM::CONTEXT_VALID_R10)
          sequence =
              PrintRegister("r10", frame_arm->context.iregs[10], sequence);
        if (frame_arm->context_validity & StackFrameARM::CONTEXT_VALID_R12)
          sequence =
              PrintRegister("r12", frame_arm->context.iregs[12], sequence);

        // Registers with a dedicated or conventional purpose.
        if (frame_arm->context_validity & StackFrameARM::CONTEXT_VALID_FP)
          sequence =
              PrintRegister("fp", frame_arm->context.iregs[11], sequence);
        if (frame_arm->context_validity & StackFrameARM::CONTEXT_VALID_SP)
          sequence =
              PrintRegister("sp", frame_arm->context.iregs[13], sequence);
        if (frame_arm->context_validity & StackFrameARM::CONTEXT_VALID_LR)
          sequence =
              PrintRegister("lr", frame_arm->context.iregs[14], sequence);
        if (frame_arm->context_validity & StackFrameARM::CONTEXT_VALID_PC)
          sequence =
              PrintRegister("pc", frame_arm->context.iregs[15], sequence);
      } else if (cpu == "arm64") {
        const StackFrameARM64* frame_arm64 =
            reinterpret_cast<const StackFrameARM64*>(frame);

        if (frame_arm64->context_validity & StackFrameARM64::CONTEXT_VALID_X0) {
          sequence =
              PrintRegister64("x0", frame_arm64->context.iregs[0], sequence);
        }
        if (frame_arm64->context_validity & StackFrameARM64::CONTEXT_VALID_X1) {
          sequence =
              PrintRegister64("x1", frame_arm64->context.iregs[1], sequence);
        }
        if (frame_arm64->context_validity & StackFrameARM64::CONTEXT_VALID_X2) {
          sequence =
              PrintRegister64("x2", frame_arm64->context.iregs[2], sequence);
        }
        if (frame_arm64->context_validity & StackFrameARM64::CONTEXT_VALID_X3) {
          sequence =
              PrintRegister64("x3", frame_arm64->context.iregs[3], sequence);
        }
        if (frame_arm64->context_validity & StackFrameARM64::CONTEXT_VALID_X4) {
          sequence =
              PrintRegister64("x4", frame_arm64->context.iregs[4], sequence);
        }
        if (frame_arm64->context_validity & StackFrameARM64::CONTEXT_VALID_X5) {
          sequence =
              PrintRegister64("x5", frame_arm64->context.iregs[5], sequence);
        }
        if (frame_arm64->context_validity & StackFrameARM64::CONTEXT_VALID_X6) {
          sequence =
              PrintRegister64("x6", frame_arm64->context.iregs[6], sequence);
        }
        if (frame_arm64->context_validity & StackFrameARM64::CONTEXT_VALID_X7) {
          sequence =
              PrintRegister64("x7", frame_arm64->context.iregs[7], sequence);
        }
        if (frame_arm64->context_validity & StackFrameARM64::CONTEXT_VALID_X8) {
          sequence =
              PrintRegister64("x8", frame_arm64->context.iregs[8], sequence);
        }
        if (frame_arm64->context_validity & StackFrameARM64::CONTEXT_VALID_X9) {
          sequence =
              PrintRegister64("x9", frame_arm64->context.iregs[9], sequence);
        }
        if (frame_arm64->context_validity &
            StackFrameARM64::CONTEXT_VALID_X10) {
          sequence =
              PrintRegister64("x10", frame_arm64->context.iregs[10], sequence);
        }
        if (frame_arm64->context_validity &
            StackFrameARM64::CONTEXT_VALID_X11) {
          sequence =
              PrintRegister64("x11", frame_arm64->context.iregs[11], sequence);
        }
        if (frame_arm64->context_validity &
            StackFrameARM64::CONTEXT_VALID_X12) {
          sequence =
              PrintRegister64("x12", frame_arm64->context.iregs[12], sequence);
        }
        if (frame_arm64->context_validity &
            StackFrameARM64::CONTEXT_VALID_X13) {
          sequence =
              PrintRegister64("x13", frame_arm64->context.iregs[13], sequence);
        }
        if (frame_arm64->context_validity &
            StackFrameARM64::CONTEXT_VALID_X14) {
          sequence =
              PrintRegister64("x14", frame_arm64->context.iregs[14], sequence);
        }
        if (frame_arm64->context_validity &
            StackFrameARM64::CONTEXT_VALID_X15) {
          sequence =
              PrintRegister64("x15", frame_arm64->context.iregs[15], sequence);
        }
        if (frame_arm64->context_validity &
            StackFrameARM64::CONTEXT_VALID_X16) {
          sequence =
              PrintRegister64("x16", frame_arm64->context.iregs[16], sequence);
        }
        if (frame_arm64->context_validity &
            StackFrameARM64::CONTEXT_VALID_X17) {
          sequence =
              PrintRegister64("x17", frame_arm64->context.iregs[17], sequence);
        }
        if (frame_arm64->context_validity &
            StackFrameARM64::CONTEXT_VALID_X18) {
          sequence =
              PrintRegister64("x18", frame_arm64->context.iregs[18], sequence);
        }
        if (frame_arm64->context_validity &
            StackFrameARM64::CONTEXT_VALID_X19) {
          sequence =
              PrintRegister64("x19", frame_arm64->context.iregs[19], sequence);
        }
        if (frame_arm64->context_validity &
            StackFrameARM64::CONTEXT_VALID_X20) {
          sequence =
              PrintRegister64("x20", frame_arm64->context.iregs[20], sequence);
        }
        if (frame_arm64->context_validity &
            StackFrameARM64::CONTEXT_VALID_X21) {
          sequence =
              PrintRegister64("x21", frame_arm64->context.iregs[21], sequence);
        }
        if (frame_arm64->context_validity &
            StackFrameARM64::CONTEXT_VALID_X22) {
          sequence =
              PrintRegister64("x22", frame_arm64->context.iregs[22], sequence);
        }
        if (frame_arm64->context_validity &
            StackFrameARM64::CONTEXT_VALID_X23) {
          sequence =
              PrintRegister64("x23", frame_arm64->context.iregs[23], sequence);
        }
        if (frame_arm64->context_validity &
            StackFrameARM64::CONTEXT_VALID_X24) {
          sequence =
              PrintRegister64("x24", frame_arm64->context.iregs[24], sequence);
        }
        if (frame_arm64->context_validity &
            StackFrameARM64::CONTEXT_VALID_X25) {
          sequence =
              PrintRegister64("x25", frame_arm64->context.iregs[25], sequence);
        }
        if (frame_arm64->context_validity &
            StackFrameARM64::CONTEXT_VALID_X26) {
          sequence =
              PrintRegister64("x26", frame_arm64->context.iregs[26], sequence);
        }
        if (frame_arm64->context_validity &
            StackFrameARM64::CONTEXT_VALID_X27) {
          sequence =
              PrintRegister64("x27", frame_arm64->context.iregs[27], sequence);
        }
        if (frame_arm64->context_validity &
            StackFrameARM64::CONTEXT_VALID_X28) {
          sequence =
              PrintRegister64("x28", frame_arm64->context.iregs[28], sequence);
        }

        // Registers with a dedicated or conventional purpose.
        if (frame_arm64->context_validity & StackFrameARM64::CONTEXT_VALID_FP) {
          sequence =
              PrintRegister64("fp", frame_arm64->context.iregs[29], sequence);
        }
        if (frame_arm64->context_validity & StackFrameARM64::CONTEXT_VALID_LR) {
          sequence =
              PrintRegister64("lr", frame_arm64->context.iregs[30], sequence);
        }
        if (frame_arm64->context_validity & StackFrameARM64::CONTEXT_VALID_SP) {
          sequence =
              PrintRegister64("sp", frame_arm64->context.iregs[31], sequence);
        }
        if (frame_arm64->context_validity & StackFrameARM64::CONTEXT_VALID_PC) {
          sequence =
              PrintRegister64("pc", frame_arm64->context.iregs[32], sequence);
        }
      } else if ((cpu == "mips") || (cpu == "mips64")) {
        const StackFrameMIPS* frame_mips =
            reinterpret_cast<const StackFrameMIPS*>(frame);

        if (frame_mips->context_validity & StackFrameMIPS::CONTEXT_VALID_GP)
          sequence = PrintRegister64(
              "gp", frame_mips->context.iregs[MD_CONTEXT_MIPS_REG_GP],
              sequence);
        if (frame_mips->context_validity & StackFrameMIPS::CONTEXT_VALID_SP)
          sequence = PrintRegister64(
              "sp", frame_mips->context.iregs[MD_CONTEXT_MIPS_REG_SP],
              sequence);
        if (frame_mips->context_validity & StackFrameMIPS::CONTEXT_VALID_FP)
          sequence = PrintRegister64(
              "fp", frame_mips->context.iregs[MD_CONTEXT_MIPS_REG_FP],
              sequence);
        if (frame_mips->context_validity & StackFrameMIPS::CONTEXT_VALID_RA)
          sequence = PrintRegister64(
              "ra", frame_mips->context.iregs[MD_CONTEXT_MIPS_REG_RA],
              sequence);
        if (frame_mips->context_validity & StackFrameMIPS::CONTEXT_VALID_PC)
          sequence = PrintRegister64("pc", frame_mips->context.epc, sequence);

        // Save registers s0-s7
        if (frame_mips->context_validity & StackFrameMIPS::CONTEXT_VALID_S0)
          sequence = PrintRegister64(
              "s0", frame_mips->context.iregs[MD_CONTEXT_MIPS_REG_S0],
              sequence);
        if (frame_mips->context_validity & StackFrameMIPS::CONTEXT_VALID_S1)
          sequence = PrintRegister64(
              "s1", frame_mips->context.iregs[MD_CONTEXT_MIPS_REG_S1],
              sequence);
        if (frame_mips->context_validity & StackFrameMIPS::CONTEXT_VALID_S2)
          sequence = PrintRegister64(
              "s2", frame_mips->context.iregs[MD_CONTEXT_MIPS_REG_S2],
              sequence);
        if (frame_mips->context_validity & StackFrameMIPS::CONTEXT_VALID_S3)
          sequence = PrintRegister64(
              "s3", frame_mips->context.iregs[MD_CONTEXT_MIPS_REG_S3],
              sequence);
        if (frame_mips->context_validity & StackFrameMIPS::CONTEXT_VALID_S4)
          sequence = PrintRegister64(
              "s4", frame_mips->context.iregs[MD_CONTEXT_MIPS_REG_S4],
              sequence);
        if (frame_mips->context_validity & StackFrameMIPS::CONTEXT_VALID_S5)
          sequence = PrintRegister64(
              "s5", frame_mips->context.iregs[MD_CONTEXT_MIPS_REG_S5],
              sequence);
        if (frame_mips->context_validity & StackFrameMIPS::CONTEXT_VALID_S6)
          sequence = PrintRegister64(
              "s6", frame_mips->context.iregs[MD_CONTEXT_MIPS_REG_S6],
              sequence);
        if (frame_mips->context_validity & StackFrameMIPS::CONTEXT_VALID_S7)
          sequence = PrintRegister64(
              "s7", frame_mips->context.iregs[MD_CONTEXT_MIPS_REG_S7],
              sequence);
      } else if (cpu == "riscv") {
        const StackFrameRISCV* frame_riscv =
            reinterpret_cast<const StackFrameRISCV*>(frame);

        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_PC)
          sequence = PrintRegister(
              "pc", frame_riscv->context.pc, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_RA)
          sequence = PrintRegister(
              "ra", frame_riscv->context.ra, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_SP)
          sequence = PrintRegister(
              "sp", frame_riscv->context.sp, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_GP)
          sequence = PrintRegister(
              "gp", frame_riscv->context.gp, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_TP)
          sequence = PrintRegister(
              "tp", frame_riscv->context.tp, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_T0)
          sequence = PrintRegister(
              "t0", frame_riscv->context.t0, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_T1)
          sequence = PrintRegister(
              "t1", frame_riscv->context.t1, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_T2)
          sequence = PrintRegister(
              "t2", frame_riscv->context.t2, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_S0)
          sequence = PrintRegister(
              "s0", frame_riscv->context.s0, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_S1)
          sequence = PrintRegister(
              "s1", frame_riscv->context.s1, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_A0)
          sequence = PrintRegister(
              "a0", frame_riscv->context.a0, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_A1)
          sequence = PrintRegister(
              "a1", frame_riscv->context.a1, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_A2)
          sequence = PrintRegister(
              "a2", frame_riscv->context.a2, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_A3)
          sequence = PrintRegister(
              "a3", frame_riscv->context.a3, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_A4)
          sequence = PrintRegister(
              "a4", frame_riscv->context.a4, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_A5)
          sequence = PrintRegister(
              "a5", frame_riscv->context.a5, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_A6)
          sequence = PrintRegister(
              "a6", frame_riscv->context.a6, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_A7)
          sequence = PrintRegister(
              "a7", frame_riscv->context.a7, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_S2)
          sequence = PrintRegister(
              "s2", frame_riscv->context.s2, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_S3)
          sequence = PrintRegister(
              "s3", frame_riscv->context.s3, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_S4)
          sequence = PrintRegister(
              "s4", frame_riscv->context.s4, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_S5)
          sequence = PrintRegister(
              "s5", frame_riscv->context.s5, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_S6)
          sequence = PrintRegister(
              "s6", frame_riscv->context.s6, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_S7)
          sequence = PrintRegister(
              "s7", frame_riscv->context.s7, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_S8)
          sequence = PrintRegister(
              "s8", frame_riscv->context.s8, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_S9)
          sequence = PrintRegister(
              "s9", frame_riscv->context.s9, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_S10)
          sequence = PrintRegister(
              "s10", frame_riscv->context.s10, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_S11)
          sequence = PrintRegister(
              "s11", frame_riscv->context.s11, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_T3)
          sequence = PrintRegister(
              "t3", frame_riscv->context.t3, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_T4)
          sequence = PrintRegister(
              "t4", frame_riscv->context.t4, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_T5)
          sequence = PrintRegister(
              "t5", frame_riscv->context.t5, sequence);
        if (frame_riscv->context_validity &
            StackFrameRISCV::CONTEXT_VALID_T6)
          sequence = PrintRegister(
              "t6", frame_riscv->context.t6, sequence);
      } else if (cpu == "riscv64") {
        const StackFrameRISCV64* frame_riscv64 =
            reinterpret_cast<const StackFrameRISCV64*>(frame);

        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_PC)
          sequence = PrintRegister64(
              "pc", frame_riscv64->context.pc, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_RA)
          sequence = PrintRegister64(
              "ra", frame_riscv64->context.ra, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_SP)
          sequence = PrintRegister64(
              "sp", frame_riscv64->context.sp, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_GP)
          sequence = PrintRegister64(
              "gp", frame_riscv64->context.gp, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_TP)
          sequence = PrintRegister64(
              "tp", frame_riscv64->context.tp, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_T0)
          sequence = PrintRegister64(
              "t0", frame_riscv64->context.t0, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_T1)
          sequence = PrintRegister64(
              "t1", frame_riscv64->context.t1, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_T2)
          sequence = PrintRegister64(
              "t2", frame_riscv64->context.t2, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_S0)
          sequence = PrintRegister64(
              "s0", frame_riscv64->context.s0, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_S1)
          sequence = PrintRegister64(
              "s1", frame_riscv64->context.s1, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_A0)
          sequence = PrintRegister64(
              "a0", frame_riscv64->context.a0, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_A1)
          sequence = PrintRegister64(
              "a1", frame_riscv64->context.a1, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_A2)
          sequence = PrintRegister64(
              "a2", frame_riscv64->context.a2, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_A3)
          sequence = PrintRegister64(
              "a3", frame_riscv64->context.a3, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_A4)
          sequence = PrintRegister64(
              "a4", frame_riscv64->context.a4, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_A5)
          sequence = PrintRegister64(
              "a5", frame_riscv64->context.a5, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_A6)
          sequence = PrintRegister64(
              "a6", frame_riscv64->context.a6, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_A7)
          sequence = PrintRegister64(
              "a7", frame_riscv64->context.a7, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_S2)
          sequence = PrintRegister64(
              "s2", frame_riscv64->context.s2, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_S3)
          sequence = PrintRegister64(
              "s3", frame_riscv64->context.s3, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_S4)
          sequence = PrintRegister64(
              "s4", frame_riscv64->context.s4, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_S5)
          sequence = PrintRegister64(
              "s5", frame_riscv64->context.s5, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_S6)
          sequence = PrintRegister64(
              "s6", frame_riscv64->context.s6, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_S7)
          sequence = PrintRegister64(
              "s7", frame_riscv64->context.s7, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_S8)
          sequence = PrintRegister64(
              "s8", frame_riscv64->context.s8, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_S9)
          sequence = PrintRegister64(
              "s9", frame_riscv64->context.s9, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_S10)
          sequence = PrintRegister64(
              "s10", frame_riscv64->context.s10, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_S11)
          sequence = PrintRegister64(
              "s11", frame_riscv64->context.s11, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_T3)
          sequence = PrintRegister64(
              "t3", frame_riscv64->context.t3, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_T4)
          sequence = PrintRegister64(
              "t4", frame_riscv64->context.t4, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_T5)
          sequence = PrintRegister64(
              "t5", frame_riscv64->context.t5, sequence);
        if (frame_riscv64->context_validity &
            StackFrameRISCV64::CONTEXT_VALID_T6)
          sequence = PrintRegister64(
              "t6", frame_riscv64->context.t6, sequence);
      }
    }
    printf("\n    Found by: %s\n", frame->trust_description().c_str());

    // Print stack contents.
    if (output_stack_contents && frame_index + 1 < frame_count) {
      const string indent("    ");
      PrintStackContents(indent, frame, stack->frames()->at(frame_index + 1),
                         cpu, memory, modules, resolver);
    }
  }
}

struct Options {
  bool brief = false;

  std::string executable_path;
  std::string minidump_file;
  std::string symbol_path;
};

// Processes |options.minidump_file| using MinidumpProcessor.
// |options.symbol_path|, if non-empty, is the base directory of a
// symbol storage area, laid out in the format required by
// SimpleSymbolSupplier.  If such a storage area is specified, it is
// made available for use by the MinidumpProcessor.
//
// Returns the value of MinidumpProcessor::Process.  If processing succeeds,
// prints identifying OS and CPU information from the minidump, crash
// information if the minidump was produced as a result of a crash, and
// call stacks for each thread contained in the minidump.  All information
// is printed to stdout.
bool PrintMinidumpProcess(const Options& options) {
  std::vector<string> symbol_paths;
  symbol_paths.push_back(options.symbol_path);
  scoped_ptr<SimpleSymbolSupplier> symbol_supplier;
  symbol_supplier.reset(new SimpleSymbolSupplier(symbol_paths));

  BasicSourceLineResolver resolver;
  MinidumpProcessor minidump_processor(symbol_supplier.get(), &resolver);

  // Increase the maximum number of threads and regions.
  MinidumpThreadList::set_max_threads(std::numeric_limits<uint32_t>::max());
  MinidumpMemoryList::set_max_regions(std::numeric_limits<uint32_t>::max());
  // Process the minidump.
  Minidump dump(options.minidump_file);
  if (!dump.Read()) {
     BPLOG(ERROR) << "Minidump " << dump.path() << " could not be read";
     return false;
  }
  ProcessState process_state;
  if (minidump_processor.Process(&dump, &process_state) !=
      google_breakpad::PROCESS_OK) {
    BPLOG(ERROR) << "MinidumpProcessor::Process failed";
    return false;
  }

    bool output_stack_contents = false;
    string cpu = process_state.system_info()->cpu;
    // Print crash information.
    if (process_state.crashed()) {
      printf("Crash reason:  %s\n", process_state.crash_reason().c_str());
      printf("Crash address: 0x%" PRIx64 "\n", process_state.crash_address());
    } else {
      printf("No crash\n");
    }
    string assertion = process_state.assertion();
      if (!assertion.empty()) {
        printf("Assertion: %s\n", assertion.c_str());
      }
    // Compute process uptime if the process creation and crash times are
    // available in the dump.
    if (process_state.time_date_stamp() != 0 &&
        process_state.process_create_time() != 0 &&
        process_state.time_date_stamp() >= process_state.process_create_time()) {
      printf("Process uptime: %d seconds\n",
             process_state.time_date_stamp() -
                 process_state.process_create_time());
    } else {
      printf("Process uptime: not available\n");
    }

  // If the thread that requested the dump is known, print it first.
  int requesting_thread = process_state.requesting_thread();
  if (requesting_thread != -1) {
    printf("\n");
    printf("Thread %d (%s)\n",
          requesting_thread,
          process_state.crashed() ? "crashed" :
                                    "requested dump, did not crash");
    PrintStack(process_state.threads()->at(requesting_thread), cpu,
               output_stack_contents,
               process_state.thread_memory_regions()->at(requesting_thread),
               process_state.modules(), &resolver, options.brief);
  }

  // Print all of the threads in the dump.
  int thread_count = process_state.threads()->size();
  for (int thread_index = 0; thread_index < thread_count; ++thread_index) {
    if (thread_index != requesting_thread) {
      // Don't print the crash thread again, it was already printed.
      printf("\n");
      printf("Thread %d\n", thread_index);
      PrintStack(process_state.threads()->at(thread_index), cpu,
                output_stack_contents,
                process_state.thread_memory_regions()->at(thread_index),
                process_state.modules(), &resolver, options.brief);
    }
  }

  return true;
}

//=============================================================================

static void Usage(int argc, const char *argv[], bool error) {
  fprintf(error ? stderr : stdout,
          "Usage: %s [options] <executable-path> <minidump-file>\n"
          "\n"
          "Options:\n"
          "\n"
          "  -b         Brief output - print only stacktraces\n",
          google_breakpad::BaseName(argv[0]).c_str());
}

static void SetupOptions(int argc, const char *argv[], Options& options) {
  int ch;
  while ((ch = getopt(argc, (char* const*)argv, "bh")) != -1) {
    switch (ch) {
      case 'h':
        Usage(argc, argv, false);
        exit(0);
        break;

      case 'b':
        options.brief = true;
        break;

      case '?':
        Usage(argc, argv, true);
        exit(1);
        break;
    }
  }

  if ((argc - optind) != 2) {
    Usage(argc, argv, true);
    exit(1);
  }

  options.executable_path = argv[optind];
  options.minidump_file = argv[optind + 1];
}

int main (int argc, const char * argv[]) {
  Options options{};
  SetupOptions(argc, argv, options);

  options.symbol_path = "syms";
  auto syms = CreateSyms(options.executable_path.c_str());
  if (!syms) return 1;
  if (!SaveSyms(std::move(*syms), options.symbol_path.c_str())) return 1;
  if (!PrintMinidumpProcess(options)) return 1;
  return 0;
}
