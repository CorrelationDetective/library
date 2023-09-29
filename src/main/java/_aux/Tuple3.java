package _aux;

import lombok.Getter;
import lombok.Setter;

public class Tuple3<X, Y, Z> extends Object {
    @Getter @Setter public X x;
    @Getter @Setter public Y y;
    @Getter @Setter public Z z;

    public Tuple3(X x, Y y, Z z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

}