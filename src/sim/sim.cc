#include "src/sim/sim.h"

#include "base/base.h"

DEFINE_int32(verbose, 5, "berobero");
DEFINE_bool(include_power_score, false, "[DEPRECATED] use --phraes_of_power=<filename>");
DEFINE_string(phrases_of_power, "", "phrases of power");
