cmake_minimum_required(VERSION 3.4.1)

include_directories(./)

add_library(native-lib
        SHARED

        # main game files
        native-lib.cpp
        Engine.cpp

        # audio engine
        audio/AAssetDataSource.cpp
        audio/Player.cpp

        # utility functions
        utils/logging.h
        utils/UtilityFunctions.cpp
        )

# Build the Oboe library
set(OBOE_DIR ../../../libs/oboe)
add_subdirectory(${OBOE_DIR} ./oboe)
# Make the Oboe public headers available to the app
include_directories(${OBOE_DIR}/include)

target_sources(native-lib PRIVATE audio/NDKExtractor.cpp)

# Specify the libraries which our native library is dependent on
target_link_libraries(native-lib log android oboe mediandk)

# Enable optimization flags
target_compile_options(native-lib PRIVATE -std=c++14 -Wall -Werror "$<$<CONFIG:RELEASE>:-Ofast>")