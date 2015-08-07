#include "base/test.h"
#include "src/sim/sim.h"

class HelloWorldTest : public testing::Test {};

TEST_F(HelloWorldTest, HelloWorld) {
  EXPECT_TRUE(true);
}
