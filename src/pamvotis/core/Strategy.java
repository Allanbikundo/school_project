/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package src.pamvotis.core;

import java.util.ArrayList;

/**
 *
 * @author Simon
 */
public class Strategy {

    private ArrayList<Canditate> canditates = new ArrayList<Canditate>();

    public void addCandidate(String name, int id, int rank) {
        canditates.add(new Canditate(name, id, rank));
    }

    public void reward(int id, long reward) {
        for (Canditate cand : canditates) {
            if (cand.getId() == id) {
                cand.setRank(cand.getRank() + reward);
            }
        }
    }

    public void penalise(int id, long penalty) {
        for (Canditate cand : canditates) {
            if (cand.getId() == id) {
                cand.setRank(cand.getRank() - penalty);
            }
        }
    }


    public Canditate getCandidate(int id) {
        for (Canditate cand : canditates) {
            if (cand.getId() == id) {
                return cand;
            }
        }
        return null;
    }

    public String report() {
        String report = "";
        for (Canditate cand : canditates) {
            report += cand.getName() + " was used " + cand.getNumOfTimeUsed() + " times\n";
        }
        return report;
    }

    public String reportRanks() {
        String report = "ALL RANKS:\n";
        for (Canditate cand : canditates) {
            report += cand.getName() + "=" + cand.getRank() + ", ";
        }
        return report;
    }

    public int selectStrategy() {
        int bestId = canditates.get(0).getId();
        long bestRank = canditates.get(0).getRank();


        //get the best rank.
        for (Canditate cand : canditates) {
            if (cand.getRank() > bestRank) {
                bestId = cand.getId();
                bestRank = cand.getRank();
            }
        }
        //get all that have that best rank.
        ArrayList<Canditate> bestCands = new ArrayList<Canditate>();
        for (Canditate cand : canditates) {
            if (cand.getRank() == bestRank) {
                bestCands.add(cand);
            }
        }
        if (bestCands.size() > 1) {
            int randId = bestCands.get((int) (Math.random() * bestCands.size())).getId();
            System.out.println(bestCands.size()+" have same best rank=" + bestRank + ", selecting one strategy randomly = " + getCandidate(randId).getName());
            getCandidate(randId).setNumOfTimeUsed(getCandidate(randId).getNumOfTimeUsed() + 1);
            return randId;
        }
        System.out.println("Selecting the best rank=" + bestRank + " strategy " + getCandidate(bestId).getName());
        getCandidate(bestId).setNumOfTimeUsed(getCandidate(bestId).getNumOfTimeUsed() + 1);
        return bestId;

        //this only works for 2 strategies
        /*
        System.out.println("_________________________________________________________________________");
        System.out.println(canditates.get(0).getName() + " has rank " + canditates.get(0).getRank());
        System.out.println(canditates.get(1).getName() + " has rank " + canditates.get(1).getRank());
        if (canditates.get(0).getRank() > canditates.get(1).getRank()) {
        System.out.println("selected 1");
        getCandidate(1).setNumOfTimeUsed(getCandidate(1).getNumOfTimeUsed() + 1);
        return 1;
        } else if (canditates.get(0).getRank() < canditates.get(1).getRank()) {
        System.out.println("selected 2");
        getCandidate(2).setNumOfTimeUsed(getCandidate(2).getNumOfTimeUsed() + 1);
        return 2;
        } else {
        int sel = (int) (Math.random() * 2) + 1;
        System.out.println("selected randomly " + sel);
        getCandidate(sel).setNumOfTimeUsed(getCandidate(sel).getNumOfTimeUsed() + 1);
        return sel;
        }
         *
         */
    }

    private class Canditate {

        private String name = null;
        private int id;
        private long rank;
        private int numOfTimeUsed;

        public Canditate(String name, int id, int rank) {
            this.rank = rank;
            this.id = id;
            this.name = name;
        }

        /**
         * @return the name
         */

        public String getName() {
            return name;
        }

        /**
         * @return the id
         */
        public int getId() {
            return id;
        }

        /**
         * @return the rank
         */
        public long getRank() {
            return rank;
        }

        /**
         * @param rank the rank to set
         */
        public void setRank(long rank) {
            this.rank = rank;
        }

        /**
         * @return the numOfTimeUsed
         */
        public int getNumOfTimeUsed() {
            return numOfTimeUsed;
        }

        /**
         * @param numOfTimeUsed the numOfTimeUsed to set
         */
        public void setNumOfTimeUsed(int numOfTimeUsed) {
            this.numOfTimeUsed = numOfTimeUsed;
        }
    }
}