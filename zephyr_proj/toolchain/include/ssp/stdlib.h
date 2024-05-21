#ifndef _SSP_STDLIB_H_
#define _SSP_STDLIB_H_

#include <ssp/ssp.h>

#if __SSP_FORTIFY_LEVEL > 0
__BEGIN_DECLS

__ssp_decl(size_t, mbstowcs, (wchar_t *__buf, const char *__src, size_t __n))
{
  if (__buf != NULL)
    __ssp_check(__buf, __n * sizeof(wchar_t), __ssp_bos);
  return __ssp_real_mbstowcs (__buf, __src, __n);
}

__ssp_redirect_raw(size_t, wcstombs, \
    (char *__buf, const wchar_t *__src, size_t __len), \
    (__buf, __src, __len), __buf != NULL, __ssp_bos);

__ssp_decl(int, wctomb, (char *__buf, wchar_t __wc))
{
  if (__buf != NULL)
    __ssp_check(__buf, MB_CUR_MAX, __ssp_bos);
  return __ssp_real_wctomb (__buf, __wc);
}

__END_DECLS

#endif /* __SSP_FORTIFY_LEVEL > 0 */
#endif /* _SSP_STDLIB_H_ */
