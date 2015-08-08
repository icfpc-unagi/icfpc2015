#ifndef SRC_SIM_SIM_H_
#define SRC_SIM_SIM_H_

#include <cstring>
#include <iostream>
#include <unordered_set>
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

inline char week_hash(const Point& p) {
  return p.first + (p.second << 4) + (p.second >> 4);
}

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
    Unit unit = problem_->units[random.next() % problem_->units.size()];
    Point control = problem_->spawn(unit);
    unordered_set<string> visit;
    int source = 0;
    int ls_old = 0;
    int score = 0;

    string s = solution_->solution;
    googleapis::strrmm(&s, kIgnored);
    for (int i = 0; i < s.size(); ++i) {
      if (source >= problem_->length) {
        cerr << "Command remaining error." << endl;
        return 0;
      }

      string h(1, week_hash(control));
      for (const auto& p : unit.members) h += week_hash(p);
      if (!visit.insert(h).second) {
        cerr << "Command error." << endl;
        return 0;
      }

      cerr << "\n\n================================\n\n";
      Field overlay = field;
      overlay.fill(unit.members, control, '&');
      overlay.print(cerr);

      char c = tolower(s[i]);
      Unit next_unit;
      Point next_control = control;
      if (strchr(kMoveW, c)) {
        cerr << "Command: move W" << endl;
        next_unit = unit;
        next_control.first -= 2;
      } else if (strchr(kMoveE, c)) {
        cerr << "Command: move E" << endl;
        next_unit = unit;
        next_control.first += 2;
      } else if (strchr(kMoveSW, c)) {
        cerr << "Command: move SW" << endl;
        next_unit = unit;
        next_control.first--;
        next_control.second++;
      } else if (strchr(kMoveSE, c)) {
        cerr << "Command: move SE" << endl;
        next_unit = unit;
        next_control.first++;
        next_control.second++;
      } else if (strchr(kRotateCW, c)) {
        cerr << "Command: rotate CW" << endl;
        next_unit = unit.rotate_cw();
      } else if (strchr(kRotateCCW, c)) {
        cerr << "Command: rotate CCW " << endl;
        next_unit = unit.rotate_ccw();
      } else {
        LOG(FATAL) << "Unrecognized command [ " << s[i] << " ] in solution";
      }
      if (field.test(next_unit.members, next_control)) {
        // Applies move
        unit = next_unit;
        control = next_control;
      } else {
        cerr << "Invalid command; Unit locked" << endl;
        // Rejects move and locks unit
        field.fill(unit.members, control, 'x');
        int size = unit.members.size();
        // Clears rows
        int ls = field.clear_rows();
        // Scoring
        int points = size + 100 * (1 + ls) * ls / 2;
        int line_bonus = max((ls_old - 1) * points / 10, 0);
        ls_old = ls;
        score += points + line_bonus;
        // Next source
        unit = problem_->units[random.next() % problem_->units.size()];
        control = problem_->spawn(unit);
        source++;
        // Game ends if the spawn location is not valid
        if (!field.test(unit.members, control)) break;
      }
    }
    // TODO: Calculate power_scores
    return score;
  }
};

#endif  // SRC_SIM_SIM_H_
