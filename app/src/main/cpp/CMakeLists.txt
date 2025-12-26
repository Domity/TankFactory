cmake_minimum_required(VERSION 3.22.1)

project("tankfactory")

find_package(OpenMP)
if(OPENMP_FOUND)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${OpenMP_C_FLAGS}")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${OpenMP_CXX_FLAGS}")
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} ${OpenMP_EXE_LINKER_FLAGS}")
endif()

add_library(
        tankfactory
        SHARED
        mirage_tank_encoder.cpp
        lsb_tank_encoder.cpp
        lsb_tank_decoder.cpp)

find_library(
        jnigraphics-lib
        jnigraphics)

target_link_libraries(
        tankfactory
        ${jnigraphics-lib}
        OpenMP::OpenMP_CXX)

set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -fvisibility=hidden -ffunction-sections -fdata-sections")
set_target_properties(tankfactory PROPERTIES LINK_FLAGS "-Wl,--gc-sections")
