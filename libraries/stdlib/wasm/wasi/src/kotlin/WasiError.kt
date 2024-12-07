/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm

/**
 * [WASI Error codes for preview1](https://github.com/WebAssembly/WASI/blob/main/legacy/preview1/docs.md)
 */
internal enum class WasiErrorCode {
    /**
     * No error occurred. System call completed successfully.
     */
    SUCCESS,

    /**
     * Argument list too long.
     */
    _2BIG,

    /**
     * Permission denied.
     */
    ACCES,

    /**
     * Address in use.
     */
    ADDRINUSE,

    /**
     * Address not available.
     */
    ADDRNOTAVAIL,

    /**
     * Address family not supported.
     */
    AFNOSUPPORT,

    /**
     * Resource unavailable, or operation would block.
     */
    AGAIN,

    /**
     * Connection already in progress.
     */
    ALREADY,

    /**
     * Bad file descriptor.
     */
    BADF,

    /**
     * Bad message.
     */
    BADMSG,

    /**
     * Device or resource busy.
     */
    BUSY,

    /**
     * Operation canceled.
     */
    CANCELED,

    /**
     * No child processes.
     */
    CHILD,

    /**
     * Connection aborted.
     */
    CONNABORTED,

    /**
     * Connection refused.
     */
    CONNREFUSED,

    /**
     * Connection reset.
     */
    CONNRESET,

    /**
     * Resource deadlock would occur.
     */
    DEADLK,

    /**
     * Destination address required.
     */
    DESTADDRREQ,

    /**
     * Mathematics argument out of domain of function.
     */
    DOM,

    /**
     * Reserved.
     */
    DQUOT,

    /**
     * File exists.
     */
    EXIST,

    /**
     * Bad address.
     */
    FAULT,

    /**
     * File too large.
     */
    FBIG,

    /**
     * Host is unreachable.
     */
    HOSTUNREACH,

    /**
     * Identifier removed.
     */
    IDRM,

    /**
     * Illegal byte sequence.
     */
    ILSEQ,

    /**
     * Operation in progress.
     */
    INPROGRESS,

    /**
     * Interrupted function.
     */
    INTR,

    /**
     * Invalid argument.
     */
    INVAL,

    /**
     * I/O error.
     */
    IO,

    /**
     * Socket is connected.
     */
    ISCONN,

    /**
     * Is a directory.
     */
    ISDIR,

    /**
     * Too many levels of symbolic links.
     */
    LOOP,

    /**
     * File descriptor value too large.
     */
    MFILE,

    /**
     * Too many links.
     */
    MLINK,

    /**
     * Message too large.
     */
    MSGSIZE,

    /**
     * Reserved.
     */
    MULTIHOP,

    /**
     * Filename too long.
     */
    NAMETOOLONG,

    /**
     * Network is down.
     */
    NETDOWN,

    /**
     * Connection aborted by network.
     */
    NETRESET,

    /**
     * Network unreachable.
     */
    NETUNREACH,

    /**
     * Too many files open in system.
     */
    NFILE,

    /**
     * No buffer space available.
     */
    NOBUFS,

    /**
     * No such device.
     */
    NODEV,

    /**
     * No such file or directory.
     */
    NOENT,

    /**
     * Executable file format error.
     */
    NOEXEC,

    /**
     * No locks available.
     */
    NOLCK,

    /**
     * Reserved.
     */
    NOLINK,

    /**
     * Not enough space.
     */
    NOMEM,

    /**
     * No message of the desired type.
     */
    NOMSG,

    /**
     * Protocol not available.
     */
    NOPROTOOPT,

    /**
     * No space left on device.
     */
    NOSPC,

    /**
     * Function not supported.
     */
    NOSYS,

    /**
     * The socket is not connected.
     */
    NOTCONN,

    /**
     * Not a directory or a symbolic link to a directory.
     */
    NOTDIR,

    /**
     * Directory not empty.
     */
    NOTEMPTY,

    /**
     * State not recoverable.
     */
    NOTRECOVERABLE,

    /**
     * Not a socket.
     */
    NOTSOCK,

    /**
     * Not supported, or operation not supported on socket.
     */
    NOTSUP,

    /**
     * Inappropriate I/O control operation.
     */
    NOTTY,

    /**
     * No such device or address.
     */
    NXIO,

    /**
     * Value too large to be stored in data type.
     */
    OVERFLOW,

    /**
     * Previous owner died.
     */
    OWNERDEAD,

    /**
     * Operation not permitted.
     */
    PERM,

    /**
     * Broken pipe.
     */
    PIPE,

    /**
     * Protocol error.
     */
    PROTO,

    /**
     * Protocol not supported.
     */
    PROTONOSUPPORT,

    /**
     * Protocol wrong type for socket.
     */
    PROTOTYPE,

    /**
     * Result too large.
     */
    RANGE,

    /**
     * Read-only file system.
     */
    ROFS,

    /**
     * Invalid seek.
     */
    SPIPE,

    /**
     * No such process.
     */
    SRCH,

    /**
     * Reserved.
     */
    STALE,

    /**
     * Connection timed out.
     */
    TIMEDOUT,

    /**
     * Text file busy.
     */
    TXTBSY,

    /**
     * Cross-device link.
     */
    XDEV,

    /**
     * Extension: Capabilities insufficient.
     */
    NOTCAPABLE,
}

internal class WasiError(val error: WasiErrorCode) : Throwable(message = "WASI call failed with $error")
