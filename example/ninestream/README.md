bazel build //example/ninestream/...
ninestream \
    --controller ./bazel-bin/example/ninestream/controller \
    --worker ./bazel-bin/example/ninestream/worker \
    --replicas 3
