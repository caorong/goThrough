package wong.spance.gothrough.process;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by spance on 14/6/4.
 */
public class ProcessState {

    private List<State> states;

    public ProcessState() {
        states = new ArrayList<State>();
    }

    public void record(int d1, int d2, Class<? extends ProcessRule> rule) {
        states.add(new State(d1, d2, rule.getSimpleName()));
    }

    @Override
    public String toString() {
        return "ProcessState" + states;
    }

    public static class State {

        private final int d1;
        private final int d2;
        private final String rule;

        public State(int d1, int d2, String rule) {
            this.d1 = d1;
            this.d2 = d2;
            this.rule = rule;
        }

        public int diff() {
            return d2 - d1;
        }

        @Override
        public String toString() {
            return rule + "[" + diff() + "]";
        }
    }

}
