#ifndef SRC_SIM_SIM_H_
#define SRC_SIM_SIM_H_

#include <cstring>
#include <fstream>
#include <iostream>
#include <unordered_set>
#include "base/base.h"
#include "googleapis/strings/case.h"
#include "googleapis/strings/strip.h"
#include "googleapis/strings/util.h"
#include "src/sim/data.h"

DECLARE_int32(verbose);
DECLARE_string(phrases_of_power);

using namespace std;

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

class Phrases {
  vector<string> d_;
public:
  Phrases(const string& filename) {
    if (!filename.empty()) {
      ifstream ifs(filename);
      CHECK(ifs.good()) << "Failed to read phrases: " << filename;
      string s;
      while (getline(ifs, s)) {
        googleapis::StripTrailingNewline(&s);
        googleapis::LowerString(&s);
        d_.push_back(s);
      }
    }
  }

  // Returns power score
  int spell(StringPiece sp) {
    int score = 0;
    // TODO: make this faster
    for (const string& phrase : d_) {
      int reps = CountSubstring(sp, phrase);
      if (reps > 0) score += 2 * phrase.size() * reps + 300;
    }
    return score;
  }
};

class Sim {
 private:
  const Problem* const problem_;
  const Solution* const solution_;

  vector<UnitCache> unit_cache;
  std::ostream* logs_ = nullptr;
  // If skip is true, specified number of first/last commands would be logged.
  bool skip_ = false;
  int head_;
  int tail_;

 public:
  Field field_;
  Phrases phrases_ = Phrases(FLAGS_phrases_of_power);

  Sim(const Problem& problem, const Solution& solution)
      : problem_(&problem), solution_(&solution),
        field_(problem_->make_field()) {
    CHECK_EQ(problem_->id, solution_->id);
  }

  void set_logstream(ostream& os) {
    logs_ = &os;
  }
  void skip_log(int head, int tail) {
    skip_ = true;
    head_ = head;
    tail_ = tail;
  }

  int Play() {
    if (logs_ && FLAGS_verbose >= 2) *logs_ << "\nPlaying problem " << solution_->id << ", seed:" << solution_->seed << " (" << solution_->tag << ")" << endl;

    unit_cache.resize(problem_->units.size());
    for (int i = 0; i < unit_cache.size(); ++i) {
      unit_cache[i].init(problem_->units[i]);
    }

    field_ = problem_->make_field();
    LCG random(solution_->seed);
    unordered_set<uint32> visit;
    UnitControl control(&unit_cache[random.next() % unit_cache.size()], field_.width());
    int source = 0;
    int ls_old = 0;
    int score = 0;

    string s = solution_->solution;
    googleapis::strrmm(&s, kIgnored);
    for (int i = 0; i < s.size(); ++i) {
      if (source >= problem_->length) {
        if (logs_ && (!skip_ || i < head_ || s.size() - i <= tail_) && FLAGS_verbose >= 2) *logs_ << "Command remaining error. (Game cleared.)" << endl;
        return -1;
      }

      // Game ends if the spawn location is not valid
      if (!control.test(&field_)) {
        if (logs_ && (!skip_ || i < head_ || s.size() - i <= tail_) && FLAGS_verbose >= 2) *logs_ << "Command remaining error. (Dead after placing " << source << "units.)" << endl;
        return -2;
      }

      if (!visit.insert(control.serialize()).second) {
        if (logs_ && (!skip_ || i < head_ || s.size() - i <= tail_) && FLAGS_verbose >= 2) *logs_ << "Command error. (Visited.)" << endl;
        return -3;
      }

      if (logs_ && (!skip_ || i < head_ || s.size() - i <= tail_) && (FLAGS_verbose >= 5 || (FLAGS_verbose >= 4 && visit.size() == 1))) {
        *logs_ << "\n\n================================\n";
        Field overlay = field_;
        control.fill(&overlay, 'o');
        if (field_.contain(control.loc)) {
          overlay.set(control.loc, overlay.test(control.loc) ? '@' : overlay.get(control.loc) == 'o' ? '&' : '$');
        }
        overlay.print(*logs_);
      }

      if (logs_ && (!skip_ || i < head_ || s.size() - i <= tail_) && FLAGS_verbose >= 3) *logs_ << "Snippet:" << i << ": " << StringPiece(s, i, 40) << endl;
      Command c = translate_command(s[i]);
      UnitControl next_control = control.command(c);
      if (logs_ && (!skip_ || i < head_ || s.size() - i <= tail_) && FLAGS_verbose >= 3) *logs_ << "Command: " << command_name(c) << endl;

      if (next_control.test(&field_)) {
        // Applies move
        control = next_control;
      } else {
        if (logs_ && (!skip_ || i < head_ || s.size() - i <= tail_) && FLAGS_verbose >= 3) *logs_ << "Invalid command; Unit locked" << endl;
        // Rejects move and locks unit
        int size = control.fill(&field_, 'x');
        // Clears rows
        int ls = field_.clear_rows();
        // Scoring
        int points = size + 100 * (1 + ls) * ls / 2;
        int line_bonus = max((ls_old - 1) * points / 10, 0);
        ls_old = ls;
        score += points + line_bonus;
        // Next source
        control = UnitControl(&unit_cache[random.next() % unit_cache.size()], field_.width());
        source++;
        visit.clear();
      }
      if (logs_ && (!skip_ || i < head_ || s.size() - i <= tail_) && FLAGS_verbose >= 2) *logs_ << "Move Score = " << score << endl;
    }
    if (!FLAGS_phrases_of_power.empty()) {
      int power_score = phrases_.spell(s);
      if (logs_ && FLAGS_verbose >= 2) *logs_ << "Power Score = " << power_score << endl;
      score += power_score;
    }
    return score;
  }
};

#endif  // SRC_SIM_SIM_H_
