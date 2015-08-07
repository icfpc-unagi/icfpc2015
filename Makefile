build:
	./build.sh
.PHONY: build

clean:
	bazel clean

run: run/problems/problem_0.json

run/%: data/%
	./play_icfp2015 --alsologtostderr -f $*
