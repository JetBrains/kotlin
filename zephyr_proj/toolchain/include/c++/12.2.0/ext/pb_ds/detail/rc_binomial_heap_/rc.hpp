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
 * @file rc_binomial_heap_/rc.hpp
 * Contains a redundant (binary counter).
 */

#ifndef PB_DS_RC_HPP
#define PB_DS_RC_HPP

namespace __gnu_pbds
{
  namespace detail
  {
    /// Redundant binary counter.
    template<typename _Node, typename _Alloc>
    class rc
    {
    private:
      typedef _Alloc 					 allocator_type;
      typedef typename allocator_type::size_type 	 size_type;
      typedef _Node 					 node;

      typedef typename rebind_traits<_Alloc, node>::pointer node_pointer;


      typedef typename rebind_traits<_Alloc, node_pointer>::pointer
	entry_pointer;
      typedef typename rebind_traits<_Alloc, node_pointer>::const_pointer
	entry_const_pointer;

      enum
	{
	  max_entries = sizeof(size_type) << 3
	};

    public:
      typedef node_pointer 				 entry;
      typedef entry_const_pointer 			 const_iterator;

      rc();

      rc(const rc&);

      inline void
      swap(rc&);

      inline void
      push(entry);

      inline node_pointer
      top() const;

      inline void
      pop();

      _GLIBCXX_NODISCARD inline bool
      empty() const;

      inline size_type
      size() const;

      void
      clear();

      const const_iterator
      begin() const;

      const const_iterator
      end() const;

#ifdef _GLIBCXX_DEBUG
      void
      assert_valid(const char*, int) const;
#endif

#ifdef PB_DS_RC_BINOMIAL_HEAP_TRACE_
      void
      trace() const;
#endif

    private:
      node_pointer 	m_a_entries[max_entries];
      size_type 	m_over_top;
    };

    template<typename _Node, typename _Alloc>
    rc<_Node, _Alloc>::
    rc() : m_over_top(0)
    { PB_DS_ASSERT_VALID((*this)) }

    template<typename _Node, typename _Alloc>
    rc<_Node, _Alloc>::
    rc(const rc<_Node, _Alloc>& other) : m_over_top(0)
    { PB_DS_ASSERT_VALID((*this)) }

    template<typename _Node, typename _Alloc>
    inline void
    rc<_Node, _Alloc>::
    swap(rc<_Node, _Alloc>& other)
    {
      PB_DS_ASSERT_VALID((*this))
      PB_DS_ASSERT_VALID(other)

      const size_type over_top = std::max(m_over_top, other.m_over_top);

      for (size_type i = 0; i < over_top; ++i)
	std::swap(m_a_entries[i], other.m_a_entries[i]);

      std::swap(m_over_top, other.m_over_top);
      PB_DS_ASSERT_VALID((*this))
      PB_DS_ASSERT_VALID(other)
     }

    template<typename _Node, typename _Alloc>
    inline void
    rc<_Node, _Alloc>::
    push(entry p_nd)
    {
      PB_DS_ASSERT_VALID((*this))
      _GLIBCXX_DEBUG_ASSERT(m_over_top < max_entries);
      m_a_entries[m_over_top++] = p_nd;
      PB_DS_ASSERT_VALID((*this))
    }

    template<typename _Node, typename _Alloc>
    inline void
    rc<_Node, _Alloc>::
    pop()
    {
      PB_DS_ASSERT_VALID((*this))
      _GLIBCXX_DEBUG_ASSERT(!empty());
      --m_over_top;
      PB_DS_ASSERT_VALID((*this))
    }

    template<typename _Node, typename _Alloc>
    inline typename rc<_Node, _Alloc>::node_pointer
    rc<_Node, _Alloc>::
    top() const
    {
      PB_DS_ASSERT_VALID((*this))
      _GLIBCXX_DEBUG_ASSERT(!empty());
      return *(m_a_entries + m_over_top - 1);
    }

    template<typename _Node, typename _Alloc>
    _GLIBCXX_NODISCARD inline bool
    rc<_Node, _Alloc>::
    empty() const
    {
      PB_DS_ASSERT_VALID((*this))
      return m_over_top == 0;
    }

    template<typename _Node, typename _Alloc>
    inline typename rc<_Node, _Alloc>::size_type
    rc<_Node, _Alloc>::
    size() const
    { return m_over_top; }

    template<typename _Node, typename _Alloc>
    void
    rc<_Node, _Alloc>::
    clear()
    {
      PB_DS_ASSERT_VALID((*this))
      m_over_top = 0;
      PB_DS_ASSERT_VALID((*this))
    }

    template<typename _Node, typename _Alloc>
    const typename rc<_Node, _Alloc>::const_iterator
    rc<_Node, _Alloc>::
    begin() const
    { return& m_a_entries[0]; }

    template<typename _Node, typename _Alloc>
    const typename rc<_Node, _Alloc>::const_iterator
    rc<_Node, _Alloc>::
    end() const
    { return& m_a_entries[m_over_top]; }

#ifdef _GLIBCXX_DEBUG
    template<typename _Node, typename _Alloc>
    void
    rc<_Node, _Alloc>::
    assert_valid(const char* __file, int __line) const
    { PB_DS_DEBUG_VERIFY(m_over_top < max_entries); }
#endif

#ifdef PB_DS_RC_BINOMIAL_HEAP_TRACE_
    template<typename _Node, typename _Alloc>
    void
    rc<_Node, _Alloc>::
    trace() const
    {
      std::cout << "rc" << std::endl;
      for (size_type i = 0; i < m_over_top; ++i)
	std::cerr << m_a_entries[i] << std::endl;
      std::cout << std::endl;
    }
#endif
} // namespace detail
} // namespace __gnu_pbds

#endif
