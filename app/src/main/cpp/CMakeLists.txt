cmake_minimum_required(VERSION 3.22.1)
project("tankfactory")

add_library(
        tankfactory
        SHARED
        mirage_tank_encoder.cpp
        lsb_tank_encoder.cpp
        lsb_tank_decoder.cpp
        obfuscation.cpp
)

find_library(jnigraphics-lib jnigraphics)

target_link_libraries(
        tankfactory
        ${jnigraphics-lib}
)

target_compile_options(tankfactory PRIVATE
        -Oz
        -ffast-math
        -fomit-frame-pointer

        -fno-exceptions
        -fno-rtti
        -fno-unwind-tables
        -fno-asynchronous-unwind-tables

        -fvisibility=hidden
        -fvisibility-inlines-hidden
        -fmerge-all-constants
        -ffunction-sections
        -fdata-sections

        -fno-stack-protector
        -flto=thin
)

target_link_options(tankfactory PRIVATE
        -Wl,--gc-sections
        -Wl,--icf=all
        -Wl,--strip-all
        -Wl,--hash-style=gnu
        -Wl,--exclude-libs,ALL
        -fuse-ld=lld
        -flto=thin
)
