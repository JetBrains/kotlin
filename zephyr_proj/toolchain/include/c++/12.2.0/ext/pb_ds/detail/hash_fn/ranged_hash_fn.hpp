// -*- C++ -*-

// Copyright (C) 2005-2022 Free Software Foundation, Inc.
//
// This file is part of the GNU ISO C++ Library.  This library is free
// software; you can redistribute it and/or modify it under the terms
// of the GNU General Public License as published by the Free Software
// Foundation; either version 3, or (at your option) any later
// version.

// This library is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.

// Under Section 7 of GPL version 3, you are granted additional
// permissions described in the GCC Runtime Library Exception, version
// 3.1, as published by the Free Software Foundation.

// You should have received a copy of the GNU General Public License and
// a copy of the GCC Runtime Library Exception along with this program;
// see the files COPYING3 and COPYING.RUNTIME respectively.  If not, see
// <http://www.gnu.org/licenses/>.

// Copyright (C) 2004 Ami Tavory and Vladimir Dreizin, IBM-HRL.

// Permission to use, copy, modify, sell, and distribute this software
// is hereby granted without fee, provided that the above copyright
// notice appears in all copies, and that both that copyright notice
// and this permission notice appear in supporting documentation. None
// of the above authors, nor IBM Haifa Research Laboratories, make any
// representation about the suitability of this software for any
// purpose. It is provided "as is" without express or implied
// warranty.

/**
 * @file ranged_hash_fn.hpp
 * Contains a unified ranged hash functor, allowing the hash tables
 * to deal with a single class for ranged hashing.
 */

#ifndef PB_DS_RANGED_HASH_FN_HPP
#define PB_DS_RANGED_HASH_FN_HPP

#include <utility>
#include <ext/pb_ds/detail/types_traits.hpp>
#include <debug/debug.h>

namespace __gnu_pbds
{
  namespace detail
  {
    /// Primary template.
    template<typename Key, typename Hash_Fn, typename _Alloc, 
	     typename Comb_Hash_Fn, bool Store_Hash>
    class ranged_hash_fn;

#define PB_DS_CLASS_T_DEC \
    template<typename Key, typename Hash_Fn, typename _Alloc, \
	     typename Comb_Hash_Fn>

#define PB_DS_CLASS_C_DEC \
    ranged_hash_fn<Key,	Hash_Fn, _Alloc, Comb_Hash_Fn, false>

    /**
     * Specialization 1
     * The client supplies a hash function and a ranged hash function,
     * and requests that hash values not be stored.
     **/
    template<typename Key, typename Hash_Fn, typename _Alloc, 
	     typename Comb_Hash_Fn>
    class ranged_hash_fn< Key, Hash_Fn, _Alloc, Comb_Hash_Fn, false> 
    : public Hash_Fn, public Comb_Hash_Fn
    {
    protected:
      typedef typename _Alloc::size_type size_type;
      typedef Hash_Fn hash_fn_base;
      typedef Comb_Hash_Fn comb_hash_fn_base;
      typedef typename rebind_traits<_Alloc, Key>::const_reference
	key_const_reference;

      ranged_hash_fn(size_type);

      ranged_hash_fn(size_type, const Hash_Fn&);

      ranged_hash_fn(size_type, const Hash_Fn&, const Comb_Hash_Fn&);

      void
      swap(PB_DS_CLASS_C_DEC&);

      void
      notify_resized(size_type);

      inline size_type
      operator()(key_const_reference) const;
    };

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_hash_fn(size_type size)
    { Comb_Hash_Fn::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_hash_fn(size_type size, const Hash_Fn& r_hash_fn) 
    : Hash_Fn(r_hash_fn)
    { Comb_Hash_Fn::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_hash_fn(size_type size, const Hash_Fn& r_hash_fn, 
		   const Comb_Hash_Fn& r_comb_hash_fn) 
    : Hash_Fn(r_hash_fn), Comb_Hash_Fn(r_comb_hash_fn)
    { comb_hash_fn_base::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    swap(PB_DS_CLASS_C_DEC& other)
    {
      comb_hash_fn_base::swap(other);
      std::swap((Hash_Fn& )(*this), (Hash_Fn& )other);
    }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    notify_resized(size_type size)
    { comb_hash_fn_base::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    inline typename PB_DS_CLASS_C_DEC::size_type
    PB_DS_CLASS_C_DEC::
    operator()(key_const_reference r_key) const
    { return (comb_hash_fn_base::operator()(hash_fn_base::operator()(r_key)));}

#undef PB_DS_CLASS_T_DEC
#undef PB_DS_CLASS_C_DEC

#define PB_DS_CLASS_T_DEC \
    template<typename Key, typename Hash_Fn, typename _Alloc, \
	     typename Comb_Hash_Fn>

#define PB_DS_CLASS_C_DEC \
    ranged_hash_fn<Key,Hash_Fn,	_Alloc, Comb_Hash_Fn, true>

    /**
     * Specialization 2
     * The client supplies a hash function and a ranged hash function,
     * and requests that hash values be stored.
     **/
    template<typename Key, typename Hash_Fn, typename _Alloc,
	     typename Comb_Hash_Fn>
    class ranged_hash_fn<Key, Hash_Fn, _Alloc, Comb_Hash_Fn, true> 
    : public Hash_Fn, public Comb_Hash_Fn
    {
    protected:
      typedef typename _Alloc::size_type size_type;
      typedef std::pair<size_type, size_type> comp_hash;
      typedef Hash_Fn hash_fn_base;
      typedef Comb_Hash_Fn comb_hash_fn_base;
      typedef typename rebind_traits<_Alloc, Key>::const_reference
	key_const_reference;

      ranged_hash_fn(size_type);

      ranged_hash_fn(size_type, const Hash_Fn&);

      ranged_hash_fn(size_type, const Hash_Fn&, const Comb_Hash_Fn&);

      void
      swap(PB_DS_CLASS_C_DEC&);

      void
      notify_resized(size_type);

      inline comp_hash
      operator()(key_const_reference) const;

      inline comp_hash
      operator()(key_const_reference, size_type) const;
    };

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_hash_fn(size_type size)
    { Comb_Hash_Fn::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_hash_fn(size_type size, const Hash_Fn& r_hash_fn) :
      Hash_Fn(r_hash_fn)
    { Comb_Hash_Fn::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_hash_fn(size_type size, const Hash_Fn& r_hash_fn, 
		   const Comb_Hash_Fn& r_comb_hash_fn) 
    : Hash_Fn(r_hash_fn), Comb_Hash_Fn(r_comb_hash_fn)
    { comb_hash_fn_base::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    swap(PB_DS_CLASS_C_DEC& other)
    {
      comb_hash_fn_base::swap(other);
      std::swap((Hash_Fn& )(*this), (Hash_Fn& )other);
    }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    notify_resized(size_type size)
    { comb_hash_fn_base::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    inline typename PB_DS_CLASS_C_DEC::comp_hash
    PB_DS_CLASS_C_DEC::
    operator()(key_const_reference r_key) const
    {
      const size_type hash = hash_fn_base::operator()(r_key);
      return std::make_pair(comb_hash_fn_base::operator()(hash), hash);
    }

    PB_DS_CLASS_T_DEC
    inline typename PB_DS_CLASS_C_DEC::comp_hash
    PB_DS_CLASS_C_DEC::
    operator()
#ifdef _GLIBCXX_DEBUG
      (key_const_reference r_key, size_type hash) const
#else 
      (key_const_reference /*r_key*/, size_type hash) const
#endif
    {
      _GLIBCXX_DEBUG_ASSERT(hash == hash_fn_base::operator()(r_key));
      return std::make_pair(comb_hash_fn_base::operator()(hash), hash);
    }

#undef PB_DS_CLASS_T_DEC
#undef PB_DS_CLASS_C_DEC

#define PB_DS_CLASS_T_DEC \
    template<typename Key, typename _Alloc, typename Comb_Hash_Fn>

#define PB_DS_CLASS_C_DEC \
    ranged_hash_fn<Key,	null_type, _Alloc, Comb_Hash_Fn, false>

    /**
     * Specialization 3
     * The client does not supply a hash function (by specifying
     * null_type as the Hash_Fn parameter), and requests that hash
     * values not be stored.
     **/
    template<typename Key, typename _Alloc, typename Comb_Hash_Fn>
    class ranged_hash_fn<Key, null_type, _Alloc, Comb_Hash_Fn, false> 
    : public Comb_Hash_Fn
    {
    protected:
      typedef typename _Alloc::size_type size_type;
      typedef Comb_Hash_Fn comb_hash_fn_base;

      ranged_hash_fn(size_type);

      ranged_hash_fn(size_type, const Comb_Hash_Fn&);

      ranged_hash_fn(size_type, const null_type&, const Comb_Hash_Fn&);

      void
      swap(PB_DS_CLASS_C_DEC&);
    };

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_hash_fn(size_type size)
    { Comb_Hash_Fn::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_hash_fn(size_type size, const Comb_Hash_Fn& r_comb_hash_fn) :
      Comb_Hash_Fn(r_comb_hash_fn)
    { }

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_hash_fn(size_type size, const null_type& r_null_type, 
		   const Comb_Hash_Fn& r_comb_hash_fn) 
    : Comb_Hash_Fn(r_comb_hash_fn)
    { }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    swap(PB_DS_CLASS_C_DEC& other)
    { comb_hash_fn_base::swap(other); }

#undef PB_DS_CLASS_T_DEC
#undef PB_DS_CLASS_C_DEC

#define PB_DS_CLASS_T_DEC \
    template<typename Key, typename _Alloc, typename Comb_Hash_Fn>

#define PB_DS_CLASS_C_DEC \
    ranged_hash_fn<Key,	null_type, _Alloc, Comb_Hash_Fn, true>

    /**
     * Specialization 4
     * The client does not supply a hash function (by specifying
     * null_type as the Hash_Fn parameter), and requests that hash
     * values be stored.
     **/
    template<typename Key, typename _Alloc, typename Comb_Hash_Fn>
    class ranged_hash_fn<Key, null_type, _Alloc, Comb_Hash_Fn, true> 
    : public Comb_Hash_Fn
    {
    protected:
      typedef typename _Alloc::size_type size_type;
      typedef Comb_Hash_Fn comb_hash_fn_base;

      ranged_hash_fn(size_type);

      ranged_hash_fn(size_type, const Comb_Hash_Fn&);

      ranged_hash_fn(size_type, const null_type&, const Comb_Hash_Fn&);

      void
      swap(PB_DS_CLASS_C_DEC&);
    };

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_hash_fn(size_type size)
    { Comb_Hash_Fn::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_hash_fn(size_type size, const Comb_Hash_Fn& r_comb_hash_fn) 
    : Comb_Hash_Fn(r_comb_hash_fn)
    { }

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_hash_fn(size_type size, const null_type& r_null_type, 
		   const Comb_Hash_Fn& r_comb_hash_fn) 
    : Comb_Hash_Fn(r_comb_hash_fn)
    { }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    swap(PB_DS_CLASS_C_DEC& other)
    { comb_hash_fn_base::swap(other); }

#undef PB_DS_CLASS_T_DEC
#undef PB_DS_CLASS_C_DEC

  } // namespace detail
} // namespace __gnu_pbds

#endif 
