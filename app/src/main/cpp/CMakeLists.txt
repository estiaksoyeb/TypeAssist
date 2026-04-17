cmake_minimum_required(VERSION 3.22.1)

project("typeassist")

add_library(
        typeassist
        SHARED
        native-lib.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        typeassist
        ${log-lib})
