import java.io.*;
import java.util.*;
import java.text.NumberFormat;
import java.text.DecimalFormat;

public class SpellChecker2 {
    Map < String, Integer > dict;   // dict contains all dictionary words and their frequency in corpus
    static final String alphabet = "abcdefghijklmnopqrstuvwxyz";

    // constructors
    public SpellChecker2() {}
    public SpellChecker2(String dictionaryFile) throws IOException {
        dict = readDictionary(dictionaryFile);
    }

    /*
        calculateLM: returns the language model P(word)
        Input: word 
        Output: P(word)
     */
    public double calculateLM(String word) {
        // Currently, we simply return P(word)=1 if the word appears in the dictionary
        //
        // TODO: modify this function so that it uses the relative frequency of the word in computing P(word).
        //       You can access frequency of word from "dict" like dict.get(word)
        //
        return dict.containsKey(word) ? dict.get(word) : 0.0;
    }

    /*
        calculateLM: returns the error model P(correction|word)
        Input: word, correction, and the editDistance between the two 
        Output: P(correction|word)
     */
    public double calculateEM(String word, String correction, double editDistance) {
        // For Task 1, we simply return 1 as the error model P(correction|word) 
        String vowels = "aeiou";
        if (word.length() == correction.length()) {
            if (editDistance == 1.0) {     
                return checkVowels(word, correction, editDistance);
            }
            else{
                return checkVowels(word, correction, editDistance);
            }
        }
        else{
            HashSet<Character> h1= new HashSet<Character>();
            HashSet<Character> h2 = new HashSet<Character>();
            for (int i = 0; i < word.length(); i++){ 
                char c = word.charAt(i); 
                h1.add(c);
            } 
            for (int i = 0; i < correction.length(); i++){ 
                char c = correction.charAt(i); 
                h2.add(c);
            } 
            // System.out.println(h1);
            // System.out.println(h2);
            // HashSet<Character> h1copy = h1;
            // HashSet<Character> h2copy = h2;
            h1.removeAll(h2);
            // h2copy.removeAll(h1copy);
            // System.out.println(h1);
            // System.out.println(h2copy);
            // System.out.println(word);
            // System.out.println(correction);
            for (Character c : h1) {
                if (vowels.indexOf(c) == -1 ){ // curtions -> curtis, h1 => [n,o], has nonvowel means not possible.
                    return 0.2;
                }
            }
        }
        return 1.0;
    }

    // Helper functions to check if different chars are vowels
    public double checkVowels(String word, String correction, double editDistance){
        String vowels = "aeiou";
        double vowelDistance = 0.0;
        for (int i = 0; i < word.length(); ++i) {
            if (word.charAt(i) != correction.charAt(i)) {
                if (vowels.contains(Character.toString(word.charAt(i))) && vowels.contains(Character.toString(correction.charAt(i)))) {
                    vowelDistance ++;
                } else {
                    vowelDistance --;
                }
            }
        }
        if (vowelDistance == editDistance){ // means all of different characters are vowels
            return 1.0;
        }else{
            return 0.2;
        }
    }
    /*
        EditDist1: computes the candidate set C within edit distance 1 of input word
        Input: word - the word typed by the user
        Output: map of all strings within edit distance 1 of "word"
                the value of the output map is the edit distnace, which is 1.0
    */
    private Map < String, Double > EditDist1(String word) {
        Map < String, Double > cands = new HashMap < > ();
        for (int i = 0; i <= word.length(); ++i) {

            // split the word at position i
            String L = word.substring(0, i);
            String R = word.substring(i);
            String temp;

            // "deletion at [i]": drop the character at the current position i
            if (R.length() > 0) {
                temp = L + R.substring(1);
                cands.put(temp, 1.0); // 1.0 indicates the edit distance 1
            }
            // "insertion at [i]" insert an alphabet (a-z) at the current position i
            for (int j = 0; j < alphabet.length(); ++j) {
                temp = L + alphabet.charAt(j) + R;
                cands.put(temp, 1.0); // 1.0 indicates the edit distance 1
            }

            // TODO: implement transposition operator here
            if (L.length() > 0 && R.length() > 0) {
                String temp1 = L.substring(0, L.length() - 1) + R.substring(0,1);
                String temp2 = L.substring(L.length() - 1) + R.substring(1, R.length());
                cands.put(temp1+temp2, 1.0);
            }

            // TODO: implement substitution operator here
            for (int k = 0; k < alphabet.length(); ++k) {
                String temp1 = L + alphabet.substring(k, k+1);
                if(R.length() >= 1){
                    temp = temp1 + R.substring(1, R.length());
                }
                else{
                    temp = temp1;
                }
                cands.put(temp, 1.0);
            }
        }
        return cands;
    }

    /*
        EditDist2: computes the candidate set C within edit distance 2 of input word
        Input: ed1 - output from EditDist1() function. 
                     i.e., map of all strings within edit distance 1 of the input word
        Output: map of all strings within edit distance 2 of the original input word
                the value of the output map is the edit distnace, which is 2.0
    */
    private Map < String, Double > EditDist2(Map < String, Double > ed1) {
        Map < String, Double > cands = new HashMap < > ();

        // iterate over every string in edit-distance-1 candidate set
        for (Map.Entry < String, Double > entry: ed1.entrySet()) {
            String str = entry.getKey();
            double dist = entry.getValue();

            // get every string within edit-distance-1 of str
            Map < String, Double > ed2 = EditDist1(str);

            // iterate over every string in the edit-distance-1 of str
            for (Map.Entry < String, Double > entry2: ed2.entrySet()) {
                String str2 = entry2.getKey();
                double dist2 = entry2.getValue();

                // put the newly obtained string into edit-distance-2 candidate set
                cands.put(str2, dist + dist2);
            }
        }
        return cands;
    }

    /*
        generateCandidates: generate all candidate corrections given the input "word"
        Input:  A word with spelling error
        Output: All candidates that may be the correct word. String is the candidate, and Double is the edit distance of the candidate.
     */
    public Map < String, Double > generateCandidates(String word) {
        Map < String, Double > cand = new HashMap < > ();

        // if the input word appears in the dictionary, we simply return the word as the only candidate with edit distance 0
        if (dict.containsKey(word)) {
            cand.put(word, 0.0);
            return cand;
        }

        // obtain all strings within edit distance 1 of the input word
        Map < String, Double > ed1 = EditDist1(word);

        // add an edit-distance-1 string to the candidate set only if it appears in the dictionary
        for (String str: ed1.keySet()) {
            if (dict.containsKey(str)) cand.put(str, ed1.get(str));
        }
        // if there is any dictionary entry within edit-distance-1, we return them as the candidate set
        if (!cand.isEmpty()) return cand;

        // there is no dictionary word within edit distance 1, so we now 
        // obtain all strings within edit distance 2 of input word
        Map < String, Double > ed2 = EditDist2(ed1);

        // add an edit-distance-2 string to candidate set only if it appears in the dictionary
        for (String str: ed2.keySet()) {
            if (dict.containsKey(str)) cand.put(str, ed2.get(str));
        }
        return cand;
    }

    /*
        findCorrection: returns the corrected spelling of the input word
        Input: word: the input word with spelling errors; 
               candidates: the list of candidates obtained from the function "generateCandidates"
        Output: The spell-corrected word
     */
    public String findCorrection(String word, Map < String, Double > candidates) {
        // if the word appears in the dictionary or there is no candidates, return the word as the "correction"
        if (dict.containsKey(word) || candidates.isEmpty())  return word;

        // iterate over every word in the candidate set and return the one with the highest probability
        double maxp = 0.0;
        String c = word;
        for (String str: candidates.keySet()) {

            // compute P(str|word) = P(word|str) * P(str)  
            double prob = calculateEM(word, str, candidates.get(str)) * calculateLM(str);
            if (prob > maxp) {
                maxp = prob;
                c = str;
            }
        }
        return c;
    }

    /*
        readDictionary: read the dictionary file and return the <dictionary word, frequency> pair in a map
        Input: The filename of dictionary
        Output: A map with contents <dictionary word, frequency>
        You do not need to modify this function
     */
    private Map < String, Integer > readDictionary(String dictionaryFile) throws IOException {
        dict = new HashMap < > ();
        InputStreamReader fis = new InputStreamReader(new FileInputStream(dictionaryFile));
        BufferedReader br = new BufferedReader(fis);
        String line;
        while ((line = br.readLine()) != null) {
            String[] tmp = line.split("\t");
            if (tmp.length > 1) {
                dict.put(tmp[0], Integer.parseInt(tmp[1]));
            } else {
                dict.put(tmp[0], 1);
            }
        }
        System.out.println("Finished loading dictionary. Total size: " + dict.size());
        br.close();
        fis.close();
        return dict;
    }

    /*
        readTestset: read testset file and return the <wrong spelling, correct word> pair in a map
        Input: The filename of testset
        Output: A map with contents <wrong spelling, correct word>
        You do not need to modify this function
     */
    private Map < String, String > readTestset(String testsetFile) throws IOException {
        Map < String, String > cases = new HashMap < > ();
        InputStreamReader fis = new InputStreamReader(new FileInputStream(testsetFile));
        BufferedReader br = new BufferedReader(fis);
        String line;
        while ((line = br.readLine()) != null) {
            String[] temp1 = line.split(":");
            String right = temp1[0].trim();
            String[] wrongs = temp1[1].split(" ");
            for (String wrong: wrongs)
                if (!wrong.equals(" ") && !wrong.isEmpty())
                    cases.put(wrong.trim(), right);
        }
        br.close();
        fis.close();
        return cases;
    }

    /*
        runTestcases: run spell correction algorithm on all test_caes and prints out the results
        Input: test_cases - a map of <wrong spelling, correct word> pairs
               print_all - if true, the function print out the correction result for each test case
        IMPORTANT: DO NOT MODIFY THIS FUNCTION!!! 
                   IT WILL MAKE OUR GRADING SCRIPT FAIL!
     */
    public void runTestcases(Map < String, String > test_cases, boolean print_all) {
        int n = test_cases.size();
        int nCorrect = 0, nUnknown = 0;
        boolean accurate;
        for (String wrong: test_cases.keySet()) {
            Map < String, Double > candidates = generateCandidates(wrong);
            String predict = findCorrection(wrong, candidates);
            accurate = predict.equals(test_cases.get(wrong));
            if (accurate) nCorrect++;
            else {
                if (!dict.containsKey(test_cases.get(wrong)))
                    nUnknown++;
            }
            if (print_all)
                System.out.println(wrong + " -> " + predict + ": " + (accurate ? '1' : '0'));
        }
        NumberFormat formatter = new DecimalFormat("#0.00");
        System.out.println("FINAL RESULT: " + 
                           nCorrect + " of " + n + " correct. ( " +
                           formatter.format(nCorrect*100.0/n) + "% accuracy " +
                           formatter.format(nUnknown*100.0/n) + "% unknown )");
    }

    /*
        The main function: 
        args[0] is the path of dictionary.
        args[1] is the path of test set.
        args[2] is optional, decide whether to output the full lists of predictions.
        IMPORTANT: DO NOT CHANGE THE COMMAND LINE PARAMETERS!!! 
                   IT WILL BREAK OUR GRADING SCRIPT
     */
    public static void main(String args[]) throws IOException {
        SpellChecker2 sc = new SpellChecker2(args[0]);
        Map < String, String > testset = sc.readTestset(args[1]);
        boolean printAll = (args.length < 3) ? true : Boolean.parseBoolean(args[2]);
        sc.runTestcases(testset, printAll);
    }
}
