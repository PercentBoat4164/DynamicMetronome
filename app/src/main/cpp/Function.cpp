#include <filesystem>
#include <vector>

std::vector<std::string> *listProgramsIn(const std::filesystem::path& path) {
    auto *results{new std::vector<std::string>{}};
    for (auto &file : path) if (file.extension() == ".met") results->push_back(file.filename());
    return results;
}

