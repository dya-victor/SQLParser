package com.dya.sqlparser

object TokenParser {
    private val separatorChars = "=.();,\r\n ".toSet()
    private val numberChars = "0123456789".toSet()
    private val sign = "+-".toSet()
    private const val decimalSeparator = '.'
    private const val stringChar = '\''

    fun parse(input: CharSequence): Either<List<Token>, String> {
        val context = Context()

        while (context.parseIndex < input.length) {
            val character = input.get(context.parseIndex)
            context.incIndex()

            when (context.state) {
                State.INITIAL -> processInitialState(context, character)
                State.WORD -> processWordState(context, character)
                State.STRING -> processStringState(context, character)
                State.NUMBER -> processNumberState(context, character)
                State.SIGN -> processSignState(character, context)
                State.SEPARATOR -> processSeparatorState(character, context)
                State.END -> break
                //noinspection REDUNDANT_ELSE_IN_WHEN
                else -> throw IllegalStateException("Unknown state ${context.state}")
            }
        }

        context.pushToken(TokenType.WORD)

        return Either.Left(context.tokens.list())
    }

    private fun processInitialState(context: Context, character: Char) {
        if (!context.tokens.bufferEmpty()) {
            throw IllegalStateException("Initial state must be started with empty buffer")
        }

        when (character) {
            in sign -> {
                context.state = State.SIGN
            }
            in separatorChars -> {
                context.decIndex()
                context.state = State.SEPARATOR
            }
            in numberChars -> {
                context.decIndex()
                context.state = State.NUMBER
                context.numberState = NumberState.INTEGER
            }
            stringChar -> {
                context.state = State.STRING
            }
            else -> {
                context.decIndex()
                context.state = State.WORD
            }
        }
    }

    private fun processWordState(context: Context, character: Char) {
        when (character) {
            in separatorChars -> {
                context.pushToken(TokenType.WORD)
                context.decIndex()
                context.state = State.INITIAL
            }
            in sign -> {
                context.endWith(Either.Right("Sign in the middle of the word/identifier at ${context.parseIndex}"))
            }
            else -> {
                context.buffer(character)
            }
        }
    }

    private fun processStringState(context: Context, character: Char) {
        if (character == stringChar) {
            context.pushToken(TokenType.STRING)
            context.state = State.INITIAL
        } else {
            context.buffer(character)
        }
    }

    private fun processNumberState(context: Context, character: Char) {
        if (context.numberState == NumberState.SIGN) {
            context.buffer(character)
            context.numberState = NumberState.INTEGER
        } else if (context.numberState == NumberState.INTEGER) {
            when (character) {
                in numberChars -> {
                    context.buffer(character)
                }
                decimalSeparator -> {
                    context.buffer(character)
                    context.numberState = NumberState.DECIMAL
                }
                else -> {
                    context.pushNumberToken()
                    // Current character does not belong to number. Rewind to retry
                    context.decIndex()
                }
            }
        } else if (context.numberState == NumberState.DECIMAL) {
            if (character in numberChars) {
                context.buffer(character)
            } else if (character == decimalSeparator && !Character.isWhitespace(character)) {
                context.endWith(Either.Right("Unexpected decimal separator at ${context.parseIndex}"))
            } else {
                context.pushNumberToken()
                // Current character does not belong to number. Rewind to retry
                context.decIndex()
            }
        } else {
            throw IllegalStateException("Unknown number state: ${context.numberState}")
        }
    }

    private fun processSignState(character: Char, context: Context) {
        when (character) {
            in numberChars -> {
                context.decIndex(2)
                context.state = State.NUMBER
                context.numberState = NumberState.SIGN
            }
            in sign -> {
                context.endWith(Either.Right("Two signs are not expected at ${context.parseIndex}"))
            }
            in separatorChars -> {
                context.endWith(Either.Right("Sign followed by separator ar ${context.parseIndex}"))
            }
            else -> {
                // Sign is separator
                context.decIndex()
                context.state = State.SEPARATOR
            }
        }
    }

    private fun processSeparatorState(character: Char, context: Context) {
        if (!Character.isWhitespace(character)) {
            context.buffer(character)
            context.pushToken(TokenType.SEPARATOR)
        }
        context.state = State.INITIAL
    }

    private class Context {
        var state = State.INITIAL
        var parseIndex : Int = 0
        var numberState = NumberState.NONE
        val tokens = Tokens()
        var result : Either<List<String>, String> = Either.Right("Result is not set")

        fun incIndex() = parseIndex++

        fun decIndex(decrement: Int = 1) {
            parseIndex -= decrement
        }

        fun pushToken(type: TokenType) = tokens.pushToken(type)

        fun pushNumberToken() {
            pushToken(TokenType.NUMBER)
            state = State.INITIAL
            numberState = NumberState.NONE
        }

        fun buffer(character: Char) = tokens.buffer(character)

        fun endWith(result: Either<List<String>, String>) {
            this.result = result
            this.state = State.END
        }
    }

    private enum class State {
        INITIAL, WORD, STRING, NUMBER, SIGN, SEPARATOR, END
    }

    private enum class NumberState {
        INTEGER, DECIMAL, NONE, SIGN
    }
}