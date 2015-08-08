#!/bin/bash

set -e -u
source "${TEST_SRCDIR}/bin/imos-variables" || exit 1
eval "${IMOSH_INIT}"

TARGET="${TEST_SRCDIR}/src/sim/sim_main"
INPUT="${TMPDIR}/input.json"
OUTPUT="${TMPDIR}/output.json"

cat << EOM > "${INPUT}"
{"height":15,"width":15,"sourceSeeds":[0],"units":[{"members":[{"x":0,"y":0},{"x":2,"y":0}],"pivot":{"x":1,"y":5}},{"members":[{"x":0,"y":0},{"x":0,"y":2}],"pivot":{"x":0,"y":6}},{"members":[{"x":2,"y":0},{"x":0,"y":1},{"x":2,"y":2}],"pivot":{"x":1,"y":6}},{"members":[{"x":0,"y":0},{"x":1,"y":1},{"x":0,"y":2}],"pivot":{"x":0,"y":6}}],"id":20,"filled":[],"sourceLength":100}
EOM

cat << EOM > "${OUTPUT}"
[{"problemId":20,"seed":0,"solution":"alllllllllllllappppaaaadddaaaaallllllllllaaaaaaaalllllllaaaaalddpaaaaaaaallkppppaadddppppppaaaadadpppppaaaaaddpaaaaaaalllllpllbbbbbkbkkpaalllllllldpaaaaaallllllllpllbbbdddpaallddpaaalllllldplllllllllbblappaaaaaaaaalllllpllbbbbkkkpaaaaaaaaalllllalllllkkpppppaaaadddplllllbbbbkbkalllllbbbkkaaaaaddpaaaaaaaaaaalllalllllkkpaaaaaaaaaaaallpaaadddpaaaaaaaaaalllappaaaaaaaaadpaaaaaaallkaplbbbbbbkbbkkapppppadddpllllbbbbkbbbkplllllllldplllddallllbbbbkbbkaaaaalllldpllllllllbklpalllllllklppppaaaddaaaaaaaaaaaallpaaaaaaaaadpppaaaaaaaaadpalllkkppppppppdpaaaadpaaalllllllldplbbbdddpppppppdaaaaaadpppppaaaaaadaaaappppppppdaaaaadplllldddpaaaalddpaalllddplllllllllbbbkblpaaalllllllkppppaaaaddpppaaaaddpllllllllllllpppppadddapaallllllllkpaaallllllllllalllllbbkkpaaaalkkplllllddpppadddpaaaallllllldpppppppdppaaaaadapllbbbbbkbkkplllllllllbklpppppppdppaaaaadapppaaaadddplllllkkplllllllllbbkbbblplllllllllblkbbaaaaaalllldalllllbbbbkbkpaaaaddpllllbbbbkkpppppppaadddppppppaadaaddpaaaaaaaaalllllpaallllllllkpaaaaaaaaaldpaaaaalllllkaaaallkkaaaaaaaaaaaalpalllllllllllllpppppppadaaaaaaalpaaaallllllllpllllbkkkpllllbbkkkpllldddplllllbbkkaaadddplllbddlpppaaaaaaaaaalaplllbbkkklppppaaaaaadda","tag":"chokudAIver0.1.6@1744"}]
EOM

CHECK [ 1744 -eq "$("${TARGET}" --output_score "${INPUT}" "${OUTPUT}")" ]
