edit() {
  if [ "${#}" -ne 0 ]; then
    open -a 'Sublime Text' "${@}"
  else
    LOG ERROR 'Wrong number of arguments.'
  fi
}
