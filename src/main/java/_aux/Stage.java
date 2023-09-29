package _aux;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class Stage {
    @Expose @NonNull @Getter @Setter public String name;
    @Expose @NonNull @Getter @Setter public Double duration;
    @Expose @Getter @Setter public Double expectedDuration;

}