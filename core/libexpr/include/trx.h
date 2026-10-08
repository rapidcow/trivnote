/*
 * trx.h - trivnote S-expression library (public symbols!)
 */
#ifndef TRX_H
#define TRX_H

#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

enum {
	TRX_OK = 0,
	TRX_EOF,        /* end of file */
	TRX_ENOMEM,     /* out of memory */
	TRX_EIO,        /* I/O error */
	TRX_EBAD,       /* malformed input */
	TRX_ENOSYS      /* not implemented (...) */
};

const char *trx_strerror(int status);

#ifdef __cplusplus
}
#endif

#endif /* TRX_H */
