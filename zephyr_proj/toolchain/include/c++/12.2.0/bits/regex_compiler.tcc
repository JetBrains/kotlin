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
 *  @file bits/regex_compiler.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{regex}
 */

// FIXME make comments doxygen format.

/*
// This compiler refers to "Regular Expression Matching Can Be Simple And Fast"
// (http://swtch.com/~rsc/regexp/regexp1.html),
// but doesn't strictly follow it.
//
// When compiling, states are *chained* instead of tree- or graph-constructed.
// It's more like structured programs: there's if statement and loop statement.
//
// For alternative structure (say "a|b"), aka "if statement", two branches
// should be constructed. However, these two shall merge to an "end_tag" at
// the end of this operator:
//
//                branch1
//              /        \
// => begin_tag            end_tag =>
//              \        /
//                branch2
//
// This is the difference between this implementation and that in Russ's
// article.
//
// That's why we introduced dummy node here ------ "end_tag" is a dummy node.
// All dummy nodes will be eliminated at the end of compilation.
*/

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

namespace __detail
{
  template<typename _TraitsT>
    _Compiler<_TraitsT>::
    _Compiler(const _CharT* __b, const _CharT* __e,
	      const typename _TraitsT::locale_type& __loc, _FlagT __flags)
    : _M_flags(_S_validate(__flags)),
      _M_scanner(__b, __e, _M_flags, __loc),
      _M_nfa(make_shared<_RegexT>(__loc, _M_flags)),
      _M_traits(_M_nfa->_M_traits),
      _M_ctype(std::use_facet<_CtypeT>(__loc))
    {
      _StateSeqT __r(*_M_nfa, _M_nfa->_M_start());
      __r._M_append(_M_nfa->_M_insert_subexpr_begin());
      this->_M_disjunction();
      if (!_M_match_token(_ScannerT::_S_token_eof))
	__throw_regex_error(regex_constants::error_paren);
      __r._M_append(_M_pop());
      __glibcxx_assert(_M_stack.empty());
      __r._M_append(_M_nfa->_M_insert_subexpr_end());
      __r._M_append(_M_nfa->_M_insert_accept());
      _M_nfa->_M_eliminate_dummy();
    }

  template<typename _TraitsT>
    void
    _Compiler<_TraitsT>::
    _M_disjunction()
    {
      this->_M_alternative();
      while (_M_match_token(_ScannerT::_S_token_or))
	{
	  _StateSeqT __alt1 = _M_pop();
	  this->_M_alternative();
	  _StateSeqT __alt2 = _M_pop();
	  auto __end = _M_nfa->_M_insert_dummy();
	  __alt1._M_append(__end);
	  __alt2._M_append(__end);
	  // __alt2 is state._M_next, __alt1 is state._M_alt. The executor
	  // executes _M_alt before _M_next, as well as executing left
	  // alternative before right one.
	  _M_stack.push(_StateSeqT(*_M_nfa,
				   _M_nfa->_M_insert_alt(
				     __alt2._M_start, __alt1._M_start, false),
				   __end));
	}
    }

  template<typename _TraitsT>
    void
    _Compiler<_TraitsT>::
    _M_alternative()
    {
      if (this->_M_term())
	{
	  _StateSeqT __re = _M_pop();
	  this->_M_alternative();
	  __re._M_append(_M_pop());
	  _M_stack.push(__re);
	}
      else
	_M_stack.push(_StateSeqT(*_M_nfa, _M_nfa->_M_insert_dummy()));
    }

  template<typename _TraitsT>
    bool
    _Compiler<_TraitsT>::
    _M_term()
    {
      if (this->_M_assertion())
	return true;
      if (this->_M_atom())
	{
	  while (this->_M_quantifier())
	    ;
	  return true;
	}
      return false;
    }

  template<typename _TraitsT>
    bool
    _Compiler<_TraitsT>::
    _M_assertion()
    {
      if (_M_match_token(_ScannerT::_S_token_line_begin))
	_M_stack.push(_StateSeqT(*_M_nfa, _M_nfa->_M_insert_line_begin()));
      else if (_M_match_token(_ScannerT::_S_token_line_end))
	_M_stack.push(_StateSeqT(*_M_nfa, _M_nfa->_M_insert_line_end()));
      else if (_M_match_token(_ScannerT::_S_token_word_bound))
	// _M_value[0] == 'n' means it's negative, say "not word boundary".
	_M_stack.push(_StateSeqT(*_M_nfa, _M_nfa->
	      _M_insert_word_bound(_M_value[0] == 'n')));
      else if (_M_match_token(_ScannerT::_S_token_subexpr_lookahead_begin))
	{
	  auto __neg = _M_value[0] == 'n';
	  this->_M_disjunction();
	  if (!_M_match_token(_ScannerT::_S_token_subexpr_end))
	    __throw_regex_error(regex_constants::error_paren);
	  auto __tmp = _M_pop();
	  __tmp._M_append(_M_nfa->_M_insert_accept());
	  _M_stack.push(
	      _StateSeqT(
		*_M_nfa,
		_M_nfa->_M_insert_lookahead(__tmp._M_start, __neg)));
	}
      else
	return false;
      return true;
    }

  template<typename _TraitsT>
    bool
    _Compiler<_TraitsT>::
    _M_quantifier()
    {
      bool __neg = (_M_flags & regex_constants::ECMAScript);
      auto __init = [this, &__neg]()
	{
	  if (_M_stack.empty())
	    __throw_regex_error(regex_constants::error_badrepeat);
	  __neg = __neg && _M_match_token(_ScannerT::_S_token_opt);
	};
      if (_M_match_token(_ScannerT::_S_token_closure0))
	{
	  __init();
	  auto __e = _M_pop();
	  _StateSeqT __r(*_M_nfa,
			 _M_nfa->_M_insert_repeat(_S_invalid_state_id,
						  __e._M_start, __neg));
	  __e._M_append(__r);
	  _M_stack.push(__r);
	}
      else if (_M_match_token(_ScannerT::_S_token_closure1))
	{
	  __init();
	  auto __e = _M_pop();
	  __e._M_append(_M_nfa->_M_insert_repeat(_S_invalid_state_id,
						 __e._M_start, __neg));
	  _M_stack.push(__e);
	}
      else if (_M_match_token(_ScannerT::_S_token_opt))
	{
	  __init();
	  auto __e = _M_pop();
	  auto __end = _M_nfa->_M_insert_dummy();
	  _StateSeqT __r(*_M_nfa,
			 _M_nfa->_M_insert_repeat(_S_invalid_state_id,
						  __e._M_start, __neg));
	  __e._M_append(__end);
	  __r._M_append(__end);
	  _M_stack.push(__r);
	}
      else if (_M_match_token(_ScannerT::_S_token_interval_begin))
	{
	  if (_M_stack.empty())
	    __throw_regex_error(regex_constants::error_badrepeat);
	  if (!_M_match_token(_ScannerT::_S_token_dup_count))
	    __throw_regex_error(regex_constants::error_badbrace);
	  _StateSeqT __r(_M_pop());
	  _StateSeqT __e(*_M_nfa, _M_nfa->_M_insert_dummy());
	  long __min_rep = _M_cur_int_value(10);
	  bool __infi = false;
	  long __n = 0;

	  // {3
	  if (_M_match_token(_ScannerT::_S_token_comma))
	    {
	      if (_M_match_token(_ScannerT::_S_token_dup_count)) // {3,7}
		__n = _M_cur_int_value(10) - __min_rep;
	      else
		__infi = true;
	    }
	  if (!_M_match_token(_ScannerT::_S_token_interval_end))
	    __throw_regex_error(regex_constants::error_brace);

	  __neg = __neg && _M_match_token(_ScannerT::_S_token_opt);

	  for (long __i = 0; __i < __min_rep; ++__i)
	    __e._M_append(__r._M_clone());

	  if (__infi)
	    {
	      auto __tmp = __r._M_clone();
	      _StateSeqT __s(*_M_nfa,
			     _M_nfa->_M_insert_repeat(_S_invalid_state_id,
						      __tmp._M_start, __neg));
	      __tmp._M_append(__s);
	      __e._M_append(__s);
	    }
	  else
	    {
	      if (__n < 0)
		__throw_regex_error(regex_constants::error_badbrace);
	      auto __end = _M_nfa->_M_insert_dummy();
	      // _M_alt is the "match more" branch, and _M_next is the
	      // "match less" one. Switch _M_alt and _M_next of all created
	      // nodes. This is a hack but IMO works well.
	      std::stack<_StateIdT> __stack;
	      for (long __i = 0; __i < __n; ++__i)
		{
		  auto __tmp = __r._M_clone();
		  auto __alt = _M_nfa->_M_insert_repeat(__tmp._M_start,
							__end, __neg);
		  __stack.push(__alt);
		  __e._M_append(_StateSeqT(*_M_nfa, __alt, __tmp._M_end));
		}
	      __e._M_append(__end);
	      while (!__stack.empty())
		{
		  auto& __tmp = (*_M_nfa)[__stack.top()];
		  __stack.pop();
		  std::swap(__tmp._M_next, __tmp._M_alt);
		}
	    }
	  _M_stack.push(__e);
	}
      else
	return false;
      return true;
    }

#define __INSERT_REGEX_MATCHER(__func, ...)\
	do {\
	  if (!(_M_flags & regex_constants::icase))\
	    if (!(_M_flags & regex_constants::collate))\
	      __func<false, false>(__VA_ARGS__);\
	    else\
	      __func<false, true>(__VA_ARGS__);\
	  else\
	    if (!(_M_flags & regex_constants::collate))\
	      __func<true, false>(__VA_ARGS__);\
	    else\
	      __func<true, true>(__VA_ARGS__);\
	} while (false)

  template<typename _TraitsT>
    bool
    _Compiler<_TraitsT>::
    _M_atom()
    {
      if (_M_match_token(_ScannerT::_S_token_anychar))
	{
	  if (!(_M_flags & regex_constants::ECMAScript))
	    __INSERT_REGEX_MATCHER(_M_insert_any_matcher_posix);
	  else
	    __INSERT_REGEX_MATCHER(_M_insert_any_matcher_ecma);
	}
      else if (_M_try_char())
	__INSERT_REGEX_MATCHER(_M_insert_char_matcher);
      else if (_M_match_token(_ScannerT::_S_token_backref))
	_M_stack.push(_StateSeqT(*_M_nfa, _M_nfa->
				 _M_insert_backref(_M_cur_int_value(10))));
      else if (_M_match_token(_ScannerT::_S_token_quoted_class))
	__INSERT_REGEX_MATCHER(_M_insert_character_class_matcher);
      else if (_M_match_token(_ScannerT::_S_token_subexpr_no_group_begin))
	{
	  _StateSeqT __r(*_M_nfa, _M_nfa->_M_insert_dummy());
	  this->_M_disjunction();
	  if (!_M_match_token(_ScannerT::_S_token_subexpr_end))
	    __throw_regex_error(regex_constants::error_paren);
	  __r._M_append(_M_pop());
	  _M_stack.push(__r);
	}
      else if (_M_match_token(_ScannerT::_S_token_subexpr_begin))
	{
	  _StateSeqT __r(*_M_nfa, _M_nfa->_M_insert_subexpr_begin());
	  this->_M_disjunction();
	  if (!_M_match_token(_ScannerT::_S_token_subexpr_end))
	    __throw_regex_error(regex_constants::error_paren);
	  __r._M_append(_M_pop());
	  __r._M_append(_M_nfa->_M_insert_subexpr_end());
	  _M_stack.push(__r);
	}
      else if (!_M_bracket_expression())
	return false;
      return true;
    }

  template<typename _TraitsT>
    bool
    _Compiler<_TraitsT>::
    _M_bracket_expression()
    {
      bool __neg =
	_M_match_token(_ScannerT::_S_token_bracket_neg_begin);
      if (!(__neg || _M_match_token(_ScannerT::_S_token_bracket_begin)))
	return false;
      __INSERT_REGEX_MATCHER(_M_insert_bracket_matcher, __neg);
      return true;
    }
#undef __INSERT_REGEX_MATCHER

  template<typename _TraitsT>
  template<bool __icase, bool __collate>
    void
    _Compiler<_TraitsT>::
    _M_insert_any_matcher_ecma()
    {
      _M_stack.push(_StateSeqT(*_M_nfa,
	_M_nfa->_M_insert_matcher
	  (_AnyMatcher<_TraitsT, true, __icase, __collate>
	    (_M_traits))));
    }

  template<typename _TraitsT>
  template<bool __icase, bool __collate>
    void
    _Compiler<_TraitsT>::
    _M_insert_any_matcher_posix()
    {
      _M_stack.push(_StateSeqT(*_M_nfa,
	_M_nfa->_M_insert_matcher
	  (_AnyMatcher<_TraitsT, false, __icase, __collate>
	    (_M_traits))));
    }

  template<typename _TraitsT>
  template<bool __icase, bool __collate>
    void
    _Compiler<_TraitsT>::
    _M_insert_char_matcher()
    {
      _M_stack.push(_StateSeqT(*_M_nfa,
	_M_nfa->_M_insert_matcher
	  (_CharMatcher<_TraitsT, __icase, __collate>
	    (_M_value[0], _M_traits))));
    }

  template<typename _TraitsT>
  template<bool __icase, bool __collate>
    void
    _Compiler<_TraitsT>::
    _M_insert_character_class_matcher()
    {
      __glibcxx_assert(_M_value.size() == 1);
      _BracketMatcher<__icase, __collate> __matcher
	(_M_ctype.is(_CtypeT::upper, _M_value[0]), _M_traits);
      __matcher._M_add_character_class(_M_value, false);
      __matcher._M_ready();
      _M_stack.push(_StateSeqT(*_M_nfa,
	_M_nfa->_M_insert_matcher(std::move(__matcher))));
    }

  template<typename _TraitsT>
  template<bool __icase, bool __collate>
    void
    _Compiler<_TraitsT>::
    _M_insert_bracket_matcher(bool __neg)
    {
      _BracketMatcher<__icase, __collate> __matcher(__neg, _M_traits);
      _BracketState __last_char;
      if (_M_try_char())
	__last_char.set(_M_value[0]);
      else if (_M_match_token(_ScannerT::_S_token_bracket_dash))
	// Dash as first character is a normal character.
	__last_char.set('-');
      while (_M_expression_term(__last_char, __matcher))
	;
      if (__last_char._M_is_char())
	__matcher._M_add_char(__last_char.get());
      __matcher._M_ready();
      _M_stack.push(_StateSeqT(
		      *_M_nfa,
		      _M_nfa->_M_insert_matcher(std::move(__matcher))));
    }

  template<typename _TraitsT>
  template<bool __icase, bool __collate>
    bool
    _Compiler<_TraitsT>::
    _M_expression_term(_BracketState& __last_char,
		       _BracketMatcher<__icase, __collate>& __matcher)
    {
      if (_M_match_token(_ScannerT::_S_token_bracket_end))
	return false;

      // Add any previously cached char into the matcher and update cache.
      const auto __push_char = [&](_CharT __ch)
      {
	if (__last_char._M_is_char())
	  __matcher._M_add_char(__last_char.get());
	__last_char.set(__ch);
      };
      // Add any previously cached char into the matcher and update cache.
      const auto __push_class = [&]
      {
        if (__last_char._M_is_char())
	  __matcher._M_add_char(__last_char.get());
	// We don't cache anything here, just record that the last thing
	// processed was a character class (or similar).
	__last_char.reset(_BracketState::_Type::_Class);
      };

      if (_M_match_token(_ScannerT::_S_token_collsymbol))
	{
	  auto __symbol = __matcher._M_add_collate_element(_M_value);
	  if (__symbol.size() == 1)
	    __push_char(__symbol[0]);
	  else
	    __push_class();
	}
      else if (_M_match_token(_ScannerT::_S_token_equiv_class_name))
	{
	  __push_class();
	  __matcher._M_add_equivalence_class(_M_value);
	}
      else if (_M_match_token(_ScannerT::_S_token_char_class_name))
	{
	  __push_class();
	  __matcher._M_add_character_class(_M_value, false);
	}
      else if (_M_try_char())
	__push_char(_M_value[0]);
      // POSIX doesn't allow '-' as a start-range char (say [a-z--0]),
      // except when the '-' is the first or last character in the bracket
      // expression ([--0]). ECMAScript treats all '-' after a range as a
      // normal character. Also see above, where _M_expression_term gets called.
      //
      // As a result, POSIX rejects [-----], but ECMAScript doesn't.
      // Boost (1.57.0) always uses POSIX style even in its ECMAScript syntax.
      // Clang (3.5) always uses ECMAScript style even in its POSIX syntax.
      //
      // It turns out that no one reads BNFs ;)
      else if (_M_match_token(_ScannerT::_S_token_bracket_dash))
	{
	  if (_M_match_token(_ScannerT::_S_token_bracket_end))
	    {
	      // For "-]" the dash is a literal character.
	      __push_char('-');
	      return false;
	    }
	  else if (__last_char._M_is_class())
	    {
	      // "\\w-" is invalid, start of range must be a single char.
	      __throw_regex_error(regex_constants::error_range,
				  "Invalid start of '[x-x]' range in "
				  "regular expression");
	    }
	  else if (__last_char._M_is_char())
	    {
	      if (_M_try_char())
		{
		  // "x-y"
		  __matcher._M_make_range(__last_char.get(), _M_value[0]);
		  __last_char.reset();
		}
	      else if (_M_match_token(_ScannerT::_S_token_bracket_dash))
		{
		  // "x--"
		  __matcher._M_make_range(__last_char.get(), '-');
		  __last_char.reset();
		}
	      else
		__throw_regex_error(regex_constants::error_range,
				    "Invalid end of '[x-x]' range in "
				    "regular expression");
	    }
	  else if (_M_flags & regex_constants::ECMAScript)
	    {
	      // A dash that is not part of an existing range. Might be the
	      // start of a new range, or might just be a literal '-' char.
	      // Only ECMAScript allows that in the middle of a bracket expr.
	      __push_char('-');
	    }
	  else
	    __throw_regex_error(regex_constants::error_range,
				"Invalid location of '-' within '[...]' in "
				"POSIX regular expression");
	}
      else if (_M_match_token(_ScannerT::_S_token_quoted_class))
	{
	  __push_class();
	  __matcher._M_add_character_class(_M_value,
					   _M_ctype.is(_CtypeT::upper,
						       _M_value[0]));
	}
      else
	__throw_regex_error(regex_constants::error_brack,
			    "Unexpected character within '[...]' in "
			    "regular expression");
      return true;
    }

  template<typename _TraitsT>
    bool
    _Compiler<_TraitsT>::
    _M_try_char()
    {
      bool __is_char = false;
      if (_M_match_token(_ScannerT::_S_token_oct_num))
	{
	  __is_char = true;
	  _M_value.assign(1, _M_cur_int_value(8));
	}
      else if (_M_match_token(_ScannerT::_S_token_hex_num))
	{
	  __is_char = true;
	  _M_value.assign(1, _M_cur_int_value(16));
	}
      else if (_M_match_token(_ScannerT::_S_token_ord_char))
	__is_char = true;
      return __is_char;
    }

  template<typename _TraitsT>
    bool
    _Compiler<_TraitsT>::
    _M_match_token(_TokenT __token)
    {
      if (__token == _M_scanner._M_get_token())
	{
	  _M_value = _M_scanner._M_get_value();
	  _M_scanner._M_advance();
	  return true;
	}
      return false;
    }

  template<typename _TraitsT>
    int
    _Compiler<_TraitsT>::
    _M_cur_int_value(int __radix)
    {
      long __v = 0;
      for (typename _StringT::size_type __i = 0;
	   __i < _M_value.length(); ++__i)
	__v =__v * __radix + _M_traits.value(_M_value[__i], __radix);
      return __v;
    }

  template<typename _TraitsT, bool __icase, bool __collate>
    bool
    _BracketMatcher<_TraitsT, __icase, __collate>::
    _M_apply(_CharT __ch, false_type) const
    {
      return [this, __ch]
      {
	if (std::binary_search(_M_char_set.begin(), _M_char_set.end(),
			       _M_translator._M_translate(__ch)))
	  return true;
	auto __s = _M_translator._M_transform(__ch);
	for (auto& __it : _M_range_set)
	  if (_M_translator._M_match_range(__it.first, __it.second, __s))
	    return true;
	if (_M_traits.isctype(__ch, _M_class_set))
	  return true;
	if (std::find(_M_equiv_set.begin(), _M_equiv_set.end(),
		      _M_traits.transform_primary(&__ch, &__ch+1))
	    != _M_equiv_set.end())
	  return true;
	for (auto& __it : _M_neg_class_set)
	  if (!_M_traits.isctype(__ch, __it))
	    return true;
	return false;
      }() ^ _M_is_non_matching;
    }
} // namespace __detail

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace
