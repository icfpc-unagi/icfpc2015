cc_library(
    name = "sim",
    srcs = ["sim.cc"],
    hdrs = ["sim.h"],
    deps = ["//base"],
    linkstatic = 1,
)

cc_binary(
    name = "sim_main",
    srcs = ["sim_main.cc"],
    deps = [
        ":sim",
    ],
)

cc_test(
    name = "sim_test",
    srcs = ["sim_test.cc"],
    deps = [
        ":sim",
        "//base:test_main",
    ],
)
