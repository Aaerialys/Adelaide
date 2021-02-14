package bot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//original code by Mike Mirzayanov
//Thank you Mike Mirzayanov for the great systems codeforces and polygon



public class CodeforcesRatingCalculator {
    public static ArrayList<Integer> calculateRatingChanges(ArrayList<Integer> previousRatings) {
        List<Contestant> contestants = new ArrayList<>(previousRatings.size());
        int rank=1;
        for (Integer x : previousRatings) {
            contestants.add(new Contestant(rank++,x));
        }
 
        process(contestants);
 
        ArrayList<Integer> ratingChanges = new ArrayList<Integer>();
        for (Contestant contestant : contestants) ratingChanges.add(contestant.delta);
 
        return ratingChanges;
    }
 
    private static double getEloWinProbability(double ra, double rb) {
        return 1.0 / (1 + Math.pow(10, (rb - ra) / 400.0));
    }
    private static double getEloWinProbability(Contestant a, Contestant b) {
        return getEloWinProbability(a.rating, b.rating);
    }
 
    public int composeRatingsByTeamMemberRatings(int[] ratings) {
        double left = 100;
        double right = 4000;
 
        for (int tt = 0; tt < 20; tt++) {
            double r = (left + right) / 2.0;
 
            double rWinsProbability = 1.0;
            for (int rating : ratings) {
                rWinsProbability *= getEloWinProbability(r, rating);
            }
 
            double rating = Math.log10(1 / (rWinsProbability) - 1) * 400 + r;
 
            if (rating > r) {
                left = r;
            } else {
                right = r;
            }
        }
 
        return (int) Math.round((left + right) / 2);
    }
 
    private static double getSeed(List<Contestant> contestants, int rating) {
        Contestant extraContestant = new Contestant(0, rating);
 
        double result = 1;
        for (Contestant other : contestants) {
            result += getEloWinProbability(other, extraContestant);
        }
 
        return result;
    }
 
    private static int getRatingToRank(List<Contestant> contestants, double rank) {
        int left = 1;
        int right = 8000;
 
        while (right - left > 1) {
            int mid = (left + right) / 2;
 
            if (getSeed(contestants, mid) < rank) {
                right = mid;
            } else {
                left = mid;
            }
        }
 
        return left;
    }
 
    private static void process(List<Contestant> contestants) {
        if (contestants.isEmpty()) {
            return;
        }
 
        for (Contestant a : contestants) {
            a.seed = 1;
            for (Contestant b : contestants) {
                if (a != b) {
                    a.seed += getEloWinProbability(b, a);
                }
            }
        }
 
        for (Contestant contestant : contestants) {
            double midRank = Math.sqrt(contestant.rank * contestant.seed);
            contestant.needRating = getRatingToRank(contestants, midRank);
            contestant.delta = (contestant.needRating - contestant.rating) / 2;
        }
 
        sortByRatingDesc(contestants);
 
        // Total sum should not be more than zero.
        {
            int sum = 0;
            for (Contestant c : contestants) {
                sum += c.delta;
            }
            int inc = -sum / contestants.size() - 1;
            for (Contestant contestant : contestants) {
                contestant.delta += inc;
            }
        }
 
        // Sum of top-4*sqrt should be adjusted to zero.
        {
            int sum = 0;
            int zeroSumCount = Math.min((int) (4 * Math.round(Math.sqrt(contestants.size()))), contestants.size());
            for (int i = 0; i < zeroSumCount; i++) {
                sum += contestants.get(i).delta;
            }
            int inc = Math.min(Math.max(-sum / zeroSumCount, -10), 0);
            for (Contestant contestant : contestants) {
                contestant.delta += inc;
            }
        }
 
        validateDeltas(contestants);
    }
 
    private static void validateDeltas(List<Contestant> contestants) {
    	sortByRank(contestants);
        for (int i = 0; i < contestants.size(); i++) {
            for (int j = i + 1; j < contestants.size(); j++) {
                if (contestants.get(i).rating > contestants.get(j).rating) {
                    ensure(contestants.get(i).rating + contestants.get(i).delta >= contestants.get(j).rating + contestants.get(j).delta,
                            "First rating invariant failed: " + i + " vs. " + j + ".");
                }
                if (contestants.get(i).rating < contestants.get(j).rating) {
                    if (contestants.get(i).delta < contestants.get(j).delta) {
                        System.out.println(1);
                    }
                    ensure(contestants.get(i).delta >= contestants.get(j).delta,
                            "Second rating invariant failed: " + i + " vs. " + j + ".");
                }
            }
        }
    }
 
    private static void sortByRank(List<Contestant> contestants) {
        Collections.sort(contestants, new Comparator<Contestant>() {
            @Override
            public int compare(Contestant o1, Contestant o2) {
                return Integer.compare(o1.rank, o2.rank);
            }
        });
    }
 

	private static void ensure(boolean b, String message) {
        if (!b) {
            throw new RuntimeException(message);
        }
    }
 
    private static void sortByRatingDesc(List<Contestant> contestants) {
        Collections.sort(contestants, new Comparator<Contestant>() {
            @Override
            public int compare(Contestant o1, Contestant o2) {
                return -Integer.compare(o1.rating, o2.rating);
            }
        });
    }
 
    private static final class Contestant {
        private int rank;
        private int rating;
        private int needRating;
        private double seed;
        private int delta;
 
        private Contestant(int rank, int rating) {
            this.rank = rank;
            this.rating = rating;
        }
    }
}