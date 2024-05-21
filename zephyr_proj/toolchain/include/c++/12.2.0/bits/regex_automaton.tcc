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
 *  @file bits/regex_automaton.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{regex}
 */

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

namespace __detail
{
#ifdef _GLIBCXX_DEBUG
  inline std::ostream&
  _State_base::_M_print(std::ostream& ostr) const
  {
    switch (_M_opcode)
    {
      case _S_opcode_alternative:
      case _S_opcode_repeat:
	ostr << "alt next=" << _M_next << " alt=" << _M_alt;
	break;
      case _S_opcode_subexpr_begin:
	ostr << "subexpr begin next=" << _M_next << " index=" << _M_subexpr;
	break;
      case _S_opcode_subexpr_end:
	ostr << "subexpr end next=" << _M_next << " index=" << _M_subexpr;
	break;
      case _S_opcode_backref:
	ostr << "backref next=" << _M_next << " index=" << _M_backref_index;
	break;
      case _S_opcode_match:
	ostr << "match next=" << _M_next;
	break;
      case _S_opcode_accept:
	ostr << "accept next=" << _M_next;
	break;
      default:
	ostr << "unknown next=" << _M_next;
	break;
    }
    return ostr;
  }

  // Prints graphviz dot commands for state.
  inline std::ostream&
  _State_base::_M_dot(std::ostream& __ostr, _StateIdT __id) const
  {
    switch (_M_opcode)
    {
      case _S_opcode_alternative:
      case _S_opcode_repeat:
	__ostr << __id << " [label=\"" << __id << "\\nALT\"];\n"
	       << __id << " -> " << _M_next
	       << " [label=\"next\", tailport=\"s\"];\n"
	       << __id << " -> " << _M_alt
	       << " [label=\"alt\", tailport=\"n\"];\n";
	break;
      case _S_opcode_backref:
	__ostr << __id << " [label=\"" << __id << "\\nBACKREF "
	       << _M_subexpr << "\"];\n"
	       << __id << " -> " << _M_next << " [label=\"<match>\"];\n";
	break;
      case _S_opcode_line_begin_assertion:
	__ostr << __id << " [label=\"" << __id << "\\nLINE_BEGIN \"];\n"
	       << __id << " -> " << _M_next << " [label=\"epsilon\"];\n";
	break;
      case _S_opcode_line_end_assertion:
	__ostr << __id << " [label=\"" << __id << "\\nLINE_END \"];\n"
	       << __id << " -> " << _M_next << " [label=\"epsilon\"];\n";
	break;
      case _S_opcode_word_boundary:
	__ostr << __id << " [label=\"" << __id << "\\nWORD_BOUNDRY "
	       << _M_neg << "\"];\n"
	       << __id << " -> " << _M_next << " [label=\"epsilon\"];\n";
	break;
      case _S_opcode_subexpr_lookahead:
	__ostr << __id << " [label=\"" << __id << "\\nLOOK_AHEAD\"];\n"
	       << __id << " -> " << _M_next
	       << " [label=\"epsilon\", tailport=\"s\"];\n"
	       << __id << " -> " << _M_alt
	       << " [label=\"<assert>\", tailport=\"n\"];\n";
	break;
      case _S_opcode_subexpr_begin:
	__ostr << __id << " [label=\"" << __id << "\\nSBEGIN "
	       << _M_subexpr << "\"];\n"
	       << __id << " -> " << _M_next << " [label=\"epsilon\"];\n";
	break;
      case _S_opcode_subexpr_end:
	__ostr << __id << " [label=\"" << __id << "\\nSEND "
	       << _M_subexpr << "\"];\n"
	       << __id << " -> " << _M_next << " [label=\"epsilon\"];\n";
	break;
      case _S_opcode_dummy:
	break;
      case _S_opcode_match:
	__ostr << __id << " [label=\"" << __id << "\\nMATCH\"];\n"
	       << __id << " -> " << _M_next << " [label=\"<match>\"];\n";
	break;
      case _S_opcode_accept:
	__ostr << __id << " [label=\"" << __id << "\\nACC\"];\n" ;
	break;
      default:
	_GLIBCXX_DEBUG_ASSERT(false);
	break;
    }
    return __ostr;
  }

  template<typename _TraitsT>
    std::ostream&
    _NFA<_TraitsT>::_M_dot(std::ostream& __ostr) const
    {
      __ostr << "digraph _Nfa {\n"
		"  rankdir=LR;\n";
      for (size_t __i = 0; __i < this->size(); ++__i)
	(*this)[__i]._M_dot(__ostr, __i);
      __ostr << "}\n";
      return __ostr;
    }
#endif

  template<typename _TraitsT>
    _StateIdT
    _NFA<_TraitsT>::_M_insert_backref(size_t __index)
    {
      if (this->_M_flags & regex_constants::__polynomial)
	__throw_regex_error(regex_constants::error_complexity,
			    "Unexpected back-reference in polynomial mode.");
      // To figure out whether a backref is valid, a stack is used to store
      // unfinished sub-expressions. For example, when parsing
      // "(a(b)(c\\1(d)))" at '\\1', _M_subexpr_count is 3, indicating that 3
      // sub expressions are parsed or partially parsed(in the stack), aka,
      // "(a..", "(b)" and "(c..").
      // _M_paren_stack is {1, 3}, for incomplete "(a.." and "(c..". At this
      // time, "\\2" is valid, but "\\1" and "\\3" are not.
      if (__index >= _M_subexpr_count)
	__throw_regex_error(
	  regex_constants::error_backref,
	  "Back-reference index exceeds current sub-expression count.");
      for (auto __it : this->_M_paren_stack)
	if (__index == __it)
	  __throw_regex_error(
	    regex_constants::error_backref,
	    "Back-reference referred to an opened sub-expression.");
      this->_M_has_backref = true;
      _StateT __tmp(_S_opcode_backref);
      __tmp._M_backref_index = __index;
      return _M_insert_state(std::move(__tmp));
    }

  template<typename _TraitsT>
    void
    _NFA<_TraitsT>::_M_eliminate_dummy()
    {
      for (auto& __it : *this)
	{
	  while (__it._M_next >= 0 && (*this)[__it._M_next]._M_opcode()
		 == _S_opcode_dummy)
	    __it._M_next = (*this)[__it._M_next]._M_next;
	  if (__it._M_has_alt())
	    while (__it._M_alt >= 0 && (*this)[__it._M_alt]._M_opcode()
		   == _S_opcode_dummy)
	      __it._M_alt = (*this)[__it._M_alt]._M_next;
	}
    }

  // Just apply DFS on the sequence and re-link their links.
  template<typename _TraitsT>
    _StateSeq<_TraitsT>
    _StateSeq<_TraitsT>::_M_clone()
    {
      _GLIBCXX_STD_C::map<_StateIdT, _StateIdT> __m;
      std::stack<_StateIdT, _GLIBCXX_STD_C::deque<_StateIdT>> __stack;
      __stack.push(_M_start);
      while (!__stack.empty())
	{
	  auto __u = __stack.top();
	  __stack.pop();
	  auto __dup = _M_nfa[__u];
	  // _M_insert_state() never return -1
	  auto __id = _M_nfa._M_insert_state(std::move(__dup));
	  __m[__u] = __id;
	  if (__dup._M_has_alt())
	    if (__dup._M_alt != _S_invalid_state_id
		&& __m.count(__dup._M_alt) == 0)
	      __stack.push(__dup._M_alt);
	  if (__u == _M_end)
	    continue;
	  if (__dup._M_next != _S_invalid_state_id
	      && __m.count(__dup._M_next) == 0)
	    __stack.push(__dup._M_next);
	}
      for (auto __it : __m)
	{
	  auto __v = __it.second;
	  auto& __ref = _M_nfa[__v];
	  if (__ref._M_next != _S_invalid_state_id)
	    __ref._M_next = __m.find(__ref._M_next)->second;
	  if (__ref._M_has_alt() && __ref._M_alt != _S_invalid_state_id)
	    __ref._M_alt = __m.find(__ref._M_alt)->second;
	}
      return _StateSeq(_M_nfa, __m[_M_start], __m[_M_end]);
    }
} // namespace __detail

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace
