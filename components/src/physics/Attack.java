package physics;

import com.artemis.Component;

import java.io.Serializable;

public class Attack extends Component implements Serializable {

    public static float DEFAULT_INTERVAL = 0.8f;

    public float interval = DEFAULT_INTERVAL;

    public Attack() {
    }
}
