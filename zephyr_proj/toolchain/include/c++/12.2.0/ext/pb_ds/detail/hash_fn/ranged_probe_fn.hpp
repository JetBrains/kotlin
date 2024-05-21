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
 * @file ranged_probe_fn.hpp
 * Contains a unified ranged probe functor, allowing the probe tables to deal with
 *    a single class for ranged probeing.
 */

#ifndef PB_DS_RANGED_PROBE_FN_HPP
#define PB_DS_RANGED_PROBE_FN_HPP

#include <utility>
#include <debug/debug.h>

namespace __gnu_pbds
{
  namespace detail
  {
    /// Primary template.
    template<typename Key, typename Hash_Fn, typename _Alloc,
	     typename Comb_Probe_Fn, typename Probe_Fn, bool Store_Hash>
    class ranged_probe_fn;

#define PB_DS_CLASS_T_DEC \
    template<typename Key, typename Hash_Fn, typename _Alloc, \
	     typename Comb_Probe_Fn, typename Probe_Fn>

#define PB_DS_CLASS_C_DEC \
    ranged_probe_fn<Key, Hash_Fn, _Alloc, Comb_Probe_Fn, Probe_Fn, false>

    /**
     * Specialization 1     
     * The client supplies a probe function and a ranged probe
     * function, and requests that hash values not be stored.
     **/
    template<typename Key, typename Hash_Fn, typename _Alloc,
	     typename Comb_Probe_Fn, typename Probe_Fn>
    class ranged_probe_fn<Key, Hash_Fn, _Alloc, Comb_Probe_Fn,
			  Probe_Fn, false> 
    : public Hash_Fn, public Comb_Probe_Fn, public Probe_Fn
    {
    protected:
      typedef typename _Alloc::size_type size_type;
      typedef Comb_Probe_Fn comb_probe_fn_base;
      typedef Hash_Fn hash_fn_base;
      typedef Probe_Fn probe_fn_base;
      typedef typename rebind_traits<_Alloc, Key>::const_reference
	key_const_reference;

      ranged_probe_fn(size_type);

      ranged_probe_fn(size_type, const Hash_Fn&);

      ranged_probe_fn(size_type, const Hash_Fn&, const Comb_Probe_Fn&);

      ranged_probe_fn(size_type, const Hash_Fn&, const Comb_Probe_Fn&, 
		      const Probe_Fn&);

      void
      swap(PB_DS_CLASS_C_DEC&);

      void
      notify_resized(size_type);

      inline size_type
      operator()(key_const_reference) const;

      inline size_type
      operator()(key_const_reference, size_type, size_type) const;
    };

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_probe_fn(size_type size)
    { Comb_Probe_Fn::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_probe_fn(size_type size, const Hash_Fn& r_hash_fn) 
    : Hash_Fn(r_hash_fn)
    { Comb_Probe_Fn::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_probe_fn(size_type size, const Hash_Fn& r_hash_fn, 
		    const Comb_Probe_Fn& r_comb_probe_fn) 
    : Hash_Fn(r_hash_fn), Comb_Probe_Fn(r_comb_probe_fn)
    { comb_probe_fn_base::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_probe_fn(size_type size, const Hash_Fn& r_hash_fn, 
		    const Comb_Probe_Fn& r_comb_probe_fn, 
		    const Probe_Fn& r_probe_fn) 
    : Hash_Fn(r_hash_fn), Comb_Probe_Fn(r_comb_probe_fn), Probe_Fn(r_probe_fn)
    { comb_probe_fn_base::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    swap(PB_DS_CLASS_C_DEC& other)
    {
      comb_probe_fn_base::swap(other);
      std::swap((Hash_Fn& )(*this), (Hash_Fn&)other);
    }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    notify_resized(size_type size)
    { comb_probe_fn_base::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    inline typename PB_DS_CLASS_C_DEC::size_type
    PB_DS_CLASS_C_DEC::
    operator()(key_const_reference r_key) const
    { return comb_probe_fn_base::operator()(hash_fn_base::operator()(r_key)); }

    PB_DS_CLASS_T_DEC
    inline typename PB_DS_CLASS_C_DEC::size_type
    PB_DS_CLASS_C_DEC::
    operator()(key_const_reference, size_type hash, size_type i) const
    {
      return comb_probe_fn_base::operator()(hash + probe_fn_base::operator()(i));
    }

#undef PB_DS_CLASS_T_DEC
#undef PB_DS_CLASS_C_DEC

#define PB_DS_CLASS_T_DEC \
    template<typename Key, typename Hash_Fn, typename _Alloc, \
	     typename Comb_Probe_Fn, typename Probe_Fn>

#define PB_DS_CLASS_C_DEC \
    ranged_probe_fn<Key, Hash_Fn, _Alloc, Comb_Probe_Fn, Probe_Fn, true>

    /**
     * Specialization 2- The client supplies a probe function and a ranged
     *    probe function, and requests that hash values not be stored.
     **/
    template<typename Key, typename Hash_Fn, typename _Alloc,
	     typename Comb_Probe_Fn, typename Probe_Fn>
    class ranged_probe_fn<Key, Hash_Fn, _Alloc, Comb_Probe_Fn, 
			  Probe_Fn, true> 
    : public Hash_Fn, public Comb_Probe_Fn, public Probe_Fn
    {
    protected:
      typedef typename _Alloc::size_type size_type;
      typedef std::pair<size_type, size_type> comp_hash;
      typedef Comb_Probe_Fn comb_probe_fn_base;
      typedef Hash_Fn hash_fn_base;
      typedef Probe_Fn probe_fn_base;
      typedef typename rebind_traits<_Alloc, Key>::const_reference
	key_const_reference;

      ranged_probe_fn(size_type);

      ranged_probe_fn(size_type, const Hash_Fn&);

      ranged_probe_fn(size_type, const Hash_Fn&, 
		      const Comb_Probe_Fn&);

      ranged_probe_fn(size_type, const Hash_Fn&, const Comb_Probe_Fn&, 
		      const Probe_Fn&);

      void
      swap(PB_DS_CLASS_C_DEC&);

      void
      notify_resized(size_type);

      inline comp_hash
      operator()(key_const_reference) const;

      inline size_type
      operator()(key_const_reference, size_type, size_type) const;

      inline size_type
      operator()(key_const_reference, size_type) const;
    };

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_probe_fn(size_type size)
    { Comb_Probe_Fn::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_probe_fn(size_type size, const Hash_Fn& r_hash_fn) 
    : Hash_Fn(r_hash_fn)
    { Comb_Probe_Fn::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_probe_fn(size_type size, const Hash_Fn& r_hash_fn, 
		    const Comb_Probe_Fn& r_comb_probe_fn) 
    : Hash_Fn(r_hash_fn), Comb_Probe_Fn(r_comb_probe_fn)
    { comb_probe_fn_base::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ranged_probe_fn(size_type size, const Hash_Fn& r_hash_fn, 
		    const Comb_Probe_Fn& r_comb_probe_fn, 
		    const Probe_Fn& r_probe_fn) 
    : Hash_Fn(r_hash_fn), Comb_Probe_Fn(r_comb_probe_fn), Probe_Fn(r_probe_fn)
    { comb_probe_fn_base::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    swap(PB_DS_CLASS_C_DEC& other)
    {
      comb_probe_fn_base::swap(other);
      std::swap((Hash_Fn& )(*this), (Hash_Fn& )other);
    }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    notify_resized(size_type size)
    { comb_probe_fn_base::notify_resized(size); }

    PB_DS_CLASS_T_DEC
    inline typename PB_DS_CLASS_C_DEC::comp_hash
    PB_DS_CLASS_C_DEC::
    operator()(key_const_reference r_key) const
    {
      const size_type hash = hash_fn_base::operator()(r_key);
      return std::make_pair(comb_probe_fn_base::operator()(hash), hash);
    }

    PB_DS_CLASS_T_DEC
    inline typename PB_DS_CLASS_C_DEC::size_type
    PB_DS_CLASS_C_DEC::
    operator()(key_const_reference, size_type hash, size_type i) const
    {
      return comb_probe_fn_base::operator()(hash + probe_fn_base::operator()(i));
    }

    PB_DS_CLASS_T_DEC
    inline typename PB_DS_CLASS_C_DEC::size_type
    PB_DS_CLASS_C_DEC::
    operator()
#ifdef _GLIBCXX_DEBUG
      (key_const_reference r_key, size_type hash) const
#else 
      (key_const_reference /*r_key*/, size_type hash) const
#endif 
    {
      _GLIBCXX_DEBUG_ASSERT(hash == hash_fn_base::operator()(r_key));
      return hash;
    }

#undef PB_DS_CLASS_T_DEC
#undef PB_DS_CLASS_C_DEC

    /**
     * Specialization 3 and 4
     * The client does not supply a hash function or probe function,
     * and requests that hash values not be stored.
     **/
    template<typename Key, typename _Alloc, typename Comb_Probe_Fn>
    class ranged_probe_fn<Key, null_type, _Alloc, Comb_Probe_Fn, 
			  null_type, false> 
    : public Comb_Probe_Fn
    {
    protected:
      typedef typename _Alloc::size_type size_type;
      typedef Comb_Probe_Fn comb_probe_fn_base;
      typedef typename rebind_traits<_Alloc, Key>::const_reference
	key_const_reference;

      ranged_probe_fn(size_type size)
      { Comb_Probe_Fn::notify_resized(size); }

      ranged_probe_fn(size_type, const Comb_Probe_Fn& r_comb_probe_fn)
      : Comb_Probe_Fn(r_comb_probe_fn)
      { }

      ranged_probe_fn(size_type, const null_type&, 
		      const Comb_Probe_Fn& r_comb_probe_fn, 
		      const null_type&)
      : Comb_Probe_Fn(r_comb_probe_fn)
      { }

      void
      swap(ranged_probe_fn& other)
      { comb_probe_fn_base::swap(other); }
    };
  } // namespace detail
} // namespace __gnu_pbds

#endif

