// -*- mode: c++ -*-

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
#include <unistd.h>

#include <algorithm>
#include <iostream>
#include <limits>
#include <memory>
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
#include "processor/logging.h"
#include "processor/simple_symbol_supplier.h"
#include "processor/stackwalk_common.h"

using google_breakpad::BasicSourceLineResolver;
using google_breakpad::DumpSymbols;
using google_breakpad::Minidump;
using google_breakpad::MinidumpMemoryList;
using google_breakpad::MinidumpThreadList;
using google_breakpad::MinidumpProcessor;
using google_breakpad::Module;
using google_breakpad::ProcessState;
using google_breakpad::SimpleSymbolSupplier;
using google_breakpad::scoped_ptr;
using std::vector;

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

static bool Start(string srcPath) {
  string dsymPath = srcPath + ".dSYM";
  SymbolData symbol_data = NO_DATA | CFI | SYMBOLS_AND_FILES;
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
    return false;

  // Read the primary file into a Breakpad Module.
  Module* module = NULL;
  if (!dump_symbols.ReadSymbolData(&module))
    return false;
  scoped_ptr<Module> scoped_module(module);

  // If this is a split module, read the secondary Mach-O file, from which the
  // CFI data will be extracted.
  if (split_module && primary_file == dsymPath) {
    if (!dump_symbols.Read(srcPath))
      return false;

    Module* cfi_module = NULL;
    if (!dump_symbols.ReadSymbolData(&cfi_module))
      return false;
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
      return false;
    }

    CopyCFIDataBetweenModules(module, cfi_module);
  }

  return module->Write(std::cout, symbol_data);
}

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
bool PrintMinidumpProcess(const char* minidump_file, const char* symbol_path) {
  std::vector<string> symbol_paths;
  symbol_paths.push_back(symbol_path);
  scoped_ptr<SimpleSymbolSupplier> symbol_supplier;
  symbol_supplier.reset(new SimpleSymbolSupplier(symbol_paths));

  BasicSourceLineResolver resolver;
  MinidumpProcessor minidump_processor(symbol_supplier.get(), &resolver);

  // Increase the maximum number of threads and regions.
  MinidumpThreadList::set_max_threads(std::numeric_limits<uint32_t>::max());
  MinidumpMemoryList::set_max_regions(std::numeric_limits<uint32_t>::max());
  // Process the minidump.
  Minidump dump(minidump_file);
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

  PrintProcessState(process_state, false, false, &resolver);
  return true;
}

//=============================================================================
int main (int argc, const char * argv[]) {
  if (!Start(argv[1])) return 1;
  if (!PrintMinidumpProcess(argv[2], "syms")) return 1;
  return 0;
}
