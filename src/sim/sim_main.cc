#include <fstream>
#include "base/base.h"
#include "base/strings.h"
#include "boost/property_tree/json_parser.hpp"
#include "boost/property_tree/ptree.hpp"
#include "src/sim/data.h"
#include "src/sim/sim.h"

DEFINE_bool(output_score, false, "berobero");

using namespace std;
using boost::property_tree::ptree;
using boost::property_tree::json_parser::read_json;

namespace {

ptree read_ptree(const char* filename) {
  ptree p;
  ifstream ifs(filename);
  read_json(ifs, p);
  return p;
}

}  // namespace

int main(int argc, char** argv) {
  base::Init(&argc, &argv);

  if (argc < 2) {
    LOG(ERROR) << "Usage: " << argv[0] << " <problem.json> [output.json]";
    return 1;
  }

  Problem problem;
  problem.load(read_ptree(argv[1]));

  // Print out problem information
  if (FLAGS_verbose >= 2) {
    cerr << "Problem: " << problem.id << " (" << problem.width << "x" << problem.height << ")\nSeeds: ";
    for (int i = 0; i < problem.seeds.size(); ++i) {
      cerr << problem.seeds[i] << " ";
    }
    cerr << "\nField:\n";
    problem.make_field().print(cerr);
  }

  if (FLAGS_verbose >= 3) {
    for (int i = 0; i < problem.units.size(); ++i) {
      cerr << "Unit " << i << ":\n";
      problem.units[i].make_field().print(cerr);
    }
  }

  if (argc < 3) {
    return 0;
  }

  Output output;
  output.load(read_ptree(argv[2]));

  bool found = false;
  for (const auto& s : output.solutions) {
    if (s.id != problem.id) continue;
    found = true;

    Sim sim(problem, s);
    int score = sim.Play();
    if (FLAGS_output_score) cout << score << endl;
  }

  LOG_IF(ERROR, !found) << "Output contain no solution for problem " << problem.id;

  return 0;
}
