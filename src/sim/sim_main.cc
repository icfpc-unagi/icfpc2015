#include <fstream>
#include "base/base.h"
#include "src/sim/sim.h"
#include "boost/property_tree/json_parser.hpp"
#include "boost/property_tree/ptree.hpp"

using namespace std;
using namespace boost::property_tree;
using namespace boost::property_tree::json_parser;

int main(int argc, char** argv) {
  base::Init(argc, argv);

  ifstream ifs;
  istream *is;
  if (argc > 1) {
    ifs = ifstream(argv[1]);
    is = &ifs;
  } else {
    is = &cin;
  }

  ptree pt;
  read_json(*is, pt);
  Problem problem;
  CHECK(problem.load(pt));

  Field field;
  field.init(problem);
  field.print();

  return 0;
}
