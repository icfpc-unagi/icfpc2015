package(default_visibility = ["//visibility:public"])

sh_test(
    name = "play_icfp2015_test",
    srcs = ["play_icfp2015_test.sh"],
    data = [
        ":play_icfp2015.php",
        "//bin",
        "//src/sim:sim_main",
        "//data/problems:problems",
    ],
)
