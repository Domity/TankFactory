cmake_minimum_required(VERSION 3.22.1)

project("tankfactory")

add_library(
        tankfactory
        SHARED
        native-lib.cpp)

find_library(
        jnigraphics-lib
        jnigraphics)

    target_link_libraries(
            tankfactory
            ${jnigraphics-lib})
