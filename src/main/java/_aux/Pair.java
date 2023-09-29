package _aux;

import lombok.Getter;

public class Pair<X, Y> extends Object {
    public @Getter X x;
    public @Getter Y y;

    public Pair(X x, Y y) {
        this.x = x;
        this.y = y;
    }

    public String toString(){
        return "(" + x.toString() + "," + y.toString() + ")";
    }
    public void setX(X x){this.x = x;}
    public void setY(Y y){this.y = y;}

}