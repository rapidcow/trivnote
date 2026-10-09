/*
 * expr.strerror.c - status code descriptions
 */
#include "expr.internal.h"

const char *
trix_strerror(int status)
{
	switch (status) {
	case TRIX_OK:     return "success";
	case TRIX_EOF:    return "end of input";
	case TRIX_ENOMEM: return "out of memory";
	case TRIX_EIO:    return "read error";
	case TRIX_EBAD:   return "syntax error";
	case TRIX_ENOSYS: return "not implemented";
	default:          return "unknown status";
	}
}
