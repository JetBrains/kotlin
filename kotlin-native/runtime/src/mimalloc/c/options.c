/* ----------------------------------------------------------------------------
Copyright (c) 2018, Microsoft Research, Daan Leijen
This is free software; you can redistribute it and/or modify it under the
terms of the MIT license. A copy of the license can be found in the file
"licenses/third_party/mimalloc_LICENSE.txt" at the root of this distribution.
-----------------------------------------------------------------------------*/
#include "mimalloc.h"
#include "mimalloc-internal.h"
#include "mimalloc-atomic.h"

#include <stdio.h>
#include <stdlib.h> // strtol
#include <string.h> // strncpy, strncat, strlen, strstr
#include <ctype.h>  // toupper
#include <stdarg.h>

#ifdef _MSC_VER
#pragma warning(disable:4996)   // strncpy, strncat
#endif


static uintptr_t mi_max_error_count = 16;  // stop outputting errors after this

static void mi_add_stderr_output();

int mi_version(void) mi_attr_noexcept {
  return MI_MALLOC_VERSION;
}

#ifdef _WIN32
#include <conio.h>
#endif

// --------------------------------------------------------
// Options
// These can be accessed by multiple threads and may be
// concurrently initialized, but an initializing data race
// is ok since they resolve to the same value.
// --------------------------------------------------------
typedef enum mi_init_e {
  UNINIT,       // not yet initialized
  DEFAULTED,    // not found in the environment, use default value
  INITIALIZED   // found in environment or set explicitly
} mi_init_t;

typedef struct mi_option_desc_s {
  long        value;  // the value
  mi_init_t   init;   // is it initialized yet? (from the environment)
  mi_option_t option; // for debugging: the option index should match the option
  const char* name;   // option name without `mimalloc_` prefix
} mi_option_desc_t;

#define MI_OPTION(opt)        mi_option_##opt, #opt
#define MI_OPTION_DESC(opt)   {0, UNINIT, MI_OPTION(opt) }

static mi_option_desc_t options[_mi_option_last] =
{
  // stable options
#if MI_DEBUG || defined(MI_SHOW_ERRORS)
  { 1, UNINIT, MI_OPTION(show_errors) },
#else
  { 0, UNINIT, MI_OPTION(show_errors) },
#endif
  { 0, UNINIT, MI_OPTION(show_stats) },
  { 0, UNINIT, MI_OPTION(verbose) },

  // the following options are experimental and not all combinations make sense.
  { 1, UNINIT, MI_OPTION(eager_commit) },        // commit per segment directly (4MiB)  (but see also `eager_commit_delay`)
  #if defined(_WIN32) || (MI_INTPTR_SIZE <= 4)   // and other OS's without overcommit?
  { 0, UNINIT, MI_OPTION(eager_region_commit) },
  { 1, UNINIT, MI_OPTION(reset_decommits) },     // reset decommits memory
  #else
  { 1, UNINIT, MI_OPTION(eager_region_commit) },
  { 0, UNINIT, MI_OPTION(reset_decommits) },     // reset uses MADV_FREE/MADV_DONTNEED
  #endif
  { 0, UNINIT, MI_OPTION(large_os_pages) },      // use large OS pages, use only with eager commit to prevent fragmentation of VMA's
  { 0, UNINIT, MI_OPTION(reserve_huge_os_pages) },
  { 0, UNINIT, MI_OPTION(segment_cache) },       // cache N segments per thread
  { 1, UNINIT, MI_OPTION(page_reset) },          // reset page memory on free
  { 0, UNINIT, MI_OPTION(abandoned_page_reset) },// reset free page memory when a thread terminates
  { 0, UNINIT, MI_OPTION(segment_reset) },       // reset segment memory on free (needs eager commit)
#if defined(__NetBSD__)
  { 0, UNINIT, MI_OPTION(eager_commit_delay) },  // the first N segments per thread are not eagerly committed
#else
  { 1, UNINIT, MI_OPTION(eager_commit_delay) },  // the first N segments per thread are not eagerly committed (but per page in the segment on demand)
#endif
  { 100, UNINIT, MI_OPTION(reset_delay) },       // reset delay in milli-seconds
  { 0,   UNINIT, MI_OPTION(use_numa_nodes) },    // 0 = use available numa nodes, otherwise use at most N nodes.
  { 100, UNINIT, MI_OPTION(os_tag) },            // only apple specific for now but might serve more or less related purpose
  { 16,  UNINIT, MI_OPTION(max_errors) }         // maximum errors that are output
};

static void mi_option_init(mi_option_desc_t* desc);

void _mi_options_init(void) {
  // called on process load; should not be called before the CRT is initialized!
  // (e.g. do not call this from process_init as that may run before CRT initialization)
  mi_add_stderr_output(); // now it safe to use stderr for output
  for(int i = 0; i < _mi_option_last; i++ ) {
    mi_option_t option = (mi_option_t)i;
    long l = mi_option_get(option); UNUSED(l); // initialize
    if (option != mi_option_verbose) {
      mi_option_desc_t* desc = &options[option];
      _mi_verbose_message("option '%s': %ld\n", desc->name, desc->value);
    }
  }
  mi_max_error_count = mi_option_get(mi_option_max_errors);
}

long mi_option_get(mi_option_t option) {
  mi_assert(option >= 0 && option < _mi_option_last);
  mi_option_desc_t* desc = &options[option];
  mi_assert(desc->option == option);  // index should match the option
  if (mi_unlikely(desc->init == UNINIT)) {
    mi_option_init(desc);
  }
  return desc->value;
}

void mi_option_set(mi_option_t option, long value) {
  mi_assert(option >= 0 && option < _mi_option_last);
  mi_option_desc_t* desc = &options[option];
  mi_assert(desc->option == option);  // index should match the option
  desc->value = value;
  desc->init = INITIALIZED;
}

void mi_option_set_default(mi_option_t option, long value) {
  mi_assert(option >= 0 && option < _mi_option_last);
  mi_option_desc_t* desc = &options[option];
  if (desc->init != INITIALIZED) {
    desc->value = value;
  }
}

bool mi_option_is_enabled(mi_option_t option) {
  return (mi_option_get(option) != 0);
}

void mi_option_set_enabled(mi_option_t option, bool enable) {
  mi_option_set(option, (enable ? 1 : 0));
}

void mi_option_set_enabled_default(mi_option_t option, bool enable) {
  mi_option_set_default(option, (enable ? 1 : 0));
}

void mi_option_enable(mi_option_t option) {
  mi_option_set_enabled(option,true);
}

void mi_option_disable(mi_option_t option) {
  mi_option_set_enabled(option,false);
}


static void mi_out_stderr(const char* msg, void* arg) {
  UNUSED(arg);
  #ifdef _WIN32
  // on windows with redirection, the C runtime cannot handle locale dependent output
  // after the main thread closes so we use direct console output.
  if (!_mi_preloading()) { _cputs(msg); }
  #else
  fputs(msg, stderr);
  #endif
}

// Since an output function can be registered earliest in the `main`
// function we also buffer output that happens earlier. When
// an output function is registered it is called immediately with
// the output up to that point.
#ifndef MI_MAX_DELAY_OUTPUT
#define MI_MAX_DELAY_OUTPUT ((uintptr_t)(32*1024))
#endif
static char out_buf[MI_MAX_DELAY_OUTPUT+1];
static _Atomic(uintptr_t) out_len;

static void mi_out_buf(const char* msg, void* arg) {
  UNUSED(arg);
  if (msg==NULL) return;
  if (mi_atomic_load_relaxed(&out_len)>=MI_MAX_DELAY_OUTPUT) return;
  size_t n = strlen(msg);
  if (n==0) return;
  // claim space
  uintptr_t start = mi_atomic_add_acq_rel(&out_len, n);
  if (start >= MI_MAX_DELAY_OUTPUT) return;
  // check bound
  if (start+n >= MI_MAX_DELAY_OUTPUT) {
    n = MI_MAX_DELAY_OUTPUT-start-1;
  }
  memcpy(&out_buf[start], msg, n);
}

static void mi_out_buf_flush(mi_output_fun* out, bool no_more_buf, void* arg) {
  if (out==NULL) return;
  // claim (if `no_more_buf == true`, no more output will be added after this point)
  size_t count = mi_atomic_add_acq_rel(&out_len, (no_more_buf ? MI_MAX_DELAY_OUTPUT : 1));
  // and output the current contents
  if (count>MI_MAX_DELAY_OUTPUT) count = MI_MAX_DELAY_OUTPUT;
  out_buf[count] = 0;
  out(out_buf,arg);
  if (!no_more_buf) {
    out_buf[count] = '\n'; // if continue with the buffer, insert a newline
  }
}


// Once this module is loaded, switch to this routine
// which outputs to stderr and the delayed output buffer.
static void mi_out_buf_stderr(const char* msg, void* arg) {
  mi_out_stderr(msg,arg);
  mi_out_buf(msg,arg);
}



// --------------------------------------------------------
// Default output handler
// --------------------------------------------------------

// Should be atomic but gives errors on many platforms as generally we cannot cast a function pointer to a uintptr_t.
// For now, don't register output from multiple threads.
static mi_output_fun* volatile mi_out_default; // = NULL
static _Atomic(void*) mi_out_arg; // = NULL

static mi_output_fun* mi_out_get_default(void** parg) {
  if (parg != NULL) { *parg = mi_atomic_load_ptr_acquire(void,&mi_out_arg); }
  mi_output_fun* out = mi_out_default;
  return (out == NULL ? &mi_out_buf : out);
}

void mi_register_output(mi_output_fun* out, void* arg) mi_attr_noexcept {
  mi_out_default = (out == NULL ? &mi_out_stderr : out); // stop using the delayed output buffer
  mi_atomic_store_ptr_release(void,&mi_out_arg, arg);
  if (out!=NULL) mi_out_buf_flush(out,true,arg);         // output all the delayed output now
}

// add stderr to the delayed output after the module is loaded
static void mi_add_stderr_output() {
  mi_assert_internal(mi_out_default == NULL);
  mi_out_buf_flush(&mi_out_stderr, false, NULL); // flush current contents to stderr
  mi_out_default = &mi_out_buf_stderr;           // and add stderr to the delayed output
}

// --------------------------------------------------------
// Messages, all end up calling `_mi_fputs`.
// --------------------------------------------------------
static _Atomic(uintptr_t) error_count; // = 0;  // when MAX_ERROR_COUNT stop emitting errors and warnings

// When overriding malloc, we may recurse into mi_vfprintf if an allocation
// inside the C runtime causes another message.
static mi_decl_thread bool recurse = false;

static bool mi_recurse_enter(void) {
  #ifdef MI_TLS_RECURSE_GUARD
  if (_mi_preloading()) return true;
  #endif
  if (recurse) return false;
  recurse = true;
  return true;
}

static void mi_recurse_exit(void) {
  #ifdef MI_TLS_RECURSE_GUARD
  if (_mi_preloading()) return;
  #endif
  recurse = false;
}

void _mi_fputs(mi_output_fun* out, void* arg, const char* prefix, const char* message) {
  if (out==NULL || (FILE*)out==stdout || (FILE*)out==stderr) { // TODO: use mi_out_stderr for stderr?
    if (!mi_recurse_enter()) return;
    out = mi_out_get_default(&arg);
    if (prefix != NULL) out(prefix, arg);
    out(message, arg);
    mi_recurse_exit();
  }
  else {
    if (prefix != NULL) out(prefix, arg);
    out(message, arg);
  }
}

// Define our own limited `fprintf` that avoids memory allocation.
// We do this using `snprintf` with a limited buffer.
static void mi_vfprintf( mi_output_fun* out, void* arg, const char* prefix, const char* fmt, va_list args ) {
  char buf[512];
  if (fmt==NULL) return;
  if (!mi_recurse_enter()) return;
  vsnprintf(buf,sizeof(buf)-1,fmt,args);
  mi_recurse_exit();
  _mi_fputs(out,arg,prefix,buf);
}

void _mi_fprintf( mi_output_fun* out, void* arg, const char* fmt, ... ) {
  va_list args;
  va_start(args,fmt);
  mi_vfprintf(out,arg,NULL,fmt,args);
  va_end(args);
}

void _mi_trace_message(const char* fmt, ...) {
  if (mi_option_get(mi_option_verbose) <= 1) return;  // only with verbose level 2 or higher
  va_list args;
  va_start(args, fmt);
  mi_vfprintf(NULL, NULL, "mimalloc: ", fmt, args);
  va_end(args);
}

void _mi_verbose_message(const char* fmt, ...) {
  if (!mi_option_is_enabled(mi_option_verbose)) return;
  va_list args;
  va_start(args,fmt);
  mi_vfprintf(NULL, NULL, "mimalloc: ", fmt, args);
  va_end(args);
}

static void mi_show_error_message(const char* fmt, va_list args) {
  if (!mi_option_is_enabled(mi_option_show_errors) && !mi_option_is_enabled(mi_option_verbose)) return;
  if (mi_atomic_increment_acq_rel(&error_count) > mi_max_error_count) return;
  mi_vfprintf(NULL, NULL, "mimalloc: error: ", fmt, args);
}

void _mi_warning_message(const char* fmt, ...) {
  if (!mi_option_is_enabled(mi_option_show_errors) && !mi_option_is_enabled(mi_option_verbose)) return;
  if (mi_atomic_increment_acq_rel(&error_count) > mi_max_error_count) return;
  va_list args;
  va_start(args,fmt);
  mi_vfprintf(NULL, NULL, "mimalloc: warning: ", fmt, args);
  va_end(args);
}


#if MI_DEBUG
void _mi_assert_fail(const char* assertion, const char* fname, unsigned line, const char* func ) {
  _mi_fprintf(NULL, NULL, "mimalloc: assertion failed: at \"%s\":%u, %s\n  assertion: \"%s\"\n", fname, line, (func==NULL?"":func), assertion);
  abort();
}
#endif

// --------------------------------------------------------
// Errors
// --------------------------------------------------------

static mi_error_fun* volatile  mi_error_handler; // = NULL
static _Atomic(void*) mi_error_arg;     // = NULL

static void mi_error_default(int err) {
  UNUSED(err);
#if (MI_DEBUG>0)
  if (err==EFAULT) {
    #ifdef _MSC_VER
    __debugbreak();
    #endif
    abort();
  }
#endif
#if (MI_SECURE>0)
  if (err==EFAULT) {  // abort on serious errors in secure mode (corrupted meta-data)
    abort();
  }
#endif
#if defined(MI_XMALLOC)
  if (err==ENOMEM || err==EOVERFLOW) { // abort on memory allocation fails in xmalloc mode
    abort();
  }
#endif
}

void mi_register_error(mi_error_fun* fun, void* arg) {
  mi_error_handler = fun;  // can be NULL
  mi_atomic_store_ptr_release(void,&mi_error_arg, arg);
}

void _mi_error_message(int err, const char* fmt, ...) {
  // show detailed error message
  va_list args;
  va_start(args, fmt);
  mi_show_error_message(fmt, args);
  va_end(args);
  // and call the error handler which may abort (or return normally)
  if (mi_error_handler != NULL) {
    mi_error_handler(err, mi_atomic_load_ptr_acquire(void,&mi_error_arg));
  }
  else {
    mi_error_default(err);
  }
}

// --------------------------------------------------------
// Initialize options by checking the environment
// --------------------------------------------------------

static void mi_strlcpy(char* dest, const char* src, size_t dest_size) {
  dest[0] = 0;
  strncpy(dest, src, dest_size - 1);
  dest[dest_size - 1] = 0;
}

static void mi_strlcat(char* dest, const char* src, size_t dest_size) {
  strncat(dest, src, dest_size - 1);
  dest[dest_size - 1] = 0;
}

static inline int mi_strnicmp(const char* s, const char* t, size_t n) {
  if (n==0) return 0;
  for (; *s != 0 && *t != 0 && n > 0; s++, t++, n--) {
    if (toupper(*s) != toupper(*t)) break;
  }
  return (n==0 ? 0 : *s - *t);
}

#if defined _WIN32
// On Windows use GetEnvironmentVariable instead of getenv to work
// reliably even when this is invoked before the C runtime is initialized.
// i.e. when `_mi_preloading() == true`.
// Note: on windows, environment names are not case sensitive.
#include <Windows.h>
static bool mi_getenv(const char* name, char* result, size_t result_size) {
  result[0] = 0;
  size_t len = GetEnvironmentVariableA(name, result, (DWORD)result_size);
  return (len > 0 && len < result_size);
}
#elif !defined(MI_USE_ENVIRON) || (MI_USE_ENVIRON!=0)
// On Posix systemsr use `environ` to acces environment variables
// even before the C runtime is initialized.
#if defined(__APPLE__) && defined(__has_include) && __has_include(<crt_externs.h>)
#include <crt_externs.h>
static char** mi_get_environ(void) {
  return (*_NSGetEnviron());
}
#else
extern char** environ;
static char** mi_get_environ(void) {
  return environ;
}
#endif
static bool mi_getenv(const char* name, char* result, size_t result_size) {
  if (name==NULL) return false;
  const size_t len = strlen(name);
  if (len == 0) return false;
  char** env = mi_get_environ();
  if (env == NULL) return false;
  // compare up to 256 entries
  for (int i = 0; i < 256 && env[i] != NULL; i++) {
    const char* s = env[i];
    if (mi_strnicmp(name, s, len) == 0 && s[len] == '=') { // case insensitive
      // found it
      mi_strlcpy(result, s + len + 1, result_size);
      return true;
    }
  }
  return false;
}
#else
// fallback: use standard C `getenv` but this cannot be used while initializing the C runtime
static bool mi_getenv(const char* name, char* result, size_t result_size) {
  // cannot call getenv() when still initializing the C runtime.
  if (_mi_preloading()) return false;
  const char* s = getenv(name);
  if (s == NULL) {
    // we check the upper case name too.
    char buf[64+1];
    size_t len = strlen(name);
    if (len >= sizeof(buf)) len = sizeof(buf) - 1;
    for (size_t i = 0; i < len; i++) {
      buf[i] = toupper(name[i]);
    }
    buf[len] = 0;
    s = getenv(buf);
  }
  if (s != NULL && strlen(s) < result_size) {
    mi_strlcpy(result, s, result_size);
    return true;
  }
  else {
    return false;
  }
}
#endif

static void mi_option_init(mi_option_desc_t* desc) {
  // Read option value from the environment
  char buf[64+1];
  mi_strlcpy(buf, "mimalloc_", sizeof(buf));
  mi_strlcat(buf, desc->name, sizeof(buf));
  char s[64+1];
  if (mi_getenv(buf, s, sizeof(s))) {
    size_t len = strlen(s);
    if (len >= sizeof(buf)) len = sizeof(buf) - 1;
    for (size_t i = 0; i < len; i++) {
      buf[i] = (char)toupper(s[i]);
    }
    buf[len] = 0;
    if (buf[0]==0 || strstr("1;TRUE;YES;ON", buf) != NULL) {
      desc->value = 1;
      desc->init = INITIALIZED;
    }
    else if (strstr("0;FALSE;NO;OFF", buf) != NULL) {
      desc->value = 0;
      desc->init = INITIALIZED;
    }
    else {
      char* end = buf;
      long value = strtol(buf, &end, 10);
      if (*end == 0) {
        desc->value = value;
        desc->init = INITIALIZED;
      }
      else {
        _mi_warning_message("environment option mimalloc_%s has an invalid value: %s\n", desc->name, buf);
        desc->init = DEFAULTED;
      }
    }
    mi_assert_internal(desc->init != UNINIT);
  }
  else if (!_mi_preloading()) {
    desc->init = DEFAULTED;
  }
}
