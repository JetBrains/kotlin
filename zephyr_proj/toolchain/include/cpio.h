/* POSIX.1 symbolic constants for c_mode field of cpio archive format */

#ifndef _CPIO_H
#define _CPIO_H

#define	C_IRUSR		0000400	/* Read by owner */
#define	C_IWUSR		0000200	/* Write by owner */
#define	C_IXUSR		0000100	/* Execute by owner */
#define	C_IRGRP		0000040	/* Read by group */
#define	C_IWGRP		0000020	/* Write by group */
#define	C_IXGRP		0000010	/* Execute by group */
#define	C_IROTH		0000004	/* Read by others */
#define	C_IWOTH		0000002	/* Write by others */
#define	C_IXOTH		0000001	/* Execute by others */
#define	C_ISUID		0004000	/* Set user ID */
#define	C_ISGID		0002000	/* Set group ID */
#define	C_ISVTX		0001000	/* On directories, restricted deletion flag */

#define	C_ISDIR		0040000	/* Directory */
#define	C_ISFIFO	0010000	/* FIFO */
#define	C_ISREG		0100000	/* Regular file */
#define	C_ISBLK		0060000	/* Block special */
#define	C_ISCHR		0020000	/* Character special */
#define	C_ISCTG		0110000	/* Reserved */
#define	C_ISLNK		0120000	/* Symbolic link */
#define	C_ISSOCK	0140000	/* Socket */

#define	MAGIC		"070707"

#endif /* _CPIO_H */
