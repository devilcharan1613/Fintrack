package com.tracmio.app.data.repository

internal fun parseCsvLine(line: String): List<String> {
    val values = mutableListOf<String>()
    val current = StringBuilder()
    var quoted = false
    var index = 0
    while (index < line.length) {
        val character = line[index]
        when {
            character == '"' && quoted && index + 1 < line.length && line[index + 1] == '"' -> {
                current.append('"')
                index++
            }
            character == '"' -> quoted = !quoted
            character == ',' && !quoted -> {
                values += current.toString()
                current.clear()
            }
            else -> current.append(character)
        }
        index++
    }
    values += current.toString()
    return values
}
