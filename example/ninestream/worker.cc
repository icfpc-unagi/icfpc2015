#include "base/base.h"
#include "boost/uuid/sha1.hpp"

typedef boost::uuids::detail::sha1 Sha1;

int CountBits(unsigned int value) {
  int count = 0;
  for (; value & 1; value >>= 1) {
    count++;
  }
  return count;
}

int EvaluateSha1Score(int64_t value) {
  static Sha1 sha1;
  sha1.reset();
  for (; value > 0; value /= 26) {
    int part = value % 26;
    sha1.process_byte('a' + part);
  }
  unsigned int digest[5];
  sha1.get_digest(digest);
  int sum = 0;
  for (int i = 4; i >= 0; i--) {
    int count = CountBits(digest[i]);
    sum += count;
    if (count != 32) break;
  }
  return sum;
}

string ToString(int64_t value) {
  string result = "";
  for (; value > 0; value /= 26) {
    int part = value % 26;
    result += string(1, 'a' + part);
  }
  return result;
}

int main(int argc, char** argv) {
  base::Init(&argc, &argv);
  long long start_value = 0;
  long long step = 1;
  scanf("%lld%lld", &start_value, &step);
  int max_score = 0;
  for (long long value = start_value; ; value += step) {
    int score = EvaluateSha1Score(value);
    if (score > max_score) {
      max_score = score;
      printf("%d %s\n", score, ToString(value).c_str());
      fflush(stdout);
    }
  }
  return 0;
}
