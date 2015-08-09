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
  -p "yuggoth"
  -p "blue hades"
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
      if timeout 1800s mono "${ai}" -f "${data}" "${PHRASES[@]}" \
          > "${path}.bak"; then
        mv "${path}.bak" "${path}"
      else
        rm "${path}.bak"
        echo 'null' > "${path}"
      fi
    elif [ "${ai_name%.jar}" != "${ai_name}" ]; then
      if timeout 1800s \
          java -cp "${ai}:/mirror/github/src/gson/gson-2.3.1.jar" \
          Main -f "${data}" "${PHRASES[@]}" > "${path}.bak"; then
        mv "${path}.bak" "${path}"
      else
        rm "${path}.bak"
        echo 'null' > "${path}"
      fi
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
