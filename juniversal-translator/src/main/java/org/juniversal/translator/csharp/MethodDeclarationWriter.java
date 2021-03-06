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

package org.juniversal.translator.csharp;

import org.eclipse.jdt.core.dom.*;
import org.jetbrains.annotations.Nullable;
import org.xuniversal.translator.core.Flag;
import org.xuniversal.translator.core.Var;

import java.util.ArrayList;
import java.util.List;

import static org.juniversal.translator.core.ASTUtil.*;

// TODO: Finish this

public class MethodDeclarationWriter extends CSharpASTNodeWriter<MethodDeclaration> {
    public MethodDeclarationWriter(CSharpTranslator cSharpASTWriters) {
        super(cSharpASTWriters);
    }

    @Override
    public void write(MethodDeclaration methodDeclaration) {
        AbstractTypeDeclaration typeDeclaration = getContext().getTypeDeclaration();
        boolean isInterface = isInterface(typeDeclaration);
        boolean classIsFinal = isFinal(typeDeclaration);
        boolean methodIsAbstract = isAbstract(methodDeclaration);
        boolean methodIsFinal = isFinal(methodDeclaration);
        boolean methodIsOverride = isMethodEffectivelyOverride(methodDeclaration);
        boolean methodIsStatic = isStatic(methodDeclaration);
        boolean methodIsConstructor = isConstructor(methodDeclaration);

/*
        boolean isGeneric = !typeParameters.isEmpty();
        if (isGeneric && context.isWritingMethodImplementation()) {
            write("template ");
            writeTypeParameters(typeParameters, true, context);
            writeln();
        }
*/

        // TODO: Handle arrays with extra dimensions

        // Get return type if present
        @Nullable Type returnType = null;
        if (!methodDeclaration.isConstructor())
            returnType = methodDeclaration.getReturnType2();

        List<?> modifiers = methodDeclaration.modifiers();

        writeMappedAnnotations(modifiers);

        // Handle finalize here separately; it maps to a C# destructor, with no access modifiers or return type
        if (isThisMethod(methodDeclaration, "finalize")) {
            write("~" + typeDeclaration.getName().getIdentifier());
            setPositionToEndOfNode(methodDeclaration.getName());

            writeParameterList(methodDeclaration);

            writeThrownExceptions(methodDeclaration);
            writeBody(methodDeclaration.getBody());
            return;
        }

        // Write the access modifier.  For C# interfaces, methods are always public and the access modifier isn't allowed
        if (!isInterface)
            writeAccessModifier(modifiers);

        // Write the virtual/abstract/override/static/sealed modifiers, which, for lack of a better term, we'll call the
        // "overridability" modifiers
        if (methodIsStatic)
            writeStaticModifier();
        else if (isInterface || methodIsConstructor) {
            // C# interface methods can't take any overridability modifiers--they are always implicitly abstract;
            // constructors can't take modifiers
        } else if (methodIsAbstract) {
            // Java methods (as well as C# methods) can be both overrides & abstract; perhaps the method is redeclared
            // here (thus the override) just to update its Javadoc.   Or perhaps a method with a default implementation
            // (non-abstract) higher up in the inheritance tree is forced abstract here, so that subclasses need to
            // supply their own implementations
            if (methodIsOverride)
                writeOverrideModifier();
            writeAbstractModifier();
        } else if (methodIsOverride) {
            writeOverrideModifier();
            if (methodIsFinal)
                writeSealedModifier();
        } else if (!classIsFinal && !methodIsFinal && !isPrivate(methodDeclaration)) {
            // In Java methods are virtual by default whereas in C# they aren't, so add the virtual keyword when
            // appropriate. If the type is final nothing can be overridden.   If the method is final or private it
            // can't be overridden, so again no need for virtual.   Otherwise, mark as virtual
            writeModifier("virtual");
        }

        // Skip any modifiers & type parameters in the source
        setPositionToStartOfNode(returnType != null ? returnType : methodDeclaration.getName());

        if (returnType != null)
            writeNode(returnType);

        copySpaceAndComments();

        // Map overridden Object methods to their appropriate name in C#
        String mappedMethodName;
        if (isThisMethod(methodDeclaration, "equals", "java.lang.Object"))
            mappedMethodName = "Equals";
        else if (isThisMethod(methodDeclaration, "hashCode"))
            mappedMethodName = "GetHashCode";
        else if (isThisMethod(methodDeclaration, "toString"))
            mappedMethodName = "ToString";
        else mappedMethodName = methodDeclaration.getName().getIdentifier();

        validateIdentifier(methodDeclaration.getName().getIdentifier());
        matchAndWrite(methodDeclaration.getName().getIdentifier(), mappedMethodName);

        ArrayList<WildcardType> wildcardTypes = new ArrayList<>();
        forEach(methodDeclaration.parameters(), (SingleVariableDeclaration parameter) -> {
            addWildcardTypes(parameter.getType(), wildcardTypes);
        });

        boolean methodIsGeneric = ! methodDeclaration.typeParameters().isEmpty() || ! wildcardTypes.isEmpty();

        if (methodIsConstructor && !wildcardTypes.isEmpty())
            throw sourceNotSupported("C# constructors can't take arguments that use generic wildcard types; consider replacing this constructor with a static create method instead, taking the same generic arguments");

        if (methodIsGeneric)
            writeTypeParameters(methodDeclaration.typeParameters(), wildcardTypes);

        copySpaceAndComments();

        getContext().setMethodWildcardTypes(wildcardTypes);
        writeParameterList(methodDeclaration);
        getContext().setMethodWildcardTypes(null);

        // Write the generic type constraints, unless the method is an override in which case C# (for some reason)
        // requires the type constraints to only be specified on the top level method, disallowing them being repeated
        // on overrides
        if (!methodIsOverride)
            writeTypeConstraints(methodDeclaration, wildcardTypes);

        // TODO: Ignore thrown exceptions
        writeThrownExceptions(methodDeclaration);

        if (methodDeclaration.isConstructor())
            writeOtherConstructorInvocation(methodDeclaration);

        Block body = methodDeclaration.getBody();
        if (body != null) {
            writeBody(body);
        } else {
            copySpaceAndComments();
            matchAndWrite(";");
        }
    }

    /**
     * Go up the superclass tree to see if this method overrides a method higher up the tree.   We ignore interfaces
     * here because implementations of interface methods in C# don't get the override keyword; only overrides of
     * superclass methods (be they defined or abstract) do.   We also ignore the @Override keyword in Java, as that's
     * just optional.
     *
     * @param methodDeclaration method in question
     * @return true if this method is an override (and thus should use the override keyword in the generated C#)
     */
    private boolean isMethodEffectivelyOverride(MethodDeclaration methodDeclaration) {
        IMethodBinding methodBinding = methodDeclaration.resolveBinding();
        if (methodBinding == null)
            return false;

        ITypeBinding typeBinding = methodBinding.getDeclaringClass();

        // See if any of the superclasses specify a method that we're overriding
        return anySuperclassMatch(typeBinding, superclass ->
                anyMatch(superclass.getDeclaredMethods(), methodBinding::overrides));
    }

    private void writeTypeParameters(List typeParameters, ArrayList<WildcardType> wildcardTypes) {
        write("<");

        Flag outputTypeParameter = new Flag();
        forEach(typeParameters, (TypeParameter typeParameter) -> {
            if (outputTypeParameter.isSet())
                write(", ");

            write(typeParameter.getName().getIdentifier());
            outputTypeParameter.set();
        });

        for (WildcardType wildcardType : wildcardTypes) {
            if (outputTypeParameter.isSet())
                write(", ");

            writeWildcardTypeSyntheticName(wildcardTypes, wildcardType);
            outputTypeParameter.set();
        }

        write(">");
    }

    private void writeTypeConstraints(MethodDeclaration methodDeclaration, ArrayList<WildcardType> wildcardTypes) {
        writeTypeParameterConstraints(methodDeclaration.typeParameters());

        for (WildcardType wildcardType : wildcardTypes) {
            @Nullable Type bound = wildcardType.getBound();
            if (bound != null) {
                write(" where ");
                writeWildcardTypeSyntheticName(wildcardTypes, wildcardType);
                write(" : ");

                if (!wildcardType.isUpperBound())
                    throw sourceNotSupported("Wildcard lower bounds ('? super') aren't supported; only upper bounds ('? extends') are supported");

                writeNodeFromOtherPosition(bound);
            }
        }
    }

    private void writeParameterList(MethodDeclaration methodDeclaration) {
        matchAndWrite("(");

        forEach(methodDeclaration.parameters(), (SingleVariableDeclaration singleVariableDeclaration, boolean first) -> {
            if (!first) {
                copySpaceAndComments();
                matchAndWrite(",");
            }

            copySpaceAndComments();
            writeNode(singleVariableDeclaration);
        });

        copySpaceAndComments();
        matchAndWrite(")");
    }

    private void writeThrownExceptions(MethodDeclaration methodDeclaration) {
        // If there are any checked exceptions, output them just as a comment. We don't turn them
        // into C++ checked exceptions because we don't declare runtime exceptions in the C++; since
        // we don't declare all exceptions for C++ we can't declare any since we never want
        // unexpected() to be called
        List<?> thrownExceptions = methodDeclaration.thrownExceptionTypes();
        if (thrownExceptions.size() > 0) {
            copySpaceAndComments();
            write("/* ");
            matchAndWrite("throws");

            forEach(thrownExceptions, (Type exceptionType, boolean first) -> {
                skipSpaceAndComments();
                if (first)
                    write(" ");
                else {
                    matchAndWrite(",");

                    skipSpaceAndComments();
                    write(" ");
                }

                writeNode(exceptionType);
            });

            write(" */");
        }
    }

    private void writeOtherConstructorInvocation(MethodDeclaration methodDeclaration) {
        Block body = methodDeclaration.getBody();

        List statements = body.statements();
        if (!statements.isEmpty()) {
            Statement firstStatement = (Statement) statements.get(0);

            if (firstStatement instanceof SuperConstructorInvocation || firstStatement instanceof ConstructorInvocation) {
                write(" : ");

                int savedPosition = getPosition();
                setPositionToStartOfNode(firstStatement);

                // TODO: Handle type arguments
                if (firstStatement instanceof SuperConstructorInvocation) {
                    SuperConstructorInvocation superConstructorInvocation = (SuperConstructorInvocation) firstStatement;

                    if (!superConstructorInvocation.typeArguments().isEmpty())
                        throw sourceNotSupported("Type arguments not currently supported on a super constructor invocation");

                    matchAndWrite("super", "base");

                    copySpaceAndComments();
                    writeMethodInvocationArgumentList(superConstructorInvocation.arguments());
                } else {
                    ConstructorInvocation constructorInvocation = (ConstructorInvocation) firstStatement;

                    if (!constructorInvocation.typeArguments().isEmpty())
                        throw sourceNotSupported("Type arguments not currently supported on a delegating constructor invocation");

                    matchAndWrite("this");

                    copySpaceAndComments();
                    writeMethodInvocationArgumentList(constructorInvocation.arguments());
                }

                setPosition(savedPosition);
            }
        }
    }

    private void writeBody(Block body) {
        copySpaceAndComments();
        matchAndWrite("{");

        forEach(body.statements(), (Statement statement, boolean first) -> {
            // If the first statement is a super constructor invocation, we skip it since
            // it's included as part of the method declaration in C++. If a super
            // constructor invocation is a statement other than the first, which it should
            // never be, we let that error out since writeNode won't find a match for it.
            if (first && (statement instanceof SuperConstructorInvocation || statement instanceof ConstructorInvocation))
                setPositionToEndOfNodeSpaceAndComments(statement);
            else {
                copySpaceAndComments();
                writeNode(statement);
            }
        });

        copySpaceAndComments();
        matchAndWrite("}");
    }
}
