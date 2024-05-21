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
 * @file detail/debug_map_base.hpp
 * Contains a debug-mode base for all maps.
 */

#ifndef PB_DS_DEBUG_MAP_BASE_HPP
#define PB_DS_DEBUG_MAP_BASE_HPP

#ifdef _GLIBCXX_DEBUG

#include <list>
#include <utility>
#include <cstdlib>
#include <iostream>
#include <ext/throw_allocator.h>
#include <debug/debug.h>

namespace __gnu_pbds
{
  namespace detail
  {
    // Need std::pair ostream extractor.
    template<typename _CharT, typename _Traits, typename _Tp1, typename _Tp2>
    inline std::basic_ostream<_CharT, _Traits>&
    operator<<(std::basic_ostream<_CharT, _Traits>& __out,
	       const std::pair<_Tp1, _Tp2>& p)
    { return (__out << '(' << p.first << ',' << p.second << ')'); }

#define PB_DS_CLASS_T_DEC \
    template<typename Key, typename Eq_Fn, typename Const_Key_Reference>

#define PB_DS_CLASS_C_DEC \
    debug_map_base<Key, Eq_Fn, Const_Key_Reference>

    /// Debug base class.
    template<typename Key, typename Eq_Fn, typename Const_Key_Reference>
    class debug_map_base
    {
    private:
      typedef Const_Key_Reference 			key_const_reference;
      typedef std::_GLIBCXX_STD_C::list<Key> 		key_repository;
      typedef typename key_repository::size_type       	size_type;
      typedef typename key_repository::iterator	       	iterator;
      typedef typename key_repository::const_iterator  	const_iterator;

    protected:
      debug_map_base();

      debug_map_base(const PB_DS_CLASS_C_DEC&);

      ~debug_map_base();

      inline void
      insert_new(key_const_reference);

      inline void
      erase_existing(key_const_reference);

      void
      clear();

      inline void
      check_key_exists(key_const_reference, const char*, int) const;

      inline void
      check_key_does_not_exist(key_const_reference, const char*, int) const;

      inline void
      check_size(size_type, const char*, int) const;

      void
      swap(PB_DS_CLASS_C_DEC&);

      template<typename Cmp_Fn>
      void
      split(key_const_reference, Cmp_Fn, PB_DS_CLASS_C_DEC&);

      void
      join(PB_DS_CLASS_C_DEC&, bool with_cleanup = true);

    private:
      void
      assert_valid(const char*, int) const;

      const_iterator
      find(key_const_reference) const;

      iterator
      find(key_const_reference);

      key_repository 	m_keys;
      Eq_Fn 		m_eq;
    };

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    debug_map_base()
    { PB_DS_ASSERT_VALID((*this)) }

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    debug_map_base(const PB_DS_CLASS_C_DEC& other)
    : m_keys(other.m_keys), m_eq(other.m_eq)
    { PB_DS_ASSERT_VALID((*this)) }

    PB_DS_CLASS_T_DEC
    PB_DS_CLASS_C_DEC::
    ~debug_map_base()
    { PB_DS_ASSERT_VALID((*this)) }

    PB_DS_CLASS_T_DEC
    inline void
    PB_DS_CLASS_C_DEC::
    insert_new(key_const_reference r_key)
    {
      PB_DS_ASSERT_VALID((*this))

      if (find(r_key) != m_keys.end())
	{
	  std::cerr << "insert_new key already present " << r_key << std::endl;
	  std::abort();
	}

      __try
	{
	  m_keys.push_back(r_key);
	}
      __catch(...)
	{
	  std::cerr << "insert_new " << r_key << std::endl;
	  std::abort();
	}

      PB_DS_ASSERT_VALID((*this))
    }

    PB_DS_CLASS_T_DEC
    inline void
    PB_DS_CLASS_C_DEC::
    erase_existing(key_const_reference r_key)
    {
      PB_DS_ASSERT_VALID((*this))
      iterator it = find(r_key);
      if (it == m_keys.end())
	{
	  std::cerr << "erase_existing" << r_key << std::endl;
	  std::abort();
	}
      m_keys.erase(it);
      PB_DS_ASSERT_VALID((*this))
    }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    clear()
    {
      PB_DS_ASSERT_VALID((*this))
      m_keys.clear();
      PB_DS_ASSERT_VALID((*this))
    }

    PB_DS_CLASS_T_DEC
    inline void
    PB_DS_CLASS_C_DEC::
    check_key_exists(key_const_reference r_key,
		     const char* __file, int __line) const
    {
      assert_valid(__file, __line);
      if (find(r_key) == m_keys.end())
	{
	  std::cerr << __file << ':' << __line << ": check_key_exists "
		    << r_key << std::endl;
	  std::abort();
	}
    }

    PB_DS_CLASS_T_DEC
    inline void
    PB_DS_CLASS_C_DEC::
    check_key_does_not_exist(key_const_reference r_key,
			     const char* __file, int __line) const
    {
      assert_valid(__file, __line);
      if (find(r_key) != m_keys.end())
	{
	  using std::cerr;
	  using std::endl;
	  cerr << __file << ':' << __line << ": check_key_does_not_exist "
	       << r_key << endl;
	  std::abort();
	}
    }

    PB_DS_CLASS_T_DEC
    inline void
    PB_DS_CLASS_C_DEC::
    check_size(size_type size, const char* __file, int __line) const
    {
      assert_valid(__file, __line);
      const size_type keys_size = m_keys.size();
      if (size != keys_size)
	{
	  std::cerr << __file << ':' << __line << ": check_size "
		    << size << " != " << keys_size << std::endl;
	  std::abort();
	}
     }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    swap(PB_DS_CLASS_C_DEC& other)
    {
      PB_DS_ASSERT_VALID((*this))
      m_keys.swap(other.m_keys);
      std::swap(m_eq, other.m_eq);
      PB_DS_ASSERT_VALID((*this))
    }

    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::const_iterator
    PB_DS_CLASS_C_DEC::
    find(key_const_reference r_key) const
    {
      PB_DS_ASSERT_VALID((*this))
      typedef const_iterator iterator_type;
      for (iterator_type it = m_keys.begin(); it != m_keys.end(); ++it)
	if (m_eq(*it, r_key))
	  return it;
      return m_keys.end();
    }

    PB_DS_CLASS_T_DEC
    typename PB_DS_CLASS_C_DEC::iterator
    PB_DS_CLASS_C_DEC::
    find(key_const_reference r_key)
    {
      PB_DS_ASSERT_VALID((*this))
      iterator it = m_keys.begin();
      while (it != m_keys.end())
	{
	  if (m_eq(*it, r_key))
	    return it;
	  ++it;
	}
      return it;
     }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    assert_valid(const char* __file, int __line) const
    {
      const_iterator prime_it = m_keys.begin();
      while (prime_it != m_keys.end())
	{
	  const_iterator sec_it = prime_it;
	  ++sec_it;
	  while (sec_it != m_keys.end())
	    {
	      PB_DS_DEBUG_VERIFY(!m_eq(*sec_it, *prime_it));
	      PB_DS_DEBUG_VERIFY(!m_eq(*prime_it, *sec_it));
	      ++sec_it;
	    }
	  ++prime_it;
	}
    }

    PB_DS_CLASS_T_DEC
    template<typename Cmp_Fn>
    void
    PB_DS_CLASS_C_DEC::
    split(key_const_reference r_key, Cmp_Fn cmp_fn, PB_DS_CLASS_C_DEC& other)
    {
      other.clear();
      iterator it = m_keys.begin();
      while (it != m_keys.end())
	if (cmp_fn(r_key, *it))
	  {
	    other.insert_new(*it);
	    it = m_keys.erase(it);
	  }
	else
	  ++it;
    }

    PB_DS_CLASS_T_DEC
    void
    PB_DS_CLASS_C_DEC::
    join(PB_DS_CLASS_C_DEC& other, bool with_cleanup)
    {
      iterator it = other.m_keys.begin();
      while (it != other.m_keys.end())
	{
	  insert_new(*it);
	  if (with_cleanup)
	    it = other.m_keys.erase(it);
	  else
	    ++it;
	}
      _GLIBCXX_DEBUG_ASSERT(!with_cleanup || other.m_keys.empty());
    }

#undef PB_DS_CLASS_T_DEC
#undef PB_DS_CLASS_C_DEC

} // namespace detail
} // namespace __gnu_pbds


#endif

#endif
