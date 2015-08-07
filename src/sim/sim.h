#ifndef SRC_SIM_SIM_H_
#define SRC_SIM_SIM_H_

#include <cstring>
#include <iostream>
#include "base/base.h"
#include "googleapis/strings/strip.h"
#include "src/sim/data.h"

using namespace std;

namespace {
const char kMoveW[] = "p'!.03";
const char kMoveE[] = "bcefy2";
const char kMoveSW[] = "aghij4";
const char kMoveSE[] = "lmno 5";
const char kRotateCW[] = "dqrvz1";
const char kRotateCCW[] = "kstuwx";
const char kIgnored[] = "\t\n\r";
}  // namespace

class LCG {
private:
  uint32 mul = 1103515245;
  uint32 inc = 12345;
  uint32 prev;

public:
  LCG(uint32 seed) : prev(seed) {}

  int next() {
    int ret = static_cast<int>(prev >> 16) & 0x7fff;
    prev = mul * prev + inc;
    return ret;
  }
};

class Sim {
private:
  const Problem* const problem_;
  const Solution* const solution_;

public:
  Sim(const Problem& problem, const Solution& solution)
      : problem_(&problem), solution_(&solution) {
    CHECK_EQ(problem_->id, solution_->id);
  }

  int Play() {
    cout << "Playing problem " << solution_->id << ", seed:" << solution_->seed << " (" << solution_->tag << ")" << endl;

    Field field = problem_->make_field();
    LCG random(solution_->seed);
    const Unit* unit = &problem_->units[random.next() % problem_->units.size()];
    Point control = problem_->spawn(*unit);
    int source = 1;
    int ls_old = 0;
    int score = 0;

    string s = solution_->solution;
    googleapis::strrmm(&s, kIgnored);
    for (int i = 0; i < s.size(); ++i) {
      char c = tolower(s[i]);
      Unit next_unit;
      Point next_control = control;
      if (strchr(kMoveW, c)) {
        next_unit = *unit;
        next_control.first -= 2;
      } else if (strchr(kMoveE, c)) {
        next_unit = *unit;
        next_control.first += 2;
      } else if (strchr(kMoveSW, c)) {
        next_unit = *unit;
        next_control.first--;
        next_control.second++;
      } else if (strchr(kMoveSE, c)) {
        next_unit = *unit;
        next_control.first++;
        next_control.second++;
      } else if (strchr(kRotateCW, c)) {
        next_unit = unit->rotate_cw();
      } else if (strchr(kRotateCCW, c)) {
        next_unit = unit->rotate_ccw();
      } else {
        LOG(FATAL) << "Unrecognized command [ " << s[i] << " ] in solution";
      }
      // TODO: clear rows when a unit gets locked
    }
    return score;
  }
};

#endif  // SRC_SIM_SIM_H_
