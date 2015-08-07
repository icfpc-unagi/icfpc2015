#ifndef SAMPLE_HELLO_WORLD_H_
#define SAMPLE_HELLO_WORLD_H_

#include <iostream>
#include "base/base.h"
#include "boost/property_tree/ptree.hpp"

using namespace std;
using namespace boost::property_tree;

typedef pair<int, int> Point; // x, y

inline Point load_point(const ptree& p) {
  return Point(p.get<int>("x"), p.get<int>("y"));
}

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
};

struct Field {
  // _ : empty
  // x : full
  vector<vector<char>> data;

  void init(const Problem& p) {
     data.assign(p.height, vector<char>(p.width, '_'));
     for (const auto& i : p.filled) {
      data[i.first][i.second] = 'x';
     }
  }

  int height() const { return data.size(); }
  int width() const { return data[0].size(); }

  void print(std::ostream& os = std::cout) const {
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

#endif  // SAMPLE_HELLO_WORLD_H_
