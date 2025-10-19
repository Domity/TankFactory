cmake_minimum_required(VERSION 3.22.1)

project("tankfactory")

add_library( # Sets the name of the library.
        tankfactory

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        native-lib.cpp)

find_library( # Defines the name of the NDK library that
        # you want CMake to locate.
        jnigraphics-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        jnigraphics)

# Specifies libraries CMake should link to your target library.
    target_link_libraries( # Specifies the target library.
            tankfactory

            # Links the target library to the log library
            # and allows it to use logging features.
            ${jnigraphics-lib})
