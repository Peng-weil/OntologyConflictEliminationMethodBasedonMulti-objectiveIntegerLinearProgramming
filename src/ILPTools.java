import org.semanticweb.owlapi.model.OWLAxiom;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * @author: Peng-weil
 * @date: 2020/11/30
 */
public class ILPTools {
    public static HashMap<String, OWLAxiom> getBinary(ArrayList<OWLAxiom> axioms) {
        HashMap<String, OWLAxiom> axiomBinary = new HashMap<String, OWLAxiom>();
        for (int axiomIndex = 0; axiomIndex < axioms.size(); axiomIndex++) {
            OWLAxiom axiom = axioms.get(axiomIndex);
            String binary = "x" + (axiomIndex + 1);
            axiomBinary.put(binary, axiom);
        }
        return axiomBinary;
    }


    static double calculateShapleyValue(OWLAxiom axiom, ArrayList<HashSet<OWLAxiom>> conflicts) {
        double shapleyVlaue = 1.0D;
        long molecular = 0;
        long denominator = 1;
        for (HashSet<OWLAxiom> oneMups : conflicts) {
            if (oneMups.contains(axiom)) {
                long _molecular = 1;
                long _denominator = oneMups.size();

                molecular = molecular * _denominator + denominator * _molecular;
                denominator = denominator * _denominator;

                long r, x = denominator, y = molecular;
                while (y != 0) {
                    r = x % y;
                    x = y;
                    y = r;
                }
                molecular /= x;
                denominator /= x;
            }
        }
        double d = (double) molecular / denominator;
        d = (double) Math.round(d * 100) / 100;
        return d;
    }

    public static void createILPModel(String path,
                                      ArrayList<OWLAxiom> axioms,
                                      ArrayList<HashSet<OWLAxiom>> conflicts) throws Exception {
        FileWriter modelFile = new FileWriter(path);
        BufferedWriter writer = new BufferedWriter(modelFile);
        PrintWriter outputs = new PrintWriter(writer);

        outputs.println("NAME examples");
        outputs.println();
        outputs.println("ROWS");
        outputs.println("      N obj");

        for (int conflictIndex = 1; conflictIndex <= conflicts.size(); conflictIndex++) {
            outputs.println("      G c" + conflictIndex);
        }

        outputs.println();
        outputs.println("COLUMNS");

        for (int axiomIndex = 0; axiomIndex < axioms.size(); axiomIndex++) {
            OWLAxiom axiom = axioms.get(axiomIndex);
            String binary = "x" + (axiomIndex + 1);
            outputs.println("      " + binary + "  obj   " + 1);
            for (int conflictIndex = 0; conflictIndex < conflicts.size(); conflictIndex++) {
                HashSet<OWLAxiom> oneMups = conflicts.get(conflictIndex);
                if (oneMups.contains(axiom)) {
                    outputs.println("      " + binary + " c" + (conflictIndex + 1) + " 1");
                }
            }
        }
        outputs.println();
        outputs.println("RHS");

        for (int conflictIndex = 1; conflictIndex <= conflicts.size(); conflictIndex++) {
            outputs.println("      rhs c" + conflictIndex + "  1");
        }
        outputs.println();
        outputs.println("BOUNDS");

        for (int axiomIndex = 1; axiomIndex <= axioms.size(); axiomIndex++) {
            String binary = "x" + axiomIndex;
            outputs.println("      LI bnd " + binary + " 0");
        }
        for (int axiomIndex = 1; axiomIndex <= axioms.size(); axiomIndex++) {
            String binary = "x" + axiomIndex;
            outputs.println("      UI bnd " + binary + " 1");
        }
        outputs.println();
        outputs.print("ENDATA");

        writer.close();
    }

    public static void createShapleyILPModel(String path,
                                             ArrayList<OWLAxiom> axioms,
                                             ArrayList<HashSet<OWLAxiom>> conflicts) throws Exception {
        FileWriter modelFile = new FileWriter(path);
        BufferedWriter writer = new BufferedWriter(modelFile);
        PrintWriter outputs = new PrintWriter(writer);

        outputs.println("NAME examples");
        outputs.println();
        outputs.println("ROWS");
        outputs.println("      N obj");

        for (int conflictIndex = 1; conflictIndex <= conflicts.size(); conflictIndex++) {
            outputs.println("      G c" + conflictIndex);
        }

        outputs.println();
        outputs.println("COLUMNS");
        for (int axiomIndex = 0; axiomIndex < axioms.size(); axiomIndex++) {
            OWLAxiom axiom = axioms.get(axiomIndex);
            String binary = "x" + (axiomIndex + 1);
            double reciShapleyValue = calculateShapleyValue(axiom, conflicts);

            outputs.println("      " + binary + "  obj   " + reciShapleyValue);
            for (int conflictIndex = 0; conflictIndex < conflicts.size(); conflictIndex++) {
                HashSet<OWLAxiom> oneMups = conflicts.get(conflictIndex);
                if (oneMups.contains(axiom)) {
                    outputs.println("      " + binary + " c" + (conflictIndex + 1) + " 1");
                }
            }
        }
        outputs.println();
        outputs.println("RHS");

        for (int conflictIndex = 1; conflictIndex <= conflicts.size(); conflictIndex++) {
            outputs.println("      rhs c" + conflictIndex + "  1");
        }
        outputs.println();
        outputs.println("BOUNDS");

        for (int axiomIndex = 1; axiomIndex <= axioms.size(); axiomIndex++) {
            String binary = "x" + axiomIndex;
            outputs.println("      LI bnd " + binary + " 0");
        }
        for (int axiomIndex = 1; axiomIndex <= axioms.size(); axiomIndex++) {
            String binary = "x" + axiomIndex;
            outputs.println("      UI bnd " + binary + " 1");
        }
        outputs.println();
        outputs.print("ENDATA");

        writer.close();
    }

    public static void createMultiILPModel(String path,
                                           ArrayList<OWLAxiom> axioms,
                                           ArrayList<HashSet<OWLAxiom>> conflicts,
                                           double limit) throws Exception {
        FileWriter modelFile = new FileWriter(path);
        BufferedWriter writer = new BufferedWriter(modelFile);
        PrintWriter outputs = new PrintWriter(writer);

        outputs.println("NAME examples");
        outputs.println();
        outputs.println("ROWS");
        outputs.println("      N obj");

        for (int conflictIndex = 1; conflictIndex <= conflicts.size(); conflictIndex++) {
            outputs.println("      G c" + conflictIndex);
        }

        int extrline = (conflicts.size() + 1);
        outputs.println("      L c" + extrline);

        outputs.println();
        outputs.println("COLUMNS");
        for (int axiomIndex = 0; axiomIndex < axioms.size(); axiomIndex++) {
            OWLAxiom axiom = axioms.get(axiomIndex);
            String binary = "x" + (axiomIndex + 1);
            double reciShapleyValue = calculateShapleyValue(axiom, conflicts);

            outputs.println("      " + binary + "  obj   " + reciShapleyValue);
            for (int conflictIndex = 0; conflictIndex < conflicts.size(); conflictIndex++) {
                HashSet<OWLAxiom> oneMups = conflicts.get(conflictIndex);
                if (oneMups.contains(axiom)) {
                    outputs.println("      " + binary + " c" + (conflictIndex + 1) + " 1");
                }
            }
            outputs.println("      " + binary + " c" + extrline + " 1");
        }
        outputs.println();
        outputs.println("RHS");

        for (int conflictIndex = 1; conflictIndex <= conflicts.size(); conflictIndex++) {
            outputs.println("      rhs c" + conflictIndex + "  1");
        }
        outputs.println("      rhs c" + extrline + "  " + limit);
        outputs.println();
        outputs.println("BOUNDS");

        for (int axiomIndex = 1; axiomIndex <= axioms.size(); axiomIndex++) {
            String binary = "x" + axiomIndex;
            outputs.println("      LI bnd " + binary + " 0");
        }
        for (int axiomIndex = 1; axiomIndex <= axioms.size(); axiomIndex++) {
            String binary = "x" + axiomIndex;
            outputs.println("      UI bnd " + binary + " 1");
        }
        outputs.println();
        outputs.print("ENDATA");

        writer.close();
    }
}
