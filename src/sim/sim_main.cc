#include <fstream>
#include "base/base.h"
#include "src/sim/sim.h"
#include "boost/property_tree/json_parser.hpp"
#include "boost/property_tree/ptree.hpp"

using namespace std;
using boost::property_tree::ptree;
using boost::property_tree::json_parser::read_json;

int main(int argc, char** argv) {
  base::Init(&argc, &argv);

  if (argc < 2) {
    LOG(ERROR) << "Usage: " << argv[0] << " <problem.json>";
    return 1;
  }

  ptree pt;
  ifstream ifs(argv[1]);
  read_json(ifs, pt);

  Problem problem;
  CHECK(problem.load(pt));

  cout << "Problem: " << problem.id << " (" << problem.width << "x" << problem.height << ")\nSeeds: ";
  for (int i = 0; i < problem.seeds.size(); ++i) {
    cout << problem.seeds[i] << " ";
  }
  cout << "\nField:\n";
  Field field = problem.make_field();
  field.print(cout);

  for (int i = 0; i < problem.units.size(); ++i) {
    cout << "Unit " << i << ":\n";
    Field sample = problem.units[i].make_field();
    sample.print(cout);
  }

  return 0;
}
