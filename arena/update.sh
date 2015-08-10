#!/bin/bash

source imosh

AI=(/mirror/dropbox/arena/*)
DATA=(/mirror/github/data/problems/*)
PHRASES=(
  -p "ei!"
  -p "ia! ia!"
  -p "r'lyeh"
  -p "necronomicon"
  -p "yogsothoth"
  -p "in his house at r'lyeh dead cthulhu waits dreaming."
  -p "yuggoth"
  -p "blue hades"
  -p "planet 10"
  -p "monkeyboy"
  -p "tsathoggua"
  -p "yoyodyne"
  -p "john bigboote"
  -p "the laundry"
  -p "cthulhu fhtagn!"
)

run() {
  local ai="${1}"
  local data="${2}"

  ai_name="${ai##*/}"
  ai_path="/alloc/dropbox/www/results/${ai_name}"
  mkdir -p "${ai_path}"
  data_name="${data##*/}"
  path="${ai_path}/${data_name}"

  if [ ! -f "${path}" -a ! -f "${path}.bak" ]; then
    echo "Generating ${path}..."
    if [ "${ai_name%.exe}" != "${ai_name}" ]; then
      if TMPDIR=/tmp timeout 120s php /mirror/github/play_icfp2015.php \
             -c 1 -@x "mono ${ai}" -@s /mirror/global/bin/sim_main -t 100 \
             -f "${data}" "${PHRASES[@]}" > "${path}.bak"; then
      # if timeout 1800s mono "${ai}" -f "${data}" "${PHRASES[@]}" \
      #     > "${path}.bak"; then
        tail -n 1 "${path}.bak" > "${path}"
        rm "${path}.bak"
      else
        rm "${path}.bak"
        echo 'null' > "${path}"
      fi
    elif [ "${ai_name%.jar}" != "${ai_name}" ]; then
      if TMPDIR=/tmp timeout 1800s php /mirror/github/play_icfp2015.php \
             -c 1 -@x \
             "java -Xmx4g -cp ${ai}:/mirror/github/src/gson/gson-2.3.1.jar Main" \
             -@s /mirror/global/bin/sim_main -t 1700 \
             -f "${data}" "${PHRASES[@]}" > "${path}.bak"; then
      # if timeout 1800s \
      #     java -Xmx4g -cp "${ai}:/mirror/github/src/gson/gson-2.3.1.jar" \
      #     Main -f "${data}" "${PHRASES[@]}" > "${path}.bak"; then
        tail -n 1 "${path}.bak" > "${path}"
        rm "${path}.bak"
      else
        rm "${path}.bak"
        echo 'null' > "${path}"
      fi
    elif [ "${ai_name%.txt}" != "${ai_name}" ]; then
      if TMPDIR=/tmp timeout 120s \
             php /mirror/github/play_icfp2015.php \
             -@r "${ai}" -@s /mirror/global/bin/sim_main -t 100 \
             -f "${data}" "${PHRASES[@]}" > "${path}.bak"; then
        tail -n 1 "${path}.bak" > "${path}"
        rm "${path}.bak"
      else
        rm "${path}.bak"
        echo 'null' > "${path}"
      fi
    elif [ "${ai_name}" = 'final' ]; then
      pushd /mirror/github
      if TMPDIR=/tmp timeout 360s \
             ./play_icfp2015 \
             -@s /mirror/global/bin/sim_main \
             -m 4096 \
             -t 300 \
             -f "${data}" "${PHRASES[@]}" > "${path}.bak"; then
        tail -n 1 "${path}.bak" > "${path}"
        rm "${path}.bak"
      else
        rm "${path}.bak"
        echo 'null' > "${path}"
      fi
      popd
    else
      true
    fi
  else
    echo "Skipping ${path}..."
  fi
}

for ai in "${AI[@]}"; do
  for data in "${DATA[@]}"; do
    echo "${ai}: ${data}"
    sub::throttle 32
    run "${ai}" "${data}" &
  done
done
