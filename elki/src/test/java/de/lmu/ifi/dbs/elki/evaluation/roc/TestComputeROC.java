package de.lmu.ifi.dbs.elki.evaluation.roc;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.math.geometry.XYCurve;

/**
 * Test to validate ROC curve computation.
 * 
 * @author Erich Schubert
 */
public class TestComputeROC implements JUnit4Test {
  /**
   * Test ROC curve generation, including curve simplification
   */
  @Test
  public void testROCCurve() {
    HashSetModifiableDBIDs positive = DBIDUtil.newHashSet();
    positive.add(DBIDUtil.importInteger(1));
    positive.add(DBIDUtil.importInteger(2));
    positive.add(DBIDUtil.importInteger(3));
    positive.add(DBIDUtil.importInteger(4));
    positive.add(DBIDUtil.importInteger(5));

    final ModifiableDoubleDBIDList distances = DBIDUtil.newDistanceDBIDList();
    // Starting point: ................................ 0.0,0. ++
    distances.add(0.0, DBIDUtil.importInteger(1)); // + 0.0,.2 -- redundant
    distances.add(1.0, DBIDUtil.importInteger(2)); // + 0.0,.4 ++
    distances.add(2.0, DBIDUtil.importInteger(6)); // - .25,.4 ++
    distances.add(3.0, DBIDUtil.importInteger(7)); // -
    distances.add(3.0, DBIDUtil.importInteger(3)); // + .50,.6 -- redundant
    distances.add(4.0, DBIDUtil.importInteger(8)); // -
    distances.add(4.0, DBIDUtil.importInteger(4)); // + .75,.8 ++
    distances.add(5.0, DBIDUtil.importInteger(9)); // - 1.0,.8 ++
    distances.add(6.0, DBIDUtil.importInteger(5)); // + 1.0,1. ++

    XYCurve roccurve = ROC.materializeROC(new ROC.DBIDsTest(positive), new ROC.DistanceResultAdapter(distances.iter()));
    // System.err.println(roccurve);
    assertEquals("ROC curve too complex", 6, roccurve.size());

    double auc = XYCurve.areaUnderCurve(roccurve);
    assertEquals("ROC AUC (curve) not correct.", 0.6, auc, 1e-14);
    double auc2 = ROC.computeROCAUC(positive, distances);
    assertEquals("ROC AUC (direct) not correct.", 0.6, auc2, 1e-14);
  }

  /**
   * Test Average Precision score computation.
   */
  @Test
  public void testAveragePrecision() {
    HashSetModifiableDBIDs positive = DBIDUtil.newHashSet();
    positive.add(DBIDUtil.importInteger(1));
    positive.add(DBIDUtil.importInteger(2));
    positive.add(DBIDUtil.importInteger(3));
    positive.add(DBIDUtil.importInteger(4));
    positive.add(DBIDUtil.importInteger(5));

    final ModifiableDoubleDBIDList distances = DBIDUtil.newDistanceDBIDList();
    distances.add(0.0, DBIDUtil.importInteger(1)); // Precision: 1.0
    distances.add(1.0, DBIDUtil.importInteger(2)); // Precision: 1.0
    distances.add(2.0, DBIDUtil.importInteger(6)); //
    distances.add(3.0, DBIDUtil.importInteger(7)); //
    distances.add(3.0, DBIDUtil.importInteger(3)); // Precision: 0.6
    distances.add(4.0, DBIDUtil.importInteger(8)); //
    distances.add(4.0, DBIDUtil.importInteger(4)); // Precision: 4/7.
    distances.add(5.0, DBIDUtil.importInteger(9)); //
    distances.add(6.0, DBIDUtil.importInteger(5)); // Precision: 5/9.
    // (1+1+.6+4/7.+5/9.)/5 = 0.7453968253968254

    double ap = ROC.computeAveragePrecision(positive, distances);
    assertEquals("Average precision not correct.", 0.7453968253968254, ap, 1e-14);
  }

  /**
   * Test Precision@k score computation.
   */
  @Test
  public void testPrecisionAtK() {
    HashSetModifiableDBIDs positive = DBIDUtil.newHashSet();
    positive.add(DBIDUtil.importInteger(1));
    positive.add(DBIDUtil.importInteger(2));
    positive.add(DBIDUtil.importInteger(3));
    positive.add(DBIDUtil.importInteger(4));
    positive.add(DBIDUtil.importInteger(5));

    final ModifiableDoubleDBIDList distances = DBIDUtil.newDistanceDBIDList();
    distances.add(0.0, DBIDUtil.importInteger(1)); // Precision: 1.0
    distances.add(1.0, DBIDUtil.importInteger(2)); // Precision: 1.0
    distances.add(2.0, DBIDUtil.importInteger(6)); //
    distances.add(3.0, DBIDUtil.importInteger(7)); //
    distances.add(3.0, DBIDUtil.importInteger(3)); // Precision: 0.6
    distances.add(4.0, DBIDUtil.importInteger(8)); //
    distances.add(4.0, DBIDUtil.importInteger(4)); // Precision: 4/7.
    distances.add(5.0, DBIDUtil.importInteger(9)); //
    distances.add(6.0, DBIDUtil.importInteger(5)); // Precision: 5/9.
    // (1+1+.6+4/7.+5/9.)/5 = 0.7453968253968254

    double[] precision = new double[] { 1., 1., 2. / 3., 2.5 / 4., 3 / 5., 3.5 / 6., 4. / 7., 4. / 8., 5 / 9. };

    for(int k = 0; k < precision.length; ++k) {
      double pk = ROC.computePrecisionAtK(positive, distances, k + 1);
      assertEquals("Precision at k=" + (k + 1) + " not correct.", precision[k], pk, 1e-14);
    }
  }

  /**
   * Test maximum F1 score computation
   */
  @Test
  public void testMaximumF1() {
    HashSetModifiableDBIDs positive = DBIDUtil.newHashSet();
    positive.add(DBIDUtil.importInteger(1));
    positive.add(DBIDUtil.importInteger(2));
    positive.add(DBIDUtil.importInteger(3));
    positive.add(DBIDUtil.importInteger(4));
    positive.add(DBIDUtil.importInteger(5));

    final ModifiableDoubleDBIDList distances = DBIDUtil.newDistanceDBIDList();
    distances.add(0.0, DBIDUtil.importInteger(1)); // P: 1.0 R: 0.2
    distances.add(1.0, DBIDUtil.importInteger(2)); // P: 1.0 R: 0.4
    distances.add(2.0, DBIDUtil.importInteger(6)); // P: 2/3 R: 0.4
    distances.add(3.0, DBIDUtil.importInteger(7)); // P: 0.5 R: 0.4
    distances.add(3.0, DBIDUtil.importInteger(3)); // P: 0.6 R: 0.6
    distances.add(4.0, DBIDUtil.importInteger(8)); // P: 0.5 R: 0.6
    distances.add(4.0, DBIDUtil.importInteger(4)); // P: 4/7 R: 0.8
    distances.add(5.0, DBIDUtil.importInteger(9)); // P: 0.5 R: 0.8
    distances.add(6.0, DBIDUtil.importInteger(5)); // P: 5/9 R: 1.0

    double maxf1 = ROC.computeMaximumF1(positive, distances);
    assertEquals("Maximum F1 not correct.", 0.7142857142857143, maxf1, 1e-14);
  }
}