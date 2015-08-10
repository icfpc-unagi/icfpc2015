#ifndef SRC_SIM_SIM_H_
#define SRC_SIM_SIM_H_

#include <algorithm>
#include <cstring>
#include <fstream>
#include <iostream>
#include <iterator>
#include <queue>
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

static constexpr int kMap[] = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,23,3,0,0,0,0,0,2,0,0,0,0,0,0,4,0,5,30,12,6,18,24,0,0,0,0,0,0,0,0,0,0,0,13,7,8,25,9,10,14,15,16,17,31,19,20,21,22,1,26,27,32,33,34,28,35,36,11,29,0,0,0,0,0,0,13,7,8,25,9,10,14,15,16,17,31,19,20,21,22,1,26,27,32,33,34,28,35,36,11,29};
class Phrases {
 private:
  static constexpr int N = 37;
  struct PMA {
    PMA* next[N] = {}; // 0: fail
    vector<int> ok;
  };

  vector<int> lengths_;
  const PMA* pma_;

 public:
  Phrases(const string& filename) {
    ifstream ifs(filename);
    CHECK(ifs.good()) << "Failed to read phrases: " << filename;
    vector<string> phrases;
    string s;
    while (getline(ifs, s)) {
      googleapis::StripTrailingNewline(&s);
      googleapis::LowerString(&s);
      phrases.push_back(s);
      lengths_.push_back(s.size());
    }
    pma_ = buildPMA(phrases);
  }
  ~Phrases() {
    releasePMA(pma_);
  }

  // Returns power score
  int spell(const string& s) const {
    int score = 0;
    vector<int> res = match(s);
    for (int i = 0; i < res.size(); ++i) {
      if (res[i] > 0) score += 2 * lengths_[i] * res[i] + 300;
    }
    return score;
  }

 private:
  static PMA* buildPMA(const vector<string>& patterns) {
    PMA* root = new PMA;
    root->next[0] = root;
    for (int i = 0; i < patterns.size(); ++i) {
      PMA* t = root;
      for (int c : patterns[i]) {
        int m = kMap[c];
        if (!t->next[m]) t->next[m] = new PMA;
        t = t->next[m];
      }
      t->ok.push_back(i);
    }
    queue<PMA*> que;
    for (int i = 1; i < N; ++i) {
      if (root->next[i]) {
        root->next[i]->next[0] = root;
        que.push(root->next[i]);
      } else {
        root->next[i] = root;
      }
    }
    while (!que.empty()) {
      PMA* t = que.front();
      que.pop();
      for (int i = 1; i < N; ++i) {
        if (t->next[i]) {
          PMA *r = t->next[0];
          while (!r->next[i]) r = r->next[0];
          t->next[i]->next[0] = r->next[i];
          vector<int> u;
          set_union(
              t->next[i]->ok.begin(), t->next[i]->ok.end(),
              r->next[i]->ok.begin(), r->next[i]->ok.end(),
              back_inserter(u));
          t->next[i]->ok.swap(u);
          que.push(t->next[i]);
        }
      }
    }
    return root;
  }
  vector<int> match(const string& s) const {
    const PMA* pma = pma_;
    vector<int> res(lengths_.size());
    for (int c : s) {
      int m = kMap[c];
      while (!pma->next[m]) pma = pma->next[0];
      pma = pma->next[m];
      for (int i : pma->ok) res[i]++;
    }
   return res;
  }
  static void releasePMA(const PMA* root) {
    unordered_set<const PMA*> v;
    queue<const PMA*> q;
    q.push(root);
    while (!q.empty()) {
      const PMA* pma = q.front();
      q.pop();
      if (v.insert(pma).second) for (int i = 1; i < N; ++i) if (pma->next[i]) q.push(pma->next[i]);
    }
    for (const PMA* pma : v) delete pma;
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
        // returns number of accepted commands in negative
        return -i;
      }

      // Game ends if the spawn location is not valid
      if (!control.test(&field_)) {
        if (logs_ && (!skip_ || i < head_ || s.size() - i <= tail_) && FLAGS_verbose >= 2) *logs_ << "Command remaining error. (Dead after placing " << source << "units.)" << endl;
        // returns number of accepted commands in negative
        return -i;
      }

      if (!visit.insert(control.serialize()).second) {
        if (logs_ && (!skip_ || i < head_ || s.size() - i <= tail_) && FLAGS_verbose >= 2) *logs_ << "Command error. (Visited.)" << endl;
        // returns number of accepted commands in negative
        return -i;
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
      Phrases phrases(FLAGS_phrases_of_power);
      int power_score = phrases.spell(s);
      if (logs_ && FLAGS_verbose >= 2) *logs_ << "Power Score = " << power_score << endl;
      score += power_score;
    }
    return score;
  }
};

#endif  // SRC_SIM_SIM_H_
