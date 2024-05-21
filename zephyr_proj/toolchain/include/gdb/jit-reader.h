/* JIT declarations for GDB, the GNU Debugger.

   Copyright (C) 2011-2022 Free Software Foundation, Inc.

   This file is part of GDB.

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.  */

#ifndef GDB_JIT_READER_H
#define GDB_JIT_READER_H

#ifdef __cplusplus
extern "C" {
#endif

/* Versioning information.  See gdb_reader_funcs.  */

#define GDB_READER_INTERFACE_VERSION 1

/* Readers must be released under a GPL compatible license.  To
   declare that the reader is indeed released under a GPL compatible
   license, invoke the macro GDB_DECLARE_GPL_COMPATIBLE in a source
   file.  */

#ifdef __cplusplus
#define GDB_DECLARE_GPL_COMPATIBLE_READER       \
  extern "C" {                                  \
  extern int plugin_is_GPL_compatible (void);   \
  extern int plugin_is_GPL_compatible (void)    \
  {                                             \
    return 0;                                   \
  }                                             \
  }

#else

#define GDB_DECLARE_GPL_COMPATIBLE_READER       \
  extern int plugin_is_GPL_compatible (void);   \
  extern int plugin_is_GPL_compatible (void)    \
  {                                             \
    return 0;                                   \
  }

#endif

/* Represents an address on the target system.  */

typedef unsigned long GDB_CORE_ADDR;

/* Return status codes.  */

enum gdb_status {
  GDB_FAIL = 0,
  GDB_SUCCESS = 1
};

struct gdb_object;
struct gdb_symtab;
struct gdb_block;
struct gdb_symbol_callbacks;

/* An array of these are used to represent a map from code addresses to line
   numbers in the source file.  */

struct gdb_line_mapping
{
  int line;
  GDB_CORE_ADDR pc;
};

/* Create a new GDB code object.  Each code object can have one or
   more symbol tables, each representing a compiled source file.  */

typedef struct gdb_object *(gdb_object_open) (struct gdb_symbol_callbacks *cb);

/* The callback used to create new symbol table.  CB is the
   gdb_symbol_callbacks which the structure is part of.  FILE_NAME is
   an (optionally NULL) file name to associate with this new symbol
   table.

   Returns a new instance to gdb_symtab that can later be passed to
   gdb_block_new, gdb_symtab_add_line_mapping and gdb_symtab_close.  */

typedef struct gdb_symtab *(gdb_symtab_open) (struct gdb_symbol_callbacks *cb,
                                              struct gdb_object *obj,
                                              const char *file_name);

/* Creates a new block in a given symbol table.  A symbol table is a
   forest of blocks, each block representing an code address range and
   a corresponding (optionally NULL) NAME.  In case the block
   corresponds to a function, the NAME passed should be the name of
   the function.

   If the new block to be created is a child of (i.e. is nested in)
   another block, the parent block can be passed in PARENT.  SYMTAB is
   the symbol table the new block is to belong in.  BEGIN, END is the
   code address range the block corresponds to.

   Returns a new instance of gdb_block, which, as of now, has no use.
   Note that the gdb_block returned must not be freed by the
   caller.  */

typedef struct gdb_block *(gdb_block_open) (struct gdb_symbol_callbacks *cb,
                                            struct gdb_symtab *symtab,
                                            struct gdb_block *parent,
                                            GDB_CORE_ADDR begin,
                                            GDB_CORE_ADDR end,
                                            const char *name);

/* Adds a PC to line number mapping for the symbol table SYMTAB.
   NLINES is the number of elements in LINES, each element
   corresponding to one (PC, line) pair.  */

typedef void (gdb_symtab_add_line_mapping) (struct gdb_symbol_callbacks *cb,
                                            struct gdb_symtab *symtab,
                                            int nlines,
                                            struct gdb_line_mapping *lines);

/* Close the symtab SYMTAB.  This signals to GDB that no more blocks
   will be opened on this symtab.  */

typedef void (gdb_symtab_close) (struct gdb_symbol_callbacks *cb,
                                 struct gdb_symtab *symtab);


/* Closes the gdb_object OBJ and adds the emitted information into
   GDB's internal structures.  Once this is done, the debug
   information will be picked up and used; this will usually be the
   last operation in gdb_read_debug_info.  */

typedef void (gdb_object_close) (struct gdb_symbol_callbacks *cb,
                                 struct gdb_object *obj);

/* Reads LEN bytes from TARGET_MEM in the target's virtual address
   space into GDB_BUF.

   Returns GDB_FAIL on failure, and GDB_SUCCESS on success.  */

typedef enum gdb_status (gdb_target_read) (GDB_CORE_ADDR target_mem,
                                           void *gdb_buf, int len);

/* The list of callbacks that are passed to read.  These callbacks are
   to be used to construct the symbol table.  The functions have been
   described above.  */

struct gdb_symbol_callbacks
{
  gdb_object_open *object_open;
  gdb_symtab_open *symtab_open;
  gdb_block_open *block_open;
  gdb_symtab_close *symtab_close;
  gdb_object_close *object_close;

  gdb_symtab_add_line_mapping *line_mapping_add;
  gdb_target_read *target_read;

  /* For internal use by GDB.  */
  void *priv_data;
};

/* Forward declaration.  */

struct gdb_reg_value;

/* A function of this type is used to free a gdb_reg_value.  See the
   comment on `free' in struct gdb_reg_value.  */

typedef void (gdb_reg_value_free) (struct gdb_reg_value *);

/* Denotes the value of a register.  */

struct gdb_reg_value
{
  /* The size of the register in bytes.  The reader need not set this
     field.  This will be set for (defined) register values being read
     from GDB using reg_get.  */
  int size;

  /* Set to non-zero if the value for the register is known.  The
     registers for which the reader does not call reg_set are also
     assumed to be undefined */
  int defined;

  /* Since gdb_reg_value is a variable sized structure, it will
     usually be allocated on the heap.  This function is expected to
     contain the corresponding "free" function.

     When a pointer to gdb_reg_value is being sent from GDB to the
     reader (via gdb_unwind_reg_get), the reader is expected to call
     this function (with the same gdb_reg_value as argument) once it
     is done with the value.

     When the function sends the a gdb_reg_value to GDB (via
     gdb_unwind_reg_set), it is expected to set this field to point to
     an appropriate cleanup routine (or to NULL if no cleanup is
     required).  */
  gdb_reg_value_free *free;

  /* The value of the register.  */
  unsigned char value[1];
};

/* get_frame_id in gdb_reader_funcs is to return a gdb_frame_id
   corresponding to the current frame.  The registers corresponding to
   the current frame can be read using reg_get.  Calling get_frame_id
   on a particular frame should return the same gdb_frame_id
   throughout its lifetime (i.e. till before it gets unwound).  One
   way to do this is by having the CODE_ADDRESS point to the
   function's first instruction and STACK_ADDRESS point to the value
   of the stack pointer when entering the function.  */

struct gdb_frame_id
{
  GDB_CORE_ADDR code_address;
  GDB_CORE_ADDR stack_address;
};

/* Forward declaration.  */

struct gdb_unwind_callbacks;

/* Returns the value of a particular register in the current frame.
   The current frame is the frame that needs to be unwound into the
   outer (earlier) frame.

   CB is the struct gdb_unwind_callbacks * the callback belongs to.
   REGNUM is the DWARF register number of the register that needs to
   be unwound.

   Returns the gdb_reg_value corresponding to the register requested.
   In case the value of the register has been optimized away or
   otherwise unavailable, the defined flag in the returned
   gdb_reg_value will be zero.  */

typedef struct gdb_reg_value *(gdb_unwind_reg_get)
                              (struct gdb_unwind_callbacks *cb, int regnum);

/* Sets the previous value of a particular register.  REGNUM is the
   (DWARF) register number whose value is to be set.  VAL is the value
   the register is to be set to.

   VAL is *not* copied, so the memory allocated to it cannot be
   reused.  Once GDB no longer needs the value, it is deallocated
   using the FREE function (see gdb_reg_value).

   A register can also be "set" to an undefined value by setting the
   defined in VAL to zero.  */

typedef void (gdb_unwind_reg_set) (struct gdb_unwind_callbacks *cb, int regnum,
                                   struct gdb_reg_value *val);

/* This struct is passed to unwind in gdb_reader_funcs, and is to be
   used to unwind the current frame (current being the frame whose
   registers can be read using reg_get) into the earlier frame.  The
   functions have been described above.  */

struct gdb_unwind_callbacks
{
  gdb_unwind_reg_get *reg_get;
  gdb_unwind_reg_set *reg_set;
  gdb_target_read *target_read;

  /* For internal use by GDB.  */
  void *priv_data;
};

/* Forward declaration.  */

struct gdb_reader_funcs;

/* Parse the debug info off a block of memory, pointed to by MEMORY
   (already copied to GDB's address space) and MEMORY_SZ bytes long.
   The implementation has to use the functions in CB to actually emit
   the parsed data into GDB.  SELF is the same structure returned by
   gdb_init_reader.

   Return GDB_FAIL on failure and GDB_SUCCESS on success.  */

typedef enum gdb_status (gdb_read_debug_info) (struct gdb_reader_funcs *self,
                                               struct gdb_symbol_callbacks *cb,
                                               void *memory, long memory_sz);

/* Unwind the current frame, CB is the set of unwind callbacks that
   are to be used to do this.

   Return GDB_FAIL on failure and GDB_SUCCESS on success.  */

typedef enum gdb_status (gdb_unwind_frame) (struct gdb_reader_funcs *self,
                                            struct gdb_unwind_callbacks *cb);

/* Return the frame ID corresponding to the current frame, using C to
   read the current register values.  See the comment on struct
   gdb_frame_id.  */

typedef struct gdb_frame_id (gdb_get_frame_id) (struct gdb_reader_funcs *self,
                                                struct gdb_unwind_callbacks *c);

/* Called when a reader is being unloaded.  This function should also
   free SELF, if required.  */

typedef void (gdb_destroy_reader) (struct gdb_reader_funcs *self);

/* Called when the reader is loaded.  Must either return a properly
   populated gdb_reader_funcs or NULL.  The memory allocated for the
   gdb_reader_funcs is to be managed by the reader itself (i.e. if it
   is allocated from the heap, it must also be freed in
   gdb_destroy_reader).  */

extern struct gdb_reader_funcs *gdb_init_reader (void);

/* Pointer to the functions which implement the reader's
   functionality.  The individual functions have been documented
   above.

   None of the fields are optional.  */

struct gdb_reader_funcs
{
  /* Must be set to GDB_READER_INTERFACE_VERSION.  */
  int reader_version;

  /* For use by the reader.  */
  void *priv_data;

  gdb_read_debug_info *read;
  gdb_unwind_frame *unwind;
  gdb_get_frame_id *get_frame_id;
  gdb_destroy_reader *destroy;
};

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif
