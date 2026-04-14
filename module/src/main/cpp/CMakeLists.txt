cmake_minimum_required(VERSION 3.22.1)
project("ghost_injector")

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

# Enable hidden visibility
set(CMAKE_C_VISIBILITY_PRESET hidden)

# Find required libraries
find_library(log-lib log)

# Main injector library
add_library(ghost_injector SHARED
    ghost_injector.c
    property_hook.c
    ptrace_utils.c
)

# Include directories
target_include_directories(ghost_injector PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# Link libraries
target_link_libraries(ghost_injector
    ${log-lib}
)

# Compiler flags
target_compile_options(ghost_injector PRIVATE
    -Wall
    -Wextra
    -Werror
    -fstack-protector-strong
    -fPIC
    -O2
    -D_FORTIFY_SOURCE=2
)

# Linker flags
set_target_properties(ghost_injector PROPERTIES
    LINK_FLAGS "-Wl,-z,relro -Wl,-z,now -Wl,--exclude-libs,ALL"
)
