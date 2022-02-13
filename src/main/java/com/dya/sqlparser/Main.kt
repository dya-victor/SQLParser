package com.dya.sqlparser

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val tokens = TokenParser.parse("SELECT 'abcd' as id, a.name, +12 as num, \r\nFROM activities")

            if (tokens is Either.Left) {
                println("SQL statement has been parsed, tokens: ${tokens.value}")
            } else if (tokens is Either.Right) {
                println("Error during statement parse: ${tokens.value}")
            }
        }
    }
}