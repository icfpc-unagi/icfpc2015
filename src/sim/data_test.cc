#include <strstream>

#include "base/test.h"
#include "src/sim/data.h"
#include "boost/property_tree/json_parser.hpp"
#include "boost/property_tree/ptree.hpp"

using boost::property_tree::json_parser::read_json;
using boost::property_tree::ptree;

TEST(DataTest, ReadFieldTest) {
  std::istrstream is(R"(
      {
        "members":[{"x":2,"y":2}],
        "pivot":{"x":1,"y":1}
      })");
  ptree data;
  read_json(is, data);

  Unit u;
  u.load(data);

  EXPECT_THAT(
      u.members,
      testing::ElementsAre(Point(1, 1)));

  u = u.rotate_cw();
  EXPECT_THAT(
      u.members,
      testing::ElementsAre(Point(-1, 1)));

  u = u.rotate_cw();
  EXPECT_THAT(
      u.members,
      testing::ElementsAre(Point(-2, 0)));

  u = u.rotate_cw();
  EXPECT_THAT(
      u.members,
      testing::ElementsAre(Point(-1, -1)));

  u = u.rotate_cw();
  EXPECT_THAT(
      u.members,
      testing::ElementsAre(Point(1, -1)));

  u = u.rotate_cw();
  EXPECT_THAT(
      u.members,
      testing::ElementsAre(Point(2, 0)));

  u = u.rotate_cw();
  EXPECT_THAT(
      u.members,
      testing::ElementsAre(Point(1, 1)));

  u = u.rotate_ccw();
  EXPECT_THAT(
      u.members,
      testing::ElementsAre(Point(2, 0)));

  u = u.rotate_ccw();
  EXPECT_THAT(
      u.members,
      testing::ElementsAre(Point(1, -1)));

  u = u.rotate_ccw();
  EXPECT_THAT(
      u.members,
      testing::ElementsAre(Point(-1, -1)));

  u = u.rotate_ccw();
  EXPECT_THAT(
      u.members,
      testing::ElementsAre(Point(-2, 0)));

  u = u.rotate_ccw();
  EXPECT_THAT(
      u.members,
      testing::ElementsAre(Point(-1, 1)));

  u = u.rotate_ccw();
  EXPECT_THAT(
      u.members,
      testing::ElementsAre(Point(1, 1)));
}
