/* Copyright (C) 2002, 2010 by  Red Hat, Incorporated. All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software
 * is freely granted, provided that this notice is preserved.
 */

#ifndef _WORDEXP_H_
#define _WORDEXP_H_

#include <sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

struct _wordexp_t
{
  size_t we_wordc;	/* Count of words matched by words. */
  char **we_wordv;	/* Pointer to list of expanded words. */
  size_t we_offs;	/* Slots to reserve at the beginning of we_wordv. */
};

typedef struct _wordexp_t wordexp_t;

#define	WRDE_DOOFFS	0x0001	/* Use we_offs. */
#define	WRDE_APPEND	0x0002	/* Append to output from previous call. */
#define	WRDE_NOCMD	0x0004	/* Don't perform command substitution. */
#define	WRDE_REUSE	0x0008	/* pwordexp points to a wordexp_t struct returned from
                                   a previous successful call to wordexp. */
#define	WRDE_SHOWERR	0x0010	/* Print error messages to stderr. */
#define	WRDE_UNDEF	0x0020	/* Report attempt to expand undefined shell variable. */

enum {
  WRDE_SUCCESS,
  WRDE_NOSPACE,
  WRDE_BADCHAR,
  WRDE_BADVAL,
  WRDE_CMDSUB,
  WRDE_SYNTAX,
  WRDE_NOSYS
};

/* Note: This implementation of wordexp requires a version of bash
   that supports the --wordexp and --protected arguments to be present
   on the system.  It does not support the WRDE_UNDEF flag. */
int wordexp(const char *__restrict, wordexp_t *__restrict, int);
void wordfree(wordexp_t *);

#ifdef __cplusplus
}
#endif

#endif /* _WORDEXP_H_  */
