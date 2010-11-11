/*
 * Copyright (c) 2009-2010, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * EJML is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * EJML is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EJML.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ejml.alg.dense.misc;

import org.ejml.alg.generic.CodeGeneratorMisc;

import java.io.FileNotFoundException;
import java.io.PrintStream;


/**
 * Generates code for an unrolled determinant by minor.
 *
 * NOTE:  There are some repeat calculations of inner determinants.   Maybe it could be speed up by calculating those?
 *
 * @author Peter Abeles
 */
public class GenerateDeterminantFromMinor {

    PrintStream stream;
    int N;

    public GenerateDeterminantFromMinor( String fileName ) throws FileNotFoundException {
        stream = new PrintStream(fileName);
    }

    public void createClass(int N) {
        printTop(N);

        printCalls(N);

        print2();
        print3();
        for( int i = 4; i <= N; i++ ) {
            printFunction(i);
        }

        stream.print("}\n");
    }

    private void printTop(int N) {
        String foo = CodeGeneratorMisc.COPYRIGHT +
                "\n" +
                "package org.ejml.alg.dense.misc;\n" +
                "\n" +
                "import org.ejml.data.RowD1Matrix64F;\n" +
                "\n" +
                "\n" +
                "/**\n" +
                " * This code was auto generated by  {@link GenerateDeterminantFromMinor} and should not be modified\n" +
                " * directly.  \n" +
                " * \n" +
                " * @author Peter Abeles\n" +
                " */\n" +
                "public class UnrolledDeterminantFromMinor {\n"+
                "    \n" +
                "    public static final int MAX = "+N+";\n";

        stream.print(foo);
    }

    private void print2() {
        stream.print("    public static double det2( RowD1Matrix64F mat )\n" +
                "    {\n" +
                "        return mat.get(0)*mat.get(3) - mat.get(1)*mat.get(2);\n" +
                "    }\n\n");
    }

    private void print3() {
        stream.print("    public static double det3( RowD1Matrix64F mat )\n" +
                "    {\n" +
                "        double a11 = mat.get( 0 );\n" +
                "        double a12 = mat.get( 1 );\n" +
                "        double a13 = mat.get( 2 );\n" +
                "        double a21 = mat.get( 3 );\n" +
                "        double a22 = mat.get( 4 );\n" +
                "        double a23 = mat.get( 5 );\n" +
                "        double a31 = mat.get( 6 );\n" +
                "        double a32 = mat.get( 7 );\n" +
                "        double a33 = mat.get( 8 );\n" +
                "\n" +
                "        double a = a11*(a22*a33 - a23*a32);\n" +
                "        double b = a12*(a21*a33 - a23*a31);\n" +
                "        double c = a13*(a21*a32 - a31*a22);\n" +
                "\n" +
                "        return a-b+c;\n" +
                "    }\n" +
                "\n");
    }

    private void printCalls( int N )
    {
        stream.print(
                "    \n" +
                        "    public static double det( RowD1Matrix64F mat ) {\n");
        stream.print(
                "        if( mat.numRows == 2 ) {\n" +
                "            return det2(mat);\n");
        for( int i = 3; i <= N; i++ ) {
            stream.print("        } else if( mat.numRows == "+i+" ) {\n" +
                    "            return det"+i+"(mat);            \n");
        }
        stream.print("        }\n" +
                "        \n" +
                "        throw new IllegalArgumentException(\"Not supported\");\n" +
                "    }\n\n");
    }

    private void printFunction( int N )
    {
        stream.print("    public static double det"+N+"( RowD1Matrix64F mat )\n" +
                "    {\n");

        // extracts the first minor
        int M = N-1;
        this.N = M;
        int matrix[] = new int[M*M];
        int index = 0;
        for( int i = 1; i <= M; i++ ) {
            int origIndex = i*N+1;
            for( int j = 1; j <= M; j++ , origIndex++,index++) {
                matrix[index] = index;
                stream.print("        double  "+a(index)+" = mat.get( "+origIndex+" );\n");
            }
        }

        stream.print("\n");
        stream.print("        double ret = 0;\n");
        stream.print("        ret += mat.get( 0 ) * (");
        minor(matrix,0,M);
        stream.print(");\n");
        
        for( int minor = 2; minor <= N; minor++ ) {
            for( int i = 1; i <= M; i++ ) {
                index = (minor-2)+(i-1)*M;
                int origIndex = minor-2+i*N;
                stream.print("        "+a(index)+" = mat.get( "+origIndex+" );\n");
            }

            if( minor % 2 == 0 ) {
               stream.print("        ret -= ");
            } else {
               stream.print("        ret += ");
            }
            stream.print("mat.get( "+(minor-1)+" ) * (");
            minor(matrix,0,M);
            stream.print(");\n");
        }
        stream.print("        return ret;\n");
        stream.print("    }\n");
        stream.print("\n");
    }


    private void minor( int m[] , int row , int N )
    {
        if( N == 2 ) {
            stream.print(a(m[0])+"*"+a(m[3])+" - "+a(m[1])+"*"+a(m[2]));
        } else {
            int M = N-1;
            int d[] = new int[ M*M ];

            for( int i = 0; i < N; i++ ) {
                int index = 0;

                for( int j = 1; j < N; j++ ) {
                    for( int k = 0; k < N; k++ ) {
                       if( k != i ) {
                           d[index++] = m[j*N+k];
                       }
                    }
                }

                int pow = i;

                if( pow % 2 == 0 )
                    stream.print(" + "+a(m[i])+"*(");
                else
                    stream.print(" - "+a(m[i])+"*(");

                minor(d,row+1,M);

                stream.print(")");
            }

        }
    }

    private String a( int index )
    {
        int i = index/N+1;
        int j = index%N+1;

        return "a"+i+""+j;
    }

    public static void main( String args[] ) throws FileNotFoundException {
        GenerateDeterminantFromMinor gen = new GenerateDeterminantFromMinor("UnrolledDeterminantFromMinor.java");

        gen.createClass(6);
    }
}
