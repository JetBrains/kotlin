/*	$NetBSD: search.h,v 1.12 1999/02/22 10:34:28 christos Exp $	*/
/* $FreeBSD: src/include/search.h,v 1.4 2002/03/23 17:24:53 imp Exp $ */

/*
 * Written by J.T. Conklin <jtc@netbsd.org>
 * Public domain.
 */

#ifndef _SEARCH_H_
#define _SEARCH_H_

#include <sys/cdefs.h>
#include <machine/ansi.h>
#include <sys/types.h>

typedef struct entry {
	char *key;
	void *data;
} ENTRY;

typedef enum {
	FIND, ENTER
} ACTION;

typedef enum {
	preorder,
	postorder,
	endorder,
	leaf
} VISIT;

#ifdef _SEARCH_PRIVATE
typedef struct node {
	char         *key;
	struct node  *llink, *rlink;
} node_t;
#endif

struct hsearch_data
{
  struct internal_head *htable;
  size_t htablesize;
};

#ifndef __compar_fn_t_defined
#define __compar_fn_t_defined
typedef int (*__compar_fn_t) (const void *, const void *);
#endif

__BEGIN_DECLS
int	 hcreate(size_t);
void	 hdestroy(void);
ENTRY	*hsearch(ENTRY, ACTION);
int	 hcreate_r(size_t, struct hsearch_data *);
void	 hdestroy_r(struct hsearch_data *);
int	hsearch_r(ENTRY, ACTION, ENTRY **, struct hsearch_data *);
void	*tdelete(const void *__restrict, void **__restrict, __compar_fn_t);
void	tdestroy (void *, void (*)(void *));
void	*tfind(const void *, void **, __compar_fn_t);
void	*tsearch(const void *, void **, __compar_fn_t);
void      twalk(const void *, void (*)(const void *, VISIT, int));
__END_DECLS

#endif /* !_SEARCH_H_ */
