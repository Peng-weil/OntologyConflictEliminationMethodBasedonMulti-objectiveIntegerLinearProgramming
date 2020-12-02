import ilog.concert.IloLPMatrix;
import ilog.cplex.IloCplex;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import java.io.File;
import java.util.*;

/**
 * @author: Peng-weil
 * @date: 2020/11/29
 */

public class OntologyConflictElimination {
    static ResourceBundle ILPParameter = ResourceBundle.getBundle("parameters");
    static String projectPath = System.getProperty("user.dir").replace('\\', '/');

    public static void main(String[] args) throws Exception {

        filePathInitialization(projectPath);
        String owlPath = projectPath + ILPParameter.getString("ontologyPath") + ILPParameter.getString("ontoName") + ".owl";
        String mupsPath = projectPath + ILPParameter.getString("mupsPath") + ILPParameter.getString("ontoName") + "/";
        OWLOntology sourceOnto = OWLTools.openOntology(owlPath);
        HashSet<HashSet<String>> ucMUPS = OWLTools.getMUPSStrFromText(mupsPath);

        ArrayList<HashSet<OWLAxiom>> conflicts = OWLTools.transferStrToMUPS(sourceOnto, ucMUPS);
        ArrayList<OWLAxiom> axioms = OWLTools.getAxiomList(conflicts);


        System.out.println("initialization compelete");
        System.out.println("ontology name  : " + ILPParameter.getString("ontoName"));
        System.out.println("logical axioms : " + sourceOnto.getLogicalAxioms().size());

        System.out.println("filter axioms  : " + axioms.size());
        System.out.println("conflitcs      : " + conflicts.size());

        ArrayList<HashSet<OWLAxiom>> solutionPool = new ArrayList<>();

        for (int r = 0; r < 1000; r++) {
            Random rand = new Random();
            rand.setSeed(r);
            Collections.shuffle(axioms, rand);

            String mpsPath = "";
            if (ILPParameter.getString("modelType").equals("SINGLE")) {
                mpsPath = projectPath + ILPParameter.getString("diagPath") + ILPParameter.getString("ontoName") + "/mps/ILPModel-random" + r + ".mps";
            } else if (ILPParameter.getString("modelType").equals("SHAPLEY")) {
                mpsPath = projectPath + ILPParameter.getString("diagPath") + ILPParameter.getString("ontoName") + "/mps/Shapley-ILPModel-random" + r + ".mps";
            } else if (ILPParameter.getString("modelType").equals("MULTI")) {
                mpsPath = projectPath + ILPParameter.getString("diagPath") + ILPParameter.getString("ontoName") + "/mps/Shapley-MultiModel-random" + r + ".mps";
            }
            HashSet<OWLAxiom> solutionSet = new HashSet<>();

            HashMap<String, OWLAxiom> axiomBinary = ILPTools.getBinary(axioms);
            ArrayList<String> solution = generateRepairPlan(mpsPath, axioms, conflicts);
            for (String s : solution) {
                OWLAxiom ax = axiomBinary.get(s);
                solutionSet.add(ax);
            }
            if (!solutionPool.contains(solutionSet)) {
                solutionPool.add(solutionSet);
            }

            System.out.println((r + 1) + ". SOLUTION POOL SIZE : " + solutionPool.size());

        }
    }

    static void filePathInitialization(String projectPath) {
        String owlPath = projectPath + ILPParameter.getString("ontologyPath");
        String mupsPath = projectPath + ILPParameter.getString("mupsPath") + ILPParameter.getString("ontoName");
        String modelPath = projectPath + ILPParameter.getString("diagPath") + ILPParameter.getString("ontoName") + "/mps";
        if (checkFileExit(owlPath) || checkFileExit(mupsPath)) {
            System.out.println("Please import OWL files or MUPS files.");
        }
        if (checkFileExit(modelPath)) {
            File file = new File(modelPath);
            file.mkdirs();
        }
    }

    static boolean checkFileExit(String path) {
        File file = new File(path);
        return !file.exists();
    }

    static ArrayList<String> generateRepairPlan(String mpsPath,
                                                ArrayList<OWLAxiom> axioms,
                                                ArrayList<HashSet<OWLAxiom>> conflicts) throws Exception {
        ArrayList<String> solution = new ArrayList<>();
        if (ILPParameter.getString("modelType").equals("SINGLE")) {
            ILPTools.createILPModel(mpsPath, axioms, conflicts);
        } else if (ILPParameter.getString("modelType").equals("SHAPLEY")) {
            ILPTools.createShapleyILPModel(mpsPath, axioms, conflicts);
        } else if (ILPParameter.getString("modelType").equals("MULTI")) {
            String stepOneMups = projectPath + ILPParameter.getString("diagPath") + ILPParameter.getString("ontoName") + "/mps/Shapley-MultiModel_step1-random.mps";
            ILPTools.createILPModel(stepOneMups, axioms, conflicts);
            IloCplex singleCplex = new IloCplex();
            singleCplex.setOut(null);
            singleCplex.importModel(stepOneMups);
            singleCplex.setParam(IloCplex.Param.MIP.Pool.RelGap, 0.1);
            singleCplex.solve();
            double numLimit = singleCplex.getObjValue();

            ILPTools.createMultiILPModel(mpsPath, axioms, conflicts, numLimit);
        }


        IloCplex cplex = new IloCplex();
        cplex.setOut(null);
        cplex.importModel(mpsPath);
        cplex.setParam(IloCplex.Param.MIP.Pool.RelGap, 0.1);
        cplex.solve();
        if (cplex.populate()) {
            IloLPMatrix lp = (IloLPMatrix) cplex.LPMatrixIterator().next();
            double[] x = cplex.getValues(lp);
            System.out.println("first feasible solution：");

            for (int j = 0; j < x.length; j++) {
                if (x[j] == 1.0D) {
                    solution.add("x" + (j + 1));
//                    System.out.print(" x" + (j + 1));
                }
            }
        }
        System.out.println();
        return solution;
    }

}
