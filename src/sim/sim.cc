#include "src/sim/sim.h"

#include <stdio.h>
#include <string>

#include "base/base.h"

DEFINE_string(name, "world", "Name to output");

bool HelloWorld() {
  printf("%s\n", ("Hello, " + FLAGS_name + "!").c_str());
  return true;
}
