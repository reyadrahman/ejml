/*
 * Copyright (c) 2009-2016, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ejml.alg.dense.decompose.hessenberg;

import org.ejml.alg.dense.mult.CVectorVectorMult;
import org.ejml.data.CDenseMatrix64F;
import org.ejml.data.Complex64F;
import org.ejml.interfaces.decomposition.TridiagonalSimilarDecomposition;
import org.ejml.ops.CCommonOps;
import org.ejml.ops.CMatrixFeatures;
import org.ejml.ops.CRandomMatrices;
import org.ejml.ops.CSpecializedOps;
import org.junit.Test;

import static org.ejml.alg.dense.decompose.CheckDecompositionInterface_CD64.safeDecomposition;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestTridiagonalDecompositionHouseholder_CD64 extends StandardTridiagonalTests_CD64 {

    @Override
    protected TridiagonalSimilarDecomposition<CDenseMatrix64F> createDecomposition() {
        return new TridiagonalDecompositionHouseholder_CD64();
    }

        /**
     * This checks to see the gammas and if the householder vectors stored in QH are correct. This
     * is done by extracting the vectors, computing reflectors, and multiplying them by A and seeing
     * if it has the expected response.
     */
    @Test
    public void testHouseholderVectors()
    {
        int N = 5;
        CDenseMatrix64F A = CRandomMatrices.createHermitian(N,-1,1,rand);
        CDenseMatrix64F B = new CDenseMatrix64F(N,N);

        System.out.println("A");
        A.print();
        System.out.println("-------------------");

        TridiagonalDecompositionHouseholder_CD64 decomp = new TridiagonalDecompositionHouseholder_CD64();

        assertTrue(safeDecomposition(decomp,A));

        CDenseMatrix64F QT = decomp.getQT();

        double gammas[] = decomp.getGammas();

        CDenseMatrix64F u = new CDenseMatrix64F(N,1);

        for( int i = 0; i < N-1; i++ ) {
            System.out.println("------- Column "+i);
            u.zero();
            u.data[(i+1)*2  ] = 1;
            u.data[(i+1)*2+1] = 0;

            for( int j = i+2; j < N; j++ ) {
                u.data[j*2]   = QT.getReal(i,j);
                u.data[j*2+1] = QT.getImag(i,j); // the reflector stored in the row will be the conjugate
            }

            System.out.println("Symmetric Update");
            CDenseMatrix64F BB = symmetricUpdate(A, gammas[i], u);
            BB.print();
            System.out.println("BB is hermitian "+CMatrixFeatures.isHermitian(BB,1e-5));

//            QrHelperFunctions_CD64.rank1UpdateMultR(BB,u.data,0,gammas[i],0,0,A.numCols,
//                    new double[A.numCols*2]);

            System.out.println("Direct application");
            updateLthenR(A,gammas[i],u).print();

            System.out.println("BEFORE A is hermitian "+CMatrixFeatures.isHermitian(A,1e-5));
            CDenseMatrix64F Q = CSpecializedOps.createReflector(u,gammas[i]);
            CCommonOps.mult(Q,A,B);
            CCommonOps.mult(B,Q,A);

//            QrHelperFunctions_CD64.rank1UpdateMultR(A,u.data,0,gammas[i],0,0,A.numCols,
//                    new double[A.numCols*2]);
//            QrHelperFunctions_CD64.rank1UpdateMultL(A,u.data,0,gammas[i],0,0,A.numCols);
            System.out.println("AFTER  A is hermitian "+CMatrixFeatures.isHermitian(A,1e-5));

            System.out.println("From reflector");
            A.print();
            A.set(BB);
            System.out.println("rank R");
//            BB.print();
//            B.print();

//            for( int j = i; j  < i+2; j++ ) {
//                assertTrue(Math.abs(A.getReal(j,i))> UtilEjml.TOLERANCE);
//                assertTrue(Math.abs(A.getImag(j,i))> UtilEjml.TOLERANCE);
//            }
//            for( int j = i+2; j < N; j++ ) {
//                assertEquals(0,A.getReal(j,i),UtilEjml.TOLERANCE);
//                assertEquals(0,A.getImag(j,i),UtilEjml.TOLERANCE);
//            }
//
//            for( int j = i+2; j < N; j++ ) {
//                assertEquals(0,A.getReal(i,j),UtilEjml.TOLERANCE);
//                assertEquals(0,A.getImag(i,j),UtilEjml.TOLERANCE);
//            }
        }
    }

    // (I-gamma*u*u')*A
    private CDenseMatrix64F updateL(CDenseMatrix64F A, double gamma, CDenseMatrix64F u) {
        CDenseMatrix64F v = new CDenseMatrix64F(1,u.numRows);

        CCommonOps.multTransA(u,A,v);
        CDenseMatrix64F BB = A.copy();
        CCommonOps.multAdd(-gamma,0,u,v,BB);

        return BB;
    }

    // ((I-gamma*u*u')*A)*(I-gamma*u*u')
    private CDenseMatrix64F updateLthenR(CDenseMatrix64F A, double gamma, CDenseMatrix64F u) {
        CDenseMatrix64F L = updateL(A,gamma,u);

        CDenseMatrix64F v = new CDenseMatrix64F(u.numRows,1);
        CCommonOps.mult(-gamma,0,L,u,v);
        CCommonOps.multAddTransB(v,u,L);

        return L;
    }

    private CDenseMatrix64F symmetricUpdate(CDenseMatrix64F A, double gamma, CDenseMatrix64F u) {
        // v = -gamma*A*u
        CDenseMatrix64F v = u.createLike();
        CCommonOps.mult(-gamma,0, A,u,v);

        // alpha = -0.5*gamma*u'*v
        Complex64F alpha = new Complex64F();
        CVectorVectorMult.innerProdH(v,u,alpha);
        alpha = alpha.times(new Complex64F(-0.5*gamma,0));

        // w = v + alpha*u
        CDenseMatrix64F w = v.createLike();
        CDenseMatrix64F alpha_u = u.copy();
        CCommonOps.scale(alpha.real,alpha.imaginary,alpha_u);
        CCommonOps.add(v,alpha_u,w);

        // BB = A + w*u' + u*w'
        CDenseMatrix64F M = A.createLike();
        CCommonOps.multTransB(w,u,M);
        CCommonOps.multAddTransB(u,w,M);
        CDenseMatrix64F BB = A.copy();
        CCommonOps.add(BB,M,BB);
        return BB;
    }
}