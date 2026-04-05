cmake_minimum_required(VERSION 3.22.1)

set(VEROVIO_SOURCE_DIR ${PROJECT_ROOT}/external/verovio)
set(SWIG_WRAP_FILE ${CMAKE_SOURCE_DIR}/verovio_wrap.cxx)

set(BUILD_AS_ANDROID_LIBRARY ON)
add_subdirectory(${VEROVIO_SOURCE_DIR}/cmake ${CMAKE_CURRENT_BINARY_DIR}/verovio_build)
include_directories(
    ${VEROVIO_SOURCE_DIR}/include/vrv
    ${VEROVIO_SOURCE_DIR}/include/json
    ${VEROVIO_SOURCE_DIR}/include/pugi
    ${VEROVIO_SOURCE_DIR}/libmei/dist
    ${VEROVIO_SOURCE_DIR}/libmei/addons
)

project("verovio-android")

add_library(verovio-android SHARED ${SWIG_WRAP_FILE})

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=0x4000")

find_library(log-lib log)

target_link_libraries(verovio-android verovio ${log-lib})
