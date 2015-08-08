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
    f.fill(p.units[0].members, p.spawn(p.units[0]), 'x');
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
    f.fill(p.units[1].members, p.spawn(p.units[1]), 'x');
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
    f.fill(p.units[2].members, p.spawn(p.units[2]), 'x');
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
    f.fill(p.units[3].members, p.spawn(p.units[3]), 'x');
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
    f.fill(p.units[0].members, p.spawn(p.units[0]), 'x');
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
    f.fill(p.units[1].members, p.spawn(p.units[1]), 'x');
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
    f.fill(p.units[2].members, p.spawn(p.units[2]), 'x');
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
    f.fill(p.units[3].members, p.spawn(p.units[3]), 'x');
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
  Problem p;
  p.width = 10;
  Unit u;
  u.members.push_back(Point(1, -1));
  u.members.push_back(Point(1, 1));
  EXPECT_EQ(Point(7, 1), p.spawn(u));
  u.members.push_back(Point(-2, 0));
  EXPECT_EQ(Point(9, 1), p.spawn(u));
  u.members.push_back(Point(3, -1));
  EXPECT_EQ(Point(9, 1), p.spawn(u));
  u.members.push_back(Point(0, -2));
  EXPECT_EQ(Point(8, 2), p.spawn(u));

  p.width = 9;
  u.members.clear();
  u.members.push_back(Point(1, -1));
  u.members.push_back(Point(1, 1));
  EXPECT_EQ(Point(7, 1), p.spawn(u));
  u.members.push_back(Point(-2, 0));
  EXPECT_EQ(Point(9, 1), p.spawn(u));
  u.members.push_back(Point(3, -1));
  EXPECT_EQ(Point(7, 1), p.spawn(u));
  u.members.push_back(Point(0, -2));
  EXPECT_EQ(Point(8, 2), p.spawn(u));
}
