#include "base/base.h"

namespace base {

void Init(int argc, char** argv) {
  google::ParseCommandLineFlags(&argc, &argv, true);
  google::InitGoogleLogging(argv[0]);
}

}  // namesapace
