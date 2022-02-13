package com.dya.sqlparser

class Tokens {
    private val buffer = StringBuilder()
    private val tokens = mutableListOf<Token>()

    fun buffer(character: Char) {
        buffer.append(character)
    }

    fun pushToken(type: TokenType) {
        if (buffer.isNotEmpty()) {
            tokens.add(Token(buffer.toString(), type))
            buffer.setLength(0)
        }
    }

    fun list(): List<Token> {
        return tokens
    }

    fun bufferEmpty(): Boolean {
        return buffer.isEmpty()
    }
}