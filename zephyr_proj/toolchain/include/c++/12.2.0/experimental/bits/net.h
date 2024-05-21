// Networking implementation details -*- C++ -*-

// Copyright (C) 2015-2022 Free Software Foundation, Inc.
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

/** @file experimental/bits/net.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{experimental/net}
 */

#ifndef _GLIBCXX_EXPERIMENTAL_NET_H
#define _GLIBCXX_EXPERIMENTAL_NET_H 1

#pragma GCC system_header

#if __cplusplus >= 201402L

#include <type_traits>
#include <system_error>
#include <experimental/netfwd>

#if __cplusplus > 201703L
# include <concepts>
#endif

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION
namespace experimental
{
namespace net
{
inline namespace v1
{

  /** @addtogroup networking-ts
   *  @{
   */

  template<typename _CompletionToken, typename _Signature, typename>
    class async_result;

  /// @cond undocumented

  // A type denoted by DEDUCED in the TS.
  template<typename _CompletionToken, typename _Signature>
    using __deduced_t = typename
      async_result<decay_t<_CompletionToken>, _Signature, void>::return_type;

  // Trait to check for construction from const/non-const lvalue/rvalue.
  template<typename _Tp>
    using __is_value_constructible = typename __and_<
      is_copy_constructible<_Tp>, is_move_constructible<_Tp>,
      is_constructible<_Tp, _Tp&>, is_constructible<_Tp, const _Tp&&>
      >::type;

  struct __throw_on_error
  {
    explicit
    __throw_on_error(const char* __msg) : _M_msg(__msg) { }

    ~__throw_on_error() noexcept(false)
    {
      if (_M_ec)
	_GLIBCXX_THROW_OR_ABORT(system_error(_M_ec, _M_msg));
    }

    __throw_on_error(const __throw_on_error&) = delete;
    __throw_on_error& operator=(const __throw_on_error&) = delete;

    operator error_code&() noexcept { return _M_ec; }

    const char* _M_msg;
    error_code _M_ec;
  };

  /// @endcond

  // Base class for types meeting both GettableSocketOption and
  // SettableSocketOption requirements.
  // The bool parameter allows __sockopt_base<bool> to have a
  // __sockopt_base<int, B> base class (so that its _M_value is an int)
  // but to have that be a distinct type from __sockopt_base<int>.
  template<typename _Tp, bool = true>
    struct __sockopt_base
    {
      __sockopt_base() = default;

      explicit
      __sockopt_base(_Tp __val) noexcept(noexcept(_Tp(std::declval<_Tp&>())))
      : _M_value(__val)
      { }

      template<typename _Protocol>
	void*
	data(const _Protocol&) noexcept
	{ return std::addressof(_M_value); }

      template<typename _Protocol>
	const void*
	data(const _Protocol&) const noexcept
	{ return std::addressof(_M_value); }

      template<typename _Protocol>
	size_t
	size(const _Protocol&) const noexcept
	{ return sizeof(_M_value); }

      template<typename _Protocol>
	void
	resize(const _Protocol&, size_t __s)
	{
	  if (__s != sizeof(_M_value))
	    __throw_length_error("invalid value for socket option resize");
	}

    protected:
      _Tp _M_value { };
    };

  // Base class for types meeting BooleanSocketOption requirements.
  template<>
    struct __sockopt_base<bool> : __sockopt_base<int, false>
    {
      __sockopt_base() = default;

      explicit
      __sockopt_base(bool __val) noexcept
      : __sockopt_base<int, false>(__val)
      { }

      bool value() const noexcept { return this->_M_value; }
      explicit operator bool() const noexcept { return value(); }
      bool operator!() const noexcept { return !value(); }
    };

  // Base class for types meeting IntegerSocketOption requirements.
  template<>
    struct __sockopt_base<int> : __sockopt_base<int, false>
    {
      using __sockopt_base<int, false>::__sockopt_base;

      int value() const noexcept { return this->_M_value; }
    };

  template<typename _Derived, typename _Tp = int>
    struct __sockopt_crtp : __sockopt_base<_Tp>
    {
      using __sockopt_base<_Tp>::__sockopt_base;

      _Derived&
      operator=(_Tp __value) noexcept(noexcept(__value = __value))
      {
	__sockopt_base<_Tp>::_M_value = __value;
	return static_cast<_Derived&>(*this);
      }

      template<typename _Protocol>
	int
	level(const _Protocol&) const noexcept
	{ return _Derived::_S_level; }

      template<typename _Protocol>
	int
	name(const _Protocol&) const noexcept
	{ return _Derived::_S_name; }
    };

namespace __detail
{
#if __cpp_lib_concepts
  template<typename _Tp>
    concept __protocol_like
      = copyable<_Tp> && requires { typename _Tp::endpoint; };

  // Endpoint requirements for non-extensible implementations.
  template<typename _Tp>
    concept __endpoint_base = semiregular<_Tp>
      && requires  { typename _Tp::protocol_type; }
      && __protocol_like<typename _Tp::protocol_type>
      && requires(const _Tp __a) {
	{ __a.protocol() } -> same_as<typename _Tp::protocol_type>;
      };

  // Endpoint requirements for extensible implementations.
  template<typename _Tp>
    concept __endpoint = __endpoint_base<_Tp>
      && requires (const _Tp& __a, _Tp& __b, size_t __s)
      {
	{ __a.data() } -> same_as<const void*>;
	{ __b.data() } -> same_as<void*>;
	{ __b.size() } -> same_as<size_t>;
	__b.resize(__s);
	{ __a.capacity() } -> same_as<size_t>;
      };

  // Protocol requirements for non-extensible implementations.
  template<typename _Tp>
    concept __protocol_base = __protocol_like<_Tp>
      && __endpoint_base<typename _Tp::endpoint>
      && same_as<typename _Tp::endpoint::protocol_type, _Tp>;

  // Protocol requirements for extensible implementations.
  template<typename _Tp>
    concept __protocol =  __protocol_base<_Tp>
      && __endpoint<typename _Tp::endpoint>
      && requires (const _Tp __a) {
	{ __a.family() } -> same_as<int>;
	{ __a.type() } -> same_as<int>;
	{ __a.protocol() } -> same_as<int>;
      };

  template<typename _Tp>
    concept __acceptable_protocol = __protocol<_Tp>
      && requires { typename _Tp::socket; }
      && move_constructible<typename _Tp::socket>
      && derived_from<typename _Tp::socket, basic_socket<_Tp>>;

  template<typename _Tp>
    concept __inet_protocol = __acceptable_protocol<_Tp>
      && equality_comparable<_Tp> && requires {
	{ _Tp::v4() } -> same_as<_Tp>;
	{ _Tp::v6() } -> same_as<_Tp>;
	typename _Tp::resolver;
      }
      && same_as<typename _Tp::resolver, ip::basic_resolver<_Tp>>;

#else
  // Check Endpoint requirements for extensible implementations
  template<typename _Tp, typename = void>
    struct __is_endpoint : false_type
    { };

  template<typename _Tp>
    auto
    __endpoint_reqs(const _Tp* __a = nullptr, _Tp* __b = nullptr)
    -> enable_if_t<__and_<
      is_default_constructible<_Tp>, __is_value_constructible<_Tp>,
      is_same<decltype(__a->protocol()), typename _Tp::protocol_type>,
      is_same<decltype(__a->data()), const void*>,
      is_same<decltype(__b->data()), void*>,
      is_same<decltype(__a->size()), size_t>,
      is_same<decltype(__a->capacity()), size_t>
      >::value,
    __void_t< typename _Tp::protocol_type::endpoint,
	      decltype(__b->resize(std::declval<size_t>())) >>;

  template<typename _Tp>
    struct __is_endpoint<_Tp, decltype(__detail::__endpoint_reqs<_Tp>())>
    : true_type
    { };

  // Check Protocol requirements for extensible implementations.
  template<typename _Tp, typename = void>
    struct __is_protocol
    : false_type { };

  template<typename _Tp>
    auto
    __protocol_reqs(const _Tp* __a = nullptr)
    -> enable_if_t<__and_<
      is_copy_constructible<_Tp>, is_copy_assignable<_Tp>,
      __is_endpoint<typename _Tp::endpoint>,
      is_same<decltype(__a->family()), int>,
      is_same<decltype(__a->type()), int>,
      is_same<decltype(__a->protocol()), int>
      >::value>;

  template<typename _Tp>
    struct __is_protocol<_Tp, decltype(__detail::__protocol_reqs<_Tp>())>
    : true_type
    { };

  // Check AcceptableProtocol requirements
  template<typename _Tp, typename = void>
    struct __is_acceptable_protocol
    : false_type { };

  template<typename _Tp>
    struct __is_acceptable_protocol<_Tp, __void_t<typename _Tp::socket>>
    : __and_<__is_protocol<_Tp>, is_move_constructible<typename _Tp::socket>,
	     is_convertible<typename _Tp::socket*, basic_socket<_Tp>*>>::type
    { };

  // Check InternetProtocol requirements
  template<typename _Tp, typename = void>
    struct __is_inet_protocol
    : false_type { };

  template<typename _Tp>
    auto
    __inet_proto_reqs(const _Tp* __a = nullptr)
    -> enable_if_t<__and_<
      __is_acceptable_protocol<_Tp>,
      is_same<typename _Tp::resolver, ip::basic_resolver<_Tp>>,
      is_same<decltype(_Tp::v4()), _Tp>,
      is_same<decltype(_Tp::v6()), _Tp>,
      is_convertible<decltype(*__a == *__a), bool>,
      is_convertible<decltype(*__a != *__a), bool>
      >::value>;

  template<typename _Tp>
    struct __is_inet_protocol<_Tp, decltype(__inet_proto_reqs<_Tp>())>
    : true_type { };

  // Variable templates for requirements (with same names as concepts above).

  template<typename _Tp>
    constexpr bool __endpoint = __is_endpoint<_Tp>::value;
  template<typename _Tp>
    constexpr bool __protocol = __is_protocol<_Tp>::value;
  template<typename _Tp>
    constexpr bool __acceptable_protocol = __is_acceptable_protocol<_Tp>::value;
#endif
} // namespace __detail

  /// @}

} // namespace v1
} // namespace net
} // namespace experimental
_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#endif // C++14

#endif // _GLIBCXX_EXPERIMENTAL_NET_H
