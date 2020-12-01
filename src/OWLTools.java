import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * @author: Peng-weil
 * @date: 2020/11/30
 */
public class OWLTools {
    public static OWLOntologyManager manager = null;

    public static OWLOntologyManager createOntologyManager() {
        if (manager == null) {
            manager = OWLManager.createOWLOntologyManager();
        }

        return manager;
    }

    public static String checkOntoPath(String ontoPath) {
        String path = ontoPath;
        if (!ontoPath.startsWith("http:") && !ontoPath.startsWith("https:") && !ontoPath.startsWith("ftp:") && !ontoPath.startsWith("file:")) {
            path = "file:" + ontoPath;
        }

        return path;
    }

    public static OWLOntology openOntology(String ontoPath) {
        createOntologyManager();
        String path = checkOntoPath(ontoPath);
        IRI physicalURI = IRI.create(path);
        OWLOntology ontology = null;

        try {
            ontology = manager.loadOntology(physicalURI);
        } catch (OWLOntologyCreationException exception) {
            exception.printStackTrace();
        }

        return ontology;
    }

    public static HashSet<HashSet<String>> getMUPSStrFromText(String filePath) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(filePath + "res.txt"));
        boolean mupsStart = false;
        HashSet<HashSet<String>> mupsStr = new HashSet();
        HashSet<String> oneMUPS = new HashSet();
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.trim().startsWith("Found explanation <") && !line.trim().startsWith("Explanation <")) {
                if (line.trim().length() != 0 && line.trim().startsWith("[")) {
                    if (mupsStart) {
                        String str = line.substring(line.indexOf("]") + 1).trim();
                        oneMUPS.add(str);
                    }
                } else {
                    mupsStart = false;
                    if (!oneMUPS.isEmpty()) {
                        mupsStr.add(new HashSet(oneMUPS));
                        oneMUPS.clear();
                    }
                }
            } else {
                mupsStart = true;
            }
        }

        return mupsStr;
    }

    public static ArrayList<HashSet<OWLAxiom>> transferStrToMUPS(OWLOntology sourceOnto, HashSet<HashSet<String>> ucMUPSOfStr) {
        ArrayList<HashSet<OWLAxiom>> ucMUPSOfAxiom = new ArrayList();

        HashMap<String, OWLAxiom> axiomMap = new HashMap();
        Iterator axiomIter = sourceOnto.getLogicalAxioms().iterator();

        while (axiomIter.hasNext()) {
            OWLAxiom axiom = (OWLAxiom) axiomIter.next();
            axiomMap.put(axiom.toString(), axiom);
        }

        for (HashSet<String> ucmups : ucMUPSOfStr) {
            HashSet<OWLAxiom> oneMups = new HashSet<>();
            for (String axiomInMups : ucmups) {
                OWLAxiom axiom = axiomMap.get(axiomInMups);
                if (axiom == null) {
                    System.out.println("Some axioms in MUPS are not in ontology,Please find out the reason.");
                    System.exit(0);
                } else {
                    oneMups.add(axiom);
                }
            }
            ucMUPSOfAxiom.add(oneMups);
        }
        return ucMUPSOfAxiom;
    }

    public static ArrayList<OWLAxiom> getAxiomList(ArrayList<HashSet<OWLAxiom>> axiomMups) {
        ArrayList<OWLAxiom> axiomSet = new ArrayList();
        Iterator axiomIt = axiomMups.iterator();

        while (axiomIt.hasNext()) {
            HashSet<OWLAxiom> con = (HashSet) axiomIt.next();
            Iterator conIt = con.iterator();

            while (conIt.hasNext()) {
                OWLAxiom axiom = (OWLAxiom) conIt.next();
                if (!axiomSet.contains(axiom)) {
                    axiomSet.add(axiom);
                }
            }
        }
        return axiomSet;
    }
}
