cc_library(
    name = "hello-world-lib",
    srcs = ["hello-world.cc"],
    hdrs = ["hello-world.h"],
    deps = ["//base"],
    linkstatic = 1,
)

cc_binary(
    name = "hello-world",
    srcs = ["hello-world-main.cc"],
    deps = [
        ":hello-world-lib",
    ],
)

# cc_test(
#     name = "hello-world_test",
#     srcs = ["hello-world_test.cc"],
#     deps = [
#         ":hello-world-lib",
#         "//base:test_main",
#     ],
# )
