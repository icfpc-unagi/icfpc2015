#ifndef SRC_SIM_SIM_H_
#define SRC_SIM_SIM_H_

#include <cstring>
#include <iostream>
#include <unordered_set>
#include "base/base.h"
#include "googleapis/strings/strip.h"
#include "src/sim/data.h"

DECLARE_int32(verbose);

using namespace std;

namespace {

const char kMoveW[] = "p'!.03";
const char kMoveE[] = "bcefy2";
const char kMoveSW[] = "aghij4";
const char kMoveSE[] = "lmno 5";
const char kRotateCW[] = "dqrvz1";
const char kRotateCCW[] = "kstuwx";
const char kIgnored[] = "\t\n\r";

inline string serialize(const Point& p) {
  return string(1, p.first).append(1, p.second);
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
  Field field_;

public:
  Sim(const Problem& problem, const Solution& solution)
      : problem_(&problem), solution_(&solution),
        field_(problem_->make_field()) {
    CHECK_EQ(problem_->id, solution_->id);
  }

  int Play() {
    if (FLAGS_verbose >= 2) cerr << "Playing problem " << solution_->id << ", seed:" << solution_->seed << " (" << solution_->tag << ")" << endl;

    field_ = problem_->make_field();
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
        if (FLAGS_verbose >= 2) cerr << "Command remaining error. (Game cleared.)" << endl;
        return -1;
      }

      // Game ends if the spawn location is not valid
      if (!field_.test(unit.members, control)) {
        if (FLAGS_verbose >= 2) cerr << "Command remaining error. (Dead after placing " << source << "units.)" << endl;
        return -2;
      }

      string h = serialize(control);
      for (const auto& p : unit.members) h += serialize(point_offset(p, control));
      if (!visit.insert(h).second) {
        if (FLAGS_verbose >= 2) cerr << "Command error. (Visited.)" << endl;
        return -3;
      }

      if (FLAGS_verbose >= 5 || (FLAGS_verbose >= 4 && visit.empty())) {
        cerr << "\n\n================================\n\n";
        Field overlay = field_;
        overlay.fill(unit.members, control, '?');
        if (field_.contain(control)) {
          overlay.set(control, overlay.get(control) != '_' ? '&' : '@');
        }
        overlay.print(cerr);
      }

      char c = tolower(s[i]);
      Unit next_unit;
      Point next_control = control;
      if (FLAGS_verbose >= 3) cerr << "Snippet: " << StringPiece(s, i, 50) << endl;
      if (strchr(kMoveW, c)) {
        if (FLAGS_verbose >= 3) cerr << "Command: move W" << endl;
        next_unit = unit;
        next_control.first -= 2;
      } else if (strchr(kMoveE, c)) {
        if (FLAGS_verbose >= 3) cerr << "Command: move E" << endl;
        next_unit = unit;
        next_control.first += 2;
      } else if (strchr(kMoveSW, c)) {
        if (FLAGS_verbose >= 3) cerr << "Command: move SW" << endl;
        next_unit = unit;
        next_control.first--;
        next_control.second++;
      } else if (strchr(kMoveSE, c)) {
        if (FLAGS_verbose >= 3) cerr << "Command: move SE" << endl;
        next_unit = unit;
        next_control.first++;
        next_control.second++;
      } else if (strchr(kRotateCW, c)) {
        if (FLAGS_verbose >= 3) cerr << "Command: rotate CW" << endl;
        next_unit = unit.rotate_cw();
      } else if (strchr(kRotateCCW, c)) {
        if (FLAGS_verbose >= 3) cerr << "Command: rotate CCW " << endl;
        next_unit = unit.rotate_ccw();
      } else {
        LOG(FATAL) << "Unrecognized command [ " << s[i] << " ] in solution";
      }
      if (field_.test(next_unit.members, next_control)) {
        // Applies move
        unit = next_unit;
        control = next_control;
      } else {
        if (FLAGS_verbose >= 3) cerr << "Invalid command; Unit locked" << endl;
        // Rejects move and locks unit
        field_.fill(unit.members, control, 'x');
        int size = unit.members.size();
        // Clears rows
        int ls = field_.clear_rows();
        // Scoring
        int points = size + 100 * (1 + ls) * ls / 2;
        int line_bonus = max((ls_old - 1) * points / 10, 0);
        ls_old = ls;
        score += points + line_bonus;
        // Next source
        unit = problem_->units[random.next() % problem_->units.size()];
        control = problem_->spawn(unit);
        source++;
        visit.clear();
      }
    }
    // TODO: Calculate power_scores
    return score;
  }
};

#endif  // SRC_SIM_SIM_H_
