package com.agent.android.agent.skills

import java.text.DecimalFormat
import java.util.ArrayDeque
import java.util.Deque

class CalculatorSkill {

    fun calculate(expression: String): SkillResult {
        val startTime = System.currentTimeMillis()
        return try {
            val tokens = tokenize(expression)
            if (tokens.isEmpty()) {
                return SkillResult("CALCULATE", SkillStatus.FAILED, "Empty expression", System.currentTimeMillis() - startTime, "EMPTY_EXPRESSION")
            }
            val rpn = infixToRPN(tokens)
            val result = evaluateRPN(rpn)
            val formatted = DecimalFormat("#.##########").format(result)
            SkillResult("CALCULATE", SkillStatus.SUCCESS, formatted, System.currentTimeMillis() - startTime)
        } catch (e: ArithmeticException) {
            SkillResult("CALCULATE", SkillStatus.FAILED, "Division by zero", System.currentTimeMillis() - startTime, "DIVISION_BY_ZERO")
        } catch (e: Exception) {
            SkillResult("CALCULATE", SkillStatus.FAILED, "Invalid expression: ${e.message}", System.currentTimeMillis() - startTime, "INVALID_EXPRESSION")
        }
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        val clean = expr.replace("\\s+".toRegex(), "")
        while (i < clean.length) {
            val c = clean[i]
            if (c.isDigit() || c == '.') {
                val sb = StringBuilder()
                while (i < clean.length && (clean[i].isDigit() || clean[i] == '.')) {
                    sb.append(clean[i])
                    i++
                }
                tokens.add(sb.toString())
            } else if (c in "+-*/%^()") {
                tokens.add(c.toString())
                i++
            } else {
                throw IllegalArgumentException("Invalid character: $c")
            }
        }
        return tokens
    }

    private fun precedence(op: String?): Int {
        return when (op) {
            "+", "-" -> 1
            "*", "/", "%" -> 2
            "^" -> 3
            else -> -1
        }
    }

    private fun infixToRPN(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        val stack: Deque<String> = ArrayDeque()

        for (token in tokens) {
            if (token.toDoubleOrNull() != null) {
                output.add(token)
            } else if (token == "(") {
                stack.push(token)
            } else if (token == ")") {
                while (stack.isNotEmpty() && stack.peek() != "(") {
                    output.add(stack.pop())
                }
                if (stack.isEmpty()) throw IllegalArgumentException("Mismatched parentheses")
                stack.pop()
            } else {
                while (stack.isNotEmpty() && precedence(stack.peek()) >= precedence(token) && token != "^") {
                    output.add(stack.pop())
                }
                stack.push(token)
            }
        }

        while (stack.isNotEmpty()) {
            val top = stack.pop()
            if (top == "(" || top == ")") throw IllegalArgumentException("Mismatched parentheses")
            output.add(top)
        }

        return output
    }

    private fun evaluateRPN(tokens: List<String>): Double {
        val stack: Deque<Double> = ArrayDeque()
        for (token in tokens) {
            val number = token.toDoubleOrNull()
            if (number != null) {
                stack.push(number)
            } else {
                if (stack.size < 2) throw IllegalArgumentException("Invalid syntax")
                val b = stack.pop()
                val a = stack.pop()
                val res = when (token) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> {
                        if (b == 0.0) throw ArithmeticException("Division by zero")
                        a / b
                    }
                    "%" -> a % b
                    "^" -> Math.pow(a, b)
                    else -> throw IllegalArgumentException("Unknown operator: $token")
                }
                stack.push(res)
            }
        }
        if (stack.size != 1) throw IllegalArgumentException("Invalid expression evaluation")
        return stack.pop()
    }
}
