#ifndef _SSP_WCHAR_H_
#define _SSP_WCHAR_H_

#include <sys/cdefs.h>
#include <ssp/ssp.h>

#if __SSP_FORTIFY_LEVEL > 0

/* wide character variant, __wlen is number of wchar_t */
#define __ssp_redirect_wc(rtype, fun, args, call, cond, bos) \
__ssp_decl(rtype, fun, args) \
{ \
	if (cond) \
		__ssp_check(__buf, __wlen * sizeof(wchar_t), bos); \
	return __ssp_real_(fun) call; \
}

#define __ssp_bos_wicheck3(fun) \
__ssp_redirect_wc(wchar_t *, fun, \
    (wchar_t *__buf, const wchar_t *__src, size_t __wlen), \
    (__buf, __src, __wlen), 1, __ssp_bos0)

#define __ssp_bos_wicheck3_restrict(fun) \
__ssp_redirect_wc(wchar_t *, fun, \
    (wchar_t *__restrict __buf, const wchar_t *__restrict __src, size_t __wlen), \
    (__buf, __src, __wlen), 1, __ssp_bos0)

#define __ssp_bos_wicheck2_restrict(fun) \
__ssp_decl(wchar_t *, fun, (wchar_t *__restrict __buf, const wchar_t *__restrict __src)) \
{ \
  __ssp_check(__buf, (wcslen(__src) + 1) * sizeof(wchar_t), __ssp_bos0); \
  return __ssp_real_(fun) (__buf, __src); \
}

__BEGIN_DECLS
#if __POSIX_VISIBLE >= 200809
__ssp_bos_wicheck2_restrict(wcpcpy)
__ssp_bos_wicheck3_restrict(wcpncpy)
#endif
__ssp_bos_wicheck2_restrict(wcscpy)
__ssp_bos_wicheck2_restrict(wcscat)
__ssp_bos_wicheck3_restrict(wcsncpy)
__ssp_bos_wicheck3_restrict(wcsncat)
__ssp_bos_wicheck3_restrict(wmemcpy)
__ssp_bos_wicheck3(wmemmove)
#if __GNU_VISIBLE
__ssp_bos_wicheck3_restrict(wmempcpy)
#endif
__ssp_redirect_wc(wchar_t *, wmemset, \
    (wchar_t *__buf, wchar_t __src, size_t __wlen), \
    (__buf, __src, __wlen), 1, __ssp_bos0)

__ssp_decl(size_t, wcrtomb, (char *__buf, wchar_t __src, mbstate_t *__ps))
{
  if (__buf != NULL && __src != L'\0')
    __ssp_check(__buf, sizeof(wchar_t), __ssp_bos);
  return __ssp_real_wcrtomb (__buf, __src, __ps);
}

__ssp_redirect_wc(size_t, mbsrtowcs, \
    (wchar_t *__buf, const char **__src, size_t __wlen, mbstate_t *__ps), \
    (__buf, __src, __wlen, __ps), __buf != NULL, __ssp_bos)

__ssp_redirect_raw(size_t, wcsrtombs, \
    (char *__buf, const wchar_t **__src, size_t __len, mbstate_t *__ps), \
    (__buf, __src, __len, __ps), __buf != NULL, __ssp_bos)

#if __POSIX_VISIBLE >= 200809
__ssp_redirect_wc(size_t, mbsnrtowcs, \
    (wchar_t *__buf, const char **__src, size_t __nms, size_t __wlen, mbstate_t *__ps), \
    (__buf, __src, __nms, __wlen, __ps), __buf != NULL, __ssp_bos)

__ssp_redirect_raw(size_t, wcsnrtombs, \
    (char *__buf, const wchar_t **__src, size_t __nwc, size_t __len, mbstate_t *__ps), \
    (__buf, __src, __nwc, __len, __ps), __buf != NULL, __ssp_bos)
#endif

__ssp_decl(wchar_t *, fgetws, (wchar_t *__restrict __buf, int __wlen, __FILE *__restrict __fp))
{
  if (__wlen > 0)
    __ssp_check(__buf, (size_t)__wlen * sizeof(wchar_t) , __ssp_bos);
  return __ssp_real_fgetws(__buf, __wlen, __fp);
}

#if __GNU_VISIBLE
__ssp_decl(wchar_t *, fgetws_unlocked, (wchar_t *__buf, int __wlen, __FILE *__fp))
{
  if (__wlen > 0)
    __ssp_check(__buf, (size_t)__wlen * sizeof(wchar_t) , __ssp_bos);
  return __ssp_real_fgetws_unlocked(__buf, __wlen, __fp);
}
#endif /* __GNU_VISIBLE */

__END_DECLS

#endif /* __SSP_FORTIFY_LEVEL > 0 */
#endif /* _SSP_WCHAR_H_ */
