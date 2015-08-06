package(default_visibility = ["//visibility:public"])

cc_library(
    name = "base",
    srcs = ["base.cc"],
    hdrs = [
        "base.h",
        "strings.h",
        "util.h",
    ],
    deps = [
        "//gflags",
        "//glog",
        "//googleapis:library",
    ],
    linkstatic = 1,
)

cc_library(
    name = "number",
    srcs = ["number.cc"],
    hdrs = ["number.h"],
    deps = [
        "//boost",
    ],
    linkstatic = 1,
)

cc_library(
    name = "test",
    srcs = ["test.cc"],
    hdrs = ["test.h"],
    deps = [
        ":base",
        "//gmock",
    ],
    testonly = 1,
    linkstatic = 1,
)

cc_library(
    name = "test_main",
    srcs = ["test_main.cc"],
    hdrs = ["test_main.h"],
    deps = [
        ":test",
        "//gmock:gmock_main",
    ],
    testonly = 1,
    linkstatic = 1,
)
