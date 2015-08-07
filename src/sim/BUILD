package(default_visibility = ["//visibility:public"])

cc_library(
    name = "sim",
    srcs = ["sim.cc"],
    hdrs = ["sim.h"],
    deps = [
        ":data",
        "//base",
        "//boost",
    ],
    linkstatic = 1,
)

cc_library(
    name = "data",
    srcs = ["data.cc"],
    hdrs = ["data.h"],
    deps = [
        "//base",
        "//boost",
    ],
    linkstatic = 1,
)

cc_binary(
    name = "sim_main",
    srcs = ["sim_main.cc"],
    deps = [
        ":data",
        ":sim",
    ],
)

cc_test(
    name = "sim_test",
    srcs = ["sim_test.cc"],
    deps = [
        ":data",
        ":sim",
        "//base:test_main",
    ],
)
