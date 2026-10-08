dnl configure.ac - build configuration for libtrx
dnl
dnl Process with ./bootstrap.  The public header (include/trx.h)
dnl deliberately does not depend on anything detected here, so
dnl platforms that bypass ./configure (Xcode, SwiftPM, qmake) can
dnl compile src/*.c directly with -Iinclude.
dnl
AC_PREREQ([2.69])
AC_INIT([trivnote-libexpr], [0.0.0], [],
        [trivnote-libexpr], [https://foss.rapidcow.org/project/trivnote/])
AC_CONFIG_SRCDIR([src/trx_internal.h])
AC_CONFIG_AUX_DIR([build-aux])
AC_CONFIG_MACRO_DIRS([m4])
AC_CONFIG_HEADERS([config.h])

AM_INIT_AUTOMAKE([foreign subdir-objects -Wall])
AM_SILENT_RULES([yes])

dnl User-supplied CFLAGS are left entirely alone (AC_PROG_CC only
dnl supplies a default of `-g -O2' when CFLAGS is unset).  -fPIC is
dnl libtool's business: shared objects get it, static ones do not
dnl unless --with-pic is given.
AC_PROG_CC
AM_PROG_AR
AC_PROG_INSTALL
LT_INIT([win32-dll])

dnl These are all C89, so on any sane system this changes nothing;
dnl it is here so oddball toolchains get a chance to say so.
AC_CHECK_HEADERS([stddef.h stdlib.h string.h])
AC_TYPE_SIZE_T

AC_CONFIG_FILES([Makefile trx.pc man/trx_strerror.3])
AC_OUTPUT
