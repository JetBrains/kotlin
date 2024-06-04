// Locale support -*- C++ -*-

// Copyright (C) 2000-2022 Free Software Foundation, Inc.
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

//
// ISO C++ 14882: 22.1  Locales
//

// Information as gleaned from /usr/include/ctype.h

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  /// @brief  Base class for ctype.
  struct ctype_base
  {
    // Non-standard typedefs.
    typedef const int* 		__to_type;

    // NB: Offsets into ctype<char>::_M_table force a particular size
    // on the mask type. Because of this, we don't use an enum.
    typedef char 		mask;
    static const mask upper    	= _U;
    static const mask lower 	= _L;
    static const mask alpha 	= _U | _L;
    static const mask digit 	= _N;
    static const mask xdigit 	= _X | _N;
    static const mask space 	= _S;
    static const mask print 	= _P | _U | _L | _N | _B;
    static const mask graph 	= _P | _U | _L | _N;
    static const mask cntrl 	= _C;
    static const mask punct 	= _P;
    static const mask alnum 	= _U | _L | _N;
#if __cplusplus >= 201103L
    static const mask blank 	= space;
#endif
  };

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace
