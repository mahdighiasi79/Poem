import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

class Occurrence {

    int H;
    int F;
    int M;

    Occurrence() {
        H = F = M = 0;
    }
}

class Train {

    HashMap<String, Occurrence> unigram;
    HashMap<String, Occurrence> bigram;

    Train() {

        unigram = new HashMap<>();
        bigram = new HashMap<>();
        Counter("molavi_train.txt", 1);
        Counter("hafez_train.txt", 2);
        Counter("ferdowsi_train.txt", 3);
    }

    private void Insert(String word, int gram, int poet) {

        Occurrence temp;
        if (gram == 1)
            temp = unigram.get(word);
        else
            temp = bigram.get(word);


        if (temp == null)
            temp = new Occurrence();

        if (poet == 1)
            temp.M++;
        else if (poet == 2)
            temp.H++;
        else if (poet == 3)
            temp.F++;

        if (gram == 1)
            unigram.put(word, temp);
        else
            bigram.put(word, temp);
    }

    private void Counter(String address, int poet) {

        try {
            FileInputStream fis = new FileInputStream(address);
            Scanner reader = new Scanner(fis);

            String word1, word2, words;
            word1 = word2 = "S";
            boolean b = true;

            while (reader.hasNextLine()) {

                String line = reader.nextLine();
                int start = 0, end, temp;
                String nextWord;

                for (int i = 0; i < line.length(); i++) {

                    temp = (int) line.charAt(i);
                    if ((temp == 32) || (i == line.length() - 1)) {

                        if (temp == 32)
                            end = i - 1;
                        else end = i;

                        nextWord = line.substring(start, end + 1);

                        if (b) {
                            word2 = nextWord;
                            words = word1 + " " + word2;
                            Insert(words, 2, poet);
                            Insert(word2, 1, poet);
                            b = false;

                        } else {
                            word1 = nextWord;
                            words = word2 + " " + word1;
                            Insert(words, 2, poet);
                            Insert(word1, 1, poet);
                            b = true;
                        }

                        start = i + 1;
                    }
                }

                if (b) {
                    words = word1 + " S";
                    Insert(words, 2, poet);

                } else {
                    words = word2 + " S";
                    Insert(words, 2, poet);
                }
            }

            reader.close();
            fis.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Model {

    private Train train;
    private float L1;
    private float L2;
    private float L3;
    private float e;
    int poet;

    Model(String line, Train train, float L1, float L2, float L3, float e) {

        this.train = train;
        this.L1 = L1;
        this.L2 = L2;
        this.L3 = L3;
        this.e = e;

        poet = test(line);
    }

    private float Count(Occurrence occurrence, int l) {

        float count;
        if (l == 0)
            count = occurrence.M + occurrence.F + occurrence.H;
        else if (l == 1)
            count = occurrence.M;
        else if (l == 2)
            count = occurrence.H;
        else
            count = occurrence.F;

        return count;
    }

    private float P_Uni(String unigram, int l) {

        Occurrence temp = train.unigram.get(unigram);
        float count = 0;
        if (temp != null)
            count = Count(temp, l);

        float M = 0;

        Iterator iterator = train.unigram.entrySet().iterator();
        while (iterator.hasNext()) {
            HashMap.Entry mapElement = (HashMap.Entry) iterator.next();
            temp = (Occurrence) mapElement.getValue();

            if (l == 0)
                M += (temp.M + temp.F + temp.H);
            else if (l == 1)
                M += temp.M;
            else if (l == 2)
                M += temp.H;
            else
                M += temp.F;
        }

        return count / M;
    }

    private float P_Bio(String firstWord, String secondWord, int l) {

        String bigram = firstWord + " " + secondWord;
        float countBio = 0;
        float countUni = 999999999;

        Occurrence temp = train.bigram.get(bigram);
        if (temp != null)
            countBio = Count(temp, l);

        temp = train.unigram.get(firstWord);
        if (temp != null)
            countUni = Count(temp, l);

        return countBio / countUni;
    }

    private float P_Total(String firstWord, String secondWord, int l) {

        float result;
        result = (L3 * P_Bio(firstWord, secondWord, l)) + (L2 * P_Uni(firstWord, l)) + (L1 * e);
        return result;
    }

    private int test(String line) {

        int start = 0, end, temp;
        String word1, word2, nextWord;
        word1 = "S";
        word2 = "S";
        float PL1, PL2, PL3;
        PL1 = PL2 = PL3 = 1;
        boolean b = true;

        for (int i = 0; i < line.length(); i++) {

            temp = (int) line.charAt(i);
            if ((temp == 32) || (i == line.length() - 1)) {

                if (temp == 32)
                    end = i - 1;
                else end = i;

                nextWord = line.substring(start, end+1);
                start = i + 1;

                if (b) {
                    word2 = nextWord;

                        PL1 *= P_Total(word1, word2, 1);
                        PL2 *= P_Total(word1, word2, 2);
                        PL3 *= P_Total(word1, word2, 3);

                    b = false;

                } else {
                    word1 = nextWord;

                        PL1 *= P_Total(word2, word1, 1);
                        PL2 *= P_Total(word2, word1, 2);
                        PL3 *= P_Total(word2, word1, 3);

                    b = true;
                }
            }
        }

        String word;
        if (b)
            word = word1;
        else
            word = word2;


            PL1 *= P_Total(word, "S", 1);
            PL2 *= P_Total(word, "S", 2);
            PL3 *= P_Total(word, "S", 3);


        if (PL1 > PL2) {
            if (PL1 > PL3)
                return 3;
            else if (PL1 < PL3)
                return 1;
        } else if (PL1 < PL2) {
            if (PL2 > PL3)
                return 2;
            else if (PL2 < PL3)
                return 1;
        }
        return 0;
    }
}

class Test {

    private Train train;
    private float L1;
    private float L2;
    private float L3;
    private float e;
    private float rightGuess;
    private float number;

    Test(Train train, float L1, float L2, float L3, float e) {

        this.e = e;
        this.L1 = L1;
        this.L2 = L2;
        this.L3 = L3;
        this.train = train;
        this.number = 0;
        this.rightGuess = 0;
        Guess();
    }

    private void Guess() {

        try {
            FileInputStream fis = new FileInputStream("test_file.txt");
            Scanner reader = new Scanner(fis);

            String line;
            String poem;
            Model model;
            int poet;

            while (reader.hasNextLine()) {

                line = reader.nextLine();
                poem = line.substring(2);
                model = new Model(poem, train, L1, L2, L3, e);
                poet = (int) line.charAt(0);
                poet -= 48;

                System.out.println(model.poet);

                if (poet == model.poet)
                    rightGuess++;
                number++;
            }

            float percentage = (rightGuess / number) * 100;
            System.out.println("Total number of poems: " + number);
            System.out.println("Number of right guesses: " + rightGuess);
            System.out.println("percentage: " + percentage);

            reader.close();
            fis.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public class Main {

    public static void main(String[] args) {

        Scanner reader = new Scanner(System.in);

        float L1 = reader.nextFloat();
        float L2 = reader.nextFloat();
        float L3 = reader.nextFloat();
        float e = reader.nextFloat();

        Train train = new Train();
        new Test(train, L1, L2, L3, e);

    }
}
