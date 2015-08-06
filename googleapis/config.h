#ifndef GOOGLEAPIS_CONFIG_H_
#define GOOGLEAPIS_CONFIG_H_

#define googleapis_VERSION_MAJOR 0
#define googleapis_VERSION_MINOR 2
#define googleapis_VERSION_PATCH 0
#define googleapis_VERSION_DECORATOR "devel"

// Indicates whether libproc is available. This is used on OSx for process
// information rather than /proc on standard Unix.
/* #define HAVE_LIBPROC  1 */

// Indicates fstat64 as well as lseek64 since these probably go together
/* #undef HAVE_FSTAT64 */

// Indicates whether File suppports user/group/other permissions.
// In the current windows port, user group permissions are not available.
/* #undef HAVE_UGO_PERMISSIONS */

// Indicates wWhether file suppports user/group/other permissions.
// In the current windows port, user group permissions are not available.
#define HAVE_OPENSSL  1

// Indicates whether the Mongoose Http Server library is available
// as an embedded http server.
/* #undef HAVE_MONGOOSE */

#endif  // GOOGLEAPIS_CONFIG_H_
