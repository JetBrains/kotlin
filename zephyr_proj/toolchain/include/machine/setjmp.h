
_BEGIN_STD_C

#if defined(__or1k__) || defined(__or1knd__)
/*
 * r1, r2, r9, r14, r16 .. r30, SR.
 */
#define _JBLEN 13
#define _JBTYPE unsigned long
#endif

#if defined(__arm__) || defined(__thumb__)
/*
 * All callee preserved registers:
 * v1 - v7, fp, ip, sp, lr, f4, f5, f6, f7
 */
#define _JBLEN 23
#endif

#if defined(__aarch64__)
#define _JBLEN 22
#define _JBTYPE long long
#endif

#if defined(__AVR__)
#define _JBLEN 24
#endif

#ifdef __sparc__
/*
 * onsstack,sigmask,sp,pc,npc,psr,g1,o0,wbcnt (sigcontext).
 * All else recovered by under/over(flow) handling.
 */
#define	_JBLEN	13
#endif

#ifdef __BFIN__
#define _JBLEN  40
#endif

#ifdef __epiphany__
/* All callee preserved registers: r4-r10,fp, sp, lr,r15, r32-r39  */
#define _JBTYPE long long
#define _JBLEN 10
#endif

/* necv70 was 9 as well. */

#if defined(__m68k__) || defined(__mc68000__)
/*
 * onsstack,sigmask,sp,pc,psl,d2-d7,a2-a6,
 * fp2-fp7	for 68881.
 * All else recovered by under/over(flow) handling.
 */
#define	_JBLEN	34
#endif

#if defined(__mc68hc11__) || defined(__mc68hc12__) || defined(__mc68hc1x__)
/*
 * D, X, Y are not saved.
 * Only take into account the pseudo soft registers (max 32).
 */
#define _JBLEN  32
#endif

#ifdef __nds32__
/* 17 words for GPRs,
   1 word for $fpcfg.freg and 30 words for FPUs
   Reserved 2 words for aligement-adjustment. When storeing double-precision
   floating-point register into memory, the address has to be
   double-word-aligned.
   Check libc/machine/nds32/setjmp.S for more information.  */
#if __NDS32_EXT_FPU_SP__ || __NDS32_EXT_FPU_DP__
#define	_JBLEN 50
#else
#define _JBLEN 18
#endif
#endif

#if defined(__Z8001__) || defined(__Z8002__)
/* 16 regs + pc */
#define _JBLEN 20
#endif

#ifdef _AM29K
/*
 * onsstack,sigmask,sp,pc,npc,psr,g1,o0,wbcnt (sigcontext).
 * All else recovered by under/over(flow) handling.
 */
#define	_JBLEN	9
#endif

#ifdef __i386__
# if defined(__CYGWIN__) && !defined (_JBLEN)
#  define _JBLEN (13 * 4)
# elif defined(__unix__) || defined(__rtems__)
#  define _JBLEN	9
# elif defined(__iamcu__)
/* Intel MCU jmp_buf only covers callee-saved registers. */
#  define _JBLEN	6
# else
#  include "setjmp-dj.h"
# endif
#endif

#ifdef __x86_64__
# ifdef __CYGWIN__
#  define _JBTYPE long
#  define _JBLEN  32
# else
#  define _JBTYPE long long
#  define _JBLEN  8
# endif
#endif

#ifdef __i960__
#define _JBLEN 35
#endif

#ifdef __M32R__
/* Only 8 words are currently needed.  10 gives us some slop if we need
   to expand.  */
#define _JBLEN 10
#endif

#ifdef __mips__
# if defined(__mips64)
#  define _JBTYPE long long
# endif
# ifdef __mips_soft_float
#  define _JBLEN 11
# else
#  define _JBLEN 23
# endif
#endif

#ifdef __m88000__
#define _JBLEN 21
#endif

#ifdef __H8300__
#define _JBLEN 5
#define _JBTYPE int
#endif

#ifdef __H8300H__
/* same as H8/300 but registers are twice as big */
#define _JBLEN 5
#define _JBTYPE long
#endif

#if defined (__H8300S__) || defined (__H8300SX__)
/* same as H8/300 but registers are twice as big */
#define _JBLEN 5
#define _JBTYPE long
#endif

#ifdef __H8500__
#define _JBLEN 4
#endif

#ifdef  __sh__
#if __SH5__
#define _JBLEN 50
#define _JBTYPE long long
#else
#define _JBLEN 20
#endif /* __SH5__ */
#endif

#ifdef  __v800
#define _JBLEN 28
#endif

#ifdef __PPC__
#ifdef __ALTIVEC__
#define _JBLEN 64
#else
#define _JBLEN 32
#endif
#define _JBTYPE double
#endif

#ifdef __MICROBLAZE__
#define _JBLEN  20
#define _JBTYPE unsigned int
#endif

#ifdef __hppa__
/* %r30, %r2-%r18, %r27, pad, %fr12-%fr15.
   Note space exists for the FP registers, but they are not
   saved.  */
#define _JBLEN 28
#endif

#if defined(__mn10300__) || defined(__mn10200__)
#ifdef __AM33_2__
#define _JBLEN 26
#else
/* A guess */
#define _JBLEN 10
#endif
#endif

#ifdef __v850
/* I think our setjmp is saving 15 regs at the moment.  Gives us one word
   slop if we need to expand.  */
#define _JBLEN 16
#endif

#if defined(_C4x)
#define _JBLEN 10
#endif
#if defined(_C3x)
#define _JBLEN 9
#endif

#ifdef __TMS320C6X__
#define _JBLEN 13
#endif

#ifdef __TIC80__
#define _JBLEN 13
#endif

#ifdef __D10V__
#define _JBLEN 8
#endif

#ifdef __D30V__
#define _JBLEN ((64 /* GPR */ + (2*2) /* ACs */ + 18 /* CRs */) / 2)
#define _JBTYPE double
#endif

#ifdef __frv__
#define _JBLEN (68/2)  /* room for 68 32-bit regs */
#define _JBTYPE double
#endif

#ifdef __moxie__
#define _JBLEN 10
#endif

#ifdef __CRX__
#define _JBLEN 9
#endif

#if (defined(__CR16__) || defined(__CR16C__) ||defined(__CR16CP__))
/* r6, r7, r8, r9, r10, r11, r12 (r12L, r12H),
 * r13 (r13L, r13H), ra(raL, raH), sp(spL, spH) */
#define _JBLEN 14
#define _JBTYPE unsigned short
#endif

#ifdef __fr30__
#define _JBLEN 10
#endif

#ifdef  __FT32__
#define _JBLEN 27
#endif

#ifdef __iq2000__
#define _JBLEN 32
#endif

#ifdef __mcore__
#define _JBLEN 16
#endif

#ifdef __arc__
#define _JBLEN 25 /* r13-r30,blink,lp_count,lp_start,lp_end,mlo,mhi,status32 */
#endif

#ifdef __ARC64__
/* r14-r27,sp,ilink,r30,blink  */
#define _JBLEN 18
#ifdef __ARC64_ARCH64__
#define _JBTYPE long long
#else  /* __ARC64_ARCH32__ */
#define _JBTYPE long
#endif
#endif /* __ARC64__ */

#ifdef __MMIX__
/* Using a layout compatible with GCC's built-in.  */
#define _JBLEN 5
#define _JBTYPE unsigned long
#endif

#ifdef __mt__
#define _JBLEN 16
#endif

#ifdef __SPU__
#define _JBLEN 50
#define _JBTYPE __vector signed int
#endif

#ifdef __xstormy16__
/* 4 GPRs plus SP plus PC. */
#define _JBLEN 8
#endif

#ifdef __XTENSA__
#if __XTENSA_WINDOWED_ABI__

/* The jmp_buf structure for Xtensa windowed ABI holds the following
   (where "proc" is the procedure that calls setjmp): 4-12 registers
   from the window of proc, the 4 words from the save area at proc's $sp
   (in case a subsequent alloca in proc moves $sp), and the return
   address within proc.  Everything else is saved on the stack in the
   normal save areas.  The jmp_buf structure is:

	struct jmp_buf {
	    int regs[12];
	    int save[4];
	    void *return_address;
	}

   See the setjmp code for details.  */

#define _JBLEN		17	/* 12 + 4 + 1 */

#else /* __XTENSA_CALL0_ABI__ */

#define _JBLEN		6	/* a0, a1, a12, a13, a14, a15 */

#endif /* __XTENSA_CALL0_ABI__ */
#endif /* __XTENSA__ */

#ifdef __mep__
/* 16 GPRs, pc, hi, lo */
#define _JBLEN 19
#endif

#ifdef __CRIS__
#define _JBLEN 18
#endif

#ifdef __ia64
#define _JBLEN 64
#endif

#ifdef __lm32__
#define _JBLEN 19
#endif

#ifdef __m32c__
#if defined(__r8c_cpu__) || defined(__m16c_cpu__)
#define _JBLEN (22/2)
#else
#define _JBLEN (34/2)
#endif
#define _JBTYPE unsigned short
#endif /* __m32c__ */

#ifdef __MSP430__
#define _JBLEN 9

#ifdef __MSP430X_LARGE__
#define _JBTYPE unsigned long
#else
#define _JBTYPE unsigned short
#endif
#endif

#ifdef __RL78__
/* Three banks of registers, SP, CS, ES, PC */
#define _JBLEN (8*3+8)
#define _JBTYPE unsigned char
#endif

/*
 * There are two versions of setjmp()/longjmp():
 *   1) Compiler (gcc) built-in versions.
 *   2) Function-call versions.
 *
 * The built-in versions are used most of the time.  When used, gcc replaces
 * calls to setjmp()/longjmp() with inline assembly code.  The built-in
 * versions save/restore a variable number of registers.

 * _JBLEN is set to 40 to be ultra-safe with the built-in versions.
 * It only needs to be 12 for the function-call versions
 * but this data structure is used by both versions.
 */
#ifdef __NIOS2__
#define _JBLEN 40
#define _JBTYPE unsigned long
#endif

#ifdef __PRU__
#define _JBLEN 48
#define _JBTYPE unsigned int
#endif

#ifdef __RX__
#define _JBLEN 0x44
#endif

#ifdef __VISIUM__
/* All call-saved GP registers: r11-r19,r21,r22,r23.  */
#define _JBLEN 12
#endif

#ifdef __riscv
/* _JBTYPE using long long to make sure the alignment is align to 8 byte,
   otherwise in rv32imafd, store/restore FPR may mis-align.  */
#define _JBTYPE long long
#ifdef __riscv_32e
#define _JBLEN ((4*sizeof(long))/sizeof(long))
#else
#define _JBLEN ((14*sizeof(long) + 12*sizeof(double))/sizeof(long))
#endif
#endif

#ifdef _JBLEN
#ifdef _JBTYPE
typedef	_JBTYPE jmp_buf[_JBLEN];
#else
typedef	int jmp_buf[_JBLEN];
#endif
#endif

_END_STD_C

#if (defined(__CYGWIN__) || defined(__rtems__)) && __POSIX_VISIBLE
#include <signal.h>

#ifdef __cplusplus
extern "C" {
#endif

/* POSIX sigsetjmp/siglongjmp macros */
#ifdef _JBTYPE
typedef _JBTYPE sigjmp_buf[_JBLEN+1+((sizeof (_JBTYPE) + sizeof (sigset_t) - 1)
				     /sizeof (_JBTYPE))];
#else
typedef int sigjmp_buf[_JBLEN+1+(sizeof (sigset_t)/sizeof (int))];
#endif

#define _SAVEMASK	_JBLEN
#define _SIGMASK	(_JBLEN+1)

#ifdef __CYGWIN__
# define _CYGWIN_WORKING_SIGSETJMP
#endif

#ifdef _POSIX_THREADS
#define __SIGMASK_FUNC pthread_sigmask
#else
#define __SIGMASK_FUNC sigprocmask
#endif

#ifdef __CYGWIN__
/* Per POSIX, siglongjmp has to be implemented as function.  Cygwin
   provides functions for both, siglongjmp and sigsetjmp since 2.2.0. */
extern void siglongjmp (sigjmp_buf, int) __attribute__ ((__noreturn__));
extern int sigsetjmp (sigjmp_buf, int);
#endif

#if defined(__GNUC__)

#define sigsetjmp(env, savemask) \
            __extension__ \
            ({ \
              sigjmp_buf *_sjbuf = &(env); \
              ((*_sjbuf)[_SAVEMASK] = savemask,\
              __SIGMASK_FUNC (SIG_SETMASK, 0, (sigset_t *)((*_sjbuf) + _SIGMASK)),\
              setjmp (*_sjbuf)); \
            })

#define siglongjmp(env, val) \
            __extension__ \
            ({ \
              sigjmp_buf *_sjbuf = &(env); \
              ((((*_sjbuf)[_SAVEMASK]) ? \
               __SIGMASK_FUNC (SIG_SETMASK, (sigset_t *)((*_sjbuf) + _SIGMASK), 0)\
               : 0), \
               longjmp (*_sjbuf, val)); \
            })

#else /* !__GNUC__ */

#define sigsetjmp(env, savemask) ((env)[_SAVEMASK] = savemask,\
               __SIGMASK_FUNC (SIG_SETMASK, 0, (sigset_t *) ((env) + _SIGMASK)),\
               setjmp (env))

#define siglongjmp(env, val) ((((env)[_SAVEMASK])?\
               __SIGMASK_FUNC (SIG_SETMASK, (sigset_t *) ((env) + _SIGMASK), 0):0),\
               longjmp (env, val))

#endif

/* POSIX _setjmp/_longjmp, maintained for XSI compatibility.  These
   are equivalent to sigsetjmp/siglongjmp when not saving the signal mask.
   New applications should use sigsetjmp/siglongjmp instead. */
#ifdef __CYGWIN__
extern void _longjmp (jmp_buf, int) __attribute__ ((__noreturn__));
extern int _setjmp (jmp_buf);
#else
#define _setjmp(env)		sigsetjmp ((env), 0)
#define _longjmp(env, val)	siglongjmp ((env), (val))
#endif

#ifdef __cplusplus
}
#endif
#endif /* (__CYGWIN__ or __rtems__) and __POSIX_VISIBLE */
