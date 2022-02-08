/* elf.c -- Get debug data from an ELF file for backtraces.
   Copyright (C) 2012-2021 Free Software Foundation, Inc.
   Written by Ian Lance Taylor, Google.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    (1) Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.

    (2) Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in
    the documentation and/or other materials provided with the
    distribution.

    (3) The name of the author may not be used to
    endorse or promote products derived from this software without
    specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.  */

#include "config.h"

#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>

#ifdef HAVE_DL_ITERATE_PHDR
#include <link.h>
#endif

#include "backtrace.h"
#include "internal.h"

#ifndef S_ISLNK
 #ifndef S_IFLNK
  #define S_IFLNK 0120000
 #endif
 #ifndef S_IFMT
  #define S_IFMT 0170000
 #endif
 #define S_ISLNK(m) (((m) & S_IFMT) == S_IFLNK)
#endif

#ifndef __GNUC__
#define __builtin_prefetch(p, r, l)
#define unlikely(x) (x)
#else
#define unlikely(x) __builtin_expect(!!(x), 0)
#endif

#if !defined(HAVE_DECL_STRNLEN) || !HAVE_DECL_STRNLEN

/* If strnlen is not declared, provide our own version.  */

static size_t
xstrnlen (const char *s, size_t maxlen)
{
  size_t i;

  for (i = 0; i < maxlen; ++i)
    if (s[i] == '\0')
      break;
  return i;
}

#define strnlen xstrnlen

#endif

#ifndef HAVE_LSTAT

/* Dummy version of lstat for systems that don't have it.  */

static int
xlstat (const char *path ATTRIBUTE_UNUSED, struct stat *st ATTRIBUTE_UNUSED)
{
  return -1;
}

#define lstat xlstat

#endif

#ifndef HAVE_READLINK

/* Dummy version of readlink for systems that don't have it.  */

static ssize_t
xreadlink (const char *path ATTRIBUTE_UNUSED, char *buf ATTRIBUTE_UNUSED,
	   size_t bufsz ATTRIBUTE_UNUSED)
{
  return -1;
}

#define readlink xreadlink

#endif

#ifndef HAVE_DL_ITERATE_PHDR

/* Dummy version of dl_iterate_phdr for systems that don't have it.  */

#define dl_phdr_info x_dl_phdr_info
#define dl_iterate_phdr x_dl_iterate_phdr

struct dl_phdr_info
{
  uintptr_t dlpi_addr;
  const char *dlpi_name;
};

static int
dl_iterate_phdr (int (*callback) (struct dl_phdr_info *,
				  size_t, void *) ATTRIBUTE_UNUSED,
		 void *data ATTRIBUTE_UNUSED)
{
  return 0;
}

#endif /* ! defined (HAVE_DL_ITERATE_PHDR) */

/* The configure script must tell us whether we are 32-bit or 64-bit
   ELF.  We could make this code test and support either possibility,
   but there is no point.  This code only works for the currently
   running executable, which means that we know the ELF mode at
   configure time.  */

#if BACKTRACE_ELF_SIZE != 32 && BACKTRACE_ELF_SIZE != 64
#error "Unknown BACKTRACE_ELF_SIZE"
#endif

/* <link.h> might #include <elf.h> which might define our constants
   with slightly different values.  Undefine them to be safe.  */

#undef EI_NIDENT
#undef EI_MAG0
#undef EI_MAG1
#undef EI_MAG2
#undef EI_MAG3
#undef EI_CLASS
#undef EI_DATA
#undef EI_VERSION
#undef ELF_MAG0
#undef ELF_MAG1
#undef ELF_MAG2
#undef ELF_MAG3
#undef ELFCLASS32
#undef ELFCLASS64
#undef ELFDATA2LSB
#undef ELFDATA2MSB
#undef EV_CURRENT
#undef ET_DYN
#undef EM_PPC64
#undef EF_PPC64_ABI
#undef SHN_LORESERVE
#undef SHN_XINDEX
#undef SHN_UNDEF
#undef SHT_PROGBITS
#undef SHT_SYMTAB
#undef SHT_STRTAB
#undef SHT_DYNSYM
#undef SHF_COMPRESSED
#undef STT_OBJECT
#undef STT_FUNC
#undef NT_GNU_BUILD_ID
#undef ELFCOMPRESS_ZLIB

/* Basic types.  */

typedef uint16_t b_elf_half;    /* Elf_Half.  */
typedef uint32_t b_elf_word;    /* Elf_Word.  */
typedef int32_t  b_elf_sword;   /* Elf_Sword.  */

#if BACKTRACE_ELF_SIZE == 32

typedef uint32_t b_elf_addr;    /* Elf_Addr.  */
typedef uint32_t b_elf_off;     /* Elf_Off.  */

typedef uint32_t b_elf_wxword;  /* 32-bit Elf_Word, 64-bit ELF_Xword.  */

#else

typedef uint64_t b_elf_addr;    /* Elf_Addr.  */
typedef uint64_t b_elf_off;     /* Elf_Off.  */
typedef uint64_t b_elf_xword;   /* Elf_Xword.  */
typedef int64_t  b_elf_sxword;  /* Elf_Sxword.  */

typedef uint64_t b_elf_wxword;  /* 32-bit Elf_Word, 64-bit ELF_Xword.  */

#endif

/* Data structures and associated constants.  */

#define EI_NIDENT 16

typedef struct {
  unsigned char	e_ident[EI_NIDENT];	/* ELF "magic number" */
  b_elf_half	e_type;			/* Identifies object file type */
  b_elf_half	e_machine;		/* Specifies required architecture */
  b_elf_word	e_version;		/* Identifies object file version */
  b_elf_addr	e_entry;		/* Entry point virtual address */
  b_elf_off	e_phoff;		/* Program header table file offset */
  b_elf_off	e_shoff;		/* Section header table file offset */
  b_elf_word	e_flags;		/* Processor-specific flags */
  b_elf_half	e_ehsize;		/* ELF header size in bytes */
  b_elf_half	e_phentsize;		/* Program header table entry size */
  b_elf_half	e_phnum;		/* Program header table entry count */
  b_elf_half	e_shentsize;		/* Section header table entry size */
  b_elf_half	e_shnum;		/* Section header table entry count */
  b_elf_half	e_shstrndx;		/* Section header string table index */
} b_elf_ehdr;  /* Elf_Ehdr.  */

#define EI_MAG0 0
#define EI_MAG1 1
#define EI_MAG2 2
#define EI_MAG3 3
#define EI_CLASS 4
#define EI_DATA 5
#define EI_VERSION 6

#define ELFMAG0 0x7f
#define ELFMAG1 'E'
#define ELFMAG2 'L'
#define ELFMAG3 'F'

#define ELFCLASS32 1
#define ELFCLASS64 2

#define ELFDATA2LSB 1
#define ELFDATA2MSB 2

#define EV_CURRENT 1

#define ET_DYN 3

#define EM_PPC64 21
#define EF_PPC64_ABI 3

typedef struct {
  b_elf_word	sh_name;		/* Section name, index in string tbl */
  b_elf_word	sh_type;		/* Type of section */
  b_elf_wxword	sh_flags;		/* Miscellaneous section attributes */
  b_elf_addr	sh_addr;		/* Section virtual addr at execution */
  b_elf_off	sh_offset;		/* Section file offset */
  b_elf_wxword	sh_size;		/* Size of section in bytes */
  b_elf_word	sh_link;		/* Index of another section */
  b_elf_word	sh_info;		/* Additional section information */
  b_elf_wxword	sh_addralign;		/* Section alignment */
  b_elf_wxword	sh_entsize;		/* Entry size if section holds table */
} b_elf_shdr;  /* Elf_Shdr.  */

#define SHN_UNDEF	0x0000		/* Undefined section */
#define SHN_LORESERVE	0xFF00		/* Begin range of reserved indices */
#define SHN_XINDEX	0xFFFF		/* Section index is held elsewhere */

#define SHT_PROGBITS 1
#define SHT_SYMTAB 2
#define SHT_STRTAB 3
#define SHT_DYNSYM 11

#define SHF_COMPRESSED 0x800

#if BACKTRACE_ELF_SIZE == 32

typedef struct
{
  b_elf_word	st_name;		/* Symbol name, index in string tbl */
  b_elf_addr	st_value;		/* Symbol value */
  b_elf_word	st_size;		/* Symbol size */
  unsigned char	st_info;		/* Symbol binding and type */
  unsigned char	st_other;		/* Visibility and other data */
  b_elf_half	st_shndx;		/* Symbol section index */
} b_elf_sym;  /* Elf_Sym.  */

#else /* BACKTRACE_ELF_SIZE != 32 */

typedef struct
{
  b_elf_word	st_name;		/* Symbol name, index in string tbl */
  unsigned char	st_info;		/* Symbol binding and type */
  unsigned char	st_other;		/* Visibility and other data */
  b_elf_half	st_shndx;		/* Symbol section index */
  b_elf_addr	st_value;		/* Symbol value */
  b_elf_xword	st_size;		/* Symbol size */
} b_elf_sym;  /* Elf_Sym.  */

#endif /* BACKTRACE_ELF_SIZE != 32 */

#define STT_OBJECT 1
#define STT_FUNC 2

typedef struct
{
  uint32_t namesz;
  uint32_t descsz;
  uint32_t type;
  char name[1];
} b_elf_note;

#define NT_GNU_BUILD_ID 3

#if BACKTRACE_ELF_SIZE == 32

typedef struct
{
  b_elf_word	ch_type;		/* Compresstion algorithm */
  b_elf_word	ch_size;		/* Uncompressed size */
  b_elf_word	ch_addralign;		/* Alignment for uncompressed data */
} b_elf_chdr;  /* Elf_Chdr */

#else /* BACKTRACE_ELF_SIZE != 32 */

typedef struct
{
  b_elf_word	ch_type;		/* Compression algorithm */
  b_elf_word	ch_reserved;		/* Reserved */
  b_elf_xword	ch_size;		/* Uncompressed size */
  b_elf_xword	ch_addralign;		/* Alignment for uncompressed data */
} b_elf_chdr;  /* Elf_Chdr */

#endif /* BACKTRACE_ELF_SIZE != 32 */

#define ELFCOMPRESS_ZLIB 1

/* Names of sections, indexed by enum dwarf_section in internal.h.  */

static const char * const dwarf_section_names[DEBUG_MAX] =
{
  ".debug_info",
  ".debug_line",
  ".debug_abbrev",
  ".debug_ranges",
  ".debug_str",
  ".debug_addr",
  ".debug_str_offsets",
  ".debug_line_str",
  ".debug_rnglists"
};

/* Information we gather for the sections we care about.  */

struct debug_section_info
{
  /* Section file offset.  */
  off_t offset;
  /* Section size.  */
  size_t size;
  /* Section contents, after read from file.  */
  const unsigned char *data;
  /* Whether the SHF_COMPRESSED flag is set for the section.  */
  int compressed;
};

/* Information we keep for an ELF symbol.  */

struct elf_symbol
{
  /* The name of the symbol.  */
  const char *name;
  /* The address of the symbol.  */
  uintptr_t address;
  /* The size of the symbol.  */
  size_t size;
};

/* Information to pass to elf_syminfo.  */

struct elf_syminfo_data
{
  /* Symbols for the next module.  */
  struct elf_syminfo_data *next;
  /* The ELF symbols, sorted by address.  */
  struct elf_symbol *symbols;
  /* The number of symbols.  */
  size_t count;
};

/* A view that works for either a file or memory.  */

struct elf_view
{
  struct backtrace_view view;
  int release; /* If non-zero, must call backtrace_release_view.  */
};

/* Information about PowerPC64 ELFv1 .opd section.  */

struct elf_ppc64_opd_data
{
  /* Address of the .opd section.  */
  b_elf_addr addr;
  /* Section data.  */
  const char *data;
  /* Size of the .opd section.  */
  size_t size;
  /* Corresponding section view.  */
  struct elf_view view;
};

/* Create a view of SIZE bytes from DESCRIPTOR/MEMORY at OFFSET.  */

static int
elf_get_view (struct backtrace_state *state, int descriptor,
	      const unsigned char *memory, size_t memory_size, off_t offset,
	      uint64_t size, backtrace_error_callback error_callback,
	      void *data, struct elf_view *view)
{
  if (memory == NULL)
    {
      view->release = 1;
      return backtrace_get_view (state, descriptor, offset, size,
				 error_callback, data, &view->view);
    }
  else
    {
      if ((uint64_t) offset + size > (uint64_t) memory_size)
	{
	  error_callback (data, "out of range for in-memory file", 0);
	  return 0;
	}
      view->view.data = (const void *) (memory + offset);
      view->view.base = NULL;
      view->view.len = size;
      view->release = 0;
      return 1;
    }
}

/* Release a view read by elf_get_view.  */

static void
elf_release_view (struct backtrace_state *state, struct elf_view *view,
		  backtrace_error_callback error_callback, void *data)
{
  if (view->release)
    backtrace_release_view (state, &view->view, error_callback, data);
}

/* Compute the CRC-32 of BUF/LEN.  This uses the CRC used for
   .gnu_debuglink files.  */

static uint32_t
elf_crc32 (uint32_t crc, const unsigned char *buf, size_t len)
{
  static const uint32_t crc32_table[256] =
    {
      0x00000000, 0x77073096, 0xee0e612c, 0x990951ba, 0x076dc419,
      0x706af48f, 0xe963a535, 0x9e6495a3, 0x0edb8832, 0x79dcb8a4,
      0xe0d5e91e, 0x97d2d988, 0x09b64c2b, 0x7eb17cbd, 0xe7b82d07,
      0x90bf1d91, 0x1db71064, 0x6ab020f2, 0xf3b97148, 0x84be41de,
      0x1adad47d, 0x6ddde4eb, 0xf4d4b551, 0x83d385c7, 0x136c9856,
      0x646ba8c0, 0xfd62f97a, 0x8a65c9ec, 0x14015c4f, 0x63066cd9,
      0xfa0f3d63, 0x8d080df5, 0x3b6e20c8, 0x4c69105e, 0xd56041e4,
      0xa2677172, 0x3c03e4d1, 0x4b04d447, 0xd20d85fd, 0xa50ab56b,
      0x35b5a8fa, 0x42b2986c, 0xdbbbc9d6, 0xacbcf940, 0x32d86ce3,
      0x45df5c75, 0xdcd60dcf, 0xabd13d59, 0x26d930ac, 0x51de003a,
      0xc8d75180, 0xbfd06116, 0x21b4f4b5, 0x56b3c423, 0xcfba9599,
      0xb8bda50f, 0x2802b89e, 0x5f058808, 0xc60cd9b2, 0xb10be924,
      0x2f6f7c87, 0x58684c11, 0xc1611dab, 0xb6662d3d, 0x76dc4190,
      0x01db7106, 0x98d220bc, 0xefd5102a, 0x71b18589, 0x06b6b51f,
      0x9fbfe4a5, 0xe8b8d433, 0x7807c9a2, 0x0f00f934, 0x9609a88e,
      0xe10e9818, 0x7f6a0dbb, 0x086d3d2d, 0x91646c97, 0xe6635c01,
      0x6b6b51f4, 0x1c6c6162, 0x856530d8, 0xf262004e, 0x6c0695ed,
      0x1b01a57b, 0x8208f4c1, 0xf50fc457, 0x65b0d9c6, 0x12b7e950,
      0x8bbeb8ea, 0xfcb9887c, 0x62dd1ddf, 0x15da2d49, 0x8cd37cf3,
      0xfbd44c65, 0x4db26158, 0x3ab551ce, 0xa3bc0074, 0xd4bb30e2,
      0x4adfa541, 0x3dd895d7, 0xa4d1c46d, 0xd3d6f4fb, 0x4369e96a,
      0x346ed9fc, 0xad678846, 0xda60b8d0, 0x44042d73, 0x33031de5,
      0xaa0a4c5f, 0xdd0d7cc9, 0x5005713c, 0x270241aa, 0xbe0b1010,
      0xc90c2086, 0x5768b525, 0x206f85b3, 0xb966d409, 0xce61e49f,
      0x5edef90e, 0x29d9c998, 0xb0d09822, 0xc7d7a8b4, 0x59b33d17,
      0x2eb40d81, 0xb7bd5c3b, 0xc0ba6cad, 0xedb88320, 0x9abfb3b6,
      0x03b6e20c, 0x74b1d29a, 0xead54739, 0x9dd277af, 0x04db2615,
      0x73dc1683, 0xe3630b12, 0x94643b84, 0x0d6d6a3e, 0x7a6a5aa8,
      0xe40ecf0b, 0x9309ff9d, 0x0a00ae27, 0x7d079eb1, 0xf00f9344,
      0x8708a3d2, 0x1e01f268, 0x6906c2fe, 0xf762575d, 0x806567cb,
      0x196c3671, 0x6e6b06e7, 0xfed41b76, 0x89d32be0, 0x10da7a5a,
      0x67dd4acc, 0xf9b9df6f, 0x8ebeeff9, 0x17b7be43, 0x60b08ed5,
      0xd6d6a3e8, 0xa1d1937e, 0x38d8c2c4, 0x4fdff252, 0xd1bb67f1,
      0xa6bc5767, 0x3fb506dd, 0x48b2364b, 0xd80d2bda, 0xaf0a1b4c,
      0x36034af6, 0x41047a60, 0xdf60efc3, 0xa867df55, 0x316e8eef,
      0x4669be79, 0xcb61b38c, 0xbc66831a, 0x256fd2a0, 0x5268e236,
      0xcc0c7795, 0xbb0b4703, 0x220216b9, 0x5505262f, 0xc5ba3bbe,
      0xb2bd0b28, 0x2bb45a92, 0x5cb36a04, 0xc2d7ffa7, 0xb5d0cf31,
      0x2cd99e8b, 0x5bdeae1d, 0x9b64c2b0, 0xec63f226, 0x756aa39c,
      0x026d930a, 0x9c0906a9, 0xeb0e363f, 0x72076785, 0x05005713,
      0x95bf4a82, 0xe2b87a14, 0x7bb12bae, 0x0cb61b38, 0x92d28e9b,
      0xe5d5be0d, 0x7cdcefb7, 0x0bdbdf21, 0x86d3d2d4, 0xf1d4e242,
      0x68ddb3f8, 0x1fda836e, 0x81be16cd, 0xf6b9265b, 0x6fb077e1,
      0x18b74777, 0x88085ae6, 0xff0f6a70, 0x66063bca, 0x11010b5c,
      0x8f659eff, 0xf862ae69, 0x616bffd3, 0x166ccf45, 0xa00ae278,
      0xd70dd2ee, 0x4e048354, 0x3903b3c2, 0xa7672661, 0xd06016f7,
      0x4969474d, 0x3e6e77db, 0xaed16a4a, 0xd9d65adc, 0x40df0b66,
      0x37d83bf0, 0xa9bcae53, 0xdebb9ec5, 0x47b2cf7f, 0x30b5ffe9,
      0xbdbdf21c, 0xcabac28a, 0x53b39330, 0x24b4a3a6, 0xbad03605,
      0xcdd70693, 0x54de5729, 0x23d967bf, 0xb3667a2e, 0xc4614ab8,
      0x5d681b02, 0x2a6f2b94, 0xb40bbe37, 0xc30c8ea1, 0x5a05df1b,
      0x2d02ef8d
    };
  const unsigned char *end;

  crc = ~crc;
  for (end = buf + len; buf < end; ++ buf)
    crc = crc32_table[(crc ^ *buf) & 0xff] ^ (crc >> 8);
  return ~crc;
}

/* Return the CRC-32 of the entire file open at DESCRIPTOR.  */

static uint32_t
elf_crc32_file (struct backtrace_state *state, int descriptor,
		backtrace_error_callback error_callback, void *data)
{
  struct stat st;
  struct backtrace_view file_view;
  uint32_t ret;

  if (fstat (descriptor, &st) < 0)
    {
      error_callback (data, "fstat", errno);
      return 0;
    }

  if (!backtrace_get_view (state, descriptor, 0, st.st_size, error_callback,
			   data, &file_view))
    return 0;

  ret = elf_crc32 (0, (const unsigned char *) file_view.data, st.st_size);

  backtrace_release_view (state, &file_view, error_callback, data);

  return ret;
}

/* A dummy callback function used when we can't find a symbol
   table.  */

static void
elf_nosyms (struct backtrace_state *state ATTRIBUTE_UNUSED,
	    uintptr_t addr ATTRIBUTE_UNUSED,
	    backtrace_syminfo_callback callback ATTRIBUTE_UNUSED,
	    backtrace_error_callback error_callback, void *data)
{
  error_callback (data, "no symbol table in ELF executable", -1);
}

/* A callback function used when we can't find any debug info.  */

static int
elf_nodebug (struct backtrace_state *state, uintptr_t pc,
	     backtrace_full_callback callback,
	     backtrace_error_callback error_callback, void *data)
{
  if (state->syminfo_fn != NULL && state->syminfo_fn != elf_nosyms)
    {
      struct backtrace_call_full bdata;

      /* Fetch symbol information so that we can least get the
	 function name.  */

      bdata.full_callback = callback;
      bdata.full_error_callback = error_callback;
      bdata.full_data = data;
      bdata.ret = 0;
      state->syminfo_fn (state, pc, backtrace_syminfo_to_full_callback,
			 backtrace_syminfo_to_full_error_callback, &bdata);
      return bdata.ret;
    }

  error_callback (data, "no debug info in ELF executable", -1);
  return 0;
}

/* Compare struct elf_symbol for qsort.  */

static int
elf_symbol_compare (const void *v1, const void *v2)
{
  const struct elf_symbol *e1 = (const struct elf_symbol *) v1;
  const struct elf_symbol *e2 = (const struct elf_symbol *) v2;

  if (e1->address < e2->address)
    return -1;
  else if (e1->address > e2->address)
    return 1;
  else
    return 0;
}

/* Compare an ADDR against an elf_symbol for bsearch.  We allocate one
   extra entry in the array so that this can look safely at the next
   entry.  */

static int
elf_symbol_search (const void *vkey, const void *ventry)
{
  const uintptr_t *key = (const uintptr_t *) vkey;
  const struct elf_symbol *entry = (const struct elf_symbol *) ventry;
  uintptr_t addr;

  addr = *key;
  if (addr < entry->address)
    return -1;
  else if (addr >= entry->address + entry->size)
    return 1;
  else
    return 0;
}

/* Initialize the symbol table info for elf_syminfo.  */

static int
elf_initialize_syminfo (struct backtrace_state *state,
			uintptr_t base_address,
			const unsigned char *symtab_data, size_t symtab_size,
			const unsigned char *strtab, size_t strtab_size,
			backtrace_error_callback error_callback,
			void *data, struct elf_syminfo_data *sdata,
			struct elf_ppc64_opd_data *opd)
{
  size_t sym_count;
  const b_elf_sym *sym;
  size_t elf_symbol_count;
  size_t elf_symbol_size;
  struct elf_symbol *elf_symbols;
  size_t i;
  unsigned int j;

  sym_count = symtab_size / sizeof (b_elf_sym);

  /* We only care about function symbols.  Count them.  */
  sym = (const b_elf_sym *) symtab_data;
  elf_symbol_count = 0;
  for (i = 0; i < sym_count; ++i, ++sym)
    {
      int info;

      info = sym->st_info & 0xf;
      if ((info == STT_FUNC || info == STT_OBJECT)
	  && sym->st_shndx != SHN_UNDEF)
	++elf_symbol_count;
    }

  elf_symbol_size = elf_symbol_count * sizeof (struct elf_symbol);
  elf_symbols = ((struct elf_symbol *)
		 backtrace_alloc (state, elf_symbol_size, error_callback,
				  data));
  if (elf_symbols == NULL)
    return 0;

  sym = (const b_elf_sym *) symtab_data;
  j = 0;
  for (i = 0; i < sym_count; ++i, ++sym)
    {
      int info;

      info = sym->st_info & 0xf;
      if (info != STT_FUNC && info != STT_OBJECT)
	continue;
      if (sym->st_shndx == SHN_UNDEF)
	continue;
      if (sym->st_name >= strtab_size)
	{
	  error_callback (data, "symbol string index out of range", 0);
	  backtrace_free (state, elf_symbols, elf_symbol_size, error_callback,
			  data);
	  return 0;
	}
      elf_symbols[j].name = (const char *) strtab + sym->st_name;
      /* Special case PowerPC64 ELFv1 symbols in .opd section, if the symbol
	 is a function descriptor, read the actual code address from the
	 descriptor.  */
      if (opd
	  && sym->st_value >= opd->addr
	  && sym->st_value < opd->addr + opd->size)
	elf_symbols[j].address
	  = *(const b_elf_addr *) (opd->data + (sym->st_value - opd->addr));
      else
	elf_symbols[j].address = sym->st_value;
      elf_symbols[j].address += base_address;
      elf_symbols[j].size = sym->st_size;
      ++j;
    }

  backtrace_qsort (elf_symbols, elf_symbol_count, sizeof (struct elf_symbol),
		   elf_symbol_compare);

  sdata->next = NULL;
  sdata->symbols = elf_symbols;
  sdata->count = elf_symbol_count;

  return 1;
}

/* Add EDATA to the list in STATE.  */

static void
elf_add_syminfo_data (struct backtrace_state *state,
		      struct elf_syminfo_data *edata)
{
  if (!state->threaded)
    {
      struct elf_syminfo_data **pp;

      for (pp = (struct elf_syminfo_data **) (void *) &state->syminfo_data;
	   *pp != NULL;
	   pp = &(*pp)->next)
	;
      *pp = edata;
    }
  else
    {
      while (1)
	{
	  struct elf_syminfo_data **pp;

	  pp = (struct elf_syminfo_data **) (void *) &state->syminfo_data;

	  while (1)
	    {
	      struct elf_syminfo_data *p;

	      p = backtrace_atomic_load_pointer (pp);

	      if (p == NULL)
		break;

	      pp = &p->next;
	    }

	  if (__sync_bool_compare_and_swap (pp, NULL, edata))
	    break;
	}
    }
}

/* Return the symbol name and value for an ADDR.  */

static void
elf_syminfo (struct backtrace_state *state, uintptr_t addr,
	     backtrace_syminfo_callback callback,
	     backtrace_error_callback error_callback ATTRIBUTE_UNUSED,
	     void *data)
{
  struct elf_syminfo_data *edata;
  struct elf_symbol *sym = NULL;

  if (!state->threaded)
    {
      for (edata = (struct elf_syminfo_data *) state->syminfo_data;
	   edata != NULL;
	   edata = edata->next)
	{
	  sym = ((struct elf_symbol *)
		 bsearch (&addr, edata->symbols, edata->count,
			  sizeof (struct elf_symbol), elf_symbol_search));
	  if (sym != NULL)
	    break;
	}
    }
  else
    {
      struct elf_syminfo_data **pp;

      pp = (struct elf_syminfo_data **) (void *) &state->syminfo_data;
      while (1)
	{
	  edata = backtrace_atomic_load_pointer (pp);
	  if (edata == NULL)
	    break;

	  sym = ((struct elf_symbol *)
		 bsearch (&addr, edata->symbols, edata->count,
			  sizeof (struct elf_symbol), elf_symbol_search));
	  if (sym != NULL)
	    break;

	  pp = &edata->next;
	}
    }

  if (sym == NULL)
    callback (data, addr, NULL, 0, 0);
  else
    callback (data, addr, sym->name, sym->address, sym->size);
}

/* Return whether FILENAME is a symlink.  */

static int
elf_is_symlink (const char *filename)
{
  struct stat st;

  if (lstat (filename, &st) < 0)
    return 0;
  return S_ISLNK (st.st_mode);
}

/* Return the results of reading the symlink FILENAME in a buffer
   allocated by backtrace_alloc.  Return the length of the buffer in
   *LEN.  */

static char *
elf_readlink (struct backtrace_state *state, const char *filename,
	      backtrace_error_callback error_callback, void *data,
	      size_t *plen)
{
  size_t len;
  char *buf;

  len = 128;
  while (1)
    {
      ssize_t rl;

      buf = backtrace_alloc (state, len, error_callback, data);
      if (buf == NULL)
	return NULL;
      rl = readlink (filename, buf, len);
      if (rl < 0)
	{
	  backtrace_free (state, buf, len, error_callback, data);
	  return NULL;
	}
      if ((size_t) rl < len - 1)
	{
	  buf[rl] = '\0';
	  *plen = len;
	  return buf;
	}
      backtrace_free (state, buf, len, error_callback, data);
      len *= 2;
    }
}

#define SYSTEM_BUILD_ID_DIR "/usr/lib/debug/.build-id/"

/* Open a separate debug info file, using the build ID to find it.
   Returns an open file descriptor, or -1.

   The GDB manual says that the only place gdb looks for a debug file
   when the build ID is known is in /usr/lib/debug/.build-id.  */

static int
elf_open_debugfile_by_buildid (struct backtrace_state *state,
			       const char *buildid_data, size_t buildid_size,
			       backtrace_error_callback error_callback,
			       void *data)
{
  const char * const prefix = SYSTEM_BUILD_ID_DIR;
  const size_t prefix_len = strlen (prefix);
  const char * const suffix = ".debug";
  const size_t suffix_len = strlen (suffix);
  size_t len;
  char *bd_filename;
  char *t;
  size_t i;
  int ret;
  int does_not_exist;

  len = prefix_len + buildid_size * 2 + suffix_len + 2;
  bd_filename = backtrace_alloc (state, len, error_callback, data);
  if (bd_filename == NULL)
    return -1;

  t = bd_filename;
  memcpy (t, prefix, prefix_len);
  t += prefix_len;
  for (i = 0; i < buildid_size; i++)
    {
      unsigned char b;
      unsigned char nib;

      b = (unsigned char) buildid_data[i];
      nib = (b & 0xf0) >> 4;
      *t++ = nib < 10 ? '0' + nib : 'a' + nib - 10;
      nib = b & 0x0f;
      *t++ = nib < 10 ? '0' + nib : 'a' + nib - 10;
      if (i == 0)
	*t++ = '/';
    }
  memcpy (t, suffix, suffix_len);
  t[suffix_len] = '\0';

  ret = backtrace_open (bd_filename, error_callback, data, &does_not_exist);

  backtrace_free (state, bd_filename, len, error_callback, data);

  /* gdb checks that the debuginfo file has the same build ID note.
     That seems kind of pointless to me--why would it have the right
     name but not the right build ID?--so skipping the check.  */

  return ret;
}

/* Try to open a file whose name is PREFIX (length PREFIX_LEN)
   concatenated with PREFIX2 (length PREFIX2_LEN) concatenated with
   DEBUGLINK_NAME.  Returns an open file descriptor, or -1.  */

static int
elf_try_debugfile (struct backtrace_state *state, const char *prefix,
		   size_t prefix_len, const char *prefix2, size_t prefix2_len,
		   const char *debuglink_name,
		   backtrace_error_callback error_callback, void *data)
{
  size_t debuglink_len;
  size_t try_len;
  char *try;
  int does_not_exist;
  int ret;

  debuglink_len = strlen (debuglink_name);
  try_len = prefix_len + prefix2_len + debuglink_len + 1;
  try = backtrace_alloc (state, try_len, error_callback, data);
  if (try == NULL)
    return -1;

  memcpy (try, prefix, prefix_len);
  memcpy (try + prefix_len, prefix2, prefix2_len);
  memcpy (try + prefix_len + prefix2_len, debuglink_name, debuglink_len);
  try[prefix_len + prefix2_len + debuglink_len] = '\0';

  ret = backtrace_open (try, error_callback, data, &does_not_exist);

  backtrace_free (state, try, try_len, error_callback, data);

  return ret;
}

/* Find a separate debug info file, using the debuglink section data
   to find it.  Returns an open file descriptor, or -1.  */

static int
elf_find_debugfile_by_debuglink (struct backtrace_state *state,
				 const char *filename,
				 const char *debuglink_name,
				 backtrace_error_callback error_callback,
				 void *data)
{
  int ret;
  char *alc;
  size_t alc_len;
  const char *slash;
  int ddescriptor;
  const char *prefix;
  size_t prefix_len;

  /* Resolve symlinks in FILENAME.  Since FILENAME is fairly likely to
     be /proc/self/exe, symlinks are common.  We don't try to resolve
     the whole path name, just the base name.  */
  ret = -1;
  alc = NULL;
  alc_len = 0;
  while (elf_is_symlink (filename))
    {
      char *new_buf;
      size_t new_len;

      new_buf = elf_readlink (state, filename, error_callback, data, &new_len);
      if (new_buf == NULL)
	break;

      if (new_buf[0] == '/')
	filename = new_buf;
      else
	{
	  slash = strrchr (filename, '/');
	  if (slash == NULL)
	    filename = new_buf;
	  else
	    {
	      size_t clen;
	      char *c;

	      slash++;
	      clen = slash - filename + strlen (new_buf) + 1;
	      c = backtrace_alloc (state, clen, error_callback, data);
	      if (c == NULL)
		goto done;

	      memcpy (c, filename, slash - filename);
	      memcpy (c + (slash - filename), new_buf, strlen (new_buf));
	      c[slash - filename + strlen (new_buf)] = '\0';
	      backtrace_free (state, new_buf, new_len, error_callback, data);
	      filename = c;
	      new_buf = c;
	      new_len = clen;
	    }
	}

      if (alc != NULL)
	backtrace_free (state, alc, alc_len, error_callback, data);
      alc = new_buf;
      alc_len = new_len;
    }

  /* Look for DEBUGLINK_NAME in the same directory as FILENAME.  */

  slash = strrchr (filename, '/');
  if (slash == NULL)
    {
      prefix = "";
      prefix_len = 0;
    }
  else
    {
      slash++;
      prefix = filename;
      prefix_len = slash - filename;
    }

  ddescriptor = elf_try_debugfile (state, prefix, prefix_len, "", 0,
				   debuglink_name, error_callback, data);
  if (ddescriptor >= 0)
    {
      ret = ddescriptor;
      goto done;
    }

  /* Look for DEBUGLINK_NAME in a .debug subdirectory of FILENAME.  */

  ddescriptor = elf_try_debugfile (state, prefix, prefix_len, ".debug/",
				   strlen (".debug/"), debuglink_name,
				   error_callback, data);
  if (ddescriptor >= 0)
    {
      ret = ddescriptor;
      goto done;
    }

  /* Look for DEBUGLINK_NAME in /usr/lib/debug.  */

  ddescriptor = elf_try_debugfile (state, "/usr/lib/debug/",
				   strlen ("/usr/lib/debug/"), prefix,
				   prefix_len, debuglink_name,
				   error_callback, data);
  if (ddescriptor >= 0)
    ret = ddescriptor;

 done:
  if (alc != NULL && alc_len > 0)
    backtrace_free (state, alc, alc_len, error_callback, data);
  return ret;
}

/* Open a separate debug info file, using the debuglink section data
   to find it.  Returns an open file descriptor, or -1.  */

static int
elf_open_debugfile_by_debuglink (struct backtrace_state *state,
				 const char *filename,
				 const char *debuglink_name,
				 uint32_t debuglink_crc,
				 backtrace_error_callback error_callback,
				 void *data)
{
  int ddescriptor;

  ddescriptor = elf_find_debugfile_by_debuglink (state, filename,
						 debuglink_name,
						 error_callback, data);
  if (ddescriptor < 0)
    return -1;

  if (debuglink_crc != 0)
    {
      uint32_t got_crc;

      got_crc = elf_crc32_file (state, ddescriptor, error_callback, data);
      if (got_crc != debuglink_crc)
	{
	  backtrace_close (ddescriptor, error_callback, data);
	  return -1;
	}
    }

  return ddescriptor;
}

/* A function useful for setting a breakpoint for an inflation failure
   when this code is compiled with -g.  */

static void
elf_uncompress_failed(void)
{
}

/* *PVAL is the current value being read from the stream, and *PBITS
   is the number of valid bits.  Ensure that *PVAL holds at least 15
   bits by reading additional bits from *PPIN, up to PINEND, as
   needed.  Updates *PPIN, *PVAL and *PBITS.  Returns 1 on success, 0
   on error.  */

static int
elf_zlib_fetch (const unsigned char **ppin, const unsigned char *pinend,
		uint64_t *pval, unsigned int *pbits)
{
  unsigned int bits;
  const unsigned char *pin;
  uint64_t val;
  uint32_t next;

  bits = *pbits;
  if (bits >= 15)
    return 1;
  pin = *ppin;
  val = *pval;

  if (unlikely (pinend - pin < 4))
    {
      elf_uncompress_failed ();
      return 0;
    }

#if defined(__BYTE_ORDER__) && defined(__ORDER_LITTLE_ENDIAN__) \
    && defined(__ORDER_BIG_ENDIAN__) \
    && (__BYTE_ORDER__ == __ORDER_BIG_ENDIAN__ \
        || __BYTE_ORDER__ == __ORDER_LITTLE_ENDIAN__)
  /* We've ensured that PIN is aligned.  */
  next = *(const uint32_t *)pin;

#if __BYTE_ORDER__ == __ORDER_BIG_ENDIAN__
  next = __builtin_bswap32 (next);
#endif
#else
  next = pin[0] | (pin[1] << 8) | (pin[2] << 16) | (pin[3] << 24);
#endif

  val |= (uint64_t)next << bits;
  bits += 32;
  pin += 4;

  /* We will need the next four bytes soon.  */
  __builtin_prefetch (pin, 0, 0);

  *ppin = pin;
  *pval = val;
  *pbits = bits;
  return 1;
}

/* Huffman code tables, like the rest of the zlib format, are defined
   by RFC 1951.  We store a Huffman code table as a series of tables
   stored sequentially in memory.  Each entry in a table is 16 bits.
   The first, main, table has 256 entries.  It is followed by a set of
   secondary tables of length 2 to 128 entries.  The maximum length of
   a code sequence in the deflate format is 15 bits, so that is all we
   need.  Each secondary table has an index, which is the offset of
   the table in the overall memory storage.

   The deflate format says that all codes of a given bit length are
   lexicographically consecutive.  Perhaps we could have 130 values
   that require a 15-bit code, perhaps requiring three secondary
   tables of size 128.  I don't know if this is actually possible, but
   it suggests that the maximum size required for secondary tables is
   3 * 128 + 3 * 64 ... == 768.  The zlib enough program reports 660
   as the maximum.  We permit 768, since in addition to the 256 for
   the primary table, with two bytes per entry, and with the two
   tables we need, that gives us a page.

   A single table entry needs to store a value or (for the main table
   only) the index and size of a secondary table.  Values range from 0
   to 285, inclusive.  Secondary table indexes, per above, range from
   0 to 510.  For a value we need to store the number of bits we need
   to determine that value (one value may appear multiple times in the
   table), which is 1 to 8.  For a secondary table we need to store
   the number of bits used to index into the table, which is 1 to 7.
   And of course we need 1 bit to decide whether we have a value or a
   secondary table index.  So each entry needs 9 bits for value/table
   index, 3 bits for size, 1 bit what it is.  For simplicity we use 16
   bits per entry.  */

/* Number of entries we allocate to for one code table.  We get a page
   for the two code tables we need.  */

#define HUFFMAN_TABLE_SIZE (1024)

/* Bit masks and shifts for the values in the table.  */

#define HUFFMAN_VALUE_MASK 0x01ff
#define HUFFMAN_BITS_SHIFT 9
#define HUFFMAN_BITS_MASK 0x7
#define HUFFMAN_SECONDARY_SHIFT 12

/* For working memory while inflating we need two code tables, we need
   an array of code lengths (max value 15, so we use unsigned char),
   and an array of unsigned shorts used while building a table.  The
   latter two arrays must be large enough to hold the maximum number
   of code lengths, which RFC 1951 defines as 286 + 30.  */

#define ZDEBUG_TABLE_SIZE \
  (2 * HUFFMAN_TABLE_SIZE * sizeof (uint16_t) \
   + (286 + 30) * sizeof (uint16_t)	      \
   + (286 + 30) * sizeof (unsigned char))

#define ZDEBUG_TABLE_CODELEN_OFFSET \
  (2 * HUFFMAN_TABLE_SIZE * sizeof (uint16_t) \
   + (286 + 30) * sizeof (uint16_t))

#define ZDEBUG_TABLE_WORK_OFFSET \
  (2 * HUFFMAN_TABLE_SIZE * sizeof (uint16_t))

#ifdef BACKTRACE_GENERATE_FIXED_HUFFMAN_TABLE

/* Used by the main function that generates the fixed table to learn
   the table size.  */
static size_t final_next_secondary;

#endif

/* Build a Huffman code table from an array of lengths in CODES of
   length CODES_LEN.  The table is stored into *TABLE.  ZDEBUG_TABLE
   is the same as for elf_zlib_inflate, used to find some work space.
   Returns 1 on success, 0 on error.  */

static int
elf_zlib_inflate_table (unsigned char *codes, size_t codes_len,
			uint16_t *zdebug_table, uint16_t *table)
{
  uint16_t count[16];
  uint16_t start[16];
  uint16_t prev[16];
  uint16_t firstcode[7];
  uint16_t *next;
  size_t i;
  size_t j;
  unsigned int code;
  size_t next_secondary;

  /* Count the number of code of each length.  Set NEXT[val] to be the
     next value after VAL with the same bit length.  */

  next = (uint16_t *) (((unsigned char *) zdebug_table)
		       + ZDEBUG_TABLE_WORK_OFFSET);

  memset (&count[0], 0, 16 * sizeof (uint16_t));
  for (i = 0; i < codes_len; ++i)
    {
      if (unlikely (codes[i] >= 16))
	{
	  elf_uncompress_failed ();
	  return 0;
	}

      if (count[codes[i]] == 0)
	{
	  start[codes[i]] = i;
	  prev[codes[i]] = i;
	}
      else
	{
	  next[prev[codes[i]]] = i;
	  prev[codes[i]] = i;
	}

      ++count[codes[i]];
    }

  /* For each length, fill in the table for the codes of that
     length.  */

  memset (table, 0, HUFFMAN_TABLE_SIZE * sizeof (uint16_t));

  /* Handle the values that do not require a secondary table.  */

  code = 0;
  for (j = 1; j <= 8; ++j)
    {
      unsigned int jcnt;
      unsigned int val;

      jcnt = count[j];
      if (jcnt == 0)
	continue;

      if (unlikely (jcnt > (1U << j)))
	{
	  elf_uncompress_failed ();
	  return 0;
	}

      /* There are JCNT values that have this length, the values
	 starting from START[j] continuing through NEXT[VAL].  Those
	 values are assigned consecutive values starting at CODE.  */

      val = start[j];
      for (i = 0; i < jcnt; ++i)
	{
	  uint16_t tval;
	  size_t ind;
	  unsigned int incr;

	  /* In the compressed bit stream, the value VAL is encoded as
	     J bits with the value C.  */

	  if (unlikely ((val & ~HUFFMAN_VALUE_MASK) != 0))
	    {
	      elf_uncompress_failed ();
	      return 0;
	    }

	  tval = val | ((j - 1) << HUFFMAN_BITS_SHIFT);

	  /* The table lookup uses 8 bits.  If J is less than 8, we
	     don't know what the other bits will be.  We need to fill
	     in all possibilities in the table.  Since the Huffman
	     code is unambiguous, those entries can't be used for any
	     other code.  */

	  for (ind = code; ind < 0x100; ind += 1 << j)
	    {
	      if (unlikely (table[ind] != 0))
		{
		  elf_uncompress_failed ();
		  return 0;
		}
	      table[ind] = tval;
	    }

	  /* Advance to the next value with this length.  */
	  if (i + 1 < jcnt)
	    val = next[val];

	  /* The Huffman codes are stored in the bitstream with the
	     most significant bit first, as is required to make them
	     unambiguous.  The effect is that when we read them from
	     the bitstream we see the bit sequence in reverse order:
	     the most significant bit of the Huffman code is the least
	     significant bit of the value we read from the bitstream.
	     That means that to make our table lookups work, we need
	     to reverse the bits of CODE.  Since reversing bits is
	     tedious and in general requires using a table, we instead
	     increment CODE in reverse order.  That is, if the number
	     of bits we are currently using, here named J, is 3, we
	     count as 000, 100, 010, 110, 001, 101, 011, 111, which is
	     to say the numbers from 0 to 7 but with the bits
	     reversed.  Going to more bits, aka incrementing J,
	     effectively just adds more zero bits as the beginning,
	     and as such does not change the numeric value of CODE.

	     To increment CODE of length J in reverse order, find the
	     most significant zero bit and set it to one while
	     clearing all higher bits.  In other words, add 1 modulo
	     2^J, only reversed.  */

	  incr = 1U << (j - 1);
	  while ((code & incr) != 0)
	    incr >>= 1;
	  if (incr == 0)
	    code = 0;
	  else
	    {
	      code &= incr - 1;
	      code += incr;
	    }
	}
    }

  /* Handle the values that require a secondary table.  */

  /* Set FIRSTCODE, the number at which the codes start, for each
     length.  */

  for (j = 9; j < 16; j++)
    {
      unsigned int jcnt;
      unsigned int k;

      jcnt = count[j];
      if (jcnt == 0)
	continue;

      /* There are JCNT values that have this length, the values
	 starting from START[j].  Those values are assigned
	 consecutive values starting at CODE.  */

      firstcode[j - 9] = code;

      /* Reverse add JCNT to CODE modulo 2^J.  */
      for (k = 0; k < j; ++k)
	{
	  if ((jcnt & (1U << k)) != 0)
	    {
	      unsigned int m;
	      unsigned int bit;

	      bit = 1U << (j - k - 1);
	      for (m = 0; m < j - k; ++m, bit >>= 1)
		{
		  if ((code & bit) == 0)
		    {
		      code += bit;
		      break;
		    }
		  code &= ~bit;
		}
	      jcnt &= ~(1U << k);
	    }
	}
      if (unlikely (jcnt != 0))
	{
	  elf_uncompress_failed ();
	  return 0;
	}
    }

  /* For J from 9 to 15, inclusive, we store COUNT[J] consecutive
     values starting at START[J] with consecutive codes starting at
     FIRSTCODE[J - 9].  In the primary table we need to point to the
     secondary table, and the secondary table will be indexed by J - 9
     bits.  We count down from 15 so that we install the larger
     secondary tables first, as the smaller ones may be embedded in
     the larger ones.  */

  next_secondary = 0; /* Index of next secondary table (after primary).  */
  for (j = 15; j >= 9; j--)
    {
      unsigned int jcnt;
      unsigned int val;
      size_t primary; /* Current primary index.  */
      size_t secondary; /* Offset to current secondary table.  */
      size_t secondary_bits; /* Bit size of current secondary table.  */

      jcnt = count[j];
      if (jcnt == 0)
	continue;

      val = start[j];
      code = firstcode[j - 9];
      primary = 0x100;
      secondary = 0;
      secondary_bits = 0;
      for (i = 0; i < jcnt; ++i)
	{
	  uint16_t tval;
	  size_t ind;
	  unsigned int incr;

	  if ((code & 0xff) != primary)
	    {
	      uint16_t tprimary;

	      /* Fill in a new primary table entry.  */

	      primary = code & 0xff;

	      tprimary = table[primary];
	      if (tprimary == 0)
		{
		  /* Start a new secondary table.  */

		  if (unlikely ((next_secondary & HUFFMAN_VALUE_MASK)
				!= next_secondary))
		    {
		      elf_uncompress_failed ();
		      return 0;
		    }

		  secondary = next_secondary;
		  secondary_bits = j - 8;
		  next_secondary += 1 << secondary_bits;
		  table[primary] = (secondary
				    + ((j - 8) << HUFFMAN_BITS_SHIFT)
				    + (1U << HUFFMAN_SECONDARY_SHIFT));
		}
	      else
		{
		  /* There is an existing entry.  It had better be a
		     secondary table with enough bits.  */
		  if (unlikely ((tprimary & (1U << HUFFMAN_SECONDARY_SHIFT))
				== 0))
		    {
		      elf_uncompress_failed ();
		      return 0;
		    }
		  secondary = tprimary & HUFFMAN_VALUE_MASK;
		  secondary_bits = ((tprimary >> HUFFMAN_BITS_SHIFT)
				    & HUFFMAN_BITS_MASK);
		  if (unlikely (secondary_bits < j - 8))
		    {
		      elf_uncompress_failed ();
		      return 0;
		    }
		}
	    }

	  /* Fill in secondary table entries.  */

	  tval = val | ((j - 8) << HUFFMAN_BITS_SHIFT);

	  for (ind = code >> 8;
	       ind < (1U << secondary_bits);
	       ind += 1U << (j - 8))
	    {
	      if (unlikely (table[secondary + 0x100 + ind] != 0))
		{
		  elf_uncompress_failed ();
		  return 0;
		}
	      table[secondary + 0x100 + ind] = tval;
	    }

	  if (i + 1 < jcnt)
	    val = next[val];

	  incr = 1U << (j - 1);
	  while ((code & incr) != 0)
	    incr >>= 1;
	  if (incr == 0)
	    code = 0;
	  else
	    {
	      code &= incr - 1;
	      code += incr;
	    }
	}
    }

#ifdef BACKTRACE_GENERATE_FIXED_HUFFMAN_TABLE
  final_next_secondary = next_secondary;
#endif

  return 1;
}

#ifdef BACKTRACE_GENERATE_FIXED_HUFFMAN_TABLE

/* Used to generate the fixed Huffman table for block type 1.  */

#include <stdio.h>

static uint16_t table[ZDEBUG_TABLE_SIZE];
static unsigned char codes[288];

int
main ()
{
  size_t i;

  for (i = 0; i <= 143; ++i)
    codes[i] = 8;
  for (i = 144; i <= 255; ++i)
    codes[i] = 9;
  for (i = 256; i <= 279; ++i)
    codes[i] = 7;
  for (i = 280; i <= 287; ++i)
    codes[i] = 8;
  if (!elf_zlib_inflate_table (&codes[0], 288, &table[0], &table[0]))
    {
      fprintf (stderr, "elf_zlib_inflate_table failed\n");
      exit (EXIT_FAILURE);
    }

  printf ("static const uint16_t elf_zlib_default_table[%#zx] =\n",
	  final_next_secondary + 0x100);
  printf ("{\n");
  for (i = 0; i < final_next_secondary + 0x100; i += 8)
    {
      size_t j;

      printf (" ");
      for (j = i; j < final_next_secondary + 0x100 && j < i + 8; ++j)
	printf (" %#x,", table[j]);
      printf ("\n");
    }
  printf ("};\n");
  printf ("\n");

  for (i = 0; i < 32; ++i)
    codes[i] = 5;
  if (!elf_zlib_inflate_table (&codes[0], 32, &table[0], &table[0]))
    {
      fprintf (stderr, "elf_zlib_inflate_table failed\n");
      exit (EXIT_FAILURE);
    }

  printf ("static const uint16_t elf_zlib_default_dist_table[%#zx] =\n",
	  final_next_secondary + 0x100);
  printf ("{\n");
  for (i = 0; i < final_next_secondary + 0x100; i += 8)
    {
      size_t j;

      printf (" ");
      for (j = i; j < final_next_secondary + 0x100 && j < i + 8; ++j)
	printf (" %#x,", table[j]);
      printf ("\n");
    }
  printf ("};\n");

  return 0;
}

#endif

/* The fixed tables generated by the #ifdef'ed out main function
   above.  */

static const uint16_t elf_zlib_default_table[0x170] =
{
  0xd00, 0xe50, 0xe10, 0xf18, 0xd10, 0xe70, 0xe30, 0x1230,
  0xd08, 0xe60, 0xe20, 0x1210, 0xe00, 0xe80, 0xe40, 0x1250,
  0xd04, 0xe58, 0xe18, 0x1200, 0xd14, 0xe78, 0xe38, 0x1240,
  0xd0c, 0xe68, 0xe28, 0x1220, 0xe08, 0xe88, 0xe48, 0x1260,
  0xd02, 0xe54, 0xe14, 0xf1c, 0xd12, 0xe74, 0xe34, 0x1238,
  0xd0a, 0xe64, 0xe24, 0x1218, 0xe04, 0xe84, 0xe44, 0x1258,
  0xd06, 0xe5c, 0xe1c, 0x1208, 0xd16, 0xe7c, 0xe3c, 0x1248,
  0xd0e, 0xe6c, 0xe2c, 0x1228, 0xe0c, 0xe8c, 0xe4c, 0x1268,
  0xd01, 0xe52, 0xe12, 0xf1a, 0xd11, 0xe72, 0xe32, 0x1234,
  0xd09, 0xe62, 0xe22, 0x1214, 0xe02, 0xe82, 0xe42, 0x1254,
  0xd05, 0xe5a, 0xe1a, 0x1204, 0xd15, 0xe7a, 0xe3a, 0x1244,
  0xd0d, 0xe6a, 0xe2a, 0x1224, 0xe0a, 0xe8a, 0xe4a, 0x1264,
  0xd03, 0xe56, 0xe16, 0xf1e, 0xd13, 0xe76, 0xe36, 0x123c,
  0xd0b, 0xe66, 0xe26, 0x121c, 0xe06, 0xe86, 0xe46, 0x125c,
  0xd07, 0xe5e, 0xe1e, 0x120c, 0xd17, 0xe7e, 0xe3e, 0x124c,
  0xd0f, 0xe6e, 0xe2e, 0x122c, 0xe0e, 0xe8e, 0xe4e, 0x126c,
  0xd00, 0xe51, 0xe11, 0xf19, 0xd10, 0xe71, 0xe31, 0x1232,
  0xd08, 0xe61, 0xe21, 0x1212, 0xe01, 0xe81, 0xe41, 0x1252,
  0xd04, 0xe59, 0xe19, 0x1202, 0xd14, 0xe79, 0xe39, 0x1242,
  0xd0c, 0xe69, 0xe29, 0x1222, 0xe09, 0xe89, 0xe49, 0x1262,
  0xd02, 0xe55, 0xe15, 0xf1d, 0xd12, 0xe75, 0xe35, 0x123a,
  0xd0a, 0xe65, 0xe25, 0x121a, 0xe05, 0xe85, 0xe45, 0x125a,
  0xd06, 0xe5d, 0xe1d, 0x120a, 0xd16, 0xe7d, 0xe3d, 0x124a,
  0xd0e, 0xe6d, 0xe2d, 0x122a, 0xe0d, 0xe8d, 0xe4d, 0x126a,
  0xd01, 0xe53, 0xe13, 0xf1b, 0xd11, 0xe73, 0xe33, 0x1236,
  0xd09, 0xe63, 0xe23, 0x1216, 0xe03, 0xe83, 0xe43, 0x1256,
  0xd05, 0xe5b, 0xe1b, 0x1206, 0xd15, 0xe7b, 0xe3b, 0x1246,
  0xd0d, 0xe6b, 0xe2b, 0x1226, 0xe0b, 0xe8b, 0xe4b, 0x1266,
  0xd03, 0xe57, 0xe17, 0xf1f, 0xd13, 0xe77, 0xe37, 0x123e,
  0xd0b, 0xe67, 0xe27, 0x121e, 0xe07, 0xe87, 0xe47, 0x125e,
  0xd07, 0xe5f, 0xe1f, 0x120e, 0xd17, 0xe7f, 0xe3f, 0x124e,
  0xd0f, 0xe6f, 0xe2f, 0x122e, 0xe0f, 0xe8f, 0xe4f, 0x126e,
  0x290, 0x291, 0x292, 0x293, 0x294, 0x295, 0x296, 0x297,
  0x298, 0x299, 0x29a, 0x29b, 0x29c, 0x29d, 0x29e, 0x29f,
  0x2a0, 0x2a1, 0x2a2, 0x2a3, 0x2a4, 0x2a5, 0x2a6, 0x2a7,
  0x2a8, 0x2a9, 0x2aa, 0x2ab, 0x2ac, 0x2ad, 0x2ae, 0x2af,
  0x2b0, 0x2b1, 0x2b2, 0x2b3, 0x2b4, 0x2b5, 0x2b6, 0x2b7,
  0x2b8, 0x2b9, 0x2ba, 0x2bb, 0x2bc, 0x2bd, 0x2be, 0x2bf,
  0x2c0, 0x2c1, 0x2c2, 0x2c3, 0x2c4, 0x2c5, 0x2c6, 0x2c7,
  0x2c8, 0x2c9, 0x2ca, 0x2cb, 0x2cc, 0x2cd, 0x2ce, 0x2cf,
  0x2d0, 0x2d1, 0x2d2, 0x2d3, 0x2d4, 0x2d5, 0x2d6, 0x2d7,
  0x2d8, 0x2d9, 0x2da, 0x2db, 0x2dc, 0x2dd, 0x2de, 0x2df,
  0x2e0, 0x2e1, 0x2e2, 0x2e3, 0x2e4, 0x2e5, 0x2e6, 0x2e7,
  0x2e8, 0x2e9, 0x2ea, 0x2eb, 0x2ec, 0x2ed, 0x2ee, 0x2ef,
  0x2f0, 0x2f1, 0x2f2, 0x2f3, 0x2f4, 0x2f5, 0x2f6, 0x2f7,
  0x2f8, 0x2f9, 0x2fa, 0x2fb, 0x2fc, 0x2fd, 0x2fe, 0x2ff,
};

static const uint16_t elf_zlib_default_dist_table[0x100] =
{
  0x800, 0x810, 0x808, 0x818, 0x804, 0x814, 0x80c, 0x81c,
  0x802, 0x812, 0x80a, 0x81a, 0x806, 0x816, 0x80e, 0x81e,
  0x801, 0x811, 0x809, 0x819, 0x805, 0x815, 0x80d, 0x81d,
  0x803, 0x813, 0x80b, 0x81b, 0x807, 0x817, 0x80f, 0x81f,
  0x800, 0x810, 0x808, 0x818, 0x804, 0x814, 0x80c, 0x81c,
  0x802, 0x812, 0x80a, 0x81a, 0x806, 0x816, 0x80e, 0x81e,
  0x801, 0x811, 0x809, 0x819, 0x805, 0x815, 0x80d, 0x81d,
  0x803, 0x813, 0x80b, 0x81b, 0x807, 0x817, 0x80f, 0x81f,
  0x800, 0x810, 0x808, 0x818, 0x804, 0x814, 0x80c, 0x81c,
  0x802, 0x812, 0x80a, 0x81a, 0x806, 0x816, 0x80e, 0x81e,
  0x801, 0x811, 0x809, 0x819, 0x805, 0x815, 0x80d, 0x81d,
  0x803, 0x813, 0x80b, 0x81b, 0x807, 0x817, 0x80f, 0x81f,
  0x800, 0x810, 0x808, 0x818, 0x804, 0x814, 0x80c, 0x81c,
  0x802, 0x812, 0x80a, 0x81a, 0x806, 0x816, 0x80e, 0x81e,
  0x801, 0x811, 0x809, 0x819, 0x805, 0x815, 0x80d, 0x81d,
  0x803, 0x813, 0x80b, 0x81b, 0x807, 0x817, 0x80f, 0x81f,
  0x800, 0x810, 0x808, 0x818, 0x804, 0x814, 0x80c, 0x81c,
  0x802, 0x812, 0x80a, 0x81a, 0x806, 0x816, 0x80e, 0x81e,
  0x801, 0x811, 0x809, 0x819, 0x805, 0x815, 0x80d, 0x81d,
  0x803, 0x813, 0x80b, 0x81b, 0x807, 0x817, 0x80f, 0x81f,
  0x800, 0x810, 0x808, 0x818, 0x804, 0x814, 0x80c, 0x81c,
  0x802, 0x812, 0x80a, 0x81a, 0x806, 0x816, 0x80e, 0x81e,
  0x801, 0x811, 0x809, 0x819, 0x805, 0x815, 0x80d, 0x81d,
  0x803, 0x813, 0x80b, 0x81b, 0x807, 0x817, 0x80f, 0x81f,
  0x800, 0x810, 0x808, 0x818, 0x804, 0x814, 0x80c, 0x81c,
  0x802, 0x812, 0x80a, 0x81a, 0x806, 0x816, 0x80e, 0x81e,
  0x801, 0x811, 0x809, 0x819, 0x805, 0x815, 0x80d, 0x81d,
  0x803, 0x813, 0x80b, 0x81b, 0x807, 0x817, 0x80f, 0x81f,
  0x800, 0x810, 0x808, 0x818, 0x804, 0x814, 0x80c, 0x81c,
  0x802, 0x812, 0x80a, 0x81a, 0x806, 0x816, 0x80e, 0x81e,
  0x801, 0x811, 0x809, 0x819, 0x805, 0x815, 0x80d, 0x81d,
  0x803, 0x813, 0x80b, 0x81b, 0x807, 0x817, 0x80f, 0x81f,
};

/* Inflate a zlib stream from PIN/SIN to POUT/SOUT.  Return 1 on
   success, 0 on some error parsing the stream.  */

static int
elf_zlib_inflate (const unsigned char *pin, size_t sin, uint16_t *zdebug_table,
		  unsigned char *pout, size_t sout)
{
  unsigned char *porigout;
  const unsigned char *pinend;
  unsigned char *poutend;

  /* We can apparently see multiple zlib streams concatenated
     together, so keep going as long as there is something to read.
     The last 4 bytes are the checksum.  */
  porigout = pout;
  pinend = pin + sin;
  poutend = pout + sout;
  while ((pinend - pin) > 4)
    {
      uint64_t val;
      unsigned int bits;
      int last;

      /* Read the two byte zlib header.  */

      if (unlikely ((pin[0] & 0xf) != 8)) /* 8 is zlib encoding.  */
	{
	  /* Unknown compression method.  */
	  elf_uncompress_failed ();
	  return 0;
	}
      if (unlikely ((pin[0] >> 4) > 7))
	{
	  /* Window size too large.  Other than this check, we don't
	     care about the window size.  */
	  elf_uncompress_failed ();
	  return 0;
	}
      if (unlikely ((pin[1] & 0x20) != 0))
	{
	  /* Stream expects a predefined dictionary, but we have no
	     dictionary.  */
	  elf_uncompress_failed ();
	  return 0;
	}
      val = (pin[0] << 8) | pin[1];
      if (unlikely (val % 31 != 0))
	{
	  /* Header check failure.  */
	  elf_uncompress_failed ();
	  return 0;
	}
      pin += 2;

      /* Align PIN to a 32-bit boundary.  */

      val = 0;
      bits = 0;
      while ((((uintptr_t) pin) & 3) != 0)
	{
	  val |= (uint64_t)*pin << bits;
	  bits += 8;
	  ++pin;
	}

      /* Read blocks until one is marked last.  */

      last = 0;

      while (!last)
	{
	  unsigned int type;
	  const uint16_t *tlit;
	  const uint16_t *tdist;

	  if (!elf_zlib_fetch (&pin, pinend, &val, &bits))
	    return 0;

	  last = val & 1;
	  type = (val >> 1) & 3;
	  val >>= 3;
	  bits -= 3;

	  if (unlikely (type == 3))
	    {
	      /* Invalid block type.  */
	      elf_uncompress_failed ();
	      return 0;
	    }

	  if (type == 0)
	    {
	      uint16_t len;
	      uint16_t lenc;

	      /* An uncompressed block.  */

	      /* If we've read ahead more than a byte, back up.  */
	      while (bits > 8)
		{
		  --pin;
		  bits -= 8;
		}

	      val = 0;
	      bits = 0;
	      if (unlikely ((pinend - pin) < 4))
		{
		  /* Missing length.  */
		  elf_uncompress_failed ();
		  return 0;
		}
	      len = pin[0] | (pin[1] << 8);
	      lenc = pin[2] | (pin[3] << 8);
	      pin += 4;
	      lenc = ~lenc;
	      if (unlikely (len != lenc))
		{
		  /* Corrupt data.  */
		  elf_uncompress_failed ();
		  return 0;
		}
	      if (unlikely (len > (unsigned int) (pinend - pin)
			    || len > (unsigned int) (poutend - pout)))
		{
		  /* Not enough space in buffers.  */
		  elf_uncompress_failed ();
		  return 0;
		}
	      memcpy (pout, pin, len);
	      pout += len;
	      pin += len;

	      /* Align PIN.  */
	      while ((((uintptr_t) pin) & 3) != 0)
		{
		  val |= (uint64_t)*pin << bits;
		  bits += 8;
		  ++pin;
		}

	      /* Go around to read the next block.  */
	      continue;
	    }

	  if (type == 1)
	    {
	      tlit = elf_zlib_default_table;
	      tdist = elf_zlib_default_dist_table;
	    }
	  else
	    {
	      unsigned int nlit;
	      unsigned int ndist;
	      unsigned int nclen;
	      unsigned char codebits[19];
	      unsigned char *plenbase;
	      unsigned char *plen;
	      unsigned char *plenend;

	      /* Read a Huffman encoding table.  The various magic
		 numbers here are from RFC 1951.  */

	      if (!elf_zlib_fetch (&pin, pinend, &val, &bits))
		return 0;

	      nlit = (val & 0x1f) + 257;
	      val >>= 5;
	      ndist = (val & 0x1f) + 1;
	      val >>= 5;
	      nclen = (val & 0xf) + 4;
	      val >>= 4;
	      bits -= 14;
	      if (unlikely (nlit > 286 || ndist > 30))
		{
		  /* Values out of range.  */
		  elf_uncompress_failed ();
		  return 0;
		}

	      /* Read and build the table used to compress the
		 literal, length, and distance codes.  */

	      memset(&codebits[0], 0, 19);

	      /* There are always at least 4 elements in the
		 table.  */

	      if (!elf_zlib_fetch (&pin, pinend, &val, &bits))
		return 0;

	      codebits[16] = val & 7;
	      codebits[17] = (val >> 3) & 7;
	      codebits[18] = (val >> 6) & 7;
	      codebits[0] = (val >> 9) & 7;
	      val >>= 12;
	      bits -= 12;

	      if (nclen == 4)
		goto codebitsdone;

	      codebits[8] = val & 7;
	      val >>= 3;
	      bits -= 3;

	      if (nclen == 5)
		goto codebitsdone;

	      if (!elf_zlib_fetch (&pin, pinend, &val, &bits))
		return 0;

	      codebits[7] = val & 7;
	      val >>= 3;
	      bits -= 3;

	      if (nclen == 6)
		goto codebitsdone;

	      codebits[9] = val & 7;
	      val >>= 3;
	      bits -= 3;

	      if (nclen == 7)
		goto codebitsdone;

	      codebits[6] = val & 7;
	      val >>= 3;
	      bits -= 3;

	      if (nclen == 8)
		goto codebitsdone;

	      codebits[10] = val & 7;
	      val >>= 3;
	      bits -= 3;

	      if (nclen == 9)
		goto codebitsdone;

	      codebits[5] = val & 7;
	      val >>= 3;
	      bits -= 3;

	      if (nclen == 10)
		goto codebitsdone;

	      if (!elf_zlib_fetch (&pin, pinend, &val, &bits))
		return 0;

	      codebits[11] = val & 7;
	      val >>= 3;
	      bits -= 3;

	      if (nclen == 11)
		goto codebitsdone;

	      codebits[4] = val & 7;
	      val >>= 3;
	      bits -= 3;

	      if (nclen == 12)
		goto codebitsdone;

	      codebits[12] = val & 7;
	      val >>= 3;
	      bits -= 3;

	      if (nclen == 13)
		goto codebitsdone;

	      codebits[3] = val & 7;
	      val >>= 3;
	      bits -= 3;

	      if (nclen == 14)
		goto codebitsdone;

	      codebits[13] = val & 7;
	      val >>= 3;
	      bits -= 3;

	      if (nclen == 15)
		goto codebitsdone;

	      if (!elf_zlib_fetch (&pin, pinend, &val, &bits))
		return 0;

	      codebits[2] = val & 7;
	      val >>= 3;
	      bits -= 3;

	      if (nclen == 16)
		goto codebitsdone;

	      codebits[14] = val & 7;
	      val >>= 3;
	      bits -= 3;

	      if (nclen == 17)
		goto codebitsdone;

	      codebits[1] = val & 7;
	      val >>= 3;
	      bits -= 3;

	      if (nclen == 18)
		goto codebitsdone;

	      codebits[15] = val & 7;
	      val >>= 3;
	      bits -= 3;

	    codebitsdone:

	      if (!elf_zlib_inflate_table (codebits, 19, zdebug_table,
					   zdebug_table))
		return 0;

	      /* Read the compressed bit lengths of the literal,
		 length, and distance codes.  We have allocated space
		 at the end of zdebug_table to hold them.  */

	      plenbase = (((unsigned char *) zdebug_table)
			  + ZDEBUG_TABLE_CODELEN_OFFSET);
	      plen = plenbase;
	      plenend = plen + nlit + ndist;
	      while (plen < plenend)
		{
		  uint16_t t;
		  unsigned int b;
		  uint16_t v;

		  if (!elf_zlib_fetch (&pin, pinend, &val, &bits))
		    return 0;

		  t = zdebug_table[val & 0xff];

		  /* The compression here uses bit lengths up to 7, so
		     a secondary table is never necessary.  */
		  if (unlikely ((t & (1U << HUFFMAN_SECONDARY_SHIFT)) != 0))
		    {
		      elf_uncompress_failed ();
		      return 0;
		    }

		  b = (t >> HUFFMAN_BITS_SHIFT) & HUFFMAN_BITS_MASK;
		  val >>= b + 1;
		  bits -= b + 1;

		  v = t & HUFFMAN_VALUE_MASK;
		  if (v < 16)
		    *plen++ = v;
		  else if (v == 16)
		    {
		      unsigned int c;
		      unsigned int prev;

		      /* Copy previous entry 3 to 6 times.  */

		      if (unlikely (plen == plenbase))
			{
			  elf_uncompress_failed ();
			  return 0;
			}

		      /* We used up to 7 bits since the last
			 elf_zlib_fetch, so we have at least 8 bits
			 available here.  */

		      c = 3 + (val & 0x3);
		      val >>= 2;
		      bits -= 2;
		      if (unlikely ((unsigned int) (plenend - plen) < c))
			{
			  elf_uncompress_failed ();
			  return 0;
			}

		      prev = plen[-1];
		      switch (c)
			{
			case 6:
			  *plen++ = prev;
			  ATTRIBUTE_FALLTHROUGH;
			case 5:
			  *plen++ = prev;
			  ATTRIBUTE_FALLTHROUGH;
			case 4:
			  *plen++ = prev;
			}
		      *plen++ = prev;
		      *plen++ = prev;
		      *plen++ = prev;
		    }
		  else if (v == 17)
		    {
		      unsigned int c;

		      /* Store zero 3 to 10 times.  */

		      /* We used up to 7 bits since the last
			 elf_zlib_fetch, so we have at least 8 bits
			 available here.  */

		      c = 3 + (val & 0x7);
		      val >>= 3;
		      bits -= 3;
		      if (unlikely ((unsigned int) (plenend - plen) < c))
			{
			  elf_uncompress_failed ();
			  return 0;
			}

		      switch (c)
			{
			case 10:
			  *plen++ = 0;
			  ATTRIBUTE_FALLTHROUGH;
			case 9:
			  *plen++ = 0;
			  ATTRIBUTE_FALLTHROUGH;
			case 8:
			  *plen++ = 0;
			  ATTRIBUTE_FALLTHROUGH;
			case 7:
			  *plen++ = 0;
			  ATTRIBUTE_FALLTHROUGH;
			case 6:
			  *plen++ = 0;
			  ATTRIBUTE_FALLTHROUGH;
			case 5:
			  *plen++ = 0;
			  ATTRIBUTE_FALLTHROUGH;
			case 4:
			  *plen++ = 0;
			}
		      *plen++ = 0;
		      *plen++ = 0;
		      *plen++ = 0;
		    }
		  else if (v == 18)
		    {
		      unsigned int c;

		      /* Store zero 11 to 138 times.  */

		      /* We used up to 7 bits since the last
			 elf_zlib_fetch, so we have at least 8 bits
			 available here.  */

		      c = 11 + (val & 0x7f);
		      val >>= 7;
		      bits -= 7;
		      if (unlikely ((unsigned int) (plenend - plen) < c))
			{
			  elf_uncompress_failed ();
			  return 0;
			}

		      memset (plen, 0, c);
		      plen += c;
		    }
		  else
		    {
		      elf_uncompress_failed ();
		      return 0;
		    }
		}

	      /* Make sure that the stop code can appear.  */

	      plen = plenbase;
	      if (unlikely (plen[256] == 0))
		{
		  elf_uncompress_failed ();
		  return 0;
		}

	      /* Build the decompression tables.  */

	      if (!elf_zlib_inflate_table (plen, nlit, zdebug_table,
					   zdebug_table))
		return 0;
	      if (!elf_zlib_inflate_table (plen + nlit, ndist, zdebug_table,
					   zdebug_table + HUFFMAN_TABLE_SIZE))
		return 0;
	      tlit = zdebug_table;
	      tdist = zdebug_table + HUFFMAN_TABLE_SIZE;
	    }

	  /* Inflate values until the end of the block.  This is the
	     main loop of the inflation code.  */

	  while (1)
	    {
	      uint16_t t;
	      unsigned int b;
	      uint16_t v;
	      unsigned int lit;

	      if (!elf_zlib_fetch (&pin, pinend, &val, &bits))
		return 0;

	      t = tlit[val & 0xff];
	      b = (t >> HUFFMAN_BITS_SHIFT) & HUFFMAN_BITS_MASK;
	      v = t & HUFFMAN_VALUE_MASK;

	      if ((t & (1U << HUFFMAN_SECONDARY_SHIFT)) == 0)
		{
		  lit = v;
		  val >>= b + 1;
		  bits -= b + 1;
		}
	      else
		{
		  t = tlit[v + 0x100 + ((val >> 8) & ((1U << b) - 1))];
		  b = (t >> HUFFMAN_BITS_SHIFT) & HUFFMAN_BITS_MASK;
		  lit = t & HUFFMAN_VALUE_MASK;
		  val >>= b + 8;
		  bits -= b + 8;
		}

	      if (lit < 256)
		{
		  if (unlikely (pout == poutend))
		    {
		      elf_uncompress_failed ();
		      return 0;
		    }

		  *pout++ = lit;

		  /* We will need to write the next byte soon.  We ask
		     for high temporal locality because we will write
		     to the whole cache line soon.  */
		  __builtin_prefetch (pout, 1, 3);
		}
	      else if (lit == 256)
		{
		  /* The end of the block.  */
		  break;
		}
	      else
		{
		  unsigned int dist;
		  unsigned int len;

		  /* Convert lit into a length.  */

		  if (lit < 265)
		    len = lit - 257 + 3;
		  else if (lit == 285)
		    len = 258;
		  else if (unlikely (lit > 285))
		    {
		      elf_uncompress_failed ();
		      return 0;
		    }
		  else
		    {
		      unsigned int extra;

		      if (!elf_zlib_fetch (&pin, pinend, &val, &bits))
			return 0;

		      /* This is an expression for the table of length
			 codes in RFC 1951 3.2.5.  */
		      lit -= 265;
		      extra = (lit >> 2) + 1;
		      len = (lit & 3) << extra;
		      len += 11;
		      len += ((1U << (extra - 1)) - 1) << 3;
		      len += val & ((1U << extra) - 1);
		      val >>= extra;
		      bits -= extra;
		    }

		  if (!elf_zlib_fetch (&pin, pinend, &val, &bits))
		    return 0;

		  t = tdist[val & 0xff];
		  b = (t >> HUFFMAN_BITS_SHIFT) & HUFFMAN_BITS_MASK;
		  v = t & HUFFMAN_VALUE_MASK;

		  if ((t & (1U << HUFFMAN_SECONDARY_SHIFT)) == 0)
		    {
		      dist = v;
		      val >>= b + 1;
		      bits -= b + 1;
		    }
		  else
		    {
		      t = tdist[v + 0x100 + ((val >> 8) & ((1U << b) - 1))];
		      b = (t >> HUFFMAN_BITS_SHIFT) & HUFFMAN_BITS_MASK;
		      dist = t & HUFFMAN_VALUE_MASK;
		      val >>= b + 8;
		      bits -= b + 8;
		    }

		  /* Convert dist to a distance.  */

		  if (dist == 0)
		    {
		      /* A distance of 1.  A common case, meaning
			 repeat the last character LEN times.  */

		      if (unlikely (pout == porigout))
			{
			  elf_uncompress_failed ();
			  return 0;
			}

		      if (unlikely ((unsigned int) (poutend - pout) < len))
			{
			  elf_uncompress_failed ();
			  return 0;
			}

		      memset (pout, pout[-1], len);
		      pout += len;
		    }
		  else if (unlikely (dist > 29))
		    {
		      elf_uncompress_failed ();
		      return 0;
		    }
		  else
		    {
		      if (dist < 4)
			dist = dist + 1;
		      else
			{
			  unsigned int extra;

			  if (!elf_zlib_fetch (&pin, pinend, &val, &bits))
			    return 0;

			  /* This is an expression for the table of
			     distance codes in RFC 1951 3.2.5.  */
			  dist -= 4;
			  extra = (dist >> 1) + 1;
			  dist = (dist & 1) << extra;
			  dist += 5;
			  dist += ((1U << (extra - 1)) - 1) << 2;
			  dist += val & ((1U << extra) - 1);
			  val >>= extra;
			  bits -= extra;
			}

		      /* Go back dist bytes, and copy len bytes from
			 there.  */

		      if (unlikely ((unsigned int) (pout - porigout) < dist))
			{
			  elf_uncompress_failed ();
			  return 0;
			}

		      if (unlikely ((unsigned int) (poutend - pout) < len))
			{
			  elf_uncompress_failed ();
			  return 0;
			}

		      if (dist >= len)
			{
			  memcpy (pout, pout - dist, len);
			  pout += len;
			}
		      else
			{
			  while (len > 0)
			    {
			      unsigned int copy;

			      copy = len < dist ? len : dist;
			      memcpy (pout, pout - dist, copy);
			      len -= copy;
			      pout += copy;
			    }
			}
		    }
		}
	    }
	}
    }

  /* We should have filled the output buffer.  */
  if (unlikely (pout != poutend))
    {
      elf_uncompress_failed ();
      return 0;
    }

  return 1;
}

/* Verify the zlib checksum.  The checksum is in the 4 bytes at
   CHECKBYTES, and the uncompressed data is at UNCOMPRESSED /
   UNCOMPRESSED_SIZE.  Returns 1 on success, 0 on failure.  */

static int
elf_zlib_verify_checksum (const unsigned char *checkbytes,
			  const unsigned char *uncompressed,
			  size_t uncompressed_size)
{
  unsigned int i;
  unsigned int cksum;
  const unsigned char *p;
  uint32_t s1;
  uint32_t s2;
  size_t hsz;

  cksum = 0;
  for (i = 0; i < 4; i++)
    cksum = (cksum << 8) | checkbytes[i];

  s1 = 1;
  s2 = 0;

  /* Minimize modulo operations.  */

  p = uncompressed;
  hsz = uncompressed_size;
  while (hsz >= 5552)
    {
      for (i = 0; i < 5552; i += 16)
	{
	  /* Manually unroll loop 16 times.  */
	  s1 = s1 + *p++;
	  s2 = s2 + s1;
	  s1 = s1 + *p++;
	  s2 = s2 + s1;
	  s1 = s1 + *p++;
	  s2 = s2 + s1;
	  s1 = s1 + *p++;
	  s2 = s2 + s1;
	  s1 = s1 + *p++;
	  s2 = s2 + s1;
	  s1 = s1 + *p++;
	  s2 = s2 + s1;
	  s1 = s1 + *p++;
	  s2 = s2 + s1;
	  s1 = s1 + *p++;
	  s2 = s2 + s1;
	  s1 = s1 + *p++;
	  s2 = s2 + s1;
	  s1 = s1 + *p++;
	  s2 = s2 + s1;
	  s1 = s1 + *p++;
	  s2 = s2 + s1;
	  s1 = s1 + *p++;
	  s2 = s2 + s1;
	  s1 = s1 + *p++;
	  s2 = s2 + s1;
	  s1 = s1 + *p++;
	  s2 = s2 + s1;
	  s1 = s1 + *p++;
	  s2 = s2 + s1;
	  s1 = s1 + *p++;
	  s2 = s2 + s1;
	}
      hsz -= 5552;
      s1 %= 65521;
      s2 %= 65521;
    }

  while (hsz >= 16)
    {
      /* Manually unroll loop 16 times.  */
      s1 = s1 + *p++;
      s2 = s2 + s1;
      s1 = s1 + *p++;
      s2 = s2 + s1;
      s1 = s1 + *p++;
      s2 = s2 + s1;
      s1 = s1 + *p++;
      s2 = s2 + s1;
      s1 = s1 + *p++;
      s2 = s2 + s1;
      s1 = s1 + *p++;
      s2 = s2 + s1;
      s1 = s1 + *p++;
      s2 = s2 + s1;
      s1 = s1 + *p++;
      s2 = s2 + s1;
      s1 = s1 + *p++;
      s2 = s2 + s1;
      s1 = s1 + *p++;
      s2 = s2 + s1;
      s1 = s1 + *p++;
      s2 = s2 + s1;
      s1 = s1 + *p++;
      s2 = s2 + s1;
      s1 = s1 + *p++;
      s2 = s2 + s1;
      s1 = s1 + *p++;
      s2 = s2 + s1;
      s1 = s1 + *p++;
      s2 = s2 + s1;
      s1 = s1 + *p++;
      s2 = s2 + s1;

      hsz -= 16;
    }

  for (i = 0; i < hsz; ++i)
    {
      s1 = s1 + *p++;
      s2 = s2 + s1;
    }

  s1 %= 65521;
  s2 %= 65521;

  if (unlikely ((s2 << 16) + s1 != cksum))
    {
      elf_uncompress_failed ();
      return 0;
    }

  return 1;
}

/* Inflate a zlib stream from PIN/SIN to POUT/SOUT, and verify the
   checksum.  Return 1 on success, 0 on error.  */

static int
elf_zlib_inflate_and_verify (const unsigned char *pin, size_t sin,
			     uint16_t *zdebug_table, unsigned char *pout,
			     size_t sout)
{
  if (!elf_zlib_inflate (pin, sin, zdebug_table, pout, sout))
    return 0;
  if (!elf_zlib_verify_checksum (pin + sin - 4, pout, sout))
    return 0;
  return 1;
}

/* Uncompress the old compressed debug format, the one emitted by
   --compress-debug-sections=zlib-gnu.  The compressed data is in
   COMPRESSED / COMPRESSED_SIZE, and the function writes to
   *UNCOMPRESSED / *UNCOMPRESSED_SIZE.  ZDEBUG_TABLE is work space to
   hold Huffman tables.  Returns 0 on error, 1 on successful
   decompression or if something goes wrong.  In general we try to
   carry on, by returning 1, even if we can't decompress.  */

static int
elf_uncompress_zdebug (struct backtrace_state *state,
		       const unsigned char *compressed, size_t compressed_size,
		       uint16_t *zdebug_table,
		       backtrace_error_callback error_callback, void *data,
		       unsigned char **uncompressed, size_t *uncompressed_size)
{
  size_t sz;
  size_t i;
  unsigned char *po;

  *uncompressed = NULL;
  *uncompressed_size = 0;

  /* The format starts with the four bytes ZLIB, followed by the 8
     byte length of the uncompressed data in big-endian order,
     followed by a zlib stream.  */

  if (compressed_size < 12 || memcmp (compressed, "ZLIB", 4) != 0)
    return 1;

  sz = 0;
  for (i = 0; i < 8; i++)
    sz = (sz << 8) | compressed[i + 4];

  if (*uncompressed != NULL && *uncompressed_size >= sz)
    po = *uncompressed;
  else
    {
      po = (unsigned char *) backtrace_alloc (state, sz, error_callback, data);
      if (po == NULL)
	return 0;
    }

  if (!elf_zlib_inflate_and_verify (compressed + 12, compressed_size - 12,
				    zdebug_table, po, sz))
    return 1;

  *uncompressed = po;
  *uncompressed_size = sz;

  return 1;
}

/* Uncompress the new compressed debug format, the official standard
   ELF approach emitted by --compress-debug-sections=zlib-gabi.  The
   compressed data is in COMPRESSED / COMPRESSED_SIZE, and the
   function writes to *UNCOMPRESSED / *UNCOMPRESSED_SIZE.
   ZDEBUG_TABLE is work space as for elf_uncompress_zdebug.  Returns 0
   on error, 1 on successful decompression or if something goes wrong.
   In general we try to carry on, by returning 1, even if we can't
   decompress.  */

static int
elf_uncompress_chdr (struct backtrace_state *state,
		     const unsigned char *compressed, size_t compressed_size,
		     uint16_t *zdebug_table,
		     backtrace_error_callback error_callback, void *data,
		     unsigned char **uncompressed, size_t *uncompressed_size)
{
  const b_elf_chdr *chdr;
  unsigned char *po;

  *uncompressed = NULL;
  *uncompressed_size = 0;

  /* The format starts with an ELF compression header.  */
  if (compressed_size < sizeof (b_elf_chdr))
    return 1;

  chdr = (const b_elf_chdr *) compressed;

  if (chdr->ch_type != ELFCOMPRESS_ZLIB)
    {
      /* Unsupported compression algorithm.  */
      return 1;
    }

  if (*uncompressed != NULL && *uncompressed_size >= chdr->ch_size)
    po = *uncompressed;
  else
    {
      po = (unsigned char *) backtrace_alloc (state, chdr->ch_size,
					      error_callback, data);
      if (po == NULL)
	return 0;
    }

  if (!elf_zlib_inflate_and_verify (compressed + sizeof (b_elf_chdr),
				    compressed_size - sizeof (b_elf_chdr),
				    zdebug_table, po, chdr->ch_size))
    return 1;

  *uncompressed = po;
  *uncompressed_size = chdr->ch_size;

  return 1;
}

/* This function is a hook for testing the zlib support.  It is only
   used by tests.  */

int
backtrace_uncompress_zdebug (struct backtrace_state *state,
			     const unsigned char *compressed,
			     size_t compressed_size,
			     backtrace_error_callback error_callback,
			     void *data, unsigned char **uncompressed,
			     size_t *uncompressed_size)
{
  uint16_t *zdebug_table;
  int ret;

  zdebug_table = ((uint16_t *) backtrace_alloc (state, ZDEBUG_TABLE_SIZE,
						error_callback, data));
  if (zdebug_table == NULL)
    return 0;
  ret = elf_uncompress_zdebug (state, compressed, compressed_size,
			       zdebug_table, error_callback, data,
			       uncompressed, uncompressed_size);
  backtrace_free (state, zdebug_table, ZDEBUG_TABLE_SIZE,
		  error_callback, data);
  return ret;
}

/* Number of LZMA states.  */
#define LZMA_STATES (12)

/* Number of LZMA position states.  The pb value of the property byte
   is the number of bits to include in these states, and the maximum
   value of pb is 4.  */
#define LZMA_POS_STATES (16)

/* Number of LZMA distance states.  These are used match distances
   with a short match length: up to 4 bytes.  */
#define LZMA_DIST_STATES (4)

/* Number of LZMA distance slots.  LZMA uses six bits to encode larger
   match lengths, so 1 << 6 possible probabilities.  */
#define LZMA_DIST_SLOTS (64)

/* LZMA distances 0 to 3 are encoded directly, larger values use a
   probability model.  */
#define LZMA_DIST_MODEL_START (4)

/* The LZMA probability model ends at 14.  */
#define LZMA_DIST_MODEL_END (14)

/* LZMA distance slots for distances less than 127.  */
#define LZMA_FULL_DISTANCES (128)

/* LZMA uses four alignment bits.  */
#define LZMA_ALIGN_SIZE (16)

/* LZMA match length is encoded with 4, 5, or 10 bits, some of which
   are already known.  */
#define LZMA_LEN_LOW_SYMBOLS (8)
#define LZMA_LEN_MID_SYMBOLS (8)
#define LZMA_LEN_HIGH_SYMBOLS (256)

/* LZMA literal encoding.  */
#define LZMA_LITERAL_CODERS_MAX (16)
#define LZMA_LITERAL_CODER_SIZE (0x300)

/* LZMA is based on a large set of probabilities, each managed
   independently.  Each probability is an 11 bit number that we store
   in a uint16_t.  We use a single large array of probabilities.  */

/* Lengths of entries in the LZMA probabilities array.  The names used
   here are copied from the Linux kernel implementation.  */

#define LZMA_PROB_IS_MATCH_LEN (LZMA_STATES * LZMA_POS_STATES)
#define LZMA_PROB_IS_REP_LEN LZMA_STATES
#define LZMA_PROB_IS_REP0_LEN LZMA_STATES
#define LZMA_PROB_IS_REP1_LEN LZMA_STATES
#define LZMA_PROB_IS_REP2_LEN LZMA_STATES
#define LZMA_PROB_IS_REP0_LONG_LEN (LZMA_STATES * LZMA_POS_STATES)
#define LZMA_PROB_DIST_SLOT_LEN (LZMA_DIST_STATES * LZMA_DIST_SLOTS)
#define LZMA_PROB_DIST_SPECIAL_LEN (LZMA_FULL_DISTANCES - LZMA_DIST_MODEL_END)
#define LZMA_PROB_DIST_ALIGN_LEN LZMA_ALIGN_SIZE
#define LZMA_PROB_MATCH_LEN_CHOICE_LEN 1
#define LZMA_PROB_MATCH_LEN_CHOICE2_LEN 1
#define LZMA_PROB_MATCH_LEN_LOW_LEN (LZMA_POS_STATES * LZMA_LEN_LOW_SYMBOLS)
#define LZMA_PROB_MATCH_LEN_MID_LEN (LZMA_POS_STATES * LZMA_LEN_MID_SYMBOLS)
#define LZMA_PROB_MATCH_LEN_HIGH_LEN LZMA_LEN_HIGH_SYMBOLS
#define LZMA_PROB_REP_LEN_CHOICE_LEN 1
#define LZMA_PROB_REP_LEN_CHOICE2_LEN 1
#define LZMA_PROB_REP_LEN_LOW_LEN (LZMA_POS_STATES * LZMA_LEN_LOW_SYMBOLS)
#define LZMA_PROB_REP_LEN_MID_LEN (LZMA_POS_STATES * LZMA_LEN_MID_SYMBOLS)
#define LZMA_PROB_REP_LEN_HIGH_LEN LZMA_LEN_HIGH_SYMBOLS
#define LZMA_PROB_LITERAL_LEN \
  (LZMA_LITERAL_CODERS_MAX * LZMA_LITERAL_CODER_SIZE)

/* Offsets into the LZMA probabilities array.  This is mechanically
   generated from the above lengths.  */

#define LZMA_PROB_IS_MATCH_OFFSET 0
#define LZMA_PROB_IS_REP_OFFSET \
  (LZMA_PROB_IS_MATCH_OFFSET + LZMA_PROB_IS_MATCH_LEN)
#define LZMA_PROB_IS_REP0_OFFSET \
  (LZMA_PROB_IS_REP_OFFSET + LZMA_PROB_IS_REP_LEN)
#define LZMA_PROB_IS_REP1_OFFSET \
  (LZMA_PROB_IS_REP0_OFFSET + LZMA_PROB_IS_REP0_LEN)
#define LZMA_PROB_IS_REP2_OFFSET \
  (LZMA_PROB_IS_REP1_OFFSET + LZMA_PROB_IS_REP1_LEN)
#define LZMA_PROB_IS_REP0_LONG_OFFSET \
  (LZMA_PROB_IS_REP2_OFFSET + LZMA_PROB_IS_REP2_LEN)
#define LZMA_PROB_DIST_SLOT_OFFSET \
  (LZMA_PROB_IS_REP0_LONG_OFFSET + LZMA_PROB_IS_REP0_LONG_LEN)
#define LZMA_PROB_DIST_SPECIAL_OFFSET \
  (LZMA_PROB_DIST_SLOT_OFFSET + LZMA_PROB_DIST_SLOT_LEN)
#define LZMA_PROB_DIST_ALIGN_OFFSET \
  (LZMA_PROB_DIST_SPECIAL_OFFSET + LZMA_PROB_DIST_SPECIAL_LEN)
#define LZMA_PROB_MATCH_LEN_CHOICE_OFFSET \
  (LZMA_PROB_DIST_ALIGN_OFFSET + LZMA_PROB_DIST_ALIGN_LEN)
#define LZMA_PROB_MATCH_LEN_CHOICE2_OFFSET \
  (LZMA_PROB_MATCH_LEN_CHOICE_OFFSET + LZMA_PROB_MATCH_LEN_CHOICE_LEN)
#define LZMA_PROB_MATCH_LEN_LOW_OFFSET \
  (LZMA_PROB_MATCH_LEN_CHOICE2_OFFSET + LZMA_PROB_MATCH_LEN_CHOICE2_LEN)
#define LZMA_PROB_MATCH_LEN_MID_OFFSET \
  (LZMA_PROB_MATCH_LEN_LOW_OFFSET + LZMA_PROB_MATCH_LEN_LOW_LEN)
#define LZMA_PROB_MATCH_LEN_HIGH_OFFSET \
  (LZMA_PROB_MATCH_LEN_MID_OFFSET + LZMA_PROB_MATCH_LEN_MID_LEN)
#define LZMA_PROB_REP_LEN_CHOICE_OFFSET \
  (LZMA_PROB_MATCH_LEN_HIGH_OFFSET + LZMA_PROB_MATCH_LEN_HIGH_LEN)
#define LZMA_PROB_REP_LEN_CHOICE2_OFFSET \
  (LZMA_PROB_REP_LEN_CHOICE_OFFSET + LZMA_PROB_REP_LEN_CHOICE_LEN)
#define LZMA_PROB_REP_LEN_LOW_OFFSET \
  (LZMA_PROB_REP_LEN_CHOICE2_OFFSET + LZMA_PROB_REP_LEN_CHOICE2_LEN)
#define LZMA_PROB_REP_LEN_MID_OFFSET \
  (LZMA_PROB_REP_LEN_LOW_OFFSET + LZMA_PROB_REP_LEN_LOW_LEN)
#define LZMA_PROB_REP_LEN_HIGH_OFFSET \
  (LZMA_PROB_REP_LEN_MID_OFFSET + LZMA_PROB_REP_LEN_MID_LEN)
#define LZMA_PROB_LITERAL_OFFSET \
  (LZMA_PROB_REP_LEN_HIGH_OFFSET + LZMA_PROB_REP_LEN_HIGH_LEN)

#define LZMA_PROB_TOTAL_COUNT \
  (LZMA_PROB_LITERAL_OFFSET + LZMA_PROB_LITERAL_LEN)

/* Check that the number of LZMA probabilities is the same as the
   Linux kernel implementation.  */

#if LZMA_PROB_TOTAL_COUNT != 1846 + (1 << 4) * 0x300
 #error Wrong number of LZMA probabilities
#endif

/* Expressions for the offset in the LZMA probabilities array of a
   specific probability.  */

#define LZMA_IS_MATCH(state, pos) \
  (LZMA_PROB_IS_MATCH_OFFSET + (state) * LZMA_POS_STATES + (pos))
#define LZMA_IS_REP(state) \
  (LZMA_PROB_IS_REP_OFFSET + (state))
#define LZMA_IS_REP0(state) \
  (LZMA_PROB_IS_REP0_OFFSET + (state))
#define LZMA_IS_REP1(state) \
  (LZMA_PROB_IS_REP1_OFFSET + (state))
#define LZMA_IS_REP2(state) \
  (LZMA_PROB_IS_REP2_OFFSET + (state))
#define LZMA_IS_REP0_LONG(state, pos) \
  (LZMA_PROB_IS_REP0_LONG_OFFSET + (state) * LZMA_POS_STATES + (pos))
#define LZMA_DIST_SLOT(dist, slot) \
  (LZMA_PROB_DIST_SLOT_OFFSET + (dist) * LZMA_DIST_SLOTS + (slot))
#define LZMA_DIST_SPECIAL(dist) \
  (LZMA_PROB_DIST_SPECIAL_OFFSET + (dist))
#define LZMA_DIST_ALIGN(dist) \
  (LZMA_PROB_DIST_ALIGN_OFFSET + (dist))
#define LZMA_MATCH_LEN_CHOICE \
  LZMA_PROB_MATCH_LEN_CHOICE_OFFSET
#define LZMA_MATCH_LEN_CHOICE2 \
  LZMA_PROB_MATCH_LEN_CHOICE2_OFFSET
#define LZMA_MATCH_LEN_LOW(pos, sym) \
  (LZMA_PROB_MATCH_LEN_LOW_OFFSET + (pos) * LZMA_LEN_LOW_SYMBOLS + (sym))
#define LZMA_MATCH_LEN_MID(pos, sym) \
  (LZMA_PROB_MATCH_LEN_MID_OFFSET + (pos) * LZMA_LEN_MID_SYMBOLS + (sym))
#define LZMA_MATCH_LEN_HIGH(sym) \
  (LZMA_PROB_MATCH_LEN_HIGH_OFFSET + (sym))
#define LZMA_REP_LEN_CHOICE \
  LZMA_PROB_REP_LEN_CHOICE_OFFSET
#define LZMA_REP_LEN_CHOICE2 \
  LZMA_PROB_REP_LEN_CHOICE2_OFFSET
#define LZMA_REP_LEN_LOW(pos, sym) \
  (LZMA_PROB_REP_LEN_LOW_OFFSET + (pos) * LZMA_LEN_LOW_SYMBOLS + (sym))
#define LZMA_REP_LEN_MID(pos, sym) \
  (LZMA_PROB_REP_LEN_MID_OFFSET + (pos) * LZMA_LEN_MID_SYMBOLS + (sym))
#define LZMA_REP_LEN_HIGH(sym) \
  (LZMA_PROB_REP_LEN_HIGH_OFFSET + (sym))
#define LZMA_LITERAL(code, size) \
  (LZMA_PROB_LITERAL_OFFSET + (code) * LZMA_LITERAL_CODER_SIZE + (size))

/* Read an LZMA varint from BUF, reading and updating *POFFSET,
   setting *VAL.  Returns 0 on error, 1 on success.  */

static int
elf_lzma_varint (const unsigned char *compressed, size_t compressed_size,
		 size_t *poffset, uint64_t *val)
{
  size_t off;
  int i;
  uint64_t v;
  unsigned char b;

  off = *poffset;
  i = 0;
  v = 0;
  while (1)
    {
      if (unlikely (off >= compressed_size))
	{
	  elf_uncompress_failed ();
	  return 0;
	}
      b = compressed[off];
      v |= (b & 0x7f) << (i * 7);
      ++off;
      if ((b & 0x80) == 0)
	{
	  *poffset = off;
	  *val = v;
	  return 1;
	}
      ++i;
      if (unlikely (i >= 9))
	{
	  elf_uncompress_failed ();
	  return 0;
	}
    }
}

/* Normalize the LZMA range decoder, pulling in an extra input byte if
   needed.  */

static void
elf_lzma_range_normalize (const unsigned char *compressed,
			  size_t compressed_size, size_t *poffset,
			  uint32_t *prange, uint32_t *pcode)
{
  if (*prange < (1U << 24))
    {
      if (unlikely (*poffset >= compressed_size))
	{
	  /* We assume this will be caught elsewhere.  */
	  elf_uncompress_failed ();
	  return;
	}
      *prange <<= 8;
      *pcode <<= 8;
      *pcode += compressed[*poffset];
      ++*poffset;
    }
}

/* Read and return a single bit from the LZMA stream, reading and
   updating *PROB.  Each bit comes from the range coder.  */

static int
elf_lzma_bit (const unsigned char *compressed, size_t compressed_size,
	      uint16_t *prob, size_t *poffset, uint32_t *prange,
	      uint32_t *pcode)
{
  uint32_t bound;

  elf_lzma_range_normalize (compressed, compressed_size, poffset,
			    prange, pcode);
  bound = (*prange >> 11) * (uint32_t) *prob;
  if (*pcode < bound)
    {
      *prange = bound;
      *prob += ((1U << 11) - *prob) >> 5;
      return 0;
    }
  else
    {
      *prange -= bound;
      *pcode -= bound;
      *prob -= *prob >> 5;
      return 1;
    }
}

/* Read an integer of size BITS from the LZMA stream, most significant
   bit first.  The bits are predicted using PROBS.  */

static uint32_t
elf_lzma_integer (const unsigned char *compressed, size_t compressed_size,
		  uint16_t *probs, uint32_t bits, size_t *poffset,
		  uint32_t *prange, uint32_t *pcode)
{
  uint32_t sym;
  uint32_t i;

  sym = 1;
  for (i = 0; i < bits; i++)
    {
      int bit;

      bit = elf_lzma_bit (compressed, compressed_size, probs + sym, poffset,
			  prange, pcode);
      sym <<= 1;
      sym += bit;
    }
  return sym - (1 << bits);
}

/* Read an integer of size BITS from the LZMA stream, least
   significant bit first.  The bits are predicted using PROBS.  */

static uint32_t
elf_lzma_reverse_integer (const unsigned char *compressed,
			  size_t compressed_size, uint16_t *probs,
			  uint32_t bits, size_t *poffset, uint32_t *prange,
			  uint32_t *pcode)
{
  uint32_t sym;
  uint32_t val;
  uint32_t i;

  sym = 1;
  val = 0;
  for (i = 0; i < bits; i++)
    {
      int bit;

      bit = elf_lzma_bit (compressed, compressed_size, probs + sym, poffset,
			  prange, pcode);
      sym <<= 1;
      sym += bit;
      val += bit << i;
    }
  return val;
}

/* Read a length from the LZMA stream.  IS_REP picks either LZMA_MATCH
   or LZMA_REP probabilities.  */

static uint32_t
elf_lzma_len (const unsigned char *compressed, size_t compressed_size,
	      uint16_t *probs, int is_rep, unsigned int pos_state,
	      size_t *poffset, uint32_t *prange, uint32_t *pcode)
{
  uint16_t *probs_choice;
  uint16_t *probs_sym;
  uint32_t bits;
  uint32_t len;

  probs_choice = probs + (is_rep
			  ? LZMA_REP_LEN_CHOICE
			  : LZMA_MATCH_LEN_CHOICE);
  if (elf_lzma_bit (compressed, compressed_size, probs_choice, poffset,
		    prange, pcode))
    {
      probs_choice = probs + (is_rep
			      ? LZMA_REP_LEN_CHOICE2
			      : LZMA_MATCH_LEN_CHOICE2);
      if (elf_lzma_bit (compressed, compressed_size, probs_choice,
			poffset, prange, pcode))
	{
	  probs_sym = probs + (is_rep
			       ? LZMA_REP_LEN_HIGH (0)
			       : LZMA_MATCH_LEN_HIGH (0));
	  bits = 8;
	  len = 2 + 8 + 8;
	}
      else
	{
	  probs_sym = probs + (is_rep
			       ? LZMA_REP_LEN_MID (pos_state, 0)
			       : LZMA_MATCH_LEN_MID (pos_state, 0));
	  bits = 3;
	  len = 2 + 8;
	}
    }
  else
    {
      probs_sym = probs + (is_rep
			   ? LZMA_REP_LEN_LOW (pos_state, 0)
			   : LZMA_MATCH_LEN_LOW (pos_state, 0));
      bits = 3;
      len = 2;
    }

  len += elf_lzma_integer (compressed, compressed_size, probs_sym, bits,
			   poffset, prange, pcode);
  return len;
}

/* Uncompress one LZMA block from a minidebug file.  The compressed
   data is at COMPRESSED + *POFFSET.  Update *POFFSET.  Store the data
   into the memory at UNCOMPRESSED, size UNCOMPRESSED_SIZE.  CHECK is
   the stream flag from the xz header.  Return 1 on successful
   decompression.  */

static int
elf_uncompress_lzma_block (const unsigned char *compressed,
			   size_t compressed_size, unsigned char check,
			   uint16_t *probs, unsigned char *uncompressed,
			   size_t uncompressed_size, size_t *poffset)
{
  size_t off;
  size_t block_header_offset;
  size_t block_header_size;
  unsigned char block_flags;
  uint64_t header_compressed_size;
  uint64_t header_uncompressed_size;
  unsigned char lzma2_properties;
  uint32_t computed_crc;
  uint32_t stream_crc;
  size_t uncompressed_offset;
  size_t dict_start_offset;
  unsigned int lc;
  unsigned int lp;
  unsigned int pb;
  uint32_t range;
  uint32_t code;
  uint32_t lstate;
  uint32_t dist[4];

  off = *poffset;
  block_header_offset = off;

  /* Block header size is a single byte.  */
  if (unlikely (off >= compressed_size))
    {
      elf_uncompress_failed ();
      return 0;
    }
  block_header_size = (compressed[off] + 1) * 4;
  if (unlikely (off + block_header_size > compressed_size))
    {
      elf_uncompress_failed ();
      return 0;
    }

  /* Block flags.  */
  block_flags = compressed[off + 1];
  if (unlikely ((block_flags & 0x3c) != 0))
    {
      elf_uncompress_failed ();
      return 0;
    }

  off += 2;

  /* Optional compressed size.  */
  header_compressed_size = 0;
  if ((block_flags & 0x40) != 0)
    {
      *poffset = off;
      if (!elf_lzma_varint (compressed, compressed_size, poffset,
			    &header_compressed_size))
	return 0;
      off = *poffset;
    }

  /* Optional uncompressed size.  */
  header_uncompressed_size = 0;
  if ((block_flags & 0x80) != 0)
    {
      *poffset = off;
      if (!elf_lzma_varint (compressed, compressed_size, poffset,
			    &header_uncompressed_size))
	return 0;
      off = *poffset;
    }

  /* The recipe for creating a minidebug file is to run the xz program
     with no arguments, so we expect exactly one filter: lzma2.  */

  if (unlikely ((block_flags & 0x3) != 0))
    {
      elf_uncompress_failed ();
      return 0;
    }

  if (unlikely (off + 2 >= block_header_offset + block_header_size))
    {
      elf_uncompress_failed ();
      return 0;
    }

  /* The filter ID for LZMA2 is 0x21.  */
  if (unlikely (compressed[off] != 0x21))
    {
      elf_uncompress_failed ();
      return 0;
    }
  ++off;

  /* The size of the filter properties for LZMA2 is 1.  */
  if (unlikely (compressed[off] != 1))
    {
      elf_uncompress_failed ();
      return 0;
    }
  ++off;

  lzma2_properties = compressed[off];
  ++off;

  if (unlikely (lzma2_properties > 40))
    {
      elf_uncompress_failed ();
      return 0;
    }

  /* The properties describe the dictionary size, but we don't care
     what that is.  */

  /* Block header padding.  */
  if (unlikely (off + 4 > compressed_size))
    {
      elf_uncompress_failed ();
      return 0;
    }

  off = (off + 3) &~ (size_t) 3;

  if (unlikely (off + 4 > compressed_size))
    {
      elf_uncompress_failed ();
      return 0;
    }

  /* Block header CRC.  */
  computed_crc = elf_crc32 (0, compressed + block_header_offset,
			    block_header_size - 4);
  stream_crc = (compressed[off]
		| (compressed[off + 1] << 8)
		| (compressed[off + 2] << 16)
		| (compressed[off + 3] << 24));
  if (unlikely (computed_crc != stream_crc))
    {
      elf_uncompress_failed ();
      return 0;
    }
  off += 4;

  /* Read a sequence of LZMA2 packets.  */

  uncompressed_offset = 0;
  dict_start_offset = 0;
  lc = 0;
  lp = 0;
  pb = 0;
  lstate = 0;
  while (off < compressed_size)
    {
      unsigned char control;

      range = 0xffffffff;
      code = 0;

      control = compressed[off];
      ++off;
      if (unlikely (control == 0))
	{
	  /* End of packets.  */
	  break;
	}

      if (control == 1 || control >= 0xe0)
	{
	  /* Reset dictionary to empty.  */
	  dict_start_offset = uncompressed_offset;
	}

      if (control < 0x80)
	{
	  size_t chunk_size;

	  /* The only valid values here are 1 or 2.  A 1 means to
	     reset the dictionary (done above).  Then we see an
	     uncompressed chunk.  */

	  if (unlikely (control > 2))
	    {
	      elf_uncompress_failed ();
	      return 0;
	    }

	  /* An uncompressed chunk is a two byte size followed by
	     data.  */

	  if (unlikely (off + 2 > compressed_size))
	    {
	      elf_uncompress_failed ();
	      return 0;
	    }

	  chunk_size = compressed[off] << 8;
	  chunk_size += compressed[off + 1];
	  ++chunk_size;

	  off += 2;

	  if (unlikely (off + chunk_size > compressed_size))
	    {
	      elf_uncompress_failed ();
	      return 0;
	    }
	  if (unlikely (uncompressed_offset + chunk_size > uncompressed_size))
	    {
	      elf_uncompress_failed ();
	      return 0;
	    }

	  memcpy (uncompressed + uncompressed_offset, compressed + off,
		  chunk_size);
	  uncompressed_offset += chunk_size;
	  off += chunk_size;
	}
      else
	{
	  size_t uncompressed_chunk_start;
	  size_t uncompressed_chunk_size;
	  size_t compressed_chunk_size;
	  size_t limit;

	  /* An LZMA chunk.  This starts with an uncompressed size and
	     a compressed size.  */

	  if (unlikely (off + 4 >= compressed_size))
	    {
	      elf_uncompress_failed ();
	      return 0;
	    }

	  uncompressed_chunk_start = uncompressed_offset;

	  uncompressed_chunk_size = (control & 0x1f) << 16;
	  uncompressed_chunk_size += compressed[off] << 8;
	  uncompressed_chunk_size += compressed[off + 1];
	  ++uncompressed_chunk_size;

	  compressed_chunk_size = compressed[off + 2] << 8;
	  compressed_chunk_size += compressed[off + 3];
	  ++compressed_chunk_size;

	  off += 4;

	  /* Bit 7 (0x80) is set.
	     Bits 6 and 5 (0x40 and 0x20) are as follows:
	     0: don't reset anything
	     1: reset state
	     2: reset state, read properties
	     3: reset state, read properties, reset dictionary (done above) */

	  if (control >= 0xc0)
	    {
	      unsigned char props;

	      /* Bit 6 is set, read properties.  */

	      if (unlikely (off >= compressed_size))
		{
		  elf_uncompress_failed ();
		  return 0;
		}
	      props = compressed[off];
	      ++off;
	      if (unlikely (props > (4 * 5 + 4) * 9 + 8))
		{
		  elf_uncompress_failed ();
		  return 0;
		}
	      pb = 0;
	      while (props >= 9 * 5)
		{
		  props -= 9 * 5;
		  ++pb;
		}
	      lp = 0;
	      while (props > 9)
		{
		  props -= 9;
		  ++lp;
		}
	      lc = props;
	      if (unlikely (lc + lp > 4))
		{
		  elf_uncompress_failed ();
		  return 0;
		}
	    }

	  if (control >= 0xa0)
	    {
	      size_t i;

	      /* Bit 5 or 6 is set, reset LZMA state.  */

	      lstate = 0;
	      memset (&dist, 0, sizeof dist);
	      for (i = 0; i < LZMA_PROB_TOTAL_COUNT; i++)
		probs[i] = 1 << 10;
	      range = 0xffffffff;
	      code = 0;
	    }

	  /* Read the range code.  */

	  if (unlikely (off + 5 > compressed_size))
	    {
	      elf_uncompress_failed ();
	      return 0;
	    }

	  /* The byte at compressed[off] is ignored for some
	     reason.  */

	  code = ((compressed[off + 1] << 24)
		  + (compressed[off + 2] << 16)
		  + (compressed[off + 3] << 8)
		  + compressed[off + 4]);
	  off += 5;

	  /* This is the main LZMA decode loop.  */

	  limit = off + compressed_chunk_size;
	  *poffset = off;
	  while (*poffset < limit)
	    {
	      unsigned int pos_state;

	      if (unlikely (uncompressed_offset
			    == (uncompressed_chunk_start
				+ uncompressed_chunk_size)))
		{
		  /* We've decompressed all the expected bytes.  */
		  break;
		}

	      pos_state = ((uncompressed_offset - dict_start_offset)
			   & ((1 << pb) - 1));

	      if (elf_lzma_bit (compressed, compressed_size,
				probs + LZMA_IS_MATCH (lstate, pos_state),
				poffset, &range, &code))
		{
		  uint32_t len;

		  if (elf_lzma_bit (compressed, compressed_size,
				    probs + LZMA_IS_REP (lstate),
				    poffset, &range, &code))
		    {
		      int short_rep;
		      uint32_t next_dist;

		      /* Repeated match.  */

		      short_rep = 0;
		      if (elf_lzma_bit (compressed, compressed_size,
					probs + LZMA_IS_REP0 (lstate),
					poffset, &range, &code))
			{
			  if (elf_lzma_bit (compressed, compressed_size,
					    probs + LZMA_IS_REP1 (lstate),
					    poffset, &range, &code))
			    {
			      if (elf_lzma_bit (compressed, compressed_size,
						probs + LZMA_IS_REP2 (lstate),
						poffset, &range, &code))
				{
				  next_dist = dist[3];
				  dist[3] = dist[2];
				}
			      else
				{
				  next_dist = dist[2];
				}
			      dist[2] = dist[1];
			    }
			  else
			    {
			      next_dist = dist[1];
			    }

			  dist[1] = dist[0];
			  dist[0] = next_dist;
			}
		      else
			{
			  if (!elf_lzma_bit (compressed, compressed_size,
					    (probs
					     + LZMA_IS_REP0_LONG (lstate,
								  pos_state)),
					    poffset, &range, &code))
			    short_rep = 1;
			}

		      if (lstate < 7)
			lstate = short_rep ? 9 : 8;
		      else
			lstate = 11;

		      if (short_rep)
			len = 1;
		      else
			len = elf_lzma_len (compressed, compressed_size,
					    probs, 1, pos_state, poffset,
					    &range, &code);
		    }
		  else
		    {
		      uint32_t dist_state;
		      uint32_t dist_slot;
		      uint16_t *probs_dist;

		      /* Match.  */

		      if (lstate < 7)
			lstate = 7;
		      else
			lstate = 10;
		      dist[3] = dist[2];
		      dist[2] = dist[1];
		      dist[1] = dist[0];
		      len = elf_lzma_len (compressed, compressed_size,
					  probs, 0, pos_state, poffset,
					  &range, &code);

		      if (len < 4 + 2)
			dist_state = len - 2;
		      else
			dist_state = 3;
		      probs_dist = probs + LZMA_DIST_SLOT (dist_state, 0);
		      dist_slot = elf_lzma_integer (compressed,
						    compressed_size,
						    probs_dist, 6,
						    poffset, &range,
						    &code);
		      if (dist_slot < LZMA_DIST_MODEL_START)
			dist[0] = dist_slot;
		      else
			{
			  uint32_t limit;

			  limit = (dist_slot >> 1) - 1;
			  dist[0] = 2 + (dist_slot & 1);
			  if (dist_slot < LZMA_DIST_MODEL_END)
			    {
			      dist[0] <<= limit;
			      probs_dist = (probs
					    + LZMA_DIST_SPECIAL(dist[0]
								- dist_slot
								- 1));
			      dist[0] +=
				elf_lzma_reverse_integer (compressed,
							  compressed_size,
							  probs_dist,
							  limit, poffset,
							  &range, &code);
			    }
			  else
			    {
			      uint32_t dist0;
			      uint32_t i;

			      dist0 = dist[0];
			      for (i = 0; i < limit - 4; i++)
				{
				  uint32_t mask;

				  elf_lzma_range_normalize (compressed,
							    compressed_size,
							    poffset,
							    &range, &code);
				  range >>= 1;
				  code -= range;
				  mask = -(code >> 31);
				  code += range & mask;
				  dist0 <<= 1;
				  dist0 += mask + 1;
				}
			      dist0 <<= 4;
			      probs_dist = probs + LZMA_DIST_ALIGN (0);
			      dist0 +=
				elf_lzma_reverse_integer (compressed,
							  compressed_size,
							  probs_dist, 4,
							  poffset,
							  &range, &code);
			      dist[0] = dist0;
			    }
			}
		    }

		  if (unlikely (uncompressed_offset
				- dict_start_offset < dist[0] + 1))
		    {
		      elf_uncompress_failed ();
		      return 0;
		    }
		  if (unlikely (uncompressed_offset + len > uncompressed_size))
		    {
		      elf_uncompress_failed ();
		      return 0;
		    }

		  if (dist[0] == 0)
		    {
		      /* A common case, meaning repeat the last
			 character LEN times.  */
		      memset (uncompressed + uncompressed_offset,
			      uncompressed[uncompressed_offset - 1],
			      len);
		      uncompressed_offset += len;
		    }
		  else if (dist[0] + 1 >= len)
		    {
		      memcpy (uncompressed + uncompressed_offset,
			      uncompressed + uncompressed_offset - dist[0] - 1,
			      len);
		      uncompressed_offset += len;
		    }
		  else
		    {
		      while (len > 0)
			{
			  uint32_t copy;

			  copy = len < dist[0] + 1 ? len : dist[0] + 1;
			  memcpy (uncompressed + uncompressed_offset,
				  (uncompressed + uncompressed_offset
				   - dist[0] - 1),
				  copy);
			  len -= copy;
			  uncompressed_offset += copy;
			}
		    }
		}
	      else
		{
		  unsigned char prev;
		  unsigned char low;
		  size_t high;
		  uint16_t *lit_probs;
		  unsigned int sym;

		  /* Literal value.  */

		  if (uncompressed_offset > 0)
		    prev = uncompressed[uncompressed_offset - 1];
		  else
		    prev = 0;
		  low = prev >> (8 - lc);
		  high = (((uncompressed_offset - dict_start_offset)
			   & ((1 << lp) - 1))
			  << lc);
		  lit_probs = probs + LZMA_LITERAL (low + high, 0);
		  if (lstate < 7)
		    sym = elf_lzma_integer (compressed, compressed_size,
					    lit_probs, 8, poffset, &range,
					    &code);
		  else
		    {
		      unsigned int match;
		      unsigned int bit;
		      unsigned int match_bit;
		      unsigned int idx;

		      sym = 1;
		      if (uncompressed_offset >= dist[0] + 1)
			match = uncompressed[uncompressed_offset - dist[0] - 1];
		      else
			match = 0;
		      match <<= 1;
		      bit = 0x100;
		      do
			{
			  match_bit = match & bit;
			  match <<= 1;
			  idx = bit + match_bit + sym;
			  sym <<= 1;
			  if (elf_lzma_bit (compressed, compressed_size,
					    lit_probs + idx, poffset,
					    &range, &code))
			    {
			      ++sym;
			      bit &= match_bit;
			    }
			  else
			    {
			      bit &= ~ match_bit;
			    }
			}
		      while (sym < 0x100);
		    }

		  if (unlikely (uncompressed_offset >= uncompressed_size))
		    {
		      elf_uncompress_failed ();
		      return 0;
		    }

		  uncompressed[uncompressed_offset] = (unsigned char) sym;
		  ++uncompressed_offset;
		  if (lstate <= 3)
		    lstate = 0;
		  else if (lstate <= 9)
		    lstate -= 3;
		  else
		    lstate -= 6;
		}
	    }

	  elf_lzma_range_normalize (compressed, compressed_size, poffset,
				    &range, &code);

	  off = *poffset;
	}
    }

  /* We have reached the end of the block.  Pad to four byte
     boundary.  */
  off = (off + 3) &~ (size_t) 3;
  if (unlikely (off > compressed_size))
    {
      elf_uncompress_failed ();
      return 0;
    }

  switch (check)
    {
    case 0:
      /* No check.  */
      break;

    case 1:
      /* CRC32 */
      if (unlikely (off + 4 > compressed_size))
	{
	  elf_uncompress_failed ();
	  return 0;
	}
      computed_crc = elf_crc32 (0, uncompressed, uncompressed_offset);
      stream_crc = (compressed[off]
		    | (compressed[off + 1] << 8)
		    | (compressed[off + 2] << 16)
		    | (compressed[off + 3] << 24));
      if (computed_crc != stream_crc)
	{
	  elf_uncompress_failed ();
	  return 0;
	}
      off += 4;
      break;

    case 4:
      /* CRC64.  We don't bother computing a CRC64 checksum.  */
      if (unlikely (off + 8 > compressed_size))
	{
	  elf_uncompress_failed ();
	  return 0;
	}
      off += 8;
      break;

    case 10:
      /* SHA.  We don't bother computing a SHA checksum.  */
      if (unlikely (off + 32 > compressed_size))
	{
	  elf_uncompress_failed ();
	  return 0;
	}
      off += 32;
      break;

    default:
      elf_uncompress_failed ();
      return 0;
    }

  *poffset = off;

  return 1;
}

/* Uncompress LZMA data found in a minidebug file.  The minidebug
   format is described at
   https://sourceware.org/gdb/current/onlinedocs/gdb/MiniDebugInfo.html.
   Returns 0 on error, 1 on successful decompression.  For this
   function we return 0 on failure to decompress, as the calling code
   will carry on in that case.  */

static int
elf_uncompress_lzma (struct backtrace_state *state,
		     const unsigned char *compressed, size_t compressed_size,
		     backtrace_error_callback error_callback, void *data,
		     unsigned char **uncompressed, size_t *uncompressed_size)
{
  size_t header_size;
  size_t footer_size;
  unsigned char check;
  uint32_t computed_crc;
  uint32_t stream_crc;
  size_t offset;
  size_t index_size;
  size_t footer_offset;
  size_t index_offset;
  uint64_t index_compressed_size;
  uint64_t index_uncompressed_size;
  unsigned char *mem;
  uint16_t *probs;
  size_t compressed_block_size;

  /* The format starts with a stream header and ends with a stream
     footer.  */
  header_size = 12;
  footer_size = 12;
  if (unlikely (compressed_size < header_size + footer_size))
    {
      elf_uncompress_failed ();
      return 0;
    }

  /* The stream header starts with a magic string.  */
  if (unlikely (memcmp (compressed, "\375" "7zXZ\0", 6) != 0))
    {
      elf_uncompress_failed ();
      return 0;
    }

  /* Next come stream flags.  The first byte is zero, the second byte
     is the check.  */
  if (unlikely (compressed[6] != 0))
    {
      elf_uncompress_failed ();
      return 0;
    }
  check = compressed[7];
  if (unlikely ((check & 0xf8) != 0))
    {
      elf_uncompress_failed ();
      return 0;
    }

  /* Next comes a CRC of the stream flags.  */
  computed_crc = elf_crc32 (0, compressed + 6, 2);
  stream_crc = (compressed[8]
		| (compressed[9] << 8)
		| (compressed[10] << 16)
		| (compressed[11] << 24));
  if (unlikely (computed_crc != stream_crc))
    {
      elf_uncompress_failed ();
      return 0;
    }

  /* Now that we've parsed the header, parse the footer, so that we
     can get the uncompressed size.  */

  /* The footer ends with two magic bytes.  */

  offset = compressed_size;
  if (unlikely (memcmp (compressed + offset - 2, "YZ", 2) != 0))
    {
      elf_uncompress_failed ();
      return 0;
    }
  offset -= 2;

  /* Before that are the stream flags, which should be the same as the
     flags in the header.  */
  if (unlikely (compressed[offset - 2] != 0
		|| compressed[offset - 1] != check))
    {
      elf_uncompress_failed ();
      return 0;
    }
  offset -= 2;

  /* Before that is the size of the index field, which precedes the
     footer.  */
  index_size = (compressed[offset - 4]
		| (compressed[offset - 3] << 8)
		| (compressed[offset - 2] << 16)
		| (compressed[offset - 1] << 24));
  index_size = (index_size + 1) * 4;
  offset -= 4;

  /* Before that is a footer CRC.  */
  computed_crc = elf_crc32 (0, compressed + offset, 6);
  stream_crc = (compressed[offset - 4]
		| (compressed[offset - 3] << 8)
		| (compressed[offset - 2] << 16)
		| (compressed[offset - 1] << 24));
  if (unlikely (computed_crc != stream_crc))
    {
      elf_uncompress_failed ();
      return 0;
    }
  offset -= 4;

  /* The index comes just before the footer.  */
  if (unlikely (offset < index_size + header_size))
    {
      elf_uncompress_failed ();
      return 0;
    }

  footer_offset = offset;
  offset -= index_size;
  index_offset = offset;

  /* The index starts with a zero byte.  */
  if (unlikely (compressed[offset] != 0))
    {
      elf_uncompress_failed ();
      return 0;
    }
  ++offset;

  /* Next is the number of blocks.  We expect zero blocks for an empty
     stream, and otherwise a single block.  */
  if (unlikely (compressed[offset] == 0))
    {
      *uncompressed = NULL;
      *uncompressed_size = 0;
      return 1;
    }
  if (unlikely (compressed[offset] != 1))
    {
      elf_uncompress_failed ();
      return 0;
    }
  ++offset;

  /* Next is the compressed size and the uncompressed size.  */
  if (!elf_lzma_varint (compressed, compressed_size, &offset,
			&index_compressed_size))
    return 0;
  if (!elf_lzma_varint (compressed, compressed_size, &offset,
			&index_uncompressed_size))
    return 0;

  /* Pad to a four byte boundary.  */
  offset = (offset + 3) &~ (size_t) 3;

  /* Next is a CRC of the index.  */
  computed_crc = elf_crc32 (0, compressed + index_offset,
			    offset - index_offset);
  stream_crc = (compressed[offset]
		| (compressed[offset + 1] << 8)
		| (compressed[offset + 2] << 16)
		| (compressed[offset + 3] << 24));
  if (unlikely (computed_crc != stream_crc))
    {
      elf_uncompress_failed ();
      return 0;
    }
  offset += 4;

  /* We should now be back at the footer.  */
  if (unlikely (offset != footer_offset))
    {
      elf_uncompress_failed ();
      return 0;
    }

  /* Allocate space to hold the uncompressed data.  If we succeed in
     uncompressing the LZMA data, we never free this memory.  */
  mem = (unsigned char *) backtrace_alloc (state, index_uncompressed_size,
					   error_callback, data);
  if (unlikely (mem == NULL))
    return 0;
  *uncompressed = mem;
  *uncompressed_size = index_uncompressed_size;

  /* Allocate space for probabilities.  */
  probs = ((uint16_t *)
	   backtrace_alloc (state,
			    LZMA_PROB_TOTAL_COUNT * sizeof (uint16_t),
			    error_callback, data));
  if (unlikely (probs == NULL))
    {
      backtrace_free (state, mem, index_uncompressed_size, error_callback,
		      data);
      return 0;
    }

  /* Uncompress the block, which follows the header.  */
  offset = 12;
  if (!elf_uncompress_lzma_block (compressed, compressed_size, check, probs,
				  mem, index_uncompressed_size, &offset))
    {
      backtrace_free (state, mem, index_uncompressed_size, error_callback,
		      data);
      return 0;
    }

  compressed_block_size = offset - 12;
  if (unlikely (compressed_block_size
		!= ((index_compressed_size + 3) &~ (size_t) 3)))
    {
      elf_uncompress_failed ();
      backtrace_free (state, mem, index_uncompressed_size, error_callback,
		      data);
      return 0;
    }

  offset = (offset + 3) &~ (size_t) 3;
  if (unlikely (offset != index_offset))
    {
      elf_uncompress_failed ();
      backtrace_free (state, mem, index_uncompressed_size, error_callback,
		      data);
      return 0;
    }

  return 1;
}

/* This function is a hook for testing the LZMA support.  It is only
   used by tests.  */

int
backtrace_uncompress_lzma (struct backtrace_state *state,
			   const unsigned char *compressed,
			   size_t compressed_size,
			   backtrace_error_callback error_callback,
			   void *data, unsigned char **uncompressed,
			   size_t *uncompressed_size)
{
  return elf_uncompress_lzma (state, compressed, compressed_size,
			      error_callback, data, uncompressed,
			      uncompressed_size);
}

/* Add the backtrace data for one ELF file.  Returns 1 on success,
   0 on failure (in both cases descriptor is closed) or -1 if exe
   is non-zero and the ELF file is ET_DYN, which tells the caller that
   elf_add will need to be called on the descriptor again after
   base_address is determined.  */

static int
elf_add (struct backtrace_state *state, const char *filename, int descriptor,
	 const unsigned char *memory, size_t memory_size,
	 uintptr_t base_address, backtrace_error_callback error_callback,
	 void *data, fileline *fileline_fn, int *found_sym, int *found_dwarf,
	 struct dwarf_data **fileline_entry, int exe, int debuginfo,
	 const char *with_buildid_data, uint32_t with_buildid_size)
{
  struct elf_view ehdr_view;
  b_elf_ehdr ehdr;
  off_t shoff;
  unsigned int shnum;
  unsigned int shstrndx;
  struct elf_view shdrs_view;
  int shdrs_view_valid;
  const b_elf_shdr *shdrs;
  const b_elf_shdr *shstrhdr;
  size_t shstr_size;
  off_t shstr_off;
  struct elf_view names_view;
  int names_view_valid;
  const char *names;
  unsigned int symtab_shndx;
  unsigned int dynsym_shndx;
  unsigned int i;
  struct debug_section_info sections[DEBUG_MAX];
  struct debug_section_info zsections[DEBUG_MAX];
  struct elf_view symtab_view;
  int symtab_view_valid;
  struct elf_view strtab_view;
  int strtab_view_valid;
  struct elf_view buildid_view;
  int buildid_view_valid;
  const char *buildid_data;
  uint32_t buildid_size;
  struct elf_view debuglink_view;
  int debuglink_view_valid;
  const char *debuglink_name;
  uint32_t debuglink_crc;
  struct elf_view debugaltlink_view;
  int debugaltlink_view_valid;
  const char *debugaltlink_name;
  const char *debugaltlink_buildid_data;
  uint32_t debugaltlink_buildid_size;
  struct elf_view gnu_debugdata_view;
  int gnu_debugdata_view_valid;
  size_t gnu_debugdata_size;
  unsigned char *gnu_debugdata_uncompressed;
  size_t gnu_debugdata_uncompressed_size;
  off_t min_offset;
  off_t max_offset;
  off_t debug_size;
  struct elf_view debug_view;
  int debug_view_valid;
  unsigned int using_debug_view;
  uint16_t *zdebug_table;
  struct elf_view split_debug_view[DEBUG_MAX];
  unsigned char split_debug_view_valid[DEBUG_MAX];
  struct elf_ppc64_opd_data opd_data, *opd;
  struct dwarf_sections dwarf_sections;

  if (!debuginfo)
    {
      *found_sym = 0;
      *found_dwarf = 0;
    }

  shdrs_view_valid = 0;
  names_view_valid = 0;
  symtab_view_valid = 0;
  strtab_view_valid = 0;
  buildid_view_valid = 0;
  buildid_data = NULL;
  buildid_size = 0;
  debuglink_view_valid = 0;
  debuglink_name = NULL;
  debuglink_crc = 0;
  debugaltlink_view_valid = 0;
  debugaltlink_name = NULL;
  debugaltlink_buildid_data = NULL;
  debugaltlink_buildid_size = 0;
  gnu_debugdata_view_valid = 0;
  gnu_debugdata_size = 0;
  debug_view_valid = 0;
  memset (&split_debug_view_valid[0], 0, sizeof split_debug_view_valid);
  opd = NULL;

  if (!elf_get_view (state, descriptor, memory, memory_size, 0, sizeof ehdr,
		     error_callback, data, &ehdr_view))
    goto fail;

  memcpy (&ehdr, ehdr_view.view.data, sizeof ehdr);

  elf_release_view (state, &ehdr_view, error_callback, data);

  if (ehdr.e_ident[EI_MAG0] != ELFMAG0
      || ehdr.e_ident[EI_MAG1] != ELFMAG1
      || ehdr.e_ident[EI_MAG2] != ELFMAG2
      || ehdr.e_ident[EI_MAG3] != ELFMAG3)
    {
      error_callback (data, "executable file is not ELF", 0);
      goto fail;
    }
  if (ehdr.e_ident[EI_VERSION] != EV_CURRENT)
    {
      error_callback (data, "executable file is unrecognized ELF version", 0);
      goto fail;
    }

#if BACKTRACE_ELF_SIZE == 32
#define BACKTRACE_ELFCLASS ELFCLASS32
#else
#define BACKTRACE_ELFCLASS ELFCLASS64
#endif

  if (ehdr.e_ident[EI_CLASS] != BACKTRACE_ELFCLASS)
    {
      error_callback (data, "executable file is unexpected ELF class", 0);
      goto fail;
    }

  if (ehdr.e_ident[EI_DATA] != ELFDATA2LSB
      && ehdr.e_ident[EI_DATA] != ELFDATA2MSB)
    {
      error_callback (data, "executable file has unknown endianness", 0);
      goto fail;
    }

  /* If the executable is ET_DYN, it is either a PIE, or we are running
     directly a shared library with .interp.  We need to wait for
     dl_iterate_phdr in that case to determine the actual base_address.  */
  if (exe && ehdr.e_type == ET_DYN)
    return -1;

  shoff = ehdr.e_shoff;
  shnum = ehdr.e_shnum;
  shstrndx = ehdr.e_shstrndx;

  if ((shnum == 0 || shstrndx == SHN_XINDEX)
      && shoff != 0)
    {
      struct elf_view shdr_view;
      const b_elf_shdr *shdr;

      if (!elf_get_view (state, descriptor, memory, memory_size, shoff,
			 sizeof shdr, error_callback, data, &shdr_view))
	goto fail;

      shdr = (const b_elf_shdr *) shdr_view.view.data;

      if (shnum == 0)
	shnum = shdr->sh_size;

      if (shstrndx == SHN_XINDEX)
	{
	  shstrndx = shdr->sh_link;

	  /* Versions of the GNU binutils between 2.12 and 2.18 did
	     not handle objects with more than SHN_LORESERVE sections
	     correctly.  All large section indexes were offset by
	     0x100.  There is more information at
	     http://sourceware.org/bugzilla/show_bug.cgi?id-5900 .
	     Fortunately these object files are easy to detect, as the
	     GNU binutils always put the section header string table
	     near the end of the list of sections.  Thus if the
	     section header string table index is larger than the
	     number of sections, then we know we have to subtract
	     0x100 to get the real section index.  */
	  if (shstrndx >= shnum && shstrndx >= SHN_LORESERVE + 0x100)
	    shstrndx -= 0x100;
	}

      elf_release_view (state, &shdr_view, error_callback, data);
    }

  if (shnum == 0 || shstrndx == 0)
    goto fail;

  /* To translate PC to file/line when using DWARF, we need to find
     the .debug_info and .debug_line sections.  */

  /* Read the section headers, skipping the first one.  */

  if (!elf_get_view (state, descriptor, memory, memory_size,
		     shoff + sizeof (b_elf_shdr),
		     (shnum - 1) * sizeof (b_elf_shdr),
		     error_callback, data, &shdrs_view))
    goto fail;
  shdrs_view_valid = 1;
  shdrs = (const b_elf_shdr *) shdrs_view.view.data;

  /* Read the section names.  */

  shstrhdr = &shdrs[shstrndx - 1];
  shstr_size = shstrhdr->sh_size;
  shstr_off = shstrhdr->sh_offset;

  if (!elf_get_view (state, descriptor, memory, memory_size, shstr_off,
		     shstrhdr->sh_size, error_callback, data, &names_view))
    goto fail;
  names_view_valid = 1;
  names = (const char *) names_view.view.data;

  symtab_shndx = 0;
  dynsym_shndx = 0;

  memset (sections, 0, sizeof sections);
  memset (zsections, 0, sizeof zsections);

  /* Look for the symbol table.  */
  for (i = 1; i < shnum; ++i)
    {
      const b_elf_shdr *shdr;
      unsigned int sh_name;
      const char *name;
      int j;

      shdr = &shdrs[i - 1];

      if (shdr->sh_type == SHT_SYMTAB)
	symtab_shndx = i;
      else if (shdr->sh_type == SHT_DYNSYM)
	dynsym_shndx = i;

      sh_name = shdr->sh_name;
      if (sh_name >= shstr_size)
	{
	  error_callback (data, "ELF section name out of range", 0);
	  goto fail;
	}

      name = names + sh_name;

      for (j = 0; j < (int) DEBUG_MAX; ++j)
	{
	  if (strcmp (name, dwarf_section_names[j]) == 0)
	    {
	      sections[j].offset = shdr->sh_offset;
	      sections[j].size = shdr->sh_size;
	      sections[j].compressed = (shdr->sh_flags & SHF_COMPRESSED) != 0;
	      break;
	    }
	}

      if (name[0] == '.' && name[1] == 'z')
	{
	  for (j = 0; j < (int) DEBUG_MAX; ++j)
	    {
	      if (strcmp (name + 2, dwarf_section_names[j] + 1) == 0)
		{
		  zsections[j].offset = shdr->sh_offset;
		  zsections[j].size = shdr->sh_size;
		  break;
		}
	    }
	}

      /* Read the build ID if present.  This could check for any
	 SHT_NOTE section with the right note name and type, but gdb
	 looks for a specific section name.  */
      if ((!debuginfo || with_buildid_data != NULL)
	  && !buildid_view_valid
	  && strcmp (name, ".note.gnu.build-id") == 0)
	{
	  const b_elf_note *note;

	  if (!elf_get_view (state, descriptor, memory, memory_size,
			     shdr->sh_offset, shdr->sh_size, error_callback,
			     data, &buildid_view))
	    goto fail;

	  buildid_view_valid = 1;
	  note = (const b_elf_note *) buildid_view.view.data;
	  if (note->type == NT_GNU_BUILD_ID
	      && note->namesz == 4
	      && strncmp (note->name, "GNU", 4) == 0
	      && shdr->sh_size <= 12 + ((note->namesz + 3) & ~ 3) + note->descsz)
	    {
	      buildid_data = &note->name[0] + ((note->namesz + 3) & ~ 3);
	      buildid_size = note->descsz;
	    }

	  if (with_buildid_size != 0)
	    {
	      if (buildid_size != with_buildid_size)
		goto fail;

	      if (memcmp (buildid_data, with_buildid_data, buildid_size) != 0)
		goto fail;
	    }
	}

      /* Read the debuglink file if present.  */
      if (!debuginfo
	  && !debuglink_view_valid
	  && strcmp (name, ".gnu_debuglink") == 0)
	{
	  const char *debuglink_data;
	  size_t crc_offset;

	  if (!elf_get_view (state, descriptor, memory, memory_size,
			     shdr->sh_offset, shdr->sh_size, error_callback,
			     data, &debuglink_view))
	    goto fail;

	  debuglink_view_valid = 1;
	  debuglink_data = (const char *) debuglink_view.view.data;
	  crc_offset = strnlen (debuglink_data, shdr->sh_size);
	  crc_offset = (crc_offset + 3) & ~3;
	  if (crc_offset + 4 <= shdr->sh_size)
	    {
	      debuglink_name = debuglink_data;
	      debuglink_crc = *(const uint32_t*)(debuglink_data + crc_offset);
	    }
	}

      if (!debugaltlink_view_valid
	  && strcmp (name, ".gnu_debugaltlink") == 0)
	{
	  const char *debugaltlink_data;
	  size_t debugaltlink_name_len;

	  if (!elf_get_view (state, descriptor, memory, memory_size,
			     shdr->sh_offset, shdr->sh_size, error_callback,
			     data, &debugaltlink_view))
	    goto fail;

	  debugaltlink_view_valid = 1;
	  debugaltlink_data = (const char *) debugaltlink_view.view.data;
	  debugaltlink_name = debugaltlink_data;
	  debugaltlink_name_len = strnlen (debugaltlink_data, shdr->sh_size);
	  if (debugaltlink_name_len < shdr->sh_size)
	    {
	      /* Include terminating zero.  */
	      debugaltlink_name_len += 1;

	      debugaltlink_buildid_data
		= debugaltlink_data + debugaltlink_name_len;
	      debugaltlink_buildid_size = shdr->sh_size - debugaltlink_name_len;
	    }
	}

      if (!gnu_debugdata_view_valid
	  && strcmp (name, ".gnu_debugdata") == 0)
	{
	  if (!elf_get_view (state, descriptor, memory, memory_size,
			     shdr->sh_offset, shdr->sh_size, error_callback,
			     data, &gnu_debugdata_view))
	    goto fail;

	  gnu_debugdata_size = shdr->sh_size;
	  gnu_debugdata_view_valid = 1;
	}

      /* Read the .opd section on PowerPC64 ELFv1.  */
      if (ehdr.e_machine == EM_PPC64
	  && (ehdr.e_flags & EF_PPC64_ABI) < 2
	  && shdr->sh_type == SHT_PROGBITS
	  && strcmp (name, ".opd") == 0)
	{
	  if (!elf_get_view (state, descriptor, memory, memory_size,
			     shdr->sh_offset, shdr->sh_size, error_callback,
			     data, &opd_data.view))
	    goto fail;

	  opd = &opd_data;
	  opd->addr = shdr->sh_addr;
	  opd->data = (const char *) opd_data.view.view.data;
	  opd->size = shdr->sh_size;
	}
    }

  if (symtab_shndx == 0)
    symtab_shndx = dynsym_shndx;
  if (symtab_shndx != 0 && !debuginfo)
    {
      const b_elf_shdr *symtab_shdr;
      unsigned int strtab_shndx;
      const b_elf_shdr *strtab_shdr;
      struct elf_syminfo_data *sdata;

      symtab_shdr = &shdrs[symtab_shndx - 1];
      strtab_shndx = symtab_shdr->sh_link;
      if (strtab_shndx >= shnum)
	{
	  error_callback (data,
			  "ELF symbol table strtab link out of range", 0);
	  goto fail;
	}
      strtab_shdr = &shdrs[strtab_shndx - 1];

      if (!elf_get_view (state, descriptor, memory, memory_size,
			 symtab_shdr->sh_offset, symtab_shdr->sh_size,
			 error_callback, data, &symtab_view))
	goto fail;
      symtab_view_valid = 1;

      if (!elf_get_view (state, descriptor, memory, memory_size,
			 strtab_shdr->sh_offset, strtab_shdr->sh_size,
			 error_callback, data, &strtab_view))
	goto fail;
      strtab_view_valid = 1;

      sdata = ((struct elf_syminfo_data *)
	       backtrace_alloc (state, sizeof *sdata, error_callback, data));
      if (sdata == NULL)
	goto fail;

      if (!elf_initialize_syminfo (state, base_address,
				   symtab_view.view.data, symtab_shdr->sh_size,
				   strtab_view.view.data, strtab_shdr->sh_size,
				   error_callback, data, sdata, opd))
	{
	  backtrace_free (state, sdata, sizeof *sdata, error_callback, data);
	  goto fail;
	}

      /* We no longer need the symbol table, but we hold on to the
	 string table permanently.  */
      elf_release_view (state, &symtab_view, error_callback, data);
      symtab_view_valid = 0;
      strtab_view_valid = 0;

      *found_sym = 1;

      elf_add_syminfo_data (state, sdata);
    }

  elf_release_view (state, &shdrs_view, error_callback, data);
  shdrs_view_valid = 0;
  elf_release_view (state, &names_view, error_callback, data);
  names_view_valid = 0;

  /* If the debug info is in a separate file, read that one instead.  */

  if (buildid_data != NULL)
    {
      int d;

      d = elf_open_debugfile_by_buildid (state, buildid_data, buildid_size,
					 error_callback, data);
      if (d >= 0)
	{
	  int ret;

	  elf_release_view (state, &buildid_view, error_callback, data);
	  if (debuglink_view_valid)
	    elf_release_view (state, &debuglink_view, error_callback, data);
	  if (debugaltlink_view_valid)
	    elf_release_view (state, &debugaltlink_view, error_callback, data);
	  ret = elf_add (state, "", d, NULL, 0, base_address, error_callback,
			 data, fileline_fn, found_sym, found_dwarf, NULL, 0,
			 1, NULL, 0);
	  if (ret < 0)
	    backtrace_close (d, error_callback, data);
	  else if (descriptor >= 0)
	    backtrace_close (descriptor, error_callback, data);
	  return ret;
	}
    }

  if (buildid_view_valid)
    {
      elf_release_view (state, &buildid_view, error_callback, data);
      buildid_view_valid = 0;
    }

  if (opd)
    {
      elf_release_view (state, &opd->view, error_callback, data);
      opd = NULL;
    }

  if (debuglink_name != NULL)
    {
      int d;

      d = elf_open_debugfile_by_debuglink (state, filename, debuglink_name,
					   debuglink_crc, error_callback,
					   data);
      if (d >= 0)
	{
	  int ret;

	  elf_release_view (state, &debuglink_view, error_callback, data);
	  if (debugaltlink_view_valid)
	    elf_release_view (state, &debugaltlink_view, error_callback, data);
	  ret = elf_add (state, "", d, NULL, 0, base_address, error_callback,
			 data, fileline_fn, found_sym, found_dwarf, NULL, 0,
			 1, NULL, 0);
	  if (ret < 0)
	    backtrace_close (d, error_callback, data);
	  else if (descriptor >= 0)
	    backtrace_close(descriptor, error_callback, data);
	  return ret;
	}
    }

  if (debuglink_view_valid)
    {
      elf_release_view (state, &debuglink_view, error_callback, data);
      debuglink_view_valid = 0;
    }

  struct dwarf_data *fileline_altlink = NULL;
  if (debugaltlink_name != NULL)
    {
      int d;

      d = elf_open_debugfile_by_debuglink (state, filename, debugaltlink_name,
					   0, error_callback, data);
      if (d >= 0)
	{
	  int ret;

	  ret = elf_add (state, filename, d, NULL, 0, base_address,
			 error_callback, data, fileline_fn, found_sym,
			 found_dwarf, &fileline_altlink, 0, 1,
			 debugaltlink_buildid_data, debugaltlink_buildid_size);
	  elf_release_view (state, &debugaltlink_view, error_callback, data);
	  debugaltlink_view_valid = 0;
	  if (ret < 0)
	    {
	      backtrace_close (d, error_callback, data);
	      return ret;
	    }
	}
    }

  if (debugaltlink_view_valid)
    {
      elf_release_view (state, &debugaltlink_view, error_callback, data);
      debugaltlink_view_valid = 0;
    }

  if (gnu_debugdata_view_valid)
    {
      int ret;

      ret = elf_uncompress_lzma (state,
				 ((const unsigned char *)
				  gnu_debugdata_view.view.data),
				 gnu_debugdata_size, error_callback, data,
				 &gnu_debugdata_uncompressed,
				 &gnu_debugdata_uncompressed_size);

      elf_release_view (state, &gnu_debugdata_view, error_callback, data);
      gnu_debugdata_view_valid = 0;

      if (ret)
	{
	  ret = elf_add (state, filename, -1, gnu_debugdata_uncompressed,
			 gnu_debugdata_uncompressed_size, base_address,
			 error_callback, data, fileline_fn, found_sym,
			 found_dwarf, NULL, 0, 0, NULL, 0);
	  if (ret >= 0 && descriptor >= 0)
	    backtrace_close(descriptor, error_callback, data);
	  return ret;
	}
    }

  /* Read all the debug sections in a single view, since they are
     probably adjacent in the file.  If any of sections are
     uncompressed, we never release this view.  */

  min_offset = 0;
  max_offset = 0;
  debug_size = 0;
  for (i = 0; i < (int) DEBUG_MAX; ++i)
    {
      off_t end;

      if (sections[i].size != 0)
	{
	  if (min_offset == 0 || sections[i].offset < min_offset)
	    min_offset = sections[i].offset;
	  end = sections[i].offset + sections[i].size;
	  if (end > max_offset)
	    max_offset = end;
	  debug_size += sections[i].size;
	}
      if (zsections[i].size != 0)
	{
	  if (min_offset == 0 || zsections[i].offset < min_offset)
	    min_offset = zsections[i].offset;
	  end = zsections[i].offset + zsections[i].size;
	  if (end > max_offset)
	    max_offset = end;
	  debug_size += zsections[i].size;
	}
    }
  if (min_offset == 0 || max_offset == 0)
    {
      if (descriptor >= 0)
	{
	  if (!backtrace_close (descriptor, error_callback, data))
	    goto fail;
	}
      return 1;
    }

  /* If the total debug section size is large, assume that there are
     gaps between the sections, and read them individually.  */

  if (max_offset - min_offset < 0x20000000
      || max_offset - min_offset < debug_size + 0x10000)
    {
      if (!elf_get_view (state, descriptor, memory, memory_size, min_offset,
			 max_offset - min_offset, error_callback, data,
			 &debug_view))
	goto fail;
      debug_view_valid = 1;
    }
  else
    {
      memset (&split_debug_view[0], 0, sizeof split_debug_view);
      for (i = 0; i < (int) DEBUG_MAX; ++i)
	{
	  struct debug_section_info *dsec;

	  if (sections[i].size != 0)
	    dsec = &sections[i];
	  else if (zsections[i].size != 0)
	    dsec = &zsections[i];
	  else
	    continue;

	  if (!elf_get_view (state, descriptor, memory, memory_size,
			     dsec->offset, dsec->size, error_callback, data,
			     &split_debug_view[i]))
	    goto fail;
	  split_debug_view_valid[i] = 1;

	  if (sections[i].size != 0)
	    sections[i].data = ((const unsigned char *)
				split_debug_view[i].view.data);
	  else
	    zsections[i].data = ((const unsigned char *)
				 split_debug_view[i].view.data);
	}
    }

  /* We've read all we need from the executable.  */
  if (descriptor >= 0)
    {
      if (!backtrace_close (descriptor, error_callback, data))
	goto fail;
      descriptor = -1;
    }

  using_debug_view = 0;
  if (debug_view_valid)
    {
      for (i = 0; i < (int) DEBUG_MAX; ++i)
	{
	  if (sections[i].size == 0)
	    sections[i].data = NULL;
	  else
	    {
	      sections[i].data = ((const unsigned char *) debug_view.view.data
				  + (sections[i].offset - min_offset));
	      ++using_debug_view;
	    }

	  if (zsections[i].size == 0)
	    zsections[i].data = NULL;
	  else
	    zsections[i].data = ((const unsigned char *) debug_view.view.data
				 + (zsections[i].offset - min_offset));
	}
    }

  /* Uncompress the old format (--compress-debug-sections=zlib-gnu).  */

  zdebug_table = NULL;
  for (i = 0; i < (int) DEBUG_MAX; ++i)
    {
      if (sections[i].size == 0 && zsections[i].size > 0)
	{
	  unsigned char *uncompressed_data;
	  size_t uncompressed_size;

	  if (zdebug_table == NULL)
	    {
	      zdebug_table = ((uint16_t *)
			      backtrace_alloc (state, ZDEBUG_TABLE_SIZE,
					       error_callback, data));
	      if (zdebug_table == NULL)
		goto fail;
	    }

	  uncompressed_data = NULL;
	  uncompressed_size = 0;
	  if (!elf_uncompress_zdebug (state, zsections[i].data,
				      zsections[i].size, zdebug_table,
				      error_callback, data,
				      &uncompressed_data, &uncompressed_size))
	    goto fail;
	  sections[i].data = uncompressed_data;
	  sections[i].size = uncompressed_size;
	  sections[i].compressed = 0;

	  if (split_debug_view_valid[i])
	    {
	      elf_release_view (state, &split_debug_view[i],
				error_callback, data);
	      split_debug_view_valid[i] = 0;
	    }
	}
    }

  /* Uncompress the official ELF format
     (--compress-debug-sections=zlib-gabi).  */
  for (i = 0; i < (int) DEBUG_MAX; ++i)
    {
      unsigned char *uncompressed_data;
      size_t uncompressed_size;

      if (sections[i].size == 0 || !sections[i].compressed)
	continue;

      if (zdebug_table == NULL)
	{
	  zdebug_table = ((uint16_t *)
			  backtrace_alloc (state, ZDEBUG_TABLE_SIZE,
					   error_callback, data));
	  if (zdebug_table == NULL)
	    goto fail;
	}

      uncompressed_data = NULL;
      uncompressed_size = 0;
      if (!elf_uncompress_chdr (state, sections[i].data, sections[i].size,
				zdebug_table, error_callback, data,
				&uncompressed_data, &uncompressed_size))
	goto fail;
      sections[i].data = uncompressed_data;
      sections[i].size = uncompressed_size;
      sections[i].compressed = 0;

      if (debug_view_valid)
	--using_debug_view;
      else if (split_debug_view_valid[i])
	{
	  elf_release_view (state, &split_debug_view[i], error_callback, data);
	  split_debug_view_valid[i] = 0;
	}
    }

  if (zdebug_table != NULL)
    backtrace_free (state, zdebug_table, ZDEBUG_TABLE_SIZE,
		    error_callback, data);

  if (debug_view_valid && using_debug_view == 0)
    {
      elf_release_view (state, &debug_view, error_callback, data);
      debug_view_valid = 0;
    }

  for (i = 0; i < (int) DEBUG_MAX; ++i)
    {
      dwarf_sections.data[i] = sections[i].data;
      dwarf_sections.size[i] = sections[i].size;
    }

  if (!backtrace_dwarf_add (state, base_address, &dwarf_sections,
			    ehdr.e_ident[EI_DATA] == ELFDATA2MSB,
			    fileline_altlink,
			    error_callback, data, fileline_fn,
			    fileline_entry))
    goto fail;

  *found_dwarf = 1;

  return 1;

 fail:
  if (shdrs_view_valid)
    elf_release_view (state, &shdrs_view, error_callback, data);
  if (names_view_valid)
    elf_release_view (state, &names_view, error_callback, data);
  if (symtab_view_valid)
    elf_release_view (state, &symtab_view, error_callback, data);
  if (strtab_view_valid)
    elf_release_view (state, &strtab_view, error_callback, data);
  if (debuglink_view_valid)
    elf_release_view (state, &debuglink_view, error_callback, data);
  if (debugaltlink_view_valid)
    elf_release_view (state, &debugaltlink_view, error_callback, data);
  if (gnu_debugdata_view_valid)
    elf_release_view (state, &gnu_debugdata_view, error_callback, data);
  if (buildid_view_valid)
    elf_release_view (state, &buildid_view, error_callback, data);
  if (debug_view_valid)
    elf_release_view (state, &debug_view, error_callback, data);
  for (i = 0; i < (int) DEBUG_MAX; ++i)
    {
      if (split_debug_view_valid[i])
	elf_release_view (state, &split_debug_view[i], error_callback, data);
    }
  if (opd)
    elf_release_view (state, &opd->view, error_callback, data);
  if (descriptor >= 0)
    backtrace_close (descriptor, error_callback, data);
  return 0;
}

/* Data passed to phdr_callback.  */

struct phdr_data
{
  struct backtrace_state *state;
  backtrace_error_callback error_callback;
  void *data;
  fileline *fileline_fn;
  int *found_sym;
  int *found_dwarf;
  const char *exe_filename;
  int exe_descriptor;
};

/* Callback passed to dl_iterate_phdr.  Load debug info from shared
   libraries.  */

static int
#ifdef __i386__
__attribute__ ((__force_align_arg_pointer__))
#endif
phdr_callback (struct dl_phdr_info *info, size_t size ATTRIBUTE_UNUSED,
	       void *pdata)
{
  struct phdr_data *pd = (struct phdr_data *) pdata;
  const char *filename;
  int descriptor;
  int does_not_exist;
  fileline elf_fileline_fn;
  int found_dwarf;

  /* There is not much we can do if we don't have the module name,
     unless executable is ET_DYN, where we expect the very first
     phdr_callback to be for the PIE.  */
  if (info->dlpi_name == NULL || info->dlpi_name[0] == '\0')
    {
      if (pd->exe_descriptor == -1)
	return 0;
      filename = pd->exe_filename;
      descriptor = pd->exe_descriptor;
      pd->exe_descriptor = -1;
    }
  else
    {
      if (pd->exe_descriptor != -1)
	{
	  backtrace_close (pd->exe_descriptor, pd->error_callback, pd->data);
	  pd->exe_descriptor = -1;
	}

      filename = info->dlpi_name;
      descriptor = backtrace_open (info->dlpi_name, pd->error_callback,
				   pd->data, &does_not_exist);
      if (descriptor < 0)
	return 0;
    }

  if (elf_add (pd->state, filename, descriptor, NULL, 0, info->dlpi_addr,
	       pd->error_callback, pd->data, &elf_fileline_fn, pd->found_sym,
	       &found_dwarf, NULL, 0, 0, NULL, 0))
    {
      if (found_dwarf)
	{
	  *pd->found_dwarf = 1;
	  *pd->fileline_fn = elf_fileline_fn;
	}
    }

  return 0;
}

/* Initialize the backtrace data we need from an ELF executable.  At
   the ELF level, all we need to do is find the debug info
   sections.  */

int
backtrace_initialize (struct backtrace_state *state, const char *filename,
		      int descriptor, backtrace_error_callback error_callback,
		      void *data, fileline *fileline_fn)
{
  int ret;
  int found_sym;
  int found_dwarf;
  fileline elf_fileline_fn = elf_nodebug;
  struct phdr_data pd;

  ret = elf_add (state, filename, descriptor, NULL, 0, 0, error_callback, data,
		 &elf_fileline_fn, &found_sym, &found_dwarf, NULL, 1, 0, NULL,
		 0);
  if (!ret)
    return 0;

  pd.state = state;
  pd.error_callback = error_callback;
  pd.data = data;
  pd.fileline_fn = &elf_fileline_fn;
  pd.found_sym = &found_sym;
  pd.found_dwarf = &found_dwarf;
  pd.exe_filename = filename;
  pd.exe_descriptor = ret < 0 ? descriptor : -1;

  dl_iterate_phdr (phdr_callback, (void *) &pd);

  if (!state->threaded)
    {
      if (found_sym)
	state->syminfo_fn = elf_syminfo;
      else if (state->syminfo_fn == NULL)
	state->syminfo_fn = elf_nosyms;
    }
  else
    {
      if (found_sym)
	backtrace_atomic_store_pointer (&state->syminfo_fn, elf_syminfo);
      else
	(void) __sync_bool_compare_and_swap (&state->syminfo_fn, NULL,
					     elf_nosyms);
    }

  if (!state->threaded)
    *fileline_fn = state->fileline_fn;
  else
    *fileline_fn = backtrace_atomic_load_pointer (&state->fileline_fn);

  if (*fileline_fn == NULL || *fileline_fn == elf_nodebug)
    *fileline_fn = elf_fileline_fn;

  return 1;
}
