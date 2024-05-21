// std::this_thread::sleep_for/until declarations -*- C++ -*-

// Copyright (C) 2008-2022 Free Software Foundation, Inc.
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

/** @file bits/this_thread_sleep.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{thread}
 */

#ifndef _GLIBCXX_THIS_THREAD_SLEEP_H
#define _GLIBCXX_THIS_THREAD_SLEEP_H 1

#pragma GCC system_header

#if __cplusplus >= 201103L
#include <bits/chrono.h> // std::chrono::*

#ifdef _GLIBCXX_USE_NANOSLEEP
# include <cerrno>  // errno, EINTR
# include <time.h>  // nanosleep
#endif

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  /** @addtogroup threads
   *  @{
   */

  /** @namespace std::this_thread
   *  @brief ISO C++ 2011 namespace for interacting with the current thread
   *
   *  C++11 30.3.2 [thread.thread.this] Namespace this_thread.
   */
  namespace this_thread
  {
#ifndef _GLIBCXX_NO_SLEEP

#ifndef _GLIBCXX_USE_NANOSLEEP
    void
    __sleep_for(chrono::seconds, chrono::nanoseconds);
#endif

    /// this_thread::sleep_for
    template<typename _Rep, typename _Period>
      inline void
      sleep_for(const chrono::duration<_Rep, _Period>& __rtime)
      {
	if (__rtime <= __rtime.zero())
	  return;
	auto __s = chrono::duration_cast<chrono::seconds>(__rtime);
	auto __ns = chrono::duration_cast<chrono::nanoseconds>(__rtime - __s);
#ifdef _GLIBCXX_USE_NANOSLEEP
	struct ::timespec __ts =
	  {
	    static_cast<std::time_t>(__s.count()),
	    static_cast<long>(__ns.count())
	  };
	while (::nanosleep(&__ts, &__ts) == -1 && errno == EINTR)
	  { }
#else
	__sleep_for(__s, __ns);
#endif
      }

    /// this_thread::sleep_until
    template<typename _Clock, typename _Duration>
      inline void
      sleep_until(const chrono::time_point<_Clock, _Duration>& __atime)
      {
#if __cplusplus > 201703L
	static_assert(chrono::is_clock_v<_Clock>);
#endif
	auto __now = _Clock::now();
	if (_Clock::is_steady)
	  {
	    if (__now < __atime)
	      sleep_for(__atime - __now);
	    return;
	  }
	while (__now < __atime)
	  {
	    sleep_for(__atime - __now);
	    __now = _Clock::now();
	  }
      }
#endif // ! NO_SLEEP
  } // namespace this_thread

  /// @}

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace
#endif // C++11

#endif // _GLIBCXX_THIS_THREAD_SLEEP_H
