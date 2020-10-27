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


#include "ExecFormat.h"
#include "Types.h"

#if USE_ELF_SYMBOLS

#include <dlfcn.h>
#include <elf.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/types.h>

#include <vector>

#include "KAssert.h"

namespace {

#if !defined(ELFSIZE)
#error "Define ELFSIZE to 32 or 64"
#endif

#if ELFSIZE == 32
#define Elf_Ehdr        Elf32_Ehdr
#define Elf_Shdr        Elf32_Shdr
#define Elf_Sym         Elf32_Sym
#elif ELFSIZE == 64
#define Elf_Ehdr        Elf64_Ehdr
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

typedef KStdVector<SymRecord> SymRecordList;

SymRecordList* symbols = nullptr;

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
  symbols = konanConstructInstance<SymRecordList>();
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

const char* addressToSymbol(const void* address) {
  if (address == nullptr) return nullptr;

  // First, look up in dynamically loaded symbols.
  Dl_info info;
  if (dladdr(address, &info) != 0 && info.dli_sname != nullptr) {
    return info.dli_sname;
  }

  // Otherwise, consult symbol table of the file.
  if (symbols == nullptr) {
    initSymbols();
  }

  unsigned long addressValue = (unsigned long)address;

  for (auto record : *symbols) {
    auto begin = record.symtabBegin;
    auto end = record.symtabEnd;
    while (begin < end) {
      // st_value is load address adjusted.
      if (addressValue >= begin->st_value && addressValue < begin->st_value + begin->st_size) {
        return &record.strtab[begin->st_name];
      }
      begin++;
    }
  }
  return nullptr;
}

}  // namespace

extern "C" bool AddressToSymbol(const void* address, char* resultBuffer, size_t resultBufferSize) {
  const char* result = addressToSymbol(address);
  if (result == nullptr) {
    return false;
  } else {
    strncpy(resultBuffer, result, resultBufferSize);
    resultBuffer[resultBufferSize - 1] = '\0';
    return true;
  }
}

#elif USE_PE_COFF_SYMBOLS

#include <windows.h>
#include <stdlib.h>
#include <string.h>

#include "KAssert.h"

namespace {

static void* mapModuleFile(HMODULE hModule) {
  DWORD bufferLength = 64;
  wchar_t* buffer = nullptr;
  for (;;) {
    auto newBuffer = (wchar_t*)konanAllocMemory(sizeof(wchar_t) * bufferLength);
    RuntimeAssert(newBuffer != nullptr, "Out of memory");
    if (buffer != nullptr) {
      konanFreeMemory(buffer);
    }
    buffer = newBuffer;

    DWORD res = GetModuleFileNameW(hModule, buffer, bufferLength);
    if (res != 0 && res < bufferLength) {
      break;
    }
    const int MAX_BUFFER_SIZE = 32768; // Max path length + 1.
    if (res == bufferLength && bufferLength < MAX_BUFFER_SIZE) {
      // Buffer is too small, continue:
      bufferLength *= 2;
      continue;
    }

    // Invalid result.
    konanFreeMemory(buffer);
    return nullptr;
  }

  HANDLE hFile = CreateFileW(
      /* lpFileName = */ buffer,
      /* dwDesiredAccess = */ GENERIC_READ,
      /* dwShareMode =  */ FILE_SHARE_READ,
      /* lpSecurityAttributes = */ nullptr,
      /* dwCreationDisposition = */ OPEN_EXISTING,
      /* dwFlagsAndAttributes = */ FILE_ATTRIBUTE_NORMAL,
      /* hTemplateFile = */ nullptr
  );
  konanFreeMemory(buffer);
  if (hFile == INVALID_HANDLE_VALUE) {
    // Can't open module file.
    return nullptr;
  }

  HANDLE hFileMappingObject = CreateFileMapping(
      hFile,
      /* lpAttributes =  */ nullptr,
      /* flProtect = */ PAGE_READONLY,
      /* dwMaximumSizeHigh = */ 0,
      /* dwMaximumSizeLow =  */ 0,
      /* lpName = */ nullptr
  );
  if (hFileMappingObject == nullptr) {
    // Can't create file mapping.
    CloseHandle(hFile);
    return nullptr;
  }

  LPVOID mapAddress = MapViewOfFile(
      hFileMappingObject,
      /* dwDesiredAccess = */ FILE_MAP_READ,
      /* dwFileOffsetHigh = */ 0,
      /* dwFileOffsetLow = */ 0,
      /* dwNumberOfBytesToMap = */ 0
  );
  if (mapAddress == nullptr) {
    // Failed to create map view.
    CloseHandle(hFileMappingObject);
    CloseHandle(hFile);
    return nullptr;
  }

  return mapAddress;
}

class SymbolTable {
 private:

  char* imageBase = nullptr;
  IMAGE_SECTION_HEADER* sectionHeaders = nullptr;
  IMAGE_SYMBOL* symbols = nullptr;
  DWORD numberOfSymbols = 0;

  // Note: it doesn't free resources yet.
  ~SymbolTable() {}

  static const int SYMBOL_SHORT_NAME_LENGTH = 8;

  void getSymbolName(IMAGE_SYMBOL* sym, char* resultBuffer, size_t resultBufferSize) {
    if (sym->N.Name.Short != 0) {
      // ShortName is not zero-terminated if its length exactly equals SYMBOL_SHORT_NAME_LENGTH.
      // Copy it to the buffer and zero-terminate explicitly:
      size_t bytesToCopy = SYMBOL_SHORT_NAME_LENGTH;
      if (bytesToCopy > resultBufferSize - 1) bytesToCopy = resultBufferSize - 1;

      memcpy(resultBuffer, sym->N.ShortName, bytesToCopy);
      resultBuffer[bytesToCopy] = '\0';
    } else {
      const char* strTable = (const char*)(symbols + numberOfSymbols);
      const char* result = strTable + sym->N.Name.Long;
      strncpy(resultBuffer, result, resultBufferSize);
      resultBuffer[resultBufferSize - 1] = '\0';
    }
  }

  const void* getSymbolAddress(IMAGE_SYMBOL* symbol) {
    IMAGE_SECTION_HEADER* sectionHeader = &sectionHeaders[symbol->SectionNumber - 1];
    return (const void*)(imageBase + sectionHeader->VirtualAddress + symbol->Value);
  }

  IMAGE_SYMBOL* findFunctionSymbol(const void* address) {
    for (DWORD i = 0; i < numberOfSymbols; ++i) {
      IMAGE_SYMBOL* symbol = &symbols[i];
      if (symbol->Type == 0x20 && address == getSymbolAddress(symbol)) {
        return symbol;
      }
    }
    return nullptr;
  }

 public:
  explicit SymbolTable(HMODULE hModule) {
    imageBase = (char*)hModule;
    IMAGE_DOS_HEADER* dosHeader = (IMAGE_DOS_HEADER*)imageBase;
    RuntimeAssert(dosHeader->e_magic == IMAGE_DOS_SIGNATURE, "PE executable e_magic mismatch");

    IMAGE_NT_HEADERS* ntHeaders = (IMAGE_NT_HEADERS*)(imageBase + dosHeader->e_lfanew);
    RuntimeAssert(ntHeaders->Signature == IMAGE_NT_SIGNATURE, "PE executable NT signature mismatch");

    IMAGE_FILE_HEADER* fileHeader = &ntHeaders->FileHeader;

    sectionHeaders = (IMAGE_SECTION_HEADER*)(((char*)(fileHeader + 1)) + fileHeader->SizeOfOptionalHeader);
    if (fileHeader->PointerToSymbolTable == 0 || fileHeader->NumberOfSymbols == 0) {
      // No symbols.
      return;
    }

    // Symbol table doesn't get mapped to the memory, so we have to load it ourselves:
    char* mappedModuleFile = (char*)mapModuleFile(hModule);
    if (mappedModuleFile != nullptr) {
      symbols = (IMAGE_SYMBOL*)(mappedModuleFile + fileHeader->PointerToSymbolTable);
      numberOfSymbols = fileHeader->NumberOfSymbols;
    }
  }

  bool functionAddressToSymbol(const void* address, char* resultBuffer, size_t resultBufferSize) {
    IMAGE_SYMBOL* symbol = findFunctionSymbol(address);
    if (symbol == nullptr) {
      return false;
    } else {
      getSymbolName(symbol, resultBuffer, resultBufferSize);
      return true;
    }
  }

};

SymbolTable* theExeSymbolTable = nullptr;

}  // namespace

extern "C" bool AddressToSymbol(const void* address, char* resultBuffer, size_t resultBufferSize) {
  if (theExeSymbolTable == nullptr) {
    // Note: do not protecting the lazy initialization by critical sections for simplicity;
    // this doesn't have any serious consequences.
    HMODULE hModule = nullptr;
    int rv = GetModuleHandleExW(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS | GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
               reinterpret_cast<LPCWSTR>(&AddressToSymbol), &hModule);
    RuntimeAssert(rv != 0, "GetModuleHandleExW fails");
    theExeSymbolTable = konanConstructInstance<SymbolTable>(hModule);
  }
  return theExeSymbolTable->functionAddressToSymbol(address, resultBuffer, resultBufferSize);
}

#else

extern "C" bool AddressToSymbol(const void* address, char* resultBuffer, size_t resultBufferSize) {
  return false;
}

#endif // USE_ELF_SYMBOLS
