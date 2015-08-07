#include "base/test.h"
#include "src/sim/sim.h"

class SimTest : public testing::Test {};

TEST_F(SimTest, LCGTest) {
  LCG lcg(17);
  EXPECT_EQ(0, lcg.next());
  EXPECT_EQ(24107, lcg.next());
  EXPECT_EQ(16552, lcg.next());
  EXPECT_EQ(12125, lcg.next());
  EXPECT_EQ(9427, lcg.next());
  EXPECT_EQ(13152, lcg.next());
  EXPECT_EQ(21440, lcg.next());
  EXPECT_EQ(3383, lcg.next());
  EXPECT_EQ(6873, lcg.next());
  EXPECT_EQ(16117, lcg.next());
}

TEST_F(SimTest, RotateCW) {
  Unit u;
  u.members.assign(1, Point(2, 0));
  u = u.rotate_cw();
  EXPECT_EQ(Point(1, 1), u.members[0]);
  u = u.rotate_cw();
  EXPECT_EQ(Point(-1, 1), u.members[0]);
  u = u.rotate_cw();
  EXPECT_EQ(Point(-2, 0), u.members[0]);
  u = u.rotate_cw();
  EXPECT_EQ(Point(-1, -1), u.members[0]);
  u = u.rotate_cw();
  EXPECT_EQ(Point(1, -1), u.members[0]);
}

TEST_F(SimTest, RotateCCW) {
  Unit u;
  u.members.assign(1, Point(2, 0));
  u = u.rotate_ccw();
  EXPECT_EQ(Point(1, -1), u.members[0]);
  u = u.rotate_ccw();
  EXPECT_EQ(Point(-1, -1), u.members[0]);
  u = u.rotate_ccw();
  EXPECT_EQ(Point(-2, 0), u.members[0]);
  u = u.rotate_ccw();
  EXPECT_EQ(Point(-1, 1), u.members[0]);
  u = u.rotate_ccw();
  EXPECT_EQ(Point(1, 1), u.members[0]);
}
