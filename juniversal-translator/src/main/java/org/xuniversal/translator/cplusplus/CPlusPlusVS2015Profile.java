/*
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

package org.xuniversal.translator.cplusplus;

import org.xuniversal.translator.core.TypeName;
import java.io.Writer;

/**
 * The CPPProfile class describes how the C++ should be generated.  Primarily this class gives
 * attributes of the target C++ compiler, for example saying what types are used to represent
 * different sizes of unsigned integers.
 *
 * @author Mike Sinkovsky
 */
public class CPlusPlusVS2015Profile extends CPlusPlusTargetProfile {

    @Override public CPlusPlusTargetWriter createTargetWriter(Writer writer) {
        return new CPlusPlusVS2015Writer(writer, this);
    }

    @Override public String getInt8Type() {
        return "int8_t";
    }

    @Override public String getInt16Type() {
        return "int16_t";
    }

    @Override public String getInt32Type() {
        return "int32_t";
    }

    @Override public String getInt64Type() {
        return "int64_t";
    }

    @Override public String getCharType() {
        return "char16_t";
    }

    private static TypeName stringType = new TypeName("std", "u16string");
    @Override public TypeName getStringType() {
        return stringType;
    }
}
