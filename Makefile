build:
	./build.sh
.PHONY: build

clean:
	bazel clean

run:
	./play_icfp2015 --alsologtostderr -f data/problems/*.json
.PHONY: run

run/%: data/%
	./play_icfp2015 --alsologtostderr -f data/$*

push:
	git pull
	git push
.PHONY: push
