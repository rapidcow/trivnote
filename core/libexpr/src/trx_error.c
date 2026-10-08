/*
 * trx_error.c - status code descriptions
 */
#include "trx_internal.h"

const char *
trx_strerror(int status)
{
	switch (status) {
	case TRX_OK:      return "success";
	case TRX_EOF:     return "end of input";
	case TRX_ENOMEM:  return "out of memory";
	case TRX_EIO:     return "read error";
	case TRX_EBAD:    return "syntax error";
	case TRX_ENOSYS:  return "not implemented";
	default:          return "unknown status";
	}
}
