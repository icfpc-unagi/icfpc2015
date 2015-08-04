#include "base/test.h"
#include "example/hello-world.h"

class HelloWorldTest : public testing::Test {};

TEST_F(HelloWorldTest, HelloWorld) {
  EXPECT_TRUE(HelloWorld());
}
