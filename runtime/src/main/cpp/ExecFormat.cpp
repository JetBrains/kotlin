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

#if USE_GCC_UNWIND

#include <dlfcn.h>
#include <elf.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/types.h>

#include <vector>

#include "Assert.h"

namespace {

#if !defined(ELFSIZE)
#error "Define ELFSIZE to 32 or 64"
#endif

#define CONCAT(x,y)     __CONCAT(x,y)
#define ELFNAME(x)      CONCAT(elf,CONCAT(ELFSIZE,CONCAT(_,x)))
#define ELFNAME2(x,y)   CONCAT(x,CONCAT(_elf,CONCAT(ELFSIZE,CONCAT(_,y))))
#define ELFNAMEEND(x)   CONCAT(x,CONCAT(_elf,ELFSIZE))
#define ELFDEFNNAME(x)  CONCAT(ELF,CONCAT(ELFSIZE,CONCAT(_,x)))

#if ELFSIZE == 32
#define Elf_Ehdr        Elf32_Ehdr
#define Elf_Phdr        Elf32_Phdr
#define Elf_Shdr        Elf32_Shdr
#define Elf_Sym         Elf32_Sym
#elif ELFSIZE == 64
#define Elf_Ehdr        Elf64_Ehdr
#define Elf_Phdr        Elf64_Phdr
#define Elf_Shdr        Elf64_Shdr
#define Elf_Sym         Elf64_Sym
#else
#error "Impossible ELFSIZE"
#endif

struct SymRecord {
  Elf_Sym* symtabBegin;
  Elf_Sym* symtabEnd;
  char* strtab;
};

std::vector<SymRecord>* symbols = nullptr;
char* mapAddress = nullptr;

// Unfortunately, symbol tables are stored in ELF sections not mapped
// during regular execution, so we have to map binary ourselves.
Elf_Ehdr* findElfHeader() {
  int fd = open("/proc/self/exe", O_RDONLY);
  if (fd < 0) return nullptr;
  struct stat fd_stat;
  if (fstat(fd, &fd_stat) < 0) return nullptr;
  void* result = mmap(nullptr, fd_stat.st_size, PROT_READ, MAP_PRIVATE, fd, 0);
  if (result == MAP_FAILED) return nullptr;
  return (Elf_Ehdr*)result;
}

void initSymbols() {
  RuntimeAssert(symbols == nullptr, "Init twice");
  symbols = new std::vector<SymRecord>();
  Elf_Ehdr* ehdr = findElfHeader();
  if (ehdr == nullptr) return;
  RuntimeAssert(strncmp((const char*)ehdr->e_ident, ELFMAG, SELFMAG) == 0, "Must be an ELF");
  char* mapAddress = (char*)ehdr;
  Elf_Shdr* shdr = (Elf_Shdr*)(mapAddress + ehdr->e_shoff);
  for (int i = 0; i < ehdr->e_shnum; i++) {
    if (shdr[i].sh_type == SHT_SYMTAB) {   // Static symbol table.
      SymRecord record;
      record.symtabBegin = (Elf_Sym*)(mapAddress + shdr[i].sh_offset);
      record.symtabEnd = (Elf_Sym*)((char*)record.symtabBegin + shdr[i].sh_size);
      record.strtab = (char *)(mapAddress + shdr[shdr[i].sh_link].sh_offset);
      symbols->push_back(record);
    }
    if (shdr[i].sh_type == SHT_DYNSYM) {   // Dynamic symbol table.
      SymRecord record;
      record.symtabBegin = (Elf_Sym*)(mapAddress + shdr[i].sh_offset);
      record.symtabEnd = (Elf_Sym*)((char*)record.symtabBegin + shdr[i].sh_size);
      record.strtab = (char*)(mapAddress + shdr[shdr[i].sh_link].sh_offset);
      symbols->push_back(record);
    }
  }
}

}  // namespace

extern "C" const char* AddressToSymbol(unsigned long address) {
  if (address == 0) return nullptr;

  // First, look up in dynamically loaded symbols.
  Dl_info info;
  if (dladdr((const void*)address, &info) != 0 && info.dli_sname != nullptr) {
    return info.dli_sname;
  }

  // Otherwise, consult symbol table of the file.
  if (symbols == nullptr) {
    initSymbols();
  }

  for (auto record : *symbols) {
    auto begin = record.symtabBegin;
    auto end = record.symtabEnd;
    while (begin < end) {
      if (address >= (unsigned long)mapAddress + begin->st_value &&
	  address < (unsigned long)mapAddress + begin->st_value + begin->st_size) {
	return &record.strtab[begin->st_name];
      }
      begin++;
    }
  }
  return nullptr;
}

#else

extern "C" const char* AddressToSymbol(unsigned long address) {
  return nullptr;
}

#endif // USE_GCC_UNWIND
