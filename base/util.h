#ifndef BASE_UTIL_H_
#define BASE_UTIL_H_

#include "googleapis/util/status.h"

namespace util {
namespace error {
using namespace googleapis::util::error;
}  // namespace error

using googleapis::util::Status;

#define CHECK_OK(condition)                     \
    do {                                        \
      util::Status status = (condition);        \
      CHECK(status.ok()) << status.ToString();  \
    } while (0)

#define RETURN_IF_ERROR(condition)        \
    do {                                  \
      util::Status status = (condition);  \
      if (!status.ok()) return status;    \
    } while (0)

}  // namespace util

#endif  // BASE_UTIL_H_
