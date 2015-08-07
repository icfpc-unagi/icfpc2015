licenses(["notice"])

package(default_visibility = ["//visibility:public"])

genrule(
    name = "chokudai",
    srcs = ["Main.cs"],
    outs = ["Main.exe"],
    cmd = "if [ \"$$(uname)\" = 'Darwin' ]; then touch $(OUTS); else dmcs -r:System,System.Net.Http,System.Runtime.Serialization -lib:/usr/lib/mono/4.5 $(locations :Main.cs) -out:$(OUTS); fi",
)
