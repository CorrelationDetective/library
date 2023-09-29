package bounding;

import _aux.lib;
import _aux.lists.FastArrayList;
import core.RunParameters;
import org.apache.commons.math3.util.FastMath;
import queries.ResultObject;
import queries.ResultTuple;
import clustering.Cluster;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import similarities.MultivariateSimilarityFunction;

import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ClusterCombination implements ResultObject {
    @NonNull @Getter private Cluster[] LHS;
    @NonNull @Getter private Cluster[] RHS;
    @NonNull @Getter private int level;
    @NonNull private long size;
    @NonNull boolean allowVectorOverlap;
    private int cardinality;
    private int hashCode = -1;

    @Getter @Setter private FastArrayList<EmpiricalBoundFactor> empiricalBoundFactors;

    @Setter @Getter boolean isPositive = false;
    @Setter @Getter boolean isDecisive = false;
    private Boolean isSingleton;
    private Cluster[] clusters;


    @Setter @Getter boolean bounded = false;
    @Setter @Getter boolean discounted = false;
    @Setter @Getter ClusterBounds bounds;
    @Getter double criticalShrinkFactor = Double.MAX_VALUE;

    @Getter Double maxSubsetSimilarity;
    FastArrayList<ClusterCombination> subsetCCs;


//    ------------------- METHODS -------------------
    public long size() {
        return size;
    }

    public double computeSize(){return Arrays.stream(this.getClusters()).map(Cluster::size).reduce((a,b) -> a*b).orElse(0);}

    public int getCardinality(){
        if (this.cardinality == 0){
            this.cardinality = LHS.length + RHS.length;
        }
        return this.cardinality;
    }

    public double getSimilarity(){ return bounds.getLB(); }

    public Double getUB(){
        if (bounds == null) return null;
        return bounds.getUB();
    }

    public Double getLB(){
        if (bounds == null) return null;
        return bounds.getLB();
    }

    public void clearBounds(){
        setBounded(false);
        setBounds(null);
        setEmpiricalBoundFactors(null);
    }

    @Override
    public boolean equals(Object other){
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof ClusterCombination))return false;
        ClusterCombination otherCC = (ClusterCombination) other;

//        Sort both sides
//        this.sortSides(false);
//        otherCC.sortSides(false);
        return Arrays.equals(this.getLHS(),otherCC.getLHS()) && Arrays.equals(this.getRHS(),otherCC.getRHS());
    }

    @Override
    public String toString(){
        return Arrays.stream(LHS).map(Cluster::toString).collect(Collectors.joining(",")) + " | " +
                Arrays.stream(RHS).map(Cluster::toString).collect(Collectors.joining(","));
    }

    public void bound(RunParameters runParameters){
        (new RecursiveBoundingTask(this, runParameters)).compute();
    }

//    @Override
//    public int hashCode(){
//        if (this.hashCode != -1) return this.hashCode;
//
//        int[] ids = new int[LHS.length + RHS.length + 1];
//        int i = 0;
//        for (Cluster c : LHS) {
//            ids[i++] = c.id;
//        }
//        if (RHS.length > 0) {
//            ids[i++] = -1; // add for side separation
//            for (Cluster c : RHS) {
//                ids[i++] = c.id;
//            }
//        }
//
//        this.hashCode = Arrays.hashCode(ids);
//        return this.hashCode;
//    }

    public static long hashClusterList(long globalClusterId, Cluster... clusters){
        long hash = 0;
        long pow = 1;
        for (int i = 0; i < clusters.length; i++) {
//            Add 1 to correct for root cluster with id 0
            hash += (clusters[i].id + 1) * pow;
            pow *= globalClusterId;
        }
        return hash;
    }

    public int hashCode(long globalClusterId){
        if (this.hashCode != -1) return this.hashCode;
        this.hashCode = (int) hashClusterList(globalClusterId, this.getClusters());
        return this.hashCode;
    }

    public void addEmpiricalBoundFactor(EmpiricalBoundFactor factor){
        if (this.isBounded()) return; // already bounded before

        if (this.empiricalBoundFactors == null){
            int nClusters = this.getClusters().length;
            int nPairs = nClusters * nClusters;
            this.empiricalBoundFactors = new FastArrayList<>(nPairs);
        }
        this.empiricalBoundFactors.add(factor);
    }

    public boolean containsLeft(int... I){
        FastArrayList<Cluster> tmp = new FastArrayList<>(Arrays.asList(this.LHS));
        outerloop:
        for (int i : I) {
            for (Cluster c: tmp){
                if (c.contains(i)){
                    tmp.remove(c);
                    continue outerloop;
                }
            }
            return false;
        }
        return true;
    }

    public boolean containsRight(int... I){
        FastArrayList<Cluster> tmp = new FastArrayList<>(Arrays.asList(this.RHS));
        outerloop:
        for (int i : I) {
            for (Cluster c: tmp){
                if (c.contains(i)){
                    tmp.remove(c);
                    continue outerloop;
                }
            }
            return false;
        }
        return true;
    }

    public ClusterCombination clone() {
        ClusterCombination cc = new ClusterCombination(Arrays.copyOf(this.LHS, this.LHS.length), Arrays.copyOf(this.RHS, this.RHS.length),
                this.level, this.size, this.allowVectorOverlap);
        cc.setPositive(this.isPositive);
        cc.setDecisive(this.isDecisive);
        cc.updateBounds(this.bounds.clone());
        return cc;
    }

    public ClusterCombination getMirror(){
        ClusterCombination cc = new ClusterCombination(Arrays.copyOf(this.RHS, this.RHS.length), Arrays.copyOf(this.LHS, this.LHS.length),
                this.level, this.size, this.allowVectorOverlap);
        cc.setPositive(this.isPositive);
        cc.setDecisive(this.isDecisive);
        cc.updateBounds(this.bounds.clone());
        return cc;
    }

    public boolean isSingleton(){
        if (this.isSingleton == null){
            for (Cluster c : this.getClusters()){
                if (c.size() > 1){
                    this.isSingleton = false;
                    return false;
                }
            }
            this.isSingleton = true;
        }
        return this.isSingleton;
    }

    public Cluster[] getClusters(){
        if (this.clusters == null){
            this.clusters = Arrays.copyOf(this.LHS, this.LHS.length + this.RHS.length);
            System.arraycopy(this.RHS, 0, this.clusters, this.LHS.length, this.RHS.length);
        }
        return this.clusters;
    }

    public double getRadiiGeometricMean(){
        double out = 1;
        for(Cluster c: this.getClusters()){
            out *= c.getRadius();
        }
        return FastMath.pow(out, 1.0/this.getClusters().length);
    }

    public double getShrunkUB(double shrinkFactor, double maxApproximationSize){
        if(!this.isSingleton() && this.getRadiiGeometricMean() < maxApproximationSize){
            return bounds.getCenterOfBounds() + shrinkFactor * (bounds.getUB() - bounds.getCenterOfBounds());
        }else{
            return bounds.getUB();
        }
    }

    public void setCriticalShrinkFactor(double threshold){
        this.criticalShrinkFactor = (threshold - bounds.getCenterOfBounds()) / (bounds.getUB() - bounds.getCenterOfBounds());
    }

    public void updateBounds(ClusterBounds newBounds){
        if (this.bounds != null){
            this.bounds.update(newBounds);
        } else{
            this.bounds = newBounds;
        }
    }

    public void sortSides(boolean ascending){
        if (ascending){
            Arrays.sort(this.LHS);
            Arrays.sort(this.RHS);
        }else{
            Arrays.sort(this.LHS, (c1, c2) -> Integer.compare(c2.id, c1.id));
            Arrays.sort(this.RHS, (c1, c2) -> Integer.compare(c2.id, c1.id));
        }
    }

    public ClusterCombination expand(Cluster c, boolean expandLeft, boolean newCC){
//        If array is full, create new array with one more element, otherwise use same array
        Cluster[] newLHS = expandLeft ? Arrays.copyOf(LHS, LHS.length+1): LHS;
        Cluster[] newRHS = expandLeft ? RHS: Arrays.copyOf(RHS, RHS.length+1);

        if (expandLeft) newLHS[LHS.length] = c;
        else newRHS[RHS.length] = c;

        if (newCC) return new ClusterCombination(newLHS, newRHS, this.level + 1, this.size * c.size(), this.allowVectorOverlap);
        else{
            this.LHS = newLHS;
            this.RHS = newRHS;
            this.level++;
            this.size += c.size();
            return this;
        }
    }

    public ClusterCombination reduce(int i, boolean reduceLeft){
        Cluster[] newLHS = reduceLeft ? new Cluster[LHS.length - 1] : LHS;
        Cluster[] newRHS = reduceLeft ? RHS : new Cluster[RHS.length - 1];

        Cluster[] out = reduceLeft ? LHS : RHS;
        int j = 0;
        for (int k = 0; k < out.length; k++) {
            if (k != i){
                if (reduceLeft) newLHS[j] = out[k];
                else newRHS[j] = out[k];
                j++;
            }
        }

        return new ClusterCombination(newLHS, newRHS, this.level - 1, this.size / out.length, this.allowVectorOverlap);
    }

    public FastArrayList<ClusterCombination> getSubsetCCs(){
        if (this.subsetCCs == null){
            this.subsetCCs = new FastArrayList<>(this.LHS.length + this.RHS.length);
            if (this.LHS.length > 1){
                for (int i = 0; i < this.LHS.length; i++) {
                    this.subsetCCs.add(this.reduce(i, true));
                }
            }
            if (this.RHS.length > 1){
                for (int i = 0; i < this.RHS.length; i++) {
                    this.subsetCCs.add(this.reduce(i, false));
                }
            }
        }
        return this.subsetCCs;
    }

    //    Find the maximum similarity of one of the subsets of this cluster combination
    public double computeMaxSubsetSimilarity(MultivariateSimilarityFunction simMetric){
        if (maxSubsetSimilarity == null){
            double subsetSimilarity;
            maxSubsetSimilarity = -Double.MAX_VALUE;

            for (ClusterCombination subCC : this.getSubsetCCs()){
                simMetric.bound(subCC);

                subsetSimilarity = subCC.bounds.getLB();
                if (FastMath.abs(subsetSimilarity - subCC.bounds.getUB()) > 0.001){
                    Logger.getGlobal().fine("Subset similarity is not tight: " + subsetSimilarity + " " + subCC.bounds.getUB());
                }

                maxSubsetSimilarity = FastMath.max(maxSubsetSimilarity, subsetSimilarity);
                maxSubsetSimilarity = FastMath.max(maxSubsetSimilarity, subCC.computeMaxSubsetSimilarity(simMetric));
            }
        }

        return maxSubsetSimilarity;
    }

    public static int getBreakCluster(Cluster... clusters){
        int cToBreak = 0;
        double maxRadius = -1;
        for (int i = 0; i < clusters.length; i++) {
            Cluster c = clusters[i];
            if (c.size() == 1) continue;
            if (c.getRadius() > maxRadius){// always break leftmost cluster with largest radius
                maxRadius = c.getRadius();
                cToBreak = i;
            }
        }
        return cToBreak;
    }

    /**
     *   No same side overlap (e.g. no X | (A,A) ) -- note that this means we also not consider X | (A,B,A), which might be interesting, but out of scope here
     *   Ensure by keeping sides in descending order on id
     */
    public static boolean hasSingleSideOverlap(Cluster sc, Cluster[] currSide, int newPos){
//        Dupe left
        if (newPos > 0 && currSide[newPos - 1].id < sc.id) return true;
//        Dupe right
        if (newPos < currSide.length - 1 && currSide[newPos + 1].id > sc.id) return true;
        return false;
    }

    public static boolean hasSingleSideOverlap(Cluster[] LHS, Cluster[] RHS){
//        LHS in descending order
        if (LHS.length > 1){
            for (int i = 1; i < LHS.length; i++) {
                if (LHS[i-1].id < LHS[i].id) return true;
            }
        }
//        RHS in ascending order
        if (RHS.length > 1){
            for (int i = 1; i < RHS.length; i++) {
                if (RHS[i-1].id < RHS[i].id) return true;
            }
        }
        return false;
    }

    //  No same vector on same or both sides
    public static boolean hasVectorOverlap(Cluster sc, Cluster[] currSide, Cluster[] otherSide){
        return sc.size() == 1 && (lib.contains(currSide, sc) || lib.contains(otherSide, sc));
    }

    public static boolean hasVectorOverlap(Cluster[] LHS, Cluster[] RHS){
        Cluster[] all = lib.concat(LHS, RHS);

//        Check if no two clusters are the same
        for (int i = 0; i < all.length; i++) {
            for (int j = i + 1; j < all.length; j++) {
                Cluster c1 = all[i];
                Cluster c2 = all[j];
                if (c1.id == c2.id && c1.size() == 1 && c2.size() == 1) return true;
            }
        }
        return false;
    }

    public static boolean hasTwoSideOverlap(Cluster[] LHS, Cluster[] RHS){
//        Check if both sides are same length and if LHS[0].id > RHS[0].id
        if (LHS.length != RHS.length) return false;
        if (LHS[0].id < RHS[0].id) return false;
        return true;
    }

    /** Check if a potential new ClusterCombination is valid
     * @param sc: the new cluster to be added
     * @param currSide: the side to which the new cluster will be added
     * @param otherSide: the other side
     * @param newPos: the position at which the new cluster will be added
     * @param isLHS: whether the new cluster will be added to the left hand side or not
     * @return an integer indicating the type of violation;
     * - 0 if no violation is found
     * - 1 if violation but con continue
     * - 2 if violation and have to break
     */
    public static int symmetryChecks(Cluster sc, Cluster[] currSide, Cluster[] otherSide, int newPos, boolean isLHS, boolean allowVectorOverlap){
        if (hasSingleSideOverlap(sc, currSide, newPos)) return 2; // break because children will have even larger ids
        if (!allowVectorOverlap && hasVectorOverlap(sc, currSide, otherSide)) return 1;

        //    Check two side order -- (AB,CD) and (CD,AB), only pass the second (i.e., descending order on id)
        if (newPos == 0 && currSide.length == otherSide.length){
            //        (NEW, other)
            if (isLHS && sc.id < otherSide[0].id) return 1;
            //        (other, NEW)
            if (!isLHS && sc.id > otherSide[0].id) return 2; // break because other children will have even larger ids
        }
        return 0;
    }

    public static boolean symmetryChecks(Cluster[] LHS, Cluster[] RHS){
        if (hasSingleSideOverlap(LHS, RHS)) return false;
        if (hasVectorOverlap(LHS, RHS)) return false;
        if (hasTwoSideOverlap(LHS, RHS)) return false;
        return true;
    }


//    Split cluster combination into 'smaller' combinations by replacing the largest cluster with its children
    public FastArrayList<ClusterCombination> split(){
        int lSize = LHS.length;

//        Get cluster with largest radius and more than one point
        Cluster[] clusters = this.getClusters();
        int cToBreak = getBreakCluster(clusters);

        boolean isLHS = cToBreak < lSize;
        int scPos = isLHS ? cToBreak : cToBreak - lSize;

        Cluster[] oldSide = isLHS ? LHS : RHS;
        Cluster[] otherSide = isLHS ? RHS : LHS;

//        Cluster to split
        Cluster largest = oldSide[scPos];

        FastArrayList<Cluster> children = largest.getChildren();

        FastArrayList<ClusterCombination> subCCs = new FastArrayList<>(children.size());

//        For each subcluster, create a new cluster combination  (considering potential sideOverlap and weightOverlap)
        for (Cluster sc : children) {
            int symmetryCode = symmetryChecks(sc, oldSide, otherSide, scPos, isLHS, this.allowVectorOverlap);
            if (symmetryCode == 2) break;
            if (symmetryCode == 1) continue;

            //            Create new side
            Cluster[] newSide = Arrays.copyOf(oldSide, oldSide.length);
            newSide[scPos] = sc;

            Cluster[] newLHS = isLHS ? newSide : otherSide;
            Cluster[] newRHS = isLHS ? otherSide : newSide;

//            Create new cluster combination
            ClusterCombination newCC = new ClusterCombination(newLHS, newRHS,this.level + 1, this.size / largest.size() * sc.size(),
                    this.allowVectorOverlap);

//            Make sure to preset bounds so that they can only tighten
            if (this.bounds != null){
                newCC.updateBounds(this.bounds.clone());
            }

//            Make sure to mirror discounting
            newCC.setDiscounted(this.isDiscounted());

            subCCs.add(newCC);
        }
        return subCCs;
    }

//    Unpack CC to all cluster combinations with singleton clusters
    public FastArrayList<ClusterCombination> getSingletons(){
        if (this.isSingleton()) return new FastArrayList<>(Collections.singletonList(this));

        int nSingletons = Arrays.stream(this.getClusters()).mapToInt(Cluster::size).reduce((a,b) -> a*b).orElse(0);
        FastArrayList<ClusterCombination> out = new FastArrayList<>(nSingletons);
        FastArrayList<ClusterCombination> splitted = this.split();
        for (ClusterCombination sc : splitted) {
            out.addAll(sc.getSingletons());
        }

        return out;
    }



    public FastArrayList<ResultObject> unpackAndCheckConstraints(RunParameters runParameters){
        FastArrayList<ClusterCombination> singletons = this.getSingletons();
        return new FastArrayList<>(singletons.stream()
                .filter(cc -> {
                    if (!cc.isBounded()) runParameters.getSimMetric().bound(cc);

                    if (FastMath.abs(cc.bounds.getLB() - cc.bounds.getUB()) > 0.001) {
                        Logger.getGlobal().info("postprocessing: found a singleton CC with LB != UB");
                        return false;
                    }

                    double threshold = runParameters.getRunningThreshold().get();

                    //        Update threshold based on minJump and irreducibility if we have canCC > 2
                    if (LHS.length + RHS.length > 2 && (runParameters.getMinJump() > 0 || runParameters.isIrreducibility())) {
                        double subsetSim = cc.computeMaxSubsetSimilarity(runParameters.getSimMetric());
                        double jumpBasedThreshold = subsetSim + runParameters.getMinJump();
                        double irrBasedThreshold = runParameters.isIrreducibility() && subsetSim >= threshold ? Double.MAX_VALUE : threshold;
                        threshold = FastMath.max(threshold, jumpBasedThreshold);
                        threshold = FastMath.max(threshold, irrBasedThreshold);
                    }

                    cc.setDecisive(true);
                    cc.setPositive(cc.bounds.getLB() >= threshold);
                    return cc.isPositive();
                })
                .collect(Collectors.toList()));
    }

    public ResultTuple toResultTuple(String[] headers){
//        Check if singleton, otherwise raise error
        int[] LHSIndices = Arrays.stream(LHS).mapToInt(c -> c.centroidIdx).toArray();
        int[] RHSIndices = Arrays.stream(RHS).mapToInt(c -> c.centroidIdx).toArray();

        if (this.isSingleton()){
            ResultTuple res = new ResultTuple(
                    LHSIndices,
                    RHSIndices,
                    this.bounds.getLB()
            );
            res.setLHeaders(Arrays.stream(LHSIndices).mapToObj(i -> headers[i]).toArray(String[]::new));
            res.setRHeaders(Arrays.stream(RHSIndices).mapToObj(i -> headers[i]).toArray(String[]::new));
            res.setTimestamp(this.bounds.getTimestamp());
            return res;
        } else {
            throw new IllegalArgumentException("Cluster combination is not a singleton");
        }

    }
}
