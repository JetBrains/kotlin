// -*- C++ -*- forwarding header.

// Copyright (C) 1997-2022 Free Software Foundation, Inc.
//
// This file is part of the GNU ISO C++ Library.  This library is free
// software; you can redistribute it and/or modify it under the
// terms of the GNU General Public License as published by the
// Free Software Foundation; either version 3, or (at your option)
// any later version.

// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// Under Section 7 of GPL version 3, you are granted additional
// permissions described in the GCC Runtime Library Exception, version
// 3.1, as published by the Free Software Foundation.

// You should have received a copy of the GNU General Public License and
// a copy of the GCC Runtime Library Exception along with this program;
// see the files COPYING3 and COPYING.RUNTIME respectively.  If not, see
// <http://www.gnu.org/licenses/>.

/** @file include/cstdio
 *  This is a Standard C++ Library file.  You should @c \#include this file
 *  in your programs, rather than any of the @a *.h implementation files.
 *
 *  This is the C++ version of the Standard C Library header @c stdio.h,
 *  and its contents are (mostly) the same as that header, but are all
 *  contained in the namespace @c std (except for names which are defined
 *  as macros in C).
 */

//
// ISO C++ 14882: 27.8.2  C Library files
//

#pragma GCC system_header

#include <bits/c++config.h>
#include <stdio.h>

#ifndef _GLIBCXX_CSTDIO
#define _GLIBCXX_CSTDIO 1

#if __cplusplus <= 201103L && !defined(_GLIBCXX_HAVE_GETS)
extern "C" char* gets (char* __s) __attribute__((__deprecated__));
#endif

// Get rid of those macros defined in <stdio.h> in lieu of real functions.
#undef clearerr
#undef fclose
#undef feof
#undef ferror
#undef fflush
#undef fgetc
#undef fgetpos
#undef fgets
#undef fopen
#undef fprintf
#undef fputc
#undef fputs
#undef fread
#undef freopen
#undef fscanf
#undef fseek
#undef fsetpos
#undef ftell
#undef fwrite
#undef getc
#undef getchar
#if __cplusplus <= 201103L
# undef gets
#endif
#undef perror
#undef printf
#undef putc
#undef putchar
#undef puts
#undef remove
#undef rename
#undef rewind
#undef scanf
#undef setbuf
#undef setvbuf
#undef sprintf
#undef sscanf
#undef tmpfile
#undef tmpnam
#undef ungetc
#undef vfprintf
#undef vprintf
#undef vsprintf

namespace std
{
  using ::FILE;
  using ::fpos_t;

  using ::clearerr;
  using ::fclose;
  using ::feof;
  using ::ferror;
  using ::fflush;
  using ::fgetc;
  using ::fgetpos;
  using ::fgets;
  using ::fopen;
  using ::fprintf;
  using ::fputc;
  using ::fputs;
  using ::fread;
  using ::freopen;
  using ::fscanf;
  using ::fseek;
  using ::fsetpos;
  using ::ftell;
  using ::fwrite;
  using ::getc;
  using ::getchar;
#if __cplusplus <= 201103L
  // LWG 2249
  using ::gets;
#endif
  using ::perror;
  using ::printf;
  using ::putc;
  using ::putchar;
  using ::puts;
  using ::remove;
  using ::rename;
  using ::rewind;
  using ::scanf;
  using ::setbuf;
  using ::setvbuf;
  using ::sprintf;
  using ::sscanf;
  using ::tmpfile;
#if _GLIBCXX_USE_TMPNAM
  using ::tmpnam;
#endif
  using ::ungetc;
  using ::vfprintf;
  using ::vprintf;
  using ::vsprintf;
} // namespace

#if _GLIBCXX_USE_C99_STDIO

#undef snprintf
#undef vfscanf
#undef vscanf
#undef vsnprintf
#undef vsscanf

namespace __gnu_cxx
{
#if _GLIBCXX_USE_C99_CHECK || _GLIBCXX_USE_C99_DYNAMIC
  extern "C" int
  (snprintf)(char * __restrict, std::size_t, const char * __restrict, ...)
  throw ();
  extern "C" int
  (vfscanf)(FILE * __restrict, const char * __restrict, __gnuc_va_list);
  extern "C" int (vscanf)(const char * __restrict, __gnuc_va_list);
  extern "C" int
  (vsnprintf)(char * __restrict, std::size_t, const char * __restrict,
	      __gnuc_va_list) throw ();
  extern "C" int
  (vsscanf)(const char * __restrict, const char * __restrict, __gnuc_va_list)
  throw ();
#endif

#if !_GLIBCXX_USE_C99_DYNAMIC
  using ::snprintf;
  using ::vfscanf;
  using ::vscanf;
  using ::vsnprintf;
  using ::vsscanf;
#endif
} // namespace __gnu_cxx

namespace std
{
  using ::__gnu_cxx::snprintf;
  using ::__gnu_cxx::vfscanf;
  using ::__gnu_cxx::vscanf;
  using ::__gnu_cxx::vsnprintf;
  using ::__gnu_cxx::vsscanf;
} // namespace std

#endif // _GLIBCXX_USE_C99_STDIO

#endif
