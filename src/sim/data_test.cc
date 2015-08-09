#include <sstream>

#include "base/test.h"
#include "src/sim/data.h"
#include "boost/property_tree/json_parser.hpp"
#include "boost/property_tree/ptree.hpp"

using boost::property_tree::json_parser::read_json;
using boost::property_tree::ptree;

TEST(DataTest, ReadFieldTest) {
  std::istringstream is(R"(
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

TEST(DataTest, OddSpawnTest) {
  std::istringstream is(R"(
      {
        "height": 5,
        "width": 5,
        "sourceSeeds": [0],
        "units": [
          {
            "members": [
              {"x":0,"y":0},
              {"x":2,"y":0}],
            "pivot":{"x":1,"y":5}
          }, {
            "members":[
              {"x":0,"y":0},
              {"x":0,"y":2}],
            "pivot":{"x":0,"y":6}
          }, {
            "members":[
              {"x":2,"y":0},
              {"x":0,"y":1},
              {"x":2,"y":2}],
            "pivot":{"x":1,"y":6}
          }, {
            "members":[
              {"x":0,"y":0},
              {"x":1,"y":1},
              {"x":0,"y":2}],
            "pivot":{"x":0,"y":6}
          }, {
            "members":[
              {"x":0,"y":0},
              {"x":0,"y":1},
              {"x":0,"y":2}],
            "pivot":{"x":0,"y":1}
          }],
          "id":20,
          "filled":[],
          "sourceLength":100})");
  ptree data;
  read_json(is, data);
  Problem p;
  p.load(data);

  {
    Field f = p.make_field();
    f.fill(p.units[0].members, p.units[0].spawn(f.width()), 'x');
    std::ostringstream ss;
    f.print(ss);

    EXPECT_EQ(
        "_ x _ x _ \n"
        " _ _ _ _ _ \n"
        "_ _ _ _ _ \n"
        " _ _ _ _ _ \n"
        "_ _ _ _ _ \n",
        ss.str());
  }

  {
    Field f = p.make_field();
    f.fill(p.units[1].members, p.units[1].spawn(f.width()), 'x');
    std::ostringstream ss;
    f.print(ss);

    EXPECT_EQ(
        "_ _ x _ _ \n"
        " _ _ _ _ _ \n"
        "_ _ x _ _ \n"
        " _ _ _ _ _ \n"
        "_ _ _ _ _ \n",
        ss.str());
  }

  {
    Field f = p.make_field();
    f.fill(p.units[2].members, p.units[2].spawn(f.width()), 'x');
    std::ostringstream ss;
    f.print(ss);

    EXPECT_EQ(
        "_ _ _ x _ \n"
        " _ x _ _ _ \n"
        "_ _ _ x _ \n"
        " _ _ _ _ _ \n"
        "_ _ _ _ _ \n",
        ss.str());
  }

  {
    Field f = p.make_field();
    f.fill(p.units[3].members, p.units[3].spawn(f.width()), 'x');
    std::ostringstream ss;
    f.print(ss);

    EXPECT_EQ(
        "_ x _ _ _ \n"
        " _ _ x _ _ \n"
        "_ x _ _ _ \n"
        " _ _ _ _ _ \n"
        "_ _ _ _ _ \n",
        ss.str());
  }
}

TEST(DataTest, EvenSpawnTest) {
  std::istringstream is(R"(
      {
        "height": 5,
        "width": 6,
        "sourceSeeds": [0],
        "units": [
          {
            "members": [
              {"x":0,"y":0},
              {"x":2,"y":0}],
            "pivot":{"x":1,"y":5}
          }, {
            "members":[
              {"x":0,"y":0},
              {"x":0,"y":2}],
            "pivot":{"x":0,"y":6}
          }, {
            "members":[
              {"x":2,"y":0},
              {"x":0,"y":1},
              {"x":2,"y":2}],
            "pivot":{"x":1,"y":6}
          }, {
            "members":[
              {"x":0,"y":0},
              {"x":1,"y":1},
              {"x":0,"y":2}],
            "pivot":{"x":0,"y":6}
          }, {
            "members":[
              {"x":0,"y":0},
              {"x":0,"y":1},
              {"x":0,"y":2}],
            "pivot":{"x":0,"y":1}
          }],
          "id":20,
          "filled":[],
          "sourceLength":100})");
  ptree data;
  read_json(is, data);
  Problem p;
  p.load(data);

  {
    Field f = p.make_field();
    f.fill(p.units[0].members, p.units[0].spawn(f.width()), 'x');
    std::ostringstream ss;
    f.print(ss);

    EXPECT_EQ(
        "_ x _ x _ _ \n"
        " _ _ _ _ _ _ \n"
        "_ _ _ _ _ _ \n"
        " _ _ _ _ _ _ \n"
        "_ _ _ _ _ _ \n",
        ss.str());
  }

  {
    Field f = p.make_field();
    f.fill(p.units[1].members, p.units[1].spawn(f.width()), 'x');
    std::ostringstream ss;
    f.print(ss);

    EXPECT_EQ(
        "_ _ x _ _ _ \n"
        " _ _ _ _ _ _ \n"
        "_ _ x _ _ _ \n"
        " _ _ _ _ _ _ \n"
        "_ _ _ _ _ _ \n",
        ss.str());
  }

  {
    Field f = p.make_field();
    f.fill(p.units[2].members, p.units[2].spawn(f.width()), 'x');
    std::ostringstream ss;
    f.print(ss);

    EXPECT_EQ(
        "_ _ _ x _ _ \n"
        " _ x _ _ _ _ \n"
        "_ _ _ x _ _ \n"
        " _ _ _ _ _ _ \n"
        "_ _ _ _ _ _ \n",
        ss.str());
  }

  {
    Field f = p.make_field();
    f.fill(p.units[3].members, p.units[3].spawn(f.width()), 'x');
    std::ostringstream ss;
    f.print(ss);

    EXPECT_EQ(
        "_ _ x _ _ _ \n"
        " _ _ _ x _ _ \n"
        "_ _ x _ _ _ \n"
        " _ _ _ _ _ _ \n"
        "_ _ _ _ _ _ \n",
        ss.str());
  }
}

TEST(DataTest, SpawnTest2) {
  int width = 2 * 10;
  Unit u;
  u.members.push_back(Point(1, -1));
  u.members.push_back(Point(1, 1));
  EXPECT_EQ(Point(7, 1), u.spawn(width));
  u.members.push_back(Point(-2, 0));
  EXPECT_EQ(Point(9, 1), u.spawn(width));
  u.members.push_back(Point(3, -1));
  EXPECT_EQ(Point(9, 1), u.spawn(width));
  u.members.push_back(Point(0, -2));
  EXPECT_EQ(Point(8, 2), u.spawn(width));

  width = 2 * 9;
  u.members.clear();
  u.members.push_back(Point(1, -1));
  u.members.push_back(Point(1, 1));
  EXPECT_EQ(Point(7, 1), u.spawn(width));
  u.members.push_back(Point(-2, 0));
  EXPECT_EQ(Point(9, 1), u.spawn(width));
  u.members.push_back(Point(3, -1));
  EXPECT_EQ(Point(7, 1), u.spawn(width));
  u.members.push_back(Point(0, -2));
  EXPECT_EQ(Point(8, 2), u.spawn(width));
}

TEST(DataTest, RandomSpawnTest) {
  srand(0);
  for (int i = 0; i < 10000; ++i) {
    int width = rand() % 100 * 2;
    Unit u;
    for (int j = 0; j < 10; ++j) {
      u.members.push_back(Point(rand() % 40 - 20, rand() % 40 - 20));
      EXPECT_EQ(u.spawn2(width), u.spawn1(width));
    }
  }
}
