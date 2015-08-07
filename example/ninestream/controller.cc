#include <iostream>
#include <stdio.h>
#include <stdlib.h>

#include "base/base.h"

class NineStream {
 public:
  static util::Status Stream(vector<int>* streams) {
    string response = Command("stream");

    vector<StringPiece> pieces = strings::Split(response, " ");
    if (pieces[0] != "OK") {
      return util::Status(util::error::UNKNOWN,
                          StrCat("Unexpected response: ", response));
    }

    streams->clear();
    for (int i = 1; i < pieces.size(); i++) {
      streams->push_back(atoi(pieces[i].ToString().c_str()));
    }

    return util::Status();
  }

  static util::Status Write(int stream_id, StringPiece content) {
    string response = Command(StrCat("write ", stream_id, " ", content));

    if (response != "OK") {
      return util::Status(util::error::UNKNOWN,
                          StrCat("Unexpected response: ", response));
    }

    return util::Status();
  }

  static util::Status WriteAll(StringPiece content) {
    string response = Command(StrCat("writeall ", content));

    if (response != "OK") {
      return util::Status(util::error::UNKNOWN,
                          StrCat("Unexpected response: ", response));
    }

    return util::Status();
  }

  static util::Status ReadAsync(double timeout,
                                int* stream_id,
                                string* content) {
    string response = Command(StringPrintf("read %.6f", timeout));

    vector<StringPiece> pieces = strings::Split(response, " ");
    if (pieces[0] == "OK") {
      if (pieces.size() < 2) {
        return util::Status(util::error::UNKNOWN, "Insufficient response.");
      }
      *stream_id = atoi(pieces[1].ToString().c_str());
      *content = strings::Join(
          vector<StringPiece>(pieces.begin() + 2, pieces.end()), " ");
      return util::Status();
    } else if (pieces[0] == "NO_CONTENT") {
      return util::Status(util::error::CANCELLED, "No contet is read.");
    } else if (pieces[0] == "NOT_FOUND") {
      return util::Status(util::error::NOT_FOUND, "No streams are available.");
    }

    return util::Status(util::error::UNKNOWN,
                        StrCat("Unknown response: ", response));
  }

  static util::Status Read(int* stream_id, string* content) {
    return ReadAsync(-1, stream_id, content);
  }

  static void Exit(int status) {
    printf("exit %d\n", status);
    fflush(stdout);
  }

 protected:
  static string Command(const string& line) {
    CHECK(line.find('\r', 0) == string::npos &&
          line.find('\n', 0) == string::npos)
        << "Command must not contain a new line, but: " << line;
    puts(line.c_str());
    fflush(stdout);

    string result;
    getline(std::cin, result);
    return result;
  }
};

int main(int argc, char** argv) {
  base::Init(&argc, &argv);

  vector<int> streams;
  CHECK_OK(NineStream::Stream(&streams));
  for (int i = 0; i < streams.size(); i++) {
    CHECK_OK(NineStream::Write(streams[i], StrCat(i, " ", streams.size())));
  }

  int max_score = 0;
  while (true) {
    int stream_id;
    string content;
    CHECK_OK(NineStream::Read(&stream_id, &content));
    vector<StringPiece> response = strings::Split(content, " ");
    int score = atoi(response[0].ToString().c_str());
    if (max_score < score) {
      max_score = score;
      CHECK_OK(NineStream::Write(-1, StrCat(content)));
    }
  }

  return 0;
}
