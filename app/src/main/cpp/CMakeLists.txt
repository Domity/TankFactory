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

set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -Os -fvisibility=hidden -ffunction-sections -fdata-sections")

set_target_properties(tankfactory PROPERTIES LINK_FLAGS "-Wl,--gc-sections")
