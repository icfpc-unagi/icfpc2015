#include <iostream>

#include "base/base.h"
#include "example/hello/hello-world.h"

int main(int argc, char** argv) {
  base::Init(&argc, &argv);
  for (int i = 0; i < argc; i++) {
    std::cout << "argv[" << i << "]: " << argv[i] << std::endl;
  }
  LOG(INFO) << "Starting hello-world-main...";
  HelloWorld();
  return 0;
}
