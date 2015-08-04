#ifndef BASE_BASE_H_
#define BASE_BASE_H_

#include "gflags/gflags.h"
#include "glog/logging.h"

#include <stdint.h>
#include <algorithm>
#include <map>
#include <memory>
#include <set>
#include <string>
#include <vector>

using std::make_pair;
using std::map;
using std::max;
using std::min;
using std::pair;
using std::set;
using std::shared_ptr;
using std::sort;
using std::string;
using std::unique_ptr;
using std::vector;

namespace base {

void Init(int argc, char** argv);

}  // namesapace base

#endif  // BASE_BASE_H_
