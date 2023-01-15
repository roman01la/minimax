#pragma once

#include <bx/debug.h>

#define DBG_STRINGIZE_(_x) #_x
#define DBG_STRINGIZE(_x) DBG_STRINGIZE_(_x)
#define DBG_FILE_LINE_LITERAL "" __FILE__ "(" DBG_STRINGIZE(__LINE__) "): "
#define DBG(_format, ...) bx::debugPrintf(DBG_FILE_LINE_LITERAL "" _format "\n", ##__VA_ARGS__)