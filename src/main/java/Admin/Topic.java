package Admin;

import java.io.Serializable;
import java.util.Objects;

public class Topic implements Serializable {
    private String name;

    public Topic(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Topic topic = (Topic) obj;
        return Objects.equals(name, topic.name);
    }

}
