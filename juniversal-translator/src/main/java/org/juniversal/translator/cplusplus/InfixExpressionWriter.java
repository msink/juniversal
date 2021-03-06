/*
 * Copyright (c) 2012-2015, Microsoft Mobile
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.juniversal.translator.cplusplus;

import java.util.HashMap;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;

import static org.juniversal.translator.core.ASTUtil.forEach;


public class InfixExpressionWriter extends CPlusPlusASTNodeWriter<InfixExpression> {
	private HashMap<InfixExpression.Operator, String> equivalentOperators;  // Operators that have the same token in both Java & C++


	public InfixExpressionWriter(CPlusPlusTranslator cPlusPlusASTWriters) {
		super(cPlusPlusASTWriters);

		equivalentOperators = new HashMap<>();
		equivalentOperators.put(InfixExpression.Operator.TIMES, "*");
		equivalentOperators.put(InfixExpression.Operator.DIVIDE, "/");
		equivalentOperators.put(InfixExpression.Operator.REMAINDER, "%");
		equivalentOperators.put(InfixExpression.Operator.PLUS, "+");
		equivalentOperators.put(InfixExpression.Operator.MINUS, "-");

		// TODO: Test signed / unsigned semantics here
		equivalentOperators.put(InfixExpression.Operator.LEFT_SHIFT, "<<");
		equivalentOperators.put(InfixExpression.Operator.RIGHT_SHIFT_SIGNED, ">>");
		//cppOperators.put(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED, "==");

		equivalentOperators.put(InfixExpression.Operator.LESS, "<");
		equivalentOperators.put(InfixExpression.Operator.GREATER, ">");
		equivalentOperators.put(InfixExpression.Operator.LESS_EQUALS, "<=");
		equivalentOperators.put(InfixExpression.Operator.GREATER_EQUALS, ">=");
		equivalentOperators.put(InfixExpression.Operator.EQUALS, "==");
		equivalentOperators.put(InfixExpression.Operator.NOT_EQUALS, "!=");

		equivalentOperators.put(InfixExpression.Operator.XOR, "^");
		equivalentOperators.put(InfixExpression.Operator.AND, "&");
		equivalentOperators.put(InfixExpression.Operator.OR, "|");

		equivalentOperators.put(InfixExpression.Operator.CONDITIONAL_AND, "&&");
		equivalentOperators.put(InfixExpression.Operator.CONDITIONAL_OR, "||");
	}
	
	@Override
	public void write(InfixExpression infixExpression) {
		InfixExpression.Operator operator = infixExpression.getOperator();

		if (operator == InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED) {
			write("xu::rightShiftUnsigned(");
            writeNode(infixExpression.getLeftOperand());

			// Skip spaces before the >>> but if there's a newline (or comments) there, copy them
			skipSpacesAndTabs();
			copySpaceAndComments();
			matchAndWrite(">>>", ",");

			copySpaceAndComments();
            writeNode(infixExpression.getRightOperand());
			write(")");
		}
		else {
            writeNode(infixExpression.getLeftOperand());

			copySpaceAndComments();
			String operatorToken = this.equivalentOperators.get(infixExpression.getOperator());
			matchAndWrite(operatorToken);

			copySpaceAndComments();
            writeNode(infixExpression.getRightOperand());

			if (infixExpression.hasExtendedOperands()) {
				forEach(infixExpression.extendedOperands(), (Expression extendedOperand) -> {
					copySpaceAndComments();
					matchAndWrite(operatorToken);

					copySpaceAndComments();
                    writeNode(extendedOperand);
				});
			}
		}
	}
}
