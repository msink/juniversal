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

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.xuniversal.translator.core.HierarchicalName;
import org.xuniversal.translator.core.TargetWriter;
import org.xuniversal.translator.core.TypeName;
import org.xuniversal.translator.cplusplus.CPlusPlusTargetWriter;

import static org.juniversal.translator.core.ASTUtil.getFirstTypeDeclaration;


public class CompilationUnitWriter extends CPlusPlusASTNodeWriter<CompilationUnit> {
    public CompilationUnitWriter(CPlusPlusTranslator translator) {
        super(translator);
    }

    public void write(CompilationUnit compilationUnit) {
        AbstractTypeDeclaration typeDeclaration = getFirstTypeDeclaration(compilationUnit);

        // First visit & write out the guts of the type, using a BufferTargetWriter to capture all output.   We do
        // this first to capture the usage of anything that needs an import.   The imports themselves are written
        // after, via writeEntireFile
        setPositionToStartOfNodeSpaceAndComments(typeDeclaration);
        String typeBuffer;
        try (TargetWriter.BufferedWriter bufferedWriter = getTargetWriter().startBuffering()) {
            copySpaceAndComments();
            writeNode(typeDeclaration);
            copySpaceAndComments();

            typeBuffer = bufferedWriter.getBufferContents();
        }

        setPositionToStartOfNode(compilationUnit);

        CPlusPlusTargetWriter targetWriter = getTargetWriter();

        TypeName typeName = getContext().getOutermostTypeName();
        HierarchicalName packageName = typeName.getPackage();

        if (getContext().getOutputType() == OutputType.HEADER_FILE) {
            targetWriter.writeHeaderFileStart(typeName);
            targetWriter.writeIncludesForHeaderFile(getContext().getReferencedOutermostTypes());
            writeln();
        } else {
            targetWriter.writeIncludesForSourceFile(typeName, getContext().getReferencedOutermostTypes());
            writeln();
        }

        if (!packageName.isEmpty()) {
            targetWriter.writeNamespaceStart(packageName);
            writeln();
        }

/*
        for (ITypeBinding typeBinding : getContext().getReferencedTypes().getTypesJustNeedingDeclaration()) {
            writeTypeForwardDeclaration(typeBinding);
        }
*/

        //targetWriter.writeUsings(getContext().getNamesNeedingImport());
        writeln();

        // Write the guts, defining the class/enum
        write(typeBuffer);

        setPositionToEndOfNode(typeDeclaration);
        skipSpaceAndComments();

        if (!packageName.isEmpty()) {
            writeln();
            targetWriter.writeNamespaceEnd(packageName);
        }

        if (getContext().getOutputType() == OutputType.HEADER_FILE)
            targetWriter.writeHeaderFileEnd(typeName);
    }
}
