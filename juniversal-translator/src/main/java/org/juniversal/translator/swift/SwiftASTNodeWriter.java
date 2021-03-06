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

package org.juniversal.translator.swift;

import org.eclipse.jdt.core.dom.*;
import org.juniversal.translator.core.ASTNodeWriter;
import org.juniversal.translator.core.JavaSourceContext;
import org.xuniversal.translator.core.TargetWriter;
import org.xuniversal.translator.swift.SwiftTargetWriter;

import java.util.List;


public abstract class SwiftASTNodeWriter<T extends ASTNode> extends ASTNodeWriter<T> {
    private SwiftTranslator translator;

    protected SwiftASTNodeWriter(SwiftTranslator translator) {
        this.translator = translator;
    }

    @Override protected SwiftTranslator getTranslator() {
        return translator;
    }

    @Override public SwiftTargetWriter getTargetWriter() {
        return getContext().getTargetWriter();
    }

    public SwiftContext getContext() { return getTranslator().getContext(); }

    public void writeStatementEnsuringBraces(Statement statement, int blockStartColumn, boolean forceSeparateLine) {
        if (statement instanceof Block) {
            copySpaceAndComments();
            writeNode(statement);
        } else {
            if (startsOnSameLine(statement)) {
                if (forceSeparateLine) {
                    write(" {\n");

                    writeSpacesUntilColumn(blockStartColumn);
                    writeSpaces(getPreferredIndent());
                    skipSpacesAndTabs();

                    copySpaceAndComments();

                    writeNode(statement);

                    copySpaceAndCommentsUntilEOL();
                    setKnowinglyProcessedTrailingSpaceAndComments(true);
                    writeln();

                    writeSpacesUntilColumn(blockStartColumn);
                    write("}");
                } else {
                    copySpaceAndCommentsEnsuringDelimiter();

                    write("{ ");
                    writeNode(statement);
                    write(" }");
                }
            } else {
                write(" {");

                copySpaceAndComments();

                writeNode(statement);

                copySpaceAndCommentsUntilEOL();
                setKnowinglyProcessedTrailingSpaceAndComments(true);
                writeln();

                writeSpacesUntilColumn(blockStartColumn);
                write("}");
            }
        }
    }

    public void writeConditionNoParens(Expression expression) {
        match("(");
        skipSpaceAndComments();

        writeNode(expression);

        skipSpaceAndComments();
        match(")");
    }

    /**
     * Write out a type, when it's used (as opposed to defined).
     *  @param type    type to write
     *
     */
    public void writeType(Type type, boolean useRawPointer) {
        boolean referenceType = !type.isPrimitiveType();

        if (!referenceType)
            writeNode(type);
        else {
            if (useRawPointer) {
                writeNode(type);
                write("*");
            } else {
                write("ptr< ");
                writeNode(type);
                write(" >");
            }
        }
    }

    /**
     * Write out the type parameters in the specified list, surrounded by "<" and ">".
     *  @param typeParameters list of TypeParameter objects
     * @param includeClassKeyword if true, each parameter is prefixed with "class "
     */
    public void writeTypeParameters(List<TypeParameter> typeParameters, boolean includeClassKeyword) {
        // If we're writing the implementation of a generic method, include the "template<...>" prefix

        boolean first = true;

        write("<");
        for (TypeParameter typeParameter : typeParameters) {
            if (! first)
                write(", ");

            if (includeClassKeyword)
                write("class ");
            write(typeParameter.getName().getIdentifier());

            first = false;
        }

        write(">");
    }
}
