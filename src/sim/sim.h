#ifndef SRC_SIM_SIM_H_
#define SRC_SIM_SIM_H_

#include <iostream>
#include "base/base.h"
#include "boost/property_tree/ptree.hpp"

using namespace std;
using boost::property_tree::ptree;

typedef pair<int, int> Point; // x, y

inline Point load_point(const ptree& p) {
  return Point(p.get<int>("x"), p.get<int>("y"));
}

struct Field {
  // _ : empty
  // x : full
  vector<vector<char>> data;

  Field(int height, int width) { init(height, width); }

  void init(int height, int width) {
    CHECK_GT(height, 0);
    CHECK_GT(width, 0);
    data.assign(height, vector<char>(width, '_'));
  }

  char get(const Point& p) const {
    if (p.first < 0 || width() <= p.first || p.second < 0 || height() <= p.second) return '#';
    return data[p.first][p.second];
  }
  void set(const Point& p, char c) {
    data[p.first][p.second] = c;
  }
  void fill(const vector<Point>& v, char c) {
    for (const auto& i : v) set(i, c);
  }

  int height() const { return data.size(); }
  int width() const { return data[0].size(); }

  void print(std::ostream& os) const {
    for (int i = 0; i < height(); ++i) {
      int odd = i % 2;
      if (odd) os << " ";
      for (int j = odd; j < width() * 2; j += 2) {
        os << data[i][j/2] << " ";
      }
      os << "\n";
    }
  }
};

struct Unit {
  vector<Point> members;
  Point pivot;

  bool load(const ptree &p) {
    for (const auto& i : p.get_child("members")) {
      members.push_back(load_point(i.second));
    }
    pivot = load_point(p.get_child("pivot"));
    return true;
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
    int h = bottom_most() - top_most() + 1;
    int w = right_most() - left_most() + 1;
    Field f(h, w);
    f.fill(members, 'o');
    return f;
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

  bool load(const ptree &p) {
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
    return true;
  }
  Field make_field() const {
    Field f(height, width);
    f.fill(filled, 'x');
    return f;
  }
};

#endif  // SRC_SIM_SIM_H_
