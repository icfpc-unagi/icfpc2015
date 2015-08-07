#ifndef SRC_SIM_DATA_H_
#define SRC_SIM_DATA_H_

#include <iostream>
#include "base/base.h"
#include "boost/property_tree/ptree.hpp"

using namespace std;
using boost::property_tree::ptree;

typedef pair<int, int> Point; // x * 2, y

inline Point load_point(const ptree& p) {
  int x = p.get<int>("x");
  int y = p.get<int>("y");
  return Point(x * 2 + y % 2, y);
}

inline Point point_offset(const Point& p, const Point& offset) {
  return Point(p.first + offset.first, p.second + offset.second);
}

struct Field {
  // _ : empty
  // x : full
  vector<vector<char>> data;

  // Assumes height is aligned to even, width is doubled
  Field(int height, int width) {
    CHECK_GT(height, 0);
    CHECK_GT(width, 0);
    data.assign(height, vector<char>((width + 1) / 2, '_'));
  }

  char get(const Point& p) const {
    CHECK_EQ(p.first % 2, p.second % 2) << "Misaligned access";
    if (p.first < 0 || width() <= p.first || p.second < 0 || height() <= p.second) return '#';
    return data[p.second][p.first / 2];
  }
  void set(const Point& p, char c) {
    CHECK_EQ(p.first % 2, p.second % 2) << "Misaligned access";
    data[p.second][p.first / 2] = c;
  }
  void fill(const vector<Point>& v, char c) {
    for (const auto& i : v) set(i, c);
  }
  void fill(const vector<Point>& v, Point offset, char c) {
    for (const auto& i : v) set(point_offset(i, offset), c);
  }
  void test(const vector<Point>& v, Point offset) {
    bool ok = true;
    for (const auto& i : v) ok = ok && get(point_offset(i, offset)) == '_';
    return ok;
  }

  int height() const { return data.size(); }
  int width() const { return data[0].size() * 2; }

  void print(std::ostream& os) const {
    for (int i = 0; i < height(); ++i) {
      int odd = i % 2;
      if (odd) os << " ";
      for (int j = 0; j < width() / 2; ++j) {
        os << data[i][j] << " ";
      }
      os << "\n";
    }
  }
};

struct Unit {
  // The local origin is shifted to the pivot.
  vector<Point> members;

  void load(const ptree &p) {
    Point pivot = load_point(p.get_child("pivot"));
    for (const auto& i : p.get_child("members")) {
      members.push_back(point_offset(load_point(i.second), pivot));
    }
    CHECK_GT(members.size(), 0);
  }
  int top_most() const {
    int e = members[0].second;
    for (int i = 1; i < members.size(); ++i) e = min(e, members[i].second);
    return e;
  }
  int bottom_most() const {
    int e = members[0].second;
    for (int i = 1; i < members.size(); ++i) e = max(e, members[i].second);
    return e;
  }
  int left_most() const {
    int e = members[0].first;
    for (int i = 1; i < members.size(); ++i) e = min(e, members[i].first);
    return e;
  }
  int right_most() const {
    int e = members[0].first;
    for (int i = 1; i < members.size(); ++i) e = max(e, members[i].first);
    return e;
  }
  Field make_field() const {
    // Aligns offset even
    int top = top_most() & ~1;
    int h = bottom_most() - top + 1;
    int left = left_most() & ~1;
    int w = right_most() - left + 1;
    Field f(h, w);
    Point offset(-left, -top);
    f.fill(members, offset, 'o');
    return f;
  }
  Unit rotate_cw() const {
    Unit u;
    u.members.resize(members.size());
    for (int i = 0; i < members.size(); ++i) {
      u.members[i] = Point((members[i].first - 3* members[i].second) / 2, (members[i].first + 2 * members[i].second) / 2);
    }
    return u;
  }
  Unit rotate_ccw() const {
    Unit u;
    u.members.resize(members.size());
    for (int i = 0; i < members.size(); ++i) {
      u.members[i] = Point((members[i].first + 3* members[i].second) / 2, (2 * members[i].second -members[i].first) / 2);
    }
    return u;
  }
};

struct Problem {
  int width;
  int height;
  vector<int> seeds;
  vector<Unit> units;
  int id;
  vector<Point> filled;
  int length;

  void load(const ptree &p) {
    width = p.get<int>("width");
    height = p.get<int>("height");
    for (const auto& i : p.get_child("sourceSeeds")) {
      seeds.push_back(i.second.get_value<int>());
    }
    for (const auto& i : p.get_child("units")) {
      Unit u;
      u.load(i.second);
      units.push_back(u);
    }
    id = p.get<int>("id");
    for (const auto& i : p.get_child("filled")) {
      filled.push_back(load_point(i.second));
    }
    length = p.get<int>("sourceLength");
  }
  Field make_field() const {
    Field f(height, width * 2);
    f.fill(filled, 'x');
    return f;
  }
  // Returns the point relative to the local cordinate system on which the unit spawns.
  Point spawn(const Unit& u) const {
    // TODO
    return Point(width, 0);
  }
};

struct Solution {
  int id;
  int seed;
  string tag;
  string solution;

  void load(const ptree &p) {
    id = p.get<int>("problemId");
    seed = p.get<int>("seed");
    tag = p.get<string>("tag");
    solution = p.get<string>("solution");
  }
};

struct Output {
  vector<Solution> solutions;

  void load(const ptree &p) {
    for (const auto& i : p) {
      Solution s;
      s.load(i.second);
      solutions.push_back(s);
    }
  }
};

#endif  // SRC_SIM_DATA_H_
