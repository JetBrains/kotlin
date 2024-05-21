// Debugging iterator implementation (out of line) -*- C++ -*-

// Copyright (C) 2003-2022 Free Software Foundation, Inc.
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

/** @file debug/safe_iterator.tcc
 *  This file is a GNU debug extension to the Standard C++ Library.
 */

#ifndef _GLIBCXX_DEBUG_SAFE_ITERATOR_TCC
#define _GLIBCXX_DEBUG_SAFE_ITERATOR_TCC 1

#include <bits/stl_algobase.h>

namespace __gnu_debug
{
  template<typename _Iterator, typename _Sequence, typename _Category>
    typename _Distance_traits<_Iterator>::__type
    _Safe_iterator<_Iterator, _Sequence, _Category>::
    _M_get_distance_from_begin() const
    {
      typedef _Sequence_traits<_Sequence> _SeqTraits;

      // No need to consider before_begin as this function is only used in
      // _M_can_advance which won't be used for forward_list iterators.
      if (_M_is_begin())
	return std::make_pair(0, __dp_exact);

      if (_M_is_end())
	return _SeqTraits::_S_size(*_M_get_sequence());

      typename _Distance_traits<_Iterator>::__type __res
	= __get_distance(_M_get_sequence()->_M_base().begin(), base());

      if (__res.second == __dp_equality)
	return std::make_pair(1, __dp_sign);

      return __res;
    }

  template<typename _Iterator, typename _Sequence, typename _Category>
    typename _Distance_traits<_Iterator>::__type
    _Safe_iterator<_Iterator, _Sequence, _Category>::
    _M_get_distance_to_end() const
    {
      typedef _Sequence_traits<_Sequence> _SeqTraits;

      // No need to consider before_begin as this function is only used in
      // _M_can_advance which won't be used for forward_list iterators.
      if (_M_is_begin())
	return _SeqTraits::_S_size(*_M_get_sequence());

      if (_M_is_end())
	return std::make_pair(0, __dp_exact);

      typename _Distance_traits<_Iterator>::__type __res
	= __get_distance(base(), _M_get_sequence()->_M_base().end());

      if (__res.second == __dp_equality)
	return std::make_pair(1, __dp_sign);

      return __res;
    }

  template<typename _Iterator, typename _Sequence, typename _Category>
    bool
    _Safe_iterator<_Iterator, _Sequence, _Category>::
    _M_can_advance(difference_type __n, bool __strict) const
    {
      if (this->_M_singular())
	return false;

      if (__n == 0)
	return true;

      std::pair<difference_type, _Distance_precision> __dist = __n < 0
	? _M_get_distance_from_begin()
	: _M_get_distance_to_end();

      if (__n < 0)
	__n = -__n;

      return __dist.second > __dp_sign
	? __dist.first >= __n
	: !__strict && __dist.first > 0;
    }

  template<typename _Iterator, typename _Sequence, typename _Category>
    template<typename _Diff>
      bool
      _Safe_iterator<_Iterator, _Sequence, _Category>::
      _M_can_advance(const std::pair<_Diff, _Distance_precision>& __dist,
		     int __way) const
      {
	return __dist.second == __dp_exact
	  ? _M_can_advance(__way * __dist.first)
	  : _M_can_advance(__way * (__dist.first == 0
				    ? 0
				    : __dist.first < 0 ? -1 : 1));
      }

  template<typename _Iterator, typename _Sequence, typename _Category>
    typename _Distance_traits<_Iterator>::__type
    _Safe_iterator<_Iterator, _Sequence, _Category>::
    _M_get_distance_to(const _Safe_iterator& __rhs) const
    {
      typedef typename _Distance_traits<_Iterator>::__type _Dist;
      typedef _Sequence_traits<_Sequence> _SeqTraits;

      _Dist __base_dist = __get_distance(this->base(), __rhs.base());
      if (__base_dist.second == __dp_exact)
	return __base_dist;

      _Dist __seq_dist = _SeqTraits::_S_size(*this->_M_get_sequence());
      if (this->_M_is_before_begin())
	{
	  if (__rhs._M_is_begin())
	    return std::make_pair(1, __dp_exact);

	  return __seq_dist.second == __dp_exact
	    ? std::make_pair(__seq_dist.first + 1, __dp_exact)
	    : __seq_dist;
	}

      if (this->_M_is_begin())
	{
	  if (__rhs._M_is_before_begin())
	    return std::make_pair(-1, __dp_exact);

	  if (__rhs._M_is_end())
	    return __seq_dist;

	  return std::make_pair(__seq_dist.first,
				__seq_dist.second == __dp_exact
				? __dp_sign_max_size : __seq_dist.second);
	}

      if (this->_M_is_end())
	{
	  if (__rhs._M_is_before_begin())
	    return __seq_dist.second == __dp_exact
	      ? std::make_pair(-__seq_dist.first - 1, __dp_exact)
	      : std::make_pair(-__seq_dist.first, __dp_sign);

	  if (__rhs._M_is_begin())
	    return std::make_pair(-__seq_dist.first, __seq_dist.second);

	  return std::make_pair(-__seq_dist.first,
				__seq_dist.second == __dp_exact
				? __dp_sign_max_size : __seq_dist.second);
	}

      if (__rhs._M_is_before_begin())
	return __seq_dist.second == __dp_exact
	  ? std::make_pair(__seq_dist.first - 1, __dp_exact)
	  : std::make_pair(-__seq_dist.first, __dp_sign);

      if (__rhs._M_is_begin())
	return std::make_pair(-__seq_dist.first,
			      __seq_dist.second == __dp_exact
			      ? __dp_sign_max_size : __seq_dist.second);

      if (__rhs._M_is_end())
	return std::make_pair(__seq_dist.first,
			      __seq_dist.second == __dp_exact
			      ? __dp_sign_max_size : __seq_dist.second);

      return std::make_pair(1, __dp_equality);
    }

  template<typename _Iterator, typename _Sequence, typename _Category>
    bool
    _Safe_iterator<_Iterator, _Sequence, _Category>::
    _M_valid_range(const _Safe_iterator& __rhs,
		   std::pair<difference_type, _Distance_precision>& __dist,
		   bool __check_dereferenceable) const
    {
      if (_M_singular() || __rhs._M_singular() || !_M_can_compare(__rhs))
	return false;

      /* Determine iterators order */
      __dist = _M_get_distance_to(__rhs);
      if (__dist.second != __dp_equality)
	{
	  // If range is not empty first iterator must be dereferenceable.
	  return __dist.first == 0
	    || (__dist.first > 0
		&& (!__check_dereferenceable || _M_dereferenceable()));
	}

      // Assume that this is a valid range; we can't check anything else.
      return true;
    }

  template<typename _Iterator, typename _Sequence>
    bool
    _Safe_iterator<_Iterator, _Sequence, std::random_access_iterator_tag>::
    _M_valid_range(const _Safe_iterator& __rhs,
		   std::pair<difference_type,
			     _Distance_precision>& __dist) const
    {
      if (this->_M_singular() || __rhs._M_singular()
	  || !this->_M_can_compare(__rhs))
	return false;

      /* Determine iterators order */
      __dist = std::make_pair(__rhs.base() - this->base(), __dp_exact);

      // If range is not empty first iterator must be dereferenceable.
      return __dist.first == 0
	|| (__dist.first > 0 && this->_M_dereferenceable());
    }
} // namespace __gnu_debug

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  template<typename _Ite, typename _Seq>
    _Ite
    __niter_base(const ::__gnu_debug::_Safe_iterator<_Ite, _Seq,
		 std::random_access_iterator_tag>& __it)
    { return __it.base(); }

  template<bool _IsMove,
	   typename _Ite, typename _Seq, typename _Cat, typename _OI>
    _OI
    __copy_move_a(
      const ::__gnu_debug::_Safe_iterator<_Ite, _Seq, _Cat>& __first,
      const ::__gnu_debug::_Safe_iterator<_Ite, _Seq, _Cat>& __last,
      _OI __result)
    {
      typename ::__gnu_debug::_Distance_traits<_Ite>::__type __dist;
      __glibcxx_check_valid_range2(__first, __last, __dist);
      __glibcxx_check_can_increment_dist(__result, __dist, 1);

      if (__dist.second > ::__gnu_debug::__dp_equality)
	return std::__copy_move_a<_IsMove>(__first.base(), __last.base(),
					   __result);

      return std::__copy_move_a1<_IsMove>(__first, __last, __result);
    }

  template<bool _IsMove,
	   typename _II, typename _Ite, typename _Seq, typename _Cat>
    __gnu_debug::_Safe_iterator<_Ite, _Seq, _Cat>
    __copy_move_a(_II __first, _II __last,
      const ::__gnu_debug::_Safe_iterator<_Ite, _Seq, _Cat>& __result)
    {
      typename ::__gnu_debug::_Distance_traits<_II>::__type __dist;
      __glibcxx_check_valid_range2(__first, __last, __dist);
      __glibcxx_check_can_increment_dist(__result, __dist, 1);

      if (__dist.second > ::__gnu_debug::__dp_sign
	  && __result._M_can_advance(__dist.first, true))
	return ::__gnu_debug::_Safe_iterator<_Ite, _Seq, _Cat>(
		std::__copy_move_a<_IsMove>(__first, __last, __result.base()),
		__result._M_sequence);

      return std::__copy_move_a1<_IsMove>(__first, __last, __result);
    }

  template<bool _IsMove,
	   typename _IIte, typename _ISeq, typename _ICat,
	   typename _OIte, typename _OSeq, typename _OCat>
    ::__gnu_debug::_Safe_iterator<_OIte, _OSeq, _OCat>
    __copy_move_a(
      const ::__gnu_debug::_Safe_iterator<_IIte, _ISeq, _ICat>& __first,
      const ::__gnu_debug::_Safe_iterator<_IIte, _ISeq, _ICat>& __last,
      const ::__gnu_debug::_Safe_iterator<_OIte, _OSeq, _OCat>& __result)
    {
      typename ::__gnu_debug::_Distance_traits<_IIte>::__type __dist;
      __glibcxx_check_valid_range2(__first, __last, __dist);
      __glibcxx_check_can_increment_dist(__result, __dist, 1);

      if (__dist.second > ::__gnu_debug::__dp_equality)
	{
	  if (__dist.second > ::__gnu_debug::__dp_sign
	      && __result._M_can_advance(__dist.first, true))
	    return ::__gnu_debug::_Safe_iterator<_OIte, _OSeq, _OCat>(
	      std::__copy_move_a<_IsMove>(__first.base(), __last.base(),
					  __result.base()),
	      __result._M_sequence);

	  return std::__copy_move_a<_IsMove>(__first.base(), __last.base(),
					     __result);
	}

      return std::__copy_move_a1<_IsMove>(__first, __last, __result);
    }

  template<bool _IsMove,
	   typename _Ite, typename _Seq, typename _Cat, typename _OI>
    _OI
    __copy_move_backward_a(
		const ::__gnu_debug::_Safe_iterator<_Ite, _Seq, _Cat>& __first,
		const ::__gnu_debug::_Safe_iterator<_Ite, _Seq, _Cat>& __last,
		_OI __result)
    {
      typename ::__gnu_debug::_Distance_traits<_Ite>::__type __dist;
      __glibcxx_check_valid_range2(__first, __last, __dist);
      __glibcxx_check_can_increment_dist(__result, __dist, -1);

      if (__dist.second > ::__gnu_debug::__dp_equality)
	return std::__copy_move_backward_a<_IsMove>(
		__first.base(), __last.base(), __result);

      return std::__copy_move_backward_a1<_IsMove>(__first, __last, __result);
    }

  template<bool _IsMove,
	   typename _II, typename _Ite, typename _Seq, typename _Cat>
    __gnu_debug::_Safe_iterator<_Ite, _Seq, _Cat>
    __copy_move_backward_a(_II __first, _II __last,
	const ::__gnu_debug::_Safe_iterator<_Ite, _Seq, _Cat>& __result)
    {
      typename ::__gnu_debug::_Distance_traits<_II>::__type __dist;
      __glibcxx_check_valid_range2(__first, __last, __dist);
      __glibcxx_check_can_increment_dist(__result, __dist, -1);

      if (__dist.second > ::__gnu_debug::__dp_sign
	  && __result._M_can_advance(-__dist.first, true))
	return ::__gnu_debug::_Safe_iterator<_Ite, _Seq, _Cat>(
		std::__copy_move_backward_a<_IsMove>(__first, __last,
						     __result.base()),
		__result._M_sequence);

      return std::__copy_move_backward_a1<_IsMove>(__first, __last, __result);
    }

  template<bool _IsMove,
	   typename _IIte, typename _ISeq, typename _ICat,
	   typename _OIte, typename _OSeq, typename _OCat>
    ::__gnu_debug::_Safe_iterator<_OIte, _OSeq, _OCat>
    __copy_move_backward_a(
	const ::__gnu_debug::_Safe_iterator<_IIte, _ISeq, _ICat>& __first,
	const ::__gnu_debug::_Safe_iterator<_IIte, _ISeq, _ICat>& __last,
	const ::__gnu_debug::_Safe_iterator<_OIte, _OSeq, _OCat>& __result)
    {
      typename ::__gnu_debug::_Distance_traits<_IIte>::__type __dist;
      __glibcxx_check_valid_range2(__first, __last, __dist);
      __glibcxx_check_can_increment_dist(__result, __dist, -1);

      if (__dist.second > ::__gnu_debug::__dp_equality)
	{
	  if (__dist.second > ::__gnu_debug::__dp_sign
	      && __result._M_can_advance(-__dist.first, true))
	    return ::__gnu_debug::_Safe_iterator<_OIte, _OSeq, _OCat>(
	      std::__copy_move_backward_a<_IsMove>(__first.base(), __last.base(),
						   __result.base()),
	      __result._M_sequence);

	  return std::__copy_move_backward_a<_IsMove>(
	    __first.base(), __last.base(), __result);
	}

      return std::__copy_move_backward_a1<_IsMove>(__first, __last, __result);
    }

  template<typename _Ite, typename _Seq, typename _Cat, typename _Tp>
    void
    __fill_a(const ::__gnu_debug::_Safe_iterator<_Ite, _Seq, _Cat>& __first,
	     const ::__gnu_debug::_Safe_iterator<_Ite, _Seq, _Cat>& __last,
	     const _Tp& __value)
    {
      typename ::__gnu_debug::_Distance_traits<_Ite>::__type __dist;
      __glibcxx_check_valid_range2(__first, __last, __dist);

      if (__dist.second > ::__gnu_debug::__dp_equality)
	std::__fill_a(__first.base(), __last.base(), __value);

      std::__fill_a1(__first, __last, __value);
    }

  template<typename _Ite, typename _Seq, typename _Cat, typename _Size,
	   typename _Tp>
    ::__gnu_debug::_Safe_iterator<_Ite, _Seq, _Cat>
    __fill_n_a(const ::__gnu_debug::_Safe_iterator<_Ite, _Seq, _Cat>& __first,
	       _Size __n, const _Tp& __value,
	       std::input_iterator_tag)
    {
#if __cplusplus >= 201103L
      static_assert(is_integral<_Size>{}, "fill_n must pass integral size");
#endif

      if (__n <= 0)
	return __first;

      __glibcxx_check_can_increment(__first, __n);
      if (__first._M_can_advance(__n, true))
	return ::__gnu_debug::_Safe_iterator<_Ite, _Seq, _Cat>(
		std::__fill_n_a(__first.base(), __n, __value, _Cat()),
		__first._M_sequence);

      return std::__fill_n_a1(__first, __n, __value);
    }

  template<typename _II1, typename _Seq1, typename _Cat1, typename _II2>
    bool
    __equal_aux(
	const ::__gnu_debug::_Safe_iterator<_II1, _Seq1, _Cat1>& __first1,
	const ::__gnu_debug::_Safe_iterator<_II1, _Seq1, _Cat1>& __last1,
	_II2 __first2)
    {
      typename ::__gnu_debug::_Distance_traits<_II1>::__type __dist;
      __glibcxx_check_valid_range2(__first1, __last1, __dist);
      __glibcxx_check_can_increment_dist(__first2, __dist, 1);

      if (__dist.second > ::__gnu_debug::__dp_equality)
	return std::__equal_aux(__first1.base(), __last1.base(), __first2);

      return std::__equal_aux1(__first1, __last1, __first2);
    }

  template<typename _II1, typename _II2, typename _Seq2, typename _Cat2>
    bool
    __equal_aux(_II1 __first1, _II1 __last1,
	const ::__gnu_debug::_Safe_iterator<_II2, _Seq2, _Cat2>& __first2)
    {
      typename ::__gnu_debug::_Distance_traits<_II1>::__type __dist;
      __glibcxx_check_valid_range2(__first1, __last1, __dist);
      __glibcxx_check_can_increment_dist(__first2, __dist, 1);

      if (__dist.second > ::__gnu_debug::__dp_sign
	  && __first2._M_can_advance(__dist.first, true))
	return std::__equal_aux(__first1, __last1, __first2.base());

      return std::__equal_aux1(__first1, __last1, __first2);
    }

  template<typename _II1, typename _Seq1, typename _Cat1,
	   typename _II2, typename _Seq2, typename _Cat2>
    bool
    __equal_aux(
	const ::__gnu_debug::_Safe_iterator<_II1, _Seq1, _Cat1>& __first1,
	const ::__gnu_debug::_Safe_iterator<_II1, _Seq1, _Cat1>& __last1,
	const ::__gnu_debug::_Safe_iterator<_II2, _Seq2, _Cat2>& __first2)
    {
      typename ::__gnu_debug::_Distance_traits<_II1>::__type __dist;
      __glibcxx_check_valid_range2(__first1, __last1, __dist);
      __glibcxx_check_can_increment_dist(__first2, __dist, 1);

      if (__dist.second > ::__gnu_debug::__dp_equality)
	{
	  if (__dist.second > ::__gnu_debug::__dp_sign &&
	      __first2._M_can_advance(__dist.first, true))
	    return std::__equal_aux(__first1.base(), __last1.base(),
				    __first2.base());
	  return std::__equal_aux(__first1.base(), __last1.base(), __first2);
	}

      return __equal_aux1(__first1, __last1, __first2);
    }

  template<typename _Ite1, typename _Seq1, typename _Cat1,
	   typename _II2>
    bool
    __lexicographical_compare_aux(
	const ::__gnu_debug::_Safe_iterator<_Ite1, _Seq1, _Cat1>& __first1,
	const ::__gnu_debug::_Safe_iterator<_Ite1, _Seq1, _Cat1>& __last1,
	_II2 __first2, _II2 __last2)
    {
      typename ::__gnu_debug::_Distance_traits<_Ite1>::__type __dist1;
      __glibcxx_check_valid_range2(__first1, __last1, __dist1);
      __glibcxx_check_valid_range(__first2, __last2);

      if (__dist1.second > ::__gnu_debug::__dp_equality)
	return std::__lexicographical_compare_aux(__first1.base(),
						  __last1.base(),
						  __first2, __last2);
      return std::__lexicographical_compare_aux1(__first1, __last1,
						 __first2, __last2);
    }

  template<typename _II1,
	   typename _Ite2, typename _Seq2, typename _Cat2>
    bool
    __lexicographical_compare_aux(
	_II1 __first1, _II1 __last1,
	const ::__gnu_debug::_Safe_iterator<_Ite2, _Seq2, _Cat2>& __first2,
	const ::__gnu_debug::_Safe_iterator<_Ite2, _Seq2, _Cat2>& __last2)
    {
      __glibcxx_check_valid_range(__first1, __last1);
      typename ::__gnu_debug::_Distance_traits<_II1>::__type __dist2;
      __glibcxx_check_valid_range2(__first2, __last2, __dist2);

      if (__dist2.second > ::__gnu_debug::__dp_equality)
	return std::__lexicographical_compare_aux(__first1, __last1,
						  __first2.base(),
						  __last2.base());
      return std::__lexicographical_compare_aux1(__first1, __last1,
						 __first2, __last2);
    }

  template<typename _Ite1, typename _Seq1, typename _Cat1,
	   typename _Ite2, typename _Seq2, typename _Cat2>
    bool
    __lexicographical_compare_aux(
	const ::__gnu_debug::_Safe_iterator<_Ite1, _Seq1, _Cat1>& __first1,
	const ::__gnu_debug::_Safe_iterator<_Ite1, _Seq1, _Cat1>& __last1,
	const ::__gnu_debug::_Safe_iterator<_Ite2, _Seq2, _Cat2>& __first2,
	const ::__gnu_debug::_Safe_iterator<_Ite2, _Seq2, _Cat2>& __last2)
    {
      typename ::__gnu_debug::_Distance_traits<_Ite1>::__type __dist1;
      __glibcxx_check_valid_range2(__first1, __last1, __dist1);
      typename ::__gnu_debug::_Distance_traits<_Ite2>::__type __dist2;
      __glibcxx_check_valid_range2(__first2, __last2, __dist2);

      if (__dist1.second > ::__gnu_debug::__dp_equality)
	{
	  if (__dist2.second > ::__gnu_debug::__dp_equality)
	    return std::__lexicographical_compare_aux(__first1.base(),
						      __last1.base(),
						      __first2.base(),
						      __last2.base());
	  return std::__lexicographical_compare_aux(__first1.base(),
						    __last1.base(),
						    __first2, __last2);
	}

      if (__dist2.second > ::__gnu_debug::__dp_equality)
	return std::__lexicographical_compare_aux(__first1, __last1,
						  __first2.base(),
						  __last2.base());
      return std::__lexicographical_compare_aux1(__first1, __last1,
						 __first2, __last2);
    }

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#endif
