package com.dya.sqlparser

class Token (val value: String, val type: TokenType) {

    override fun toString(): String {
        return "'$value': $type"
    }
}