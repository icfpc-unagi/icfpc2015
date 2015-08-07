#include "base/test.h"
#include "src/sim/sim.h"

class SimTest : public testing::Test {};

TEST_F(SimTest, LCGTest) {
  LCG lcg(17);
  EXPECT_EQ(lcg.next(), 0);
  EXPECT_EQ(lcg.next(), 24107);
  EXPECT_EQ(lcg.next(), 16552);
  EXPECT_EQ(lcg.next(), 12125);
  EXPECT_EQ(lcg.next(), 9427);
  EXPECT_EQ(lcg.next(), 13152);
  EXPECT_EQ(lcg.next(), 21440);
  EXPECT_EQ(lcg.next(), 3383);
  EXPECT_EQ(lcg.next(), 6873);
  EXPECT_EQ(lcg.next(), 16117);
}
