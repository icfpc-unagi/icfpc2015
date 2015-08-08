#!/bin/bash

set -e -u
source "${TEST_SRCDIR}/bin/imos-variables" || exit 1
eval "${IMOSH_INIT}"

TARGET="${TEST_SRCDIR}/src/sim/sim_main"

evaluate() {
  local data_set="${1}"; shift
  local input_name="${1}"; shift
  local index=0

  scores=($(
      "${TARGET}" --output_score --verbose=0 \
          "${TEST_SRCDIR}/data/problems/${input_name}.json" \
          "${TEST_SRCDIR}/data/${data_set}/${input_name}.json"))
  for score in "$@"; do
    actual_score="${scores[${index}]}"
    (( index += 1 ))
    CHECK --message="Input ${input_name} (#$index) should score ${score}." \
        [ "${score}" -eq "${actual_score}" ]
  done
}

evaluate wata62 problem_0 4849
evaluate wata62 problem_1 300
evaluate wata62 problem_10 2000
evaluate wata62 problem_11 805 402 396 610 286
evaluate wata62 problem_12 330 1282 315 1068 172 1413 187 1166 1080 964
evaluate wata62 problem_13 204
evaluate wata62 problem_14 992
evaluate wata62 problem_15 336
evaluate wata62 problem_16 3054
evaluate wata62 problem_17 800
evaluate wata62 problem_18 499
evaluate wata62 problem_19 800
evaluate wata62 problem_2 3603 3603 3503 3703 4406 4705 2901 4203 4013 3703
evaluate wata62 problem_20 2514
evaluate wata62 problem_21 320
evaluate wata62 problem_22 3710
evaluate wata62 problem_23 4730
evaluate wata62 problem_3 1060 868 862 560 1083
evaluate wata62 problem_5 970 856 856 863 1059 1060 1571 1074 870 1060
evaluate wata62 problem_6 6776 7372 6962 7382 7030 6960 7081
evaluate wata62 problem_7 360 366 373 359 374
evaluate wata62 problem_9 9272 9608 6300 8573 17321

evaluate chokudai105 problem_0 3749
evaluate chokudai105 problem_1 800
evaluate chokudai105 problem_10 3200
evaluate chokudai105 problem_11 1005 1130 1350 1145 1230
evaluate chokudai105 problem_13 304
evaluate chokudai105 problem_14 1392
evaluate chokudai105 problem_15 2000
evaluate chokudai105 problem_16 2234
evaluate chokudai105 problem_17 1400
evaluate chokudai105 problem_18 731
evaluate chokudai105 problem_19 1300
evaluate chokudai105 problem_20 2344
evaluate chokudai105 problem_21 620
evaluate chokudai105 problem_22 3500
evaluate chokudai105 problem_23 4300
evaluate chokudai105 problem_3 1360 1268 1262 1560 1173
