/*
 * stdio_ext.h
 *
 * Definitions for I/O internal operations, originally from Solaris.
 */

#ifndef _STDIO_EXT_H_
#define _STDIO_EXT_H_

#ifdef __rtems__
#error "<stdio_ext.h> not supported"
#endif

#include <stdio.h>

#define	FSETLOCKING_QUERY	0
#define	FSETLOCKING_INTERNAL	1
#define	FSETLOCKING_BYCALLER	2

_BEGIN_STD_C

void	 __fpurge (FILE *);
int	 __fsetlocking (FILE *, int);

/* TODO:

   void _flushlbf (void);
*/

#ifdef  __GNUC__

_ELIDABLE_INLINE size_t
__fbufsize (FILE *__fp) { return (size_t) __fp->_bf._size; }

_ELIDABLE_INLINE int
__freading (FILE *__fp) { return (__fp->_flags & __SRD) != 0; }

_ELIDABLE_INLINE int
__fwriting (FILE *__fp) { return (__fp->_flags & __SWR) != 0; }

_ELIDABLE_INLINE int
__freadable (FILE *__fp) { return (__fp->_flags & (__SRD | __SRW)) != 0; }

_ELIDABLE_INLINE int
__fwritable (FILE *__fp) { return (__fp->_flags & (__SWR | __SRW)) != 0; }

_ELIDABLE_INLINE int
__flbf (FILE *__fp) { return (__fp->_flags & __SLBF) != 0; }

_ELIDABLE_INLINE size_t
__fpending (FILE *__fp) { return __fp->_p - __fp->_bf._base; }

#else

size_t	 __fbufsize (FILE *);
int	 __freading (FILE *);
int	 __fwriting (FILE *);
int	 __freadable (FILE *);
int	 __fwritable (FILE *);
int	 __flbf (FILE *);
size_t	 __fpending (FILE *);

#ifndef __cplusplus

#define __fbufsize(__fp) ((size_t) (__fp)->_bf._size)
#define __freading(__fp) (((__fp)->_flags & __SRD) != 0)
#define __fwriting(__fp) (((__fp)->_flags & __SWR) != 0)
#define __freadable(__fp) (((__fp)->_flags & (__SRD | __SRW)) != 0)
#define __fwritable(__fp) (((__fp)->_flags & (__SWR | __SRW)) != 0)
#define __flbf(__fp) (((__fp)->_flags & __SLBF) != 0)
#define __fpending(__fp) ((size_t) ((__fp)->_p - (__fp)->_bf._base))

#endif /* __cplusplus */

#endif /* __GNUC__ */

_END_STD_C

#endif /* _STDIO_EXT_H_ */
