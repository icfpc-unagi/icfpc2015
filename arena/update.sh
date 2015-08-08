#!/bin/bash

source imosh

AI=(/mirror/dropbox/arena/*)
DATA=(/mirror/github/data/problems/*)

run() {
  local ai="${1}"
  local data="${2}"

  ai_name="${ai##*/}"
  ai_path="/alloc/dropbox/www/results/${ai_name}"
  mkdir -p "${ai_path}"
  data_name="${data##*/}"
  path="${ai_path}/${data_name}"

  if [ ! -f "${path}" ]; then
    echo "Generating ${path}..."
    if [ "${ai_name%.exe}" != "${ai_name}" ]; then
      if timeout 180s mono "${ai}" "${data}" > "${path}.bak"; then
        mv "${path}.bak" "${path}"
      else
        rm "${path}.bak"
        echo 'null' > "${path}"
      fi
    elif [ "${ai_name%.jar}" != "${ai_name}" ]; then
      if timeout 180s \
          java -cp "${ai}:/mirror/github/src/wata/gson-2.3.1.jar" \
          Main -f "${data}" > "${path}.bak"; then
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
    sub::throttle 16
    run "${ai}" "${data}" &
  done
done