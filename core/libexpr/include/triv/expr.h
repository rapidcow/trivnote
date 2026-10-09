/*
 * triv/expr.h - trivnote S-expression library (public symbols!)
 */
#ifndef TRIV_EXPR_H
#define TRIV_EXPR_H

#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

enum {
	TRIX_OK = 0,
	TRIX_EOF,        /* end of file */
	TRIX_ENOMEM,     /* out of memory */
	TRIX_EIO,        /* I/O error */
	TRIX_EBAD,       /* malformed input */
	TRIX_ENOSYS      /* not implemented (...) */
};

const char *
trix_strerror(int status);

#ifdef __cplusplus
}
#endif

#endif  /* !def? TRIV_EXPR_H */
