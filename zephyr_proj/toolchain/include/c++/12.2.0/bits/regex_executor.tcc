// class template regex -*- C++ -*-

// Copyright (C) 2013-2022 Free Software Foundation, Inc.
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

/**
 *  @file bits/regex_executor.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{regex}
 */

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

namespace __detail
{
  template<typename _BiIter, typename _Alloc, typename _TraitsT,
	   bool __dfs_mode>
    bool _Executor<_BiIter, _Alloc, _TraitsT, __dfs_mode>::
    _M_search()
    {
      if (_M_search_from_first())
	return true;
      if (_M_flags & regex_constants::match_continuous)
	return false;
      _M_flags |= regex_constants::match_prev_avail;
      while (_M_begin != _M_end)
	{
	  ++_M_begin;
	  if (_M_search_from_first())
	    return true;
	}
      return false;
    }

  // The _M_main function operates in different modes, DFS mode or BFS mode,
  // indicated by template parameter __dfs_mode, and dispatches to one of the
  // _M_main_dispatch overloads.
  //
  // ------------------------------------------------------------
  //
  // DFS mode:
  //
  // It applies a Depth-First-Search (aka backtracking) on given NFA and input
  // string.
  // At the very beginning the executor stands in the start state, then it
  // tries every possible state transition in current state recursively. Some
  // state transitions consume input string, say, a single-char-matcher or a
  // back-reference matcher; some don't, like assertion or other anchor nodes.
  // When the input is exhausted and/or the current state is an accepting
  // state, the whole executor returns true.
  //
  // TODO: This approach is exponentially slow for certain input.
  //       Try to compile the NFA to a DFA.
  //
  // Time complexity: \Omega(match_length), O(2^(_M_nfa.size()))
  // Space complexity: \theta(match_results.size() + match_length)
  //
  template<typename _BiIter, typename _Alloc, typename _TraitsT,
	   bool __dfs_mode>
    bool _Executor<_BiIter, _Alloc, _TraitsT, __dfs_mode>::
    _M_main_dispatch(_Match_mode __match_mode, __dfs)
    {
      _M_has_sol = false;
      *_M_states._M_get_sol_pos() = _BiIter();
      _M_cur_results = _M_results;
      _M_dfs(__match_mode, _M_states._M_start);
      return _M_has_sol;
    }

  // ------------------------------------------------------------
  //
  // BFS mode:
  //
  // Russ Cox's article (http://swtch.com/~rsc/regexp/regexp1.html)
  // explained this algorithm clearly.
  //
  // It first computes epsilon closure (states that can be achieved without
  // consuming characters) for every state that's still matching,
  // using the same DFS algorithm, but doesn't re-enter states (using
  // _M_states._M_visited to check), nor follow _S_opcode_match.
  //
  // Then apply DFS using every _S_opcode_match (in _M_states._M_match_queue)
  // as the start state.
  //
  // It significantly reduces potential duplicate states, so has a better
  // upper bound; but it requires more overhead.
  //
  // Time complexity: \Omega(match_length * match_results.size())
  //                  O(match_length * _M_nfa.size() * match_results.size())
  // Space complexity: \Omega(_M_nfa.size() + match_results.size())
  //                   O(_M_nfa.size() * match_results.size())
  template<typename _BiIter, typename _Alloc, typename _TraitsT,
	   bool __dfs_mode>
    bool _Executor<_BiIter, _Alloc, _TraitsT, __dfs_mode>::
    _M_main_dispatch(_Match_mode __match_mode, __bfs)
    {
      _M_states._M_queue(_M_states._M_start, _M_results);
      bool __ret = false;
      while (1)
	{
	  _M_has_sol = false;
	  if (_M_states._M_match_queue.empty())
	    break;
	  std::fill_n(_M_states._M_visited_states, _M_nfa.size(), false);
	  auto __old_queue = std::move(_M_states._M_match_queue);
	  for (auto& __task : __old_queue)
	    {
	      _M_cur_results = std::move(__task.second);
	      _M_dfs(__match_mode, __task.first);
	    }
	  if (__match_mode == _Match_mode::_Prefix)
	    __ret |= _M_has_sol;
	  if (_M_current == _M_end)
	    break;
	  ++_M_current;
	}
      if (__match_mode == _Match_mode::_Exact)
	__ret = _M_has_sol;
      _M_states._M_match_queue.clear();
      return __ret;
    }

  // Return whether now match the given sub-NFA.
  template<typename _BiIter, typename _Alloc, typename _TraitsT,
	   bool __dfs_mode>
    bool _Executor<_BiIter, _Alloc, _TraitsT, __dfs_mode>::
    _M_lookahead(_StateIdT __next)
    {
      // Backreferences may refer to captured content.
      // We may want to make this faster by not copying,
      // but let's not be clever prematurely.
      _ResultsVec __what(_M_cur_results);
      _Executor __sub(_M_current, _M_end, __what, _M_re, _M_flags);
      __sub._M_states._M_start = __next;
      if (__sub._M_search_from_first())
	{
	  for (size_t __i = 0; __i < __what.size(); __i++)
	    if (__what[__i].matched)
	      _M_cur_results[__i] = __what[__i];
	  return true;
	}
      return false;
    }

  // __rep_count records how many times (__rep_count.second)
  // this node is visited under certain input iterator
  // (__rep_count.first). This prevent the executor from entering
  // infinite loop by refusing to continue when it's already been
  // visited more than twice. It's `twice` instead of `once` because
  // we need to spare one more time for potential group capture.
  template<typename _BiIter, typename _Alloc, typename _TraitsT,
	   bool __dfs_mode>
    void _Executor<_BiIter, _Alloc, _TraitsT, __dfs_mode>::
    _M_rep_once_more(_Match_mode __match_mode, _StateIdT __i)
    {
      const auto& __state = _M_nfa[__i];
      auto& __rep_count = _M_rep_count[__i];
      if (__rep_count.second == 0 || __rep_count.first != _M_current)
	{
	  auto __back = __rep_count;
	  __rep_count.first = _M_current;
	  __rep_count.second = 1;
	  _M_dfs(__match_mode, __state._M_alt);
	  __rep_count = __back;
	}
      else
	{
	  if (__rep_count.second < 2)
	    {
	      __rep_count.second++;
	      _M_dfs(__match_mode, __state._M_alt);
	      __rep_count.second--;
	    }
	}
    }

  // _M_alt branch is "match once more", while _M_next is "get me out
  // of this quantifier". Executing _M_next first or _M_alt first don't
  // mean the same thing, and we need to choose the correct order under
  // given greedy mode.
  template<typename _BiIter, typename _Alloc, typename _TraitsT,
	   bool __dfs_mode>
    void _Executor<_BiIter, _Alloc, _TraitsT, __dfs_mode>::
    _M_handle_repeat(_Match_mode __match_mode, _StateIdT __i)
    {
      const auto& __state = _M_nfa[__i];

      // Greedy.
      if (!__state._M_neg)
	{
	  _M_rep_once_more(__match_mode, __i);
	  // If it's DFS executor and already accepted, we're done.
	  if (!__dfs_mode || !_M_has_sol)
	    _M_dfs(__match_mode, __state._M_next);
	}
      else // Non-greedy mode
	{
	  if (__dfs_mode)
	    {
	      // vice-versa.
	      _M_dfs(__match_mode, __state._M_next);
	      if (!_M_has_sol)
		_M_rep_once_more(__match_mode, __i);
	    }
	  else
	    {
	      // DON'T attempt anything, because there's already another
	      // state with higher priority accepted. This state cannot
	      // be better by attempting its next node.
	      if (!_M_has_sol)
		{
		  _M_dfs(__match_mode, __state._M_next);
		  // DON'T attempt anything if it's already accepted. An
		  // accepted state *must* be better than a solution that
		  // matches a non-greedy quantifier one more time.
		  if (!_M_has_sol)
		    _M_rep_once_more(__match_mode, __i);
		}
	    }
	}
    }

  template<typename _BiIter, typename _Alloc, typename _TraitsT,
	   bool __dfs_mode>
    void _Executor<_BiIter, _Alloc, _TraitsT, __dfs_mode>::
    _M_handle_subexpr_begin(_Match_mode __match_mode, _StateIdT __i)
    {
      const auto& __state = _M_nfa[__i];

      auto& __res = _M_cur_results[__state._M_subexpr];
      auto __back = __res.first;
      __res.first = _M_current;
      _M_dfs(__match_mode, __state._M_next);
      __res.first = __back;
    }

  template<typename _BiIter, typename _Alloc, typename _TraitsT,
	   bool __dfs_mode>
    void _Executor<_BiIter, _Alloc, _TraitsT, __dfs_mode>::
    _M_handle_subexpr_end(_Match_mode __match_mode, _StateIdT __i)
    {
      const auto& __state = _M_nfa[__i];

      auto& __res = _M_cur_results[__state._M_subexpr];
      auto __back = __res;
      __res.second = _M_current;
      __res.matched = true;
      _M_dfs(__match_mode, __state._M_next);
      __res = __back;
    }

  template<typename _BiIter, typename _Alloc, typename _TraitsT,
	   bool __dfs_mode>
    inline void _Executor<_BiIter, _Alloc, _TraitsT, __dfs_mode>::
    _M_handle_line_begin_assertion(_Match_mode __match_mode, _StateIdT __i)
    {
      const auto& __state = _M_nfa[__i];
      if (_M_at_begin())
	_M_dfs(__match_mode, __state._M_next);
    }

  template<typename _BiIter, typename _Alloc, typename _TraitsT,
	   bool __dfs_mode>
    inline void _Executor<_BiIter, _Alloc, _TraitsT, __dfs_mode>::
    _M_handle_line_end_assertion(_Match_mode __match_mode, _StateIdT __i)
    {
      const auto& __state = _M_nfa[__i];
      if (_M_at_end())
	_M_dfs(__match_mode, __state._M_next);
    }

  template<typename _BiIter, typename _Alloc, typename _TraitsT,
	   bool __dfs_mode>
    inline void _Executor<_BiIter, _Alloc, _TraitsT, __dfs_mode>::
    _M_handle_word_boundary(_Match_mode __match_mode, _StateIdT __i)
    {
      const auto& __state = _M_nfa[__i];
      if (_M_word_boundary() == !__state._M_neg)
	_M_dfs(__match_mode, __state._M_next);
    }

  // Here __state._M_alt offers a single start node for a sub-NFA.
  // We recursively invoke our algorithm to match the sub-NFA.
  template<typename _BiIter, typename _Alloc, typename _TraitsT,
	   bool __dfs_mode>
    void _Executor<_BiIter, _Alloc, _TraitsT, __dfs_mode>::
    _M_handle_subexpr_lookahead(_Match_mode __match_mode, _StateIdT __i)
    {
      const auto& __state = _M_nfa[__i];
      if (_M_lookahead(__state._M_alt) == !__state._M_neg)
	_M_dfs(__match_mode, __state._M_next);
    }

  template<typename _BiIter, typename _Alloc, typename _TraitsT,
	   bool __dfs_mode>
    void _Executor<_BiIter, _Alloc, _TraitsT, __dfs_mode>::
    _M_handle_match(_Match_mode __match_mode, _StateIdT __i)
    {
      const auto& __state = _M_nfa[__i];

      if (_M_current == _M_end)
	return;
      if (__dfs_mode)
	{
	  if (__state._M_matches(*_M_current))
	    {
	      ++_M_current;
	      _M_dfs(__match_mode, __state._M_next);
	      --_M_current;
	    }
	}
      else
	if (__state._M_matches(*_M_current))
	  _M_states._M_queue(__state._M_next, _M_cur_results);
    }

  template<typename _BiIter, typename _TraitsT>
    struct _Backref_matcher
    {
      _Backref_matcher(bool __icase, const _TraitsT& __traits)
      : _M_traits(__traits) { }

      bool
      _M_apply(_BiIter __expected_begin,
	       _BiIter __expected_end, _BiIter __actual_begin,
	       _BiIter __actual_end)
      {
	return _M_traits.transform(__expected_begin, __expected_end)
	    == _M_traits.transform(__actual_begin, __actual_end);
      }

      const _TraitsT& _M_traits;
    };

  template<typename _BiIter, typename _CharT>
    struct _Backref_matcher<_BiIter, std::regex_traits<_CharT>>
    {
      using _TraitsT = std::regex_traits<_CharT>;
      _Backref_matcher(bool __icase, const _TraitsT& __traits)
      : _M_icase(__icase), _M_traits(__traits) { }

      bool
      _M_apply(_BiIter __expected_begin,
	       _BiIter __expected_end, _BiIter __actual_begin,
	       _BiIter __actual_end)
      {
	if (!_M_icase)
	  return _GLIBCXX_STD_A::__equal4(__expected_begin, __expected_end,
			       __actual_begin, __actual_end);
	typedef std::ctype<_CharT> __ctype_type;
	const auto& __fctyp = use_facet<__ctype_type>(_M_traits.getloc());
	return _GLIBCXX_STD_A::__equal4(__expected_begin, __expected_end,
			     __actual_begin, __actual_end,
			     [this, &__fctyp](_CharT __lhs, _CharT __rhs)
			     {
			       return __fctyp.tolower(__lhs)
				 == __fctyp.tolower(__rhs);
			     });
      }

      bool _M_icase;
      const _TraitsT& _M_traits;
    };

  // First fetch the matched result from _M_cur_results as __submatch;
  // then compare it with
  // (_M_current, _M_current + (__submatch.second - __submatch.first)).
  // If matched, keep going; else just return and try another state.
  template<typename _BiIter, typename _Alloc, typename _TraitsT,
	   bool __dfs_mode>
    void _Executor<_BiIter, _Alloc, _TraitsT, __dfs_mode>::
    _M_handle_backref(_Match_mode __match_mode, _StateIdT __i)
    {
      __glibcxx_assert(__dfs_mode);

      const auto& __state = _M_nfa[__i];
      auto& __submatch = _M_cur_results[__state._M_backref_index];
      if (!__submatch.matched)
	return;
      auto __last = _M_current;
      for (auto __tmp = __submatch.first;
	   __last != _M_end && __tmp != __submatch.second;
	   ++__tmp)
	++__last;
      if (_Backref_matcher<_BiIter, _TraitsT>(
	      _M_re.flags() & regex_constants::icase,
	      _M_re._M_automaton->_M_traits)._M_apply(
		  __submatch.first, __submatch.second, _M_current, __last))
	{
	  if (__last != _M_current)
	    {
	      auto __backup = _M_current;
	      _M_current = __last;
	      _M_dfs(__match_mode, __state._M_next);
	      _M_current = __backup;
	    }
	  else
	    _M_dfs(__match_mode, __state._M_next);
	}
    }

  template<typename _BiIter, typename _Alloc, typename _TraitsT,
	   bool __dfs_mode>
    void _Executor<_BiIter, _Alloc, _TraitsT, __dfs_mode>::
    _M_handle_accept(_Match_mode __match_mode, _StateIdT)
    {
      if _GLIBCXX17_CONSTEXPR (__dfs_mode)
	{
	  __glibcxx_assert(!_M_has_sol);
	  if (__match_mode == _Match_mode::_Exact)
	    _M_has_sol = _M_current == _M_end;
	  else
	    _M_has_sol = true;
	  if (_M_current == _M_begin
	      && (_M_flags & regex_constants::match_not_null))
	    _M_has_sol = false;
	  if (_M_has_sol)
	    {
	      if (_M_nfa._M_flags & regex_constants::ECMAScript)
		_M_results = _M_cur_results;
	      else // POSIX
		{
		  __glibcxx_assert(_M_states._M_get_sol_pos());
		  // Here's POSIX's logic: match the longest one. However
		  // we never know which one (lhs or rhs of "|") is longer
		  // unless we try both of them and compare the results.
		  // The member variable _M_sol_pos records the end
		  // position of the last successful match. It's better
		  // to be larger, because POSIX regex is always greedy.
		  // TODO: This could be slow.
		  if (*_M_states._M_get_sol_pos() == _BiIter()
		      || std::distance(_M_begin,
				       *_M_states._M_get_sol_pos())
			 < std::distance(_M_begin, _M_current))
		    {
		      *_M_states._M_get_sol_pos() = _M_current;
		      _M_results = _M_cur_results;
		    }
		}
	    }
	}
      else
	{
	  if (_M_current == _M_begin
	      && (_M_flags & regex_constants::match_not_null))
	    return;
	  if (__match_mode == _Match_mode::_Prefix || _M_current == _M_end)
	    if (!_M_has_sol)
	      {
		_M_has_sol = true;
		_M_results = _M_cur_results;
	      }
	}
    }

  template<typename _BiIter, typename _Alloc, typename _TraitsT,
	   bool __dfs_mode>
    void _Executor<_BiIter, _Alloc, _TraitsT, __dfs_mode>::
    _M_handle_alternative(_Match_mode __match_mode, _StateIdT __i)
    {
      const auto& __state = _M_nfa[__i];

      if (_M_nfa._M_flags & regex_constants::ECMAScript)
	{
	  // TODO: Fix BFS support. It is wrong.
	  _M_dfs(__match_mode, __state._M_alt);
	  // Pick lhs if it matches. Only try rhs if it doesn't.
	  if (!_M_has_sol)
	    _M_dfs(__match_mode, __state._M_next);
	}
      else
	{
	  // Try both and compare the result.
	  // See "case _S_opcode_accept:" handling above.
	  _M_dfs(__match_mode, __state._M_alt);
	  auto __has_sol = _M_has_sol;
	  _M_has_sol = false;
	  _M_dfs(__match_mode, __state._M_next);
	  _M_has_sol |= __has_sol;
	}
    }

  template<typename _BiIter, typename _Alloc, typename _TraitsT,
	   bool __dfs_mode>
    void _Executor<_BiIter, _Alloc, _TraitsT, __dfs_mode>::
    _M_dfs(_Match_mode __match_mode, _StateIdT __i)
    {
      if (_M_states._M_visited(__i))
	return;

      switch (_M_nfa[__i]._M_opcode())
	{
	case _S_opcode_repeat:
	  _M_handle_repeat(__match_mode, __i); break;
	case _S_opcode_subexpr_begin:
	  _M_handle_subexpr_begin(__match_mode, __i); break;
	case _S_opcode_subexpr_end:
	  _M_handle_subexpr_end(__match_mode, __i); break;
	case _S_opcode_line_begin_assertion:
	  _M_handle_line_begin_assertion(__match_mode, __i); break;
	case _S_opcode_line_end_assertion:
	  _M_handle_line_end_assertion(__match_mode, __i); break;
	case _S_opcode_word_boundary:
	  _M_handle_word_boundary(__match_mode, __i); break;
	case _S_opcode_subexpr_lookahead:
	  _M_handle_subexpr_lookahead(__match_mode, __i); break;
	case _S_opcode_match:
	  _M_handle_match(__match_mode, __i); break;
	case _S_opcode_backref:
	  _M_handle_backref(__match_mode, __i); break;
	case _S_opcode_accept:
	  _M_handle_accept(__match_mode, __i); break;
	case _S_opcode_alternative:
	  _M_handle_alternative(__match_mode, __i); break;
	default:
	  __glibcxx_assert(false);
	}
    }

  // Return whether now is at some word boundary.
  template<typename _BiIter, typename _Alloc, typename _TraitsT,
	   bool __dfs_mode>
    bool _Executor<_BiIter, _Alloc, _TraitsT, __dfs_mode>::
    _M_word_boundary() const
    {
      if (_M_current == _M_begin && (_M_flags & regex_constants::match_not_bow))
	return false;
      if (_M_current == _M_end && (_M_flags & regex_constants::match_not_eow))
	return false;

      bool __left_is_word = false;
      if (_M_current != _M_begin
	  || (_M_flags & regex_constants::match_prev_avail))
	{
	  auto __prev = _M_current;
	  if (_M_is_word(*std::prev(__prev)))
	    __left_is_word = true;
	}
      bool __right_is_word =
        _M_current != _M_end && _M_is_word(*_M_current);

      return __left_is_word != __right_is_word;
    }
} // namespace __detail

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace
