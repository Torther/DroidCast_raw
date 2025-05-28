cmake_minimum_required(VERSION 3.22.1)

project("droidcast_raw")

add_subdirectory(lz4/build/cmake)

add_library(${CMAKE_PROJECT_NAME} SHARED
    droidcast_raw.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME} PUBLIC
    android log jnigraphics lz4)

target_link_options(${CMAKE_PROJECT_NAME}
        PRIVATE
        -Wl,--version-script,${CMAKE_SOURCE_DIR}/droidcast_raw.ver
        -Wl,--no-undefined-version
)

# Without this, changes to the version script will not cause the library to
# relink.
set_target_properties(${CMAKE_PROJECT_NAME}
        PROPERTIES
        LINK_DEPENDS ${CMAKE_SOURCE_DIR}/droidcast_raw.ver
)