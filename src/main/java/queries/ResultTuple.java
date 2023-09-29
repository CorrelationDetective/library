package queries;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ResultTuple implements ResultObject {
    @Expose @Getter public int[] LHS;
    @Expose @Getter public int[] RHS;
    @Expose @Getter @Setter public double similarity;
    @Expose @Setter @Getter public String[] lHeaders;
    @Expose @Setter @Getter public String[] rHeaders;
    @Expose @Setter @Getter public long timestamp;

    public ResultTuple(int[] LHS, int[] RHS, double similarity){
        this.LHS = LHS;
        this.RHS = RHS;
        this.similarity = similarity;
    }

    private boolean sorted = false;

    public String toString() {
        return String.format("%s | %s -> %.3f",
                Arrays.stream(LHS).mapToObj(Integer::toString).collect(Collectors.joining("-")),
                Arrays.stream(RHS).mapToObj(Integer::toString).collect(Collectors.joining("-")),
                similarity
        );
    }

    public int size(){
        return LHS.length + RHS.length;
    }

    public void sortSides(){
        if (!sorted) {
            Arrays.sort(LHS);
            Arrays.sort(RHS);
            sorted = true;
        }
    }

////    Get hash code of this object
//    @Override
//    public int hashCode(){
//        sortSides();
//        FastArrayList<Integer> ids = new FastArrayList(LHS.length + RHS.length + 1);
//        ids.addAll(LHS);
//        ids.add(-1);
//        ids.addAll(RHS);
//        return ids.hashCode();
//    }


    @Override
    public boolean equals(Object other){
        if (other == null) return false;
        ResultTuple otherTuple = (ResultTuple) other;

//        Make sure sides are same size
        if (LHS.length != otherTuple.LHS.length || RHS.length != otherTuple.RHS.length){
            return false;
        }

//        Sort sides of both tuples
        this.sortSides();
        otherTuple.sortSides();

        boolean baseEqual = Arrays.equals(LHS, otherTuple.LHS) && Arrays.equals(RHS, otherTuple.RHS);

        if (LHS.length != RHS.length){
            return baseEqual;
        } else {
            boolean reverseEqual = Arrays.equals(LHS, otherTuple.RHS) && Arrays.equals(RHS, otherTuple.LHS);
            return baseEqual || reverseEqual;
        }
    }

    public int compareTo(ResultTuple other){
        if (other == null) return 1;
        if (similarity > other.similarity) return 1;
        if (similarity < other.similarity) return -1;
        return 0;
    }
}
