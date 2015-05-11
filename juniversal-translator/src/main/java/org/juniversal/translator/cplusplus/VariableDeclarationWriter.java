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

import org.eclipse.jdt.core.dom.*;
import org.xuniversal.translator.cplusplus.ReferenceKind;

import java.util.List;

import static org.juniversal.translator.core.ASTUtil.forEach;

public class VariableDeclarationWriter extends CPlusPlusASTNodeWriter {
    public VariableDeclarationWriter(CPlusPlusFileTranslator cPlusPlusASTWriters) {
        super(cPlusPlusASTWriters);
    }

    @Override
    public void write(ASTNode node) {
        // Variable declaration statements & expressions are quite similar, so we handle them both
        // here together

        if (node instanceof VariableDeclarationStatement) {
            VariableDeclarationStatement variableDeclarationStatement = (VariableDeclarationStatement) node;

            writeVariableDeclaration(variableDeclarationStatement.modifiers(), variableDeclarationStatement.getType(),
                    variableDeclarationStatement.fragments());
            copySpaceAndComments();

            matchAndWrite(";");
        } else {
            VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression) node;

            writeVariableDeclaration(variableDeclarationExpression.modifiers(),
                    variableDeclarationExpression.getType(), variableDeclarationExpression.fragments());
        }
    }

    private void writeVariableDeclaration(List<?> modifiers, Type type, List<?> fragments) {
        // TODO:  Turn "final" into "const"
/*
        if (ASTUtil.containsFinal(modifiers)) {
			write("const");
			skipModifiers(modifiers);

			copySpaceAndComments();
		}
*/

        // Write the type
        writeType(type, ReferenceKind.SharedPtr);

        // TODO: Write * or & where needed, if multiple variables (multiple fragments)

        // Write the variable declaration(s)
        forEach(fragments, (VariableDeclarationFragment variableDeclarationFragment, boolean first) -> {
            copySpaceAndComments();
            if (!first) {
                matchAndWrite(",");
                copySpaceAndComments();
            }
            writeNode(variableDeclarationFragment);
        });
    }
}
