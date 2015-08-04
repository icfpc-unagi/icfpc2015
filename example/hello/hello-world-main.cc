#include "base/base.h"
#include "example/hello/hello-world.h"

int main(int argc, char** argv) {
  base::Init(argc, argv);
  LOG(INFO) << "Starting hello-world-main...";
  HelloWorld();
  return 0;
}
