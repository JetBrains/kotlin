// Class filesystem::path -*- C++ -*-

// Copyright (C) 2014-2022 Free Software Foundation, Inc.
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

/** @file include/bits/fs_path.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{filesystem}
 */

#ifndef _GLIBCXX_FS_PATH_H
#define _GLIBCXX_FS_PATH_H 1

#if __cplusplus >= 201703L

#include <type_traits>
#include <locale>
#include <iosfwd>
#include <iomanip>
#include <codecvt>
#include <string_view>
#include <system_error>
#include <bits/stl_algobase.h>
#include <bits/stl_pair.h>
#include <bits/locale_conv.h>
#include <ext/concurrence.h>
#include <bits/shared_ptr.h>
#include <bits/unique_ptr.h>

#if __cplusplus > 201703L
# include <compare>
#endif

#if defined(_WIN32) && !defined(__CYGWIN__)
# define _GLIBCXX_FILESYSTEM_IS_WINDOWS 1
#endif

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

namespace filesystem
{
_GLIBCXX_BEGIN_NAMESPACE_CXX11

  class path;

  /// @cond undocumented
namespace __detail
{
  /// @addtogroup filesystem
  /// @{
  template<typename _CharT>
    inline constexpr bool __is_encoded_char = false;
  template<>
    inline constexpr bool __is_encoded_char<char> = true;
#ifdef _GLIBCXX_USE_CHAR8_T
  template<>
    inline constexpr bool __is_encoded_char<char8_t> = true;
#endif
#if _GLIBCXX_USE_WCHAR_T
  template<>
    inline constexpr bool __is_encoded_char<wchar_t> = true;
#endif
  template<>
    inline constexpr bool __is_encoded_char<char16_t> = true;
  template<>
    inline constexpr bool __is_encoded_char<char32_t> = true;

#if __cpp_concepts >= 201907L
  template<typename _Iter>
    using __safe_iterator_traits = std::iterator_traits<_Iter>;
#else
  template<typename _Iter>
    struct __safe_iterator_traits : std::iterator_traits<_Iter>
    { };

  // Protect against ill-formed iterator_traits specializations in C++17
  template<> struct __safe_iterator_traits<void*> { };
  template<> struct __safe_iterator_traits<const void*> { };
  template<> struct __safe_iterator_traits<volatile void*> { };
  template<> struct __safe_iterator_traits<const volatile void*> { };
#endif

  template<typename _Iter_traits, typename = void>
    struct __is_path_iter_src
    : false_type
    { };

  template<typename _Iter_traits>
    struct __is_path_iter_src<_Iter_traits,
			      void_t<typename _Iter_traits::value_type>>
    : bool_constant<__is_encoded_char<typename _Iter_traits::value_type>>
    { };

  template<typename _Source>
    inline constexpr bool __is_path_src
      = __is_path_iter_src<iterator_traits<decay_t<_Source>>>::value;

  template<>
    inline constexpr bool __is_path_src<path> = false;

  template<>
    inline constexpr bool __is_path_src<volatile path> = false;

  template<>
    inline constexpr bool __is_path_src<void*> = false;

  template<>
    inline constexpr bool __is_path_src<const void*> = false;

  template<>
    inline constexpr bool __is_path_src<volatile void*> = false;

  template<>
    inline constexpr bool __is_path_src<const volatile void*> = false;

  template<typename _CharT, typename _Traits, typename _Alloc>
    inline constexpr bool
      __is_path_src<basic_string<_CharT, _Traits, _Alloc>>
	= __is_encoded_char<_CharT>;

  template<typename _CharT, typename _Traits>
    inline constexpr bool
      __is_path_src<basic_string_view<_CharT, _Traits>>
	= __is_encoded_char<_CharT>;

  // SFINAE constraint for Source parameters as required by [fs.path.req].
  template<typename _Tp>
    using _Path = enable_if_t<__is_path_src<_Tp>, path>;

  // SFINAE constraint for InputIterator parameters as required by [fs.req].
  template<typename _Iter, typename _Tr = __safe_iterator_traits<_Iter>>
    using _Path2 = enable_if_t<__is_path_iter_src<_Tr>::value, path>;

#if __cpp_lib_concepts
  template<typename _Iter>
    constexpr bool __is_contiguous = std::contiguous_iterator<_Iter>;
#else
  template<typename _Iter>
    constexpr bool __is_contiguous = false;
#endif

  template<typename _Tp>
    constexpr bool __is_contiguous<_Tp*> = true;

  template<typename _Tp, typename _Seq>
    constexpr bool
    __is_contiguous<__gnu_cxx::__normal_iterator<_Tp*, _Seq>> = true;

#if !defined _GLIBCXX_FILESYSTEM_IS_WINDOWS && defined _GLIBCXX_USE_CHAR8_T
  // For POSIX treat char8_t sequences as char without encoding conversions.
  template<typename _EcharT>
    using __unified_u8_t
      = __conditional_t<is_same_v<_EcharT, char8_t>, char, _EcharT>;
#else
  template<typename _EcharT>
    using __unified_u8_t = _EcharT;
#endif

  // The __effective_range overloads convert a Source parameter into
  // either a basic_string_view<C> or basic_string<C> containing the
  // effective range of the Source, as defined in [fs.path.req].

  template<typename _CharT, typename _Traits, typename _Alloc>
    inline basic_string_view<_CharT>
    __effective_range(const basic_string<_CharT, _Traits, _Alloc>& __source)
    noexcept
    { return __source; }

  template<typename _CharT, typename _Traits>
    inline basic_string_view<_CharT>
    __effective_range(const basic_string_view<_CharT, _Traits>& __source)
    noexcept
    { return __source; }

  // Return the effective range of an NTCTS.
  template<typename _Source>
    auto
    __effective_range(const _Source& __source)
    {
      // Remove a level of normal/safe iterator indirection, or decay an array.
      using _Iter = decltype(std::__niter_base(__source));
      using value_type = typename iterator_traits<_Iter>::value_type;

      if constexpr (__is_contiguous<_Iter>)
	return basic_string_view<value_type>{&*__source};
      else
	{
	  // _Source is an input iterator that iterates over an NTCTS.
	  // Create a basic_string by reading until the null character.
	  basic_string<__unified_u8_t<value_type>> __str;
	  _Source __it = __source;
	  for (value_type __ch = *__it; __ch != value_type(); __ch = *++__it)
	    __str.push_back(__ch);
	  return __str;
	}
    }

  // The value type of a Source parameter's effective range.
  template<typename _Source>
    struct __source_value_type_impl
    {
      using type
	= typename __safe_iterator_traits<decay_t<_Source>>::value_type;
    };

  template<typename _CharT, typename _Traits, typename _Alloc>
    struct __source_value_type_impl<basic_string<_CharT, _Traits, _Alloc>>
    {
      using type = _CharT;
    };

  template<typename _CharT, typename _Traits>
    struct __source_value_type_impl<basic_string_view<_CharT, _Traits>>
    {
      using type = _CharT;
    };

  // The value type of a Source parameter's effective range.
  template<typename _Source>
    using __source_value_t = typename __source_value_type_impl<_Source>::type;

  // SFINAE helper to check that an effective range has value_type char,
  // as required by path constructors taking a std::locale parameter.
  // The type _Tp must have already been checked by _Path<Tp> or _Path2<_Tp>.
  template<typename _Tp, typename _Val = __source_value_t<_Tp>>
    using __value_type_is_char
      = std::enable_if_t<std::is_same_v<_Val, char>, _Val>;

  // As above, but also allows char8_t, as required by u8path
  // C++20 [depr.fs.path.factory]
  template<typename _Tp, typename _Val = __source_value_t<_Tp>>
    using __value_type_is_char_or_char8_t
      = std::enable_if_t<std::is_same_v<_Val, char>
#ifdef _GLIBCXX_USE_CHAR8_T
			 || std::is_same_v<_Val, char8_t>
#endif
			 , _Val>;

  // Create a basic_string<C> or basic_string_view<C> from an iterator range.
  template<typename _InputIterator>
    inline auto
    __string_from_range(_InputIterator __first, _InputIterator __last)
    {
      using _EcharT
	= typename std::iterator_traits<_InputIterator>::value_type;
      static_assert(__is_encoded_char<_EcharT>); // C++17 [fs.req]/3

      if constexpr (__is_contiguous<_InputIterator>)
	{
	  // For contiguous iterators we can just return a string view.
	  if (auto __len = __last - __first) [[__likely__]]
	    return basic_string_view<_EcharT>(&*__first, __len);
	  return basic_string_view<_EcharT>();
	}
      else
	{
	  // Conversion requires contiguous characters, so create a string.
	  return basic_string<__unified_u8_t<_EcharT>>(__first, __last);
	}
    }

  /// @} group filesystem
} // namespace __detail
  /// @endcond

  /// @addtogroup filesystem
  /// @{

  /// A filesystem path
  /// @ingroup filesystem
  class path
  {
  public:
#ifdef _GLIBCXX_FILESYSTEM_IS_WINDOWS
    using value_type = wchar_t;
    static constexpr value_type preferred_separator = L'\\';
#else
# ifdef _GLIBCXX_DOXYGEN
    /// Windows uses wchar_t for path::value_type, POSIX uses char.
    using value_type = __os_dependent__;
# else
    using value_type =  char;
# endif
    static constexpr value_type preferred_separator = '/';
#endif
    using string_type = std::basic_string<value_type>;

    /// path::format is ignored in this implementation
    enum format : unsigned char { native_format, generic_format, auto_format };

    // constructors and destructor

    path() noexcept { }

    path(const path& __p) = default;

    path(path&& __p) noexcept
    : _M_pathname(std::move(__p._M_pathname)),
      _M_cmpts(std::move(__p._M_cmpts))
    { __p.clear(); }

    path(string_type&& __source, format = auto_format)
    : _M_pathname(std::move(__source))
    { _M_split_cmpts(); }

    template<typename _Source,
	     typename _Require = __detail::_Path<_Source>>
      path(_Source const& __source, format = auto_format)
      : _M_pathname(_S_convert(__detail::__effective_range(__source)))
      { _M_split_cmpts(); }

    template<typename _InputIterator,
	     typename _Require = __detail::_Path2<_InputIterator>>
      path(_InputIterator __first, _InputIterator __last, format = auto_format)
      : _M_pathname(_S_convert(__detail::__string_from_range(__first, __last)))
      { _M_split_cmpts(); }

    template<typename _Source,
	     typename _Require = __detail::_Path<_Source>,
	     typename _Require2 = __detail::__value_type_is_char<_Source>>
      path(_Source const& __src, const locale& __loc, format = auto_format)
      : _M_pathname(_S_convert_loc(__detail::__effective_range(__src), __loc))
      { _M_split_cmpts(); }

    template<typename _InputIterator,
	     typename _Require = __detail::_Path2<_InputIterator>,
	     typename _Req2 = __detail::__value_type_is_char<_InputIterator>>
      path(_InputIterator __first, _InputIterator __last, const locale& __loc,
	   format = auto_format)
      : _M_pathname(_S_convert_loc(__first, __last, __loc))
      { _M_split_cmpts(); }

    ~path() = default;

    // assignments

    path& operator=(const path&);
    path& operator=(path&&) noexcept;
    path& operator=(string_type&& __source);
    path& assign(string_type&& __source);

    template<typename _Source>
      __detail::_Path<_Source>&
      operator=(_Source const& __source)
      { return *this = path(__source); }

    template<typename _Source>
      __detail::_Path<_Source>&
      assign(_Source const& __source)
      { return *this = path(__source); }

    template<typename _InputIterator>
      __detail::_Path2<_InputIterator>&
      assign(_InputIterator __first, _InputIterator __last)
      { return *this = path(__first, __last); }

    // appends

    path& operator/=(const path& __p);

    template<typename _Source>
      __detail::_Path<_Source>&
      operator/=(_Source const& __source)
      {
	_M_append(_S_convert(__detail::__effective_range(__source)));
	return *this;
      }

    template<typename _Source>
      __detail::_Path<_Source>&
      append(_Source const& __source)
      {
	_M_append(_S_convert(__detail::__effective_range(__source)));
	return *this;
      }

    template<typename _InputIterator>
      __detail::_Path2<_InputIterator>&
      append(_InputIterator __first, _InputIterator __last)
      {
	_M_append(_S_convert(__detail::__string_from_range(__first, __last)));
	return *this;
      }

    // concatenation

    path& operator+=(const path& __x);
    path& operator+=(const string_type& __x);
    path& operator+=(const value_type* __x);
    path& operator+=(value_type __x);
    path& operator+=(basic_string_view<value_type> __x);

    template<typename _Source>
      __detail::_Path<_Source>&
      operator+=(_Source const& __x) { return concat(__x); }

    template<typename _CharT>
      __detail::_Path2<_CharT*>&
      operator+=(_CharT __x);

    template<typename _Source>
      __detail::_Path<_Source>&
      concat(_Source const& __x)
      {
	_M_concat(_S_convert(__detail::__effective_range(__x)));
	return *this;
      }

    template<typename _InputIterator>
      __detail::_Path2<_InputIterator>&
      concat(_InputIterator __first, _InputIterator __last)
      {
	_M_concat(_S_convert(__detail::__string_from_range(__first, __last)));
	return *this;
      }

    // modifiers

    void clear() noexcept { _M_pathname.clear(); _M_split_cmpts(); }

    path& make_preferred();
    path& remove_filename();
    path& replace_filename(const path& __replacement);
    path& replace_extension(const path& __replacement = path());

    void swap(path& __rhs) noexcept;

    // native format observers

    const string_type&  native() const noexcept { return _M_pathname; }
    const value_type*   c_str() const noexcept { return _M_pathname.c_str(); }
    operator string_type() const { return _M_pathname; }

    template<typename _CharT, typename _Traits = std::char_traits<_CharT>,
	     typename _Allocator = std::allocator<_CharT>>
      std::basic_string<_CharT, _Traits, _Allocator>
      string(const _Allocator& __a = _Allocator()) const;

    std::string    string() const;
#if _GLIBCXX_USE_WCHAR_T
    std::wstring   wstring() const;
#endif
#ifdef _GLIBCXX_USE_CHAR8_T
    __attribute__((__abi_tag__("__u8")))
    std::u8string  u8string() const;
#else
    std::string    u8string() const;
#endif // _GLIBCXX_USE_CHAR8_T
    std::u16string u16string() const;
    std::u32string u32string() const;

    // generic format observers
    template<typename _CharT, typename _Traits = std::char_traits<_CharT>,
	     typename _Allocator = std::allocator<_CharT>>
      std::basic_string<_CharT, _Traits, _Allocator>
      generic_string(const _Allocator& __a = _Allocator()) const;

    std::string    generic_string() const;
#if _GLIBCXX_USE_WCHAR_T
    std::wstring   generic_wstring() const;
#endif
#ifdef _GLIBCXX_USE_CHAR8_T
    __attribute__((__abi_tag__("__u8")))
    std::u8string  generic_u8string() const;
#else
    std::string    generic_u8string() const;
#endif // _GLIBCXX_USE_CHAR8_T
    std::u16string generic_u16string() const;
    std::u32string generic_u32string() const;

    // compare

    int compare(const path& __p) const noexcept;
    int compare(const string_type& __s) const noexcept;
    int compare(const value_type* __s) const noexcept;
    int compare(basic_string_view<value_type> __s) const noexcept;

    // decomposition

    path root_name() const;
    path root_directory() const;
    path root_path() const;
    path relative_path() const;
    path parent_path() const;
    path filename() const;
    path stem() const;
    path extension() const;

    // query

    [[nodiscard]] bool empty() const noexcept { return _M_pathname.empty(); }
    bool has_root_name() const noexcept;
    bool has_root_directory() const noexcept;
    bool has_root_path() const noexcept;
    bool has_relative_path() const noexcept;
    bool has_parent_path() const noexcept;
    bool has_filename() const noexcept;
    bool has_stem() const noexcept;
    bool has_extension() const noexcept;
    bool is_absolute() const noexcept;
    bool is_relative() const noexcept { return !is_absolute(); }

    // generation
    path lexically_normal() const;
    path lexically_relative(const path& base) const;
    path lexically_proximate(const path& base) const;

    // iterators
    class iterator;
    using const_iterator = iterator;

    iterator begin() const noexcept;
    iterator end() const noexcept;

    /// Write a path to a stream
    template<typename _CharT, typename _Traits>
      friend std::basic_ostream<_CharT, _Traits>&
      operator<<(std::basic_ostream<_CharT, _Traits>& __os, const path& __p)
      {
	__os << std::quoted(__p.string<_CharT, _Traits>());
	return __os;
      }

    /// Read a path from a stream
    template<typename _CharT, typename _Traits>
      friend std::basic_istream<_CharT, _Traits>&
      operator>>(std::basic_istream<_CharT, _Traits>& __is, path& __p)
      {
	std::basic_string<_CharT, _Traits> __tmp;
	if (__is >> std::quoted(__tmp))
	  __p = std::move(__tmp);
	return __is;
      }

    // non-member operators

    /// Compare paths
    friend bool operator==(const path& __lhs, const path& __rhs) noexcept
    { return path::_S_compare(__lhs, __rhs) == 0; }

#if __cpp_lib_three_way_comparison
    /// Compare paths
    friend strong_ordering
    operator<=>(const path& __lhs, const path& __rhs) noexcept
    { return path::_S_compare(__lhs, __rhs) <=> 0; }
#else
    /// Compare paths
    friend bool operator!=(const path& __lhs, const path& __rhs) noexcept
    { return !(__lhs == __rhs); }

    /// Compare paths
    friend bool operator<(const path& __lhs, const path& __rhs) noexcept
    { return __lhs.compare(__rhs) < 0; }

    /// Compare paths
    friend bool operator<=(const path& __lhs, const path& __rhs) noexcept
    { return !(__rhs < __lhs); }

    /// Compare paths
    friend bool operator>(const path& __lhs, const path& __rhs) noexcept
    { return __rhs < __lhs; }

    /// Compare paths
    friend bool operator>=(const path& __lhs, const path& __rhs) noexcept
    { return !(__lhs < __rhs); }
#endif

    /// Append one path to another
    friend path operator/(const path& __lhs, const path& __rhs)
    {
      path __result(__lhs);
      __result /= __rhs;
      return __result;
    }

  private:
    enum class _Type : unsigned char {
      _Multi = 0, _Root_name, _Root_dir, _Filename
    };

    path(basic_string_view<value_type> __str, _Type __type)
    : _M_pathname(__str)
    {
      __glibcxx_assert(__type != _Type::_Multi);
      _M_cmpts.type(__type);
    }

    enum class _Split { _Stem, _Extension };

    void _M_append(basic_string_view<value_type>);
    void _M_concat(basic_string_view<value_type>);

    pair<const string_type*, size_t> _M_find_extension() const noexcept;

    // path::_S_convert creates a basic_string<value_type> or
    // basic_string_view<value_type> from a basic_string<C> or
    // basic_string_view<C>, for an encoded character type C,
    // performing the conversions required by [fs.path.type.cvt].
    template<typename _Tp>
      static auto
      _S_convert(_Tp __str)
      noexcept(is_same_v<typename _Tp::value_type, value_type>)
      {
	if constexpr (is_same_v<typename _Tp::value_type, value_type>)
	  return __str; // No conversion needed.
#if !defined _GLIBCXX_FILESYSTEM_IS_WINDOWS && defined _GLIBCXX_USE_CHAR8_T
	else if constexpr (is_same_v<_Tp, std::u8string>)
	  // Calling _S_convert<char8_t> will return a u8string_view that
	  // refers to __str and would dangle after this function returns.
	  // Return a string_type instead, to avoid dangling.
	  return string_type(_S_convert(__str.data(),
					__str.data() + __str.size()));
#endif
	else
	  return _S_convert(__str.data(), __str.data() + __str.size());
      }

    template<typename _EcharT>
      static auto
      _S_convert(const _EcharT* __first, const _EcharT* __last);

    // _S_convert_loc converts a range of char to string_type, using the
    // supplied locale for encoding conversions.

    static string_type
    _S_convert_loc(const char* __first, const char* __last,
		   const std::locale& __loc);

    template<typename _Iter>
      static string_type
      _S_convert_loc(_Iter __first, _Iter __last, const std::locale& __loc)
      {
	const auto __s = __detail::__string_from_range(__first, __last);
	return _S_convert_loc(__s.data(), __s.data() + __s.size(), __loc);
      }

    template<typename _Tp>
      static string_type
      _S_convert_loc(const _Tp& __s, const std::locale& __loc)
      {
	return _S_convert_loc(__s.data(), __s.data() + __s.size(), __loc);
      }

    template<typename _CharT, typename _Traits, typename _Allocator>
      static basic_string<_CharT, _Traits, _Allocator>
      _S_str_convert(basic_string_view<value_type>, const _Allocator&);

    // Returns lhs.compare(rhs), but defined after path::iterator is complete.
    __attribute__((__always_inline__))
    static int
    _S_compare(const path& __lhs, const path& __rhs) noexcept;

    void _M_split_cmpts();

    _Type _M_type() const noexcept { return _M_cmpts.type(); }

    string_type _M_pathname;

    struct _Cmpt;

    struct _List
    {
      using value_type = _Cmpt;
      using iterator = value_type*;
      using const_iterator = const value_type*;

      _List();
      _List(const _List&);
      _List(_List&&) = default;
      _List& operator=(const _List&);
      _List& operator=(_List&&) = default;
      ~_List() = default;

      _Type type() const noexcept
      { return _Type(reinterpret_cast<uintptr_t>(_M_impl.get()) & 0x3); }

      void type(_Type) noexcept;

      int size() const noexcept; // zero unless type() == _Type::_Multi
      bool empty() const noexcept; // true unless type() == _Type::_Multi
      void clear();
      void swap(_List& __l) noexcept { _M_impl.swap(__l._M_impl); }
      int capacity() const noexcept;
      void reserve(int, bool); ///< @pre type() == _Type::_Multi

      // All the member functions below here have a precondition !empty()
      // (and they should only be called from within the library).

      iterator begin() noexcept;
      iterator end() noexcept;
      const_iterator begin() const noexcept;
      const_iterator end() const noexcept;

      value_type& front() noexcept;
      value_type& back() noexcept;
      const value_type& front() const noexcept;
      const value_type& back() const noexcept;

      void pop_back();
      void _M_erase_from(const_iterator __pos); // erases [__pos,end())

      struct _Impl;
      struct _Impl_deleter
      {
	void operator()(_Impl*) const noexcept;
      };
      unique_ptr<_Impl, _Impl_deleter> _M_impl;
    };
    _List _M_cmpts;

    struct _Parser;
  };

  /// @{
  /// @relates std::filesystem::path

  inline void swap(path& __lhs, path& __rhs) noexcept { __lhs.swap(__rhs); }

  size_t hash_value(const path& __p) noexcept;

  /// @}

  /// Exception type thrown by the Filesystem library
  class filesystem_error : public std::system_error
  {
  public:
    filesystem_error(const string& __what_arg, error_code __ec);

    filesystem_error(const string& __what_arg, const path& __p1,
		     error_code __ec);

    filesystem_error(const string& __what_arg, const path& __p1,
		     const path& __p2, error_code __ec);

    filesystem_error(const filesystem_error&) = default;
    filesystem_error& operator=(const filesystem_error&) = default;

    // No move constructor or assignment operator.
    // Copy rvalues instead, so that _M_impl is not left empty.

    ~filesystem_error();

    const path& path1() const noexcept;
    const path& path2() const noexcept;
    const char* what() const noexcept;

  private:
    struct _Impl;
    std::__shared_ptr<const _Impl> _M_impl;
  };

  /// @cond undocumented
namespace __detail
{
  [[noreturn]] inline void
  __throw_conversion_error()
  {
    _GLIBCXX_THROW_OR_ABORT(filesystem_error(
	 "Cannot convert character sequence",
	 std::make_error_code(errc::illegal_byte_sequence)));
  }

#ifdef _GLIBCXX_FILESYSTEM_IS_WINDOWS
  template<typename _Tp>
    inline std::wstring
    __wstr_from_utf8(const _Tp& __str)
    {
      static_assert(std::is_same_v<typename _Tp::value_type, char>);
      std::wstring __wstr;
      // XXX This assumes native wide encoding is UTF-16.
      std::codecvt_utf8_utf16<wchar_t> __wcvt;
      const auto __p = __str.data();
      if (!__str_codecvt_in_all(__p, __p + __str.size(), __wstr, __wcvt))
	__detail::__throw_conversion_error();
      return __wstr;
    }
#endif

} // namespace __detail
  /// @endcond


  /** Create a path from a UTF-8-encoded sequence of char
   *
   * @relates std::filesystem::path
   */
  template<typename _InputIterator,
	   typename _Require = __detail::_Path2<_InputIterator>,
	   typename _CharT
	     = __detail::__value_type_is_char_or_char8_t<_InputIterator>>
    inline path
    u8path(_InputIterator __first, _InputIterator __last)
    {
#ifdef _GLIBCXX_FILESYSTEM_IS_WINDOWS
      if constexpr (is_same_v<_CharT, char>)
	return path{ __detail::__wstr_from_utf8(
	    __detail::__string_from_range(__first, __last)) };
      else
	return path{ __first, __last }; // constructor handles char8_t
#else
      // This assumes native normal encoding is UTF-8.
      return path{ __first, __last };
#endif
    }

  /** Create a path from a UTF-8-encoded sequence of char
   *
   * @relates std::filesystem::path
   */
  template<typename _Source,
	   typename _Require = __detail::_Path<_Source>,
	   typename _CharT = __detail::__value_type_is_char_or_char8_t<_Source>>
    inline path
    u8path(const _Source& __source)
    {
#ifdef _GLIBCXX_FILESYSTEM_IS_WINDOWS
      if constexpr (is_same_v<_CharT, char>)
	return path{ __detail::__wstr_from_utf8(
	    __detail::__effective_range(__source)) };
      else
	return path{ __source }; // constructor handles char8_t
#else
      // This assumes native normal encoding is UTF-8.
      return path{ __source };
#endif
    }

  /// @cond undocumented

  struct path::_Cmpt : path
  {
    _Cmpt(basic_string_view<value_type> __s, _Type __t, size_t __pos)
      : path(__s, __t), _M_pos(__pos) { }

    _Cmpt() : _M_pos(-1) { }

    size_t _M_pos;
  };

  template<typename _EcharT>
    auto
    path::_S_convert(const _EcharT* __f, const _EcharT* __l)
    {
      static_assert(__detail::__is_encoded_char<_EcharT>);

      if constexpr (is_same_v<_EcharT, value_type>)
	return basic_string_view<value_type>(__f, __l - __f);
#if !defined _GLIBCXX_FILESYSTEM_IS_WINDOWS && defined _GLIBCXX_USE_CHAR8_T
      else if constexpr (is_same_v<_EcharT, char8_t>)
	// For POSIX converting from char8_t to char is also 'noconv'
	return string_view(reinterpret_cast<const char*>(__f), __l - __f);
#endif
      else
	{
#ifdef _GLIBCXX_FILESYSTEM_IS_WINDOWS
	  std::wstring __wstr;
	  if constexpr (is_same_v<_EcharT, char>)
	    {
	      struct _UCvt : std::codecvt<wchar_t, char, std::mbstate_t>
	      { } __cvt;
	      if (__str_codecvt_in_all(__f, __l, __wstr, __cvt))
		return __wstr;
	    }
#ifdef _GLIBCXX_USE_CHAR8_T
	  else if constexpr (is_same_v<_EcharT, char8_t>)
	    {
	      const auto __f2 = reinterpret_cast<const char*>(__f);
	      return __detail::__wstr_from_utf8(string_view(__f2, __l - __f));
	    }
#endif
	  else // char16_t or char32_t
	    {
	      struct _UCvt : std::codecvt<_EcharT, char, std::mbstate_t>
	      { } __cvt;
	      std::string __str;
	      if (__str_codecvt_out_all(__f, __l, __str, __cvt))
		return __detail::__wstr_from_utf8(__str);
	    }
#else // ! windows
	  struct _UCvt : std::codecvt<_EcharT, char, std::mbstate_t>
	  { } __cvt;
	  std::string __str;
	  if (__str_codecvt_out_all(__f, __l, __str, __cvt))
	    return __str;
#endif
	  __detail::__throw_conversion_error();
	}
    }

  /// @endcond

  /// An iterator for the components of a path
  class path::iterator
  {
  public:
    using difference_type	= std::ptrdiff_t;
    using value_type		= path;
    using reference		= const path&;
    using pointer		= const path*;
    using iterator_category	= std::bidirectional_iterator_tag;

    iterator() noexcept : _M_path(nullptr), _M_cur(), _M_at_end() { }

    iterator(const iterator&) = default;
    iterator& operator=(const iterator&) = default;

    reference operator*() const noexcept;
    pointer   operator->() const noexcept { return std::__addressof(**this); }

    iterator& operator++() noexcept;

    iterator  operator++(int) noexcept
    { auto __tmp = *this; ++*this; return __tmp; }

    iterator& operator--() noexcept;

    iterator  operator--(int) noexcept
    { auto __tmp = *this; --*this; return __tmp; }

    friend bool
    operator==(const iterator& __lhs, const iterator& __rhs) noexcept
    { return __lhs._M_equals(__rhs); }

    friend bool
    operator!=(const iterator& __lhs, const iterator& __rhs) noexcept
    { return !__lhs._M_equals(__rhs); }

  private:
    friend class path;

    bool
    _M_is_multi() const noexcept
    { return _M_path->_M_type() == _Type::_Multi; }

    friend difference_type
    __path_iter_distance(const iterator& __first, const iterator& __last)
    noexcept
    {
      __glibcxx_assert(__first._M_path != nullptr);
      __glibcxx_assert(__first._M_path == __last._M_path);
      if (__first._M_is_multi())
	return std::distance(__first._M_cur, __last._M_cur);
      else if (__first._M_at_end == __last._M_at_end)
	return 0;
      else
	return __first._M_at_end ? -1 : 1;
    }

    friend void
    __path_iter_advance(iterator& __i, difference_type __n) noexcept
    {
      if (__n == 1)
	++__i;
      else if (__n == -1)
	--__i;
      else if (__n != 0)
	{
	  __glibcxx_assert(__i._M_path != nullptr);
	  __glibcxx_assert(__i._M_is_multi());
	  // __glibcxx_assert(__i._M_path->_M_cmpts.end() - __i._M_cur >= __n);
	  __i._M_cur += __n;
	}
    }

    iterator(const path* __path, path::_List::const_iterator __iter) noexcept
    : _M_path(__path), _M_cur(__iter), _M_at_end()
    { }

    iterator(const path* __path, bool __at_end) noexcept
    : _M_path(__path), _M_cur(), _M_at_end(__at_end)
    { }

    bool _M_equals(iterator) const noexcept;

    const path* 		_M_path;
    path::_List::const_iterator _M_cur;
    bool			_M_at_end;  // only used when type != _Multi
  };


  inline path&
  path::operator=(path&& __p) noexcept
  {
    if (&__p == this) [[__unlikely__]]
      return *this;

    _M_pathname = std::move(__p._M_pathname);
    _M_cmpts = std::move(__p._M_cmpts);
    __p.clear();
    return *this;
  }

  inline path&
  path::operator=(string_type&& __source)
  { return *this = path(std::move(__source)); }

  inline path&
  path::assign(string_type&& __source)
  { return *this = path(std::move(__source)); }

  inline path&
  path::operator+=(const string_type& __x)
  {
    _M_concat(__x);
    return *this;
  }

  inline path&
  path::operator+=(const value_type* __x)
  {
    _M_concat(__x);
    return *this;
  }

  inline path&
  path::operator+=(value_type __x)
  {
    _M_concat(basic_string_view<value_type>(&__x, 1));
    return *this;
  }

  inline path&
  path::operator+=(basic_string_view<value_type> __x)
  {
    _M_concat(__x);
    return *this;
  }

  template<typename _CharT>
    inline __detail::_Path2<_CharT*>&
    path::operator+=(const _CharT __x)
    {
      _M_concat(_S_convert(&__x, &__x + 1));
      return *this;
    }

  inline path&
  path::make_preferred()
  {
#ifdef _GLIBCXX_FILESYSTEM_IS_WINDOWS
    auto __pos = _M_pathname.find(L'/');
    while (__pos != _M_pathname.npos)
      {
	_M_pathname[__pos] = preferred_separator;
	__pos = _M_pathname.find(L'/', __pos);
      }
#endif
    return *this;
  }

  inline void path::swap(path& __rhs) noexcept
  {
    _M_pathname.swap(__rhs._M_pathname);
    _M_cmpts.swap(__rhs._M_cmpts);
  }

  /// @cond undocumented
  template<typename _CharT, typename _Traits, typename _Allocator>
    std::basic_string<_CharT, _Traits, _Allocator>
    path::_S_str_convert(basic_string_view<value_type> __str,
			 const _Allocator& __a)
    {
      static_assert(!is_same_v<_CharT, value_type>);

      using _WString = basic_string<_CharT, _Traits, _Allocator>;

      if (__str.size() == 0)
	return _WString(__a);

#ifdef _GLIBCXX_FILESYSTEM_IS_WINDOWS
      // First convert native string from UTF-16 to to UTF-8.
      // XXX This assumes that the execution wide-character set is UTF-16.
      std::codecvt_utf8_utf16<value_type> __cvt;

      using _CharAlloc = __alloc_rebind<_Allocator, char>;
      using _String = basic_string<char, char_traits<char>, _CharAlloc>;
      _String __u8str{_CharAlloc{__a}};
      const value_type* __wfirst = __str.data();
      const value_type* __wlast = __wfirst + __str.size();
      if (__str_codecvt_out_all(__wfirst, __wlast, __u8str, __cvt)) {
      if constexpr (is_same_v<_CharT, char>)
	return __u8str; // XXX assumes native ordinary encoding is UTF-8.
      else {

      const char* __first = __u8str.data();
      const char* __last = __first + __u8str.size();
#else
      const value_type* __first = __str.data();
      const value_type* __last = __first + __str.size();
#endif

      // Convert UTF-8 string to requested format.
#ifdef _GLIBCXX_USE_CHAR8_T
      if constexpr (is_same_v<_CharT, char8_t>)
	return _WString(__first, __last, __a);
      else
#endif
	{
	  // Convert UTF-8 to wide string.
	  _WString __wstr(__a);
	  struct _UCvt : std::codecvt<_CharT, char, std::mbstate_t> { } __cvt;
	  if (__str_codecvt_in_all(__first, __last, __wstr, __cvt))
	    return __wstr;
	}

#ifdef _GLIBCXX_FILESYSTEM_IS_WINDOWS
      } }
#endif
      __detail::__throw_conversion_error();
    }
  /// @endcond

  template<typename _CharT, typename _Traits, typename _Allocator>
    inline basic_string<_CharT, _Traits, _Allocator>
    path::string(const _Allocator& __a) const
    {
      if constexpr (is_same_v<_CharT, value_type>)
	return { _M_pathname.c_str(), _M_pathname.length(), __a };
      else
	return _S_str_convert<_CharT, _Traits>(_M_pathname, __a);
    }

  inline std::string
  path::string() const { return string<char>(); }

#if _GLIBCXX_USE_WCHAR_T
  inline std::wstring
  path::wstring() const { return string<wchar_t>(); }
#endif

#ifdef _GLIBCXX_USE_CHAR8_T
  inline std::u8string
  path::u8string() const { return string<char8_t>(); }
#else
  inline std::string
  path::u8string() const
  {
#ifdef _GLIBCXX_FILESYSTEM_IS_WINDOWS
    std::string __str;
    // convert from native wide encoding (assumed to be UTF-16) to UTF-8
    std::codecvt_utf8_utf16<value_type> __cvt;
    const value_type* __first = _M_pathname.data();
    const value_type* __last = __first + _M_pathname.size();
    if (__str_codecvt_out_all(__first, __last, __str, __cvt))
      return __str;
    __detail::__throw_conversion_error();
#else
    return _M_pathname;
#endif
  }
#endif // _GLIBCXX_USE_CHAR8_T

  inline std::u16string
  path::u16string() const { return string<char16_t>(); }

  inline std::u32string
  path::u32string() const { return string<char32_t>(); }

  template<typename _CharT, typename _Traits, typename _Allocator>
    inline std::basic_string<_CharT, _Traits, _Allocator>
    path::generic_string(const _Allocator& __a) const
    {
#ifdef _GLIBCXX_FILESYSTEM_IS_WINDOWS
      const value_type __slash = L'/';
#else
      const value_type __slash = '/';
#endif
      using _Alloc2 = typename allocator_traits<_Allocator>::template
	rebind_alloc<value_type>;
      basic_string<value_type, char_traits<value_type>, _Alloc2> __str(__a);

      if (_M_type() == _Type::_Root_dir)
	__str.assign(1, __slash);
      else
	{
	  __str.reserve(_M_pathname.size());
	  bool __add_slash = false;
	  for (auto& __elem : *this)
	    {
#ifdef _GLIBCXX_FILESYSTEM_IS_WINDOWS
	      if (__elem._M_type() == _Type::_Root_dir)
		{
		  __str += __slash;
		  continue;
		}
#endif
	      if (__add_slash)
		__str += __slash;
	      __str += basic_string_view<value_type>(__elem._M_pathname);
	      __add_slash = __elem._M_type() == _Type::_Filename;
	    }
	}

      if constexpr (is_same_v<_CharT, value_type>)
	return __str;
      else
	return _S_str_convert<_CharT, _Traits>(__str, __a);
    }

  inline std::string
  path::generic_string() const
  { return generic_string<char>(); }

#if _GLIBCXX_USE_WCHAR_T
  inline std::wstring
  path::generic_wstring() const
  { return generic_string<wchar_t>(); }
#endif

#ifdef _GLIBCXX_USE_CHAR8_T
  inline std::u8string
  path::generic_u8string() const
  { return generic_string<char8_t>(); }
#else
  inline std::string
  path::generic_u8string() const
  { return generic_string(); }
#endif

  inline std::u16string
  path::generic_u16string() const
  { return generic_string<char16_t>(); }

  inline std::u32string
  path::generic_u32string() const
  { return generic_string<char32_t>(); }

  inline int
  path::compare(const string_type& __s) const noexcept
  { return compare(basic_string_view<value_type>(__s)); }

  inline int
  path::compare(const value_type* __s) const noexcept
  { return compare(basic_string_view<value_type>(__s)); }

  inline path
  path::filename() const
  {
    if (empty())
      return {};
    else if (_M_type() == _Type::_Filename)
      return *this;
    else if (_M_type() == _Type::_Multi)
      {
	if (_M_pathname.back() == preferred_separator)
	  return {};
	auto& __last = *--end();
	if (__last._M_type() == _Type::_Filename)
	  return __last;
      }
    return {};
  }

  inline path
  path::stem() const
  {
    auto ext = _M_find_extension();
    if (ext.first && ext.second != 0)
      return path{ext.first->substr(0, ext.second)};
    return {};
  }

  inline path
  path::extension() const
  {
    auto ext = _M_find_extension();
    if (ext.first && ext.second != string_type::npos)
      return path{ext.first->substr(ext.second)};
    return {};
  }

  inline bool
  path::has_stem() const noexcept
  {
    auto ext = _M_find_extension();
    return ext.first && ext.second != 0;
  }

  inline bool
  path::has_extension() const noexcept
  {
    auto ext = _M_find_extension();
    return ext.first && ext.second != string_type::npos;
  }

  inline bool
  path::is_absolute() const noexcept
  {
#ifdef _GLIBCXX_FILESYSTEM_IS_WINDOWS
    return has_root_name() && has_root_directory();
#else
    return has_root_directory();
#endif
  }

  inline path::iterator
  path::begin() const noexcept
  {
    if (_M_type() == _Type::_Multi)
      return iterator(this, _M_cmpts.begin());
    return iterator(this, empty());
  }

  inline path::iterator
  path::end() const noexcept
  {
    if (_M_type() == _Type::_Multi)
      return iterator(this, _M_cmpts.end());
    return iterator(this, true);
  }

  inline path::iterator&
  path::iterator::operator++() noexcept
  {
    __glibcxx_assert(_M_path != nullptr);
    if (_M_is_multi())
      {
	__glibcxx_assert(_M_cur != _M_path->_M_cmpts.end());
	++_M_cur;
      }
    else
      {
	__glibcxx_assert(!_M_at_end);
	_M_at_end = true;
      }
    return *this;
  }

  inline path::iterator&
  path::iterator::operator--() noexcept
  {
    __glibcxx_assert(_M_path != nullptr);
    if (_M_is_multi())
      {
	__glibcxx_assert(_M_cur != _M_path->_M_cmpts.begin());
	--_M_cur;
      }
    else
      {
	__glibcxx_assert(_M_at_end);
	_M_at_end = false;
      }
    return *this;
  }

  inline path::iterator::reference
  path::iterator::operator*() const noexcept
  {
    __glibcxx_assert(_M_path != nullptr);
    if (_M_is_multi())
      {
	__glibcxx_assert(_M_cur != _M_path->_M_cmpts.end());
	return *_M_cur;
      }
    return *_M_path;
  }

  inline bool
  path::iterator::_M_equals(iterator __rhs) const noexcept
  {
    if (_M_path != __rhs._M_path)
      return false;
    if (_M_path == nullptr)
      return true;
    if (_M_is_multi())
      return _M_cur == __rhs._M_cur;
    return _M_at_end == __rhs._M_at_end;
  }

  // Define this now that path and path::iterator are complete.
  // It needs to consider the string_view(Range&&) constructor during
  // overload resolution, which depends on whether range<path> is satisfied,
  // which depends on whether path::iterator is complete.
  inline int
  path::_S_compare(const path& __lhs, const path& __rhs) noexcept
  { return __lhs.compare(__rhs); }

  /// @} group filesystem
_GLIBCXX_END_NAMESPACE_CXX11
} // namespace filesystem

/// @cond undocumented

inline ptrdiff_t
distance(filesystem::path::iterator __first, filesystem::path::iterator __last)
noexcept
{ return __path_iter_distance(__first, __last); }

template<typename _Distance>
  inline void
  advance(filesystem::path::iterator& __i, _Distance __n) noexcept
  { __path_iter_advance(__i, static_cast<ptrdiff_t>(__n)); }

extern template class __shared_ptr<const filesystem::filesystem_error::_Impl>;

/// @endcond

// _GLIBCXX_RESOLVE_LIB_DEFECTS
// 3657. std::hash<std::filesystem::path> is not enabled
template<>
  struct hash<filesystem::path>
  {
    size_t
    operator()(const filesystem::path& __p) const noexcept
    { return filesystem::hash_value(__p); }
  };

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#endif // C++17

#endif // _GLIBCXX_FS_PATH_H
