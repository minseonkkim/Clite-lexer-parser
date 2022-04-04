package program;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class State extends HashMap<VariableRef, Value> {

	public State( ) { }

	public State(VariableRef key, Value val) {
		this.put(key, val);
	}

	public State update(VariableRef key, Value val) {
		this.put(key, val);
		return this;
	}

	public State update(VariableRef key, int id, Value val) {
		this.put(key, val);
		return this;
	}

	public State update (State t) {
		for (VariableRef key : t.keySet( ))
			put(key, t.get(key));
		return this;
	}

	public void display( ) {
		System.out.print("{ ");
		String sep = "";
		for (VariableRef key : keySet( )) {
			System.out.print(sep + "<" + key + ", " + get(key) + ">");
			sep = ", ";
		}
		System.out.println(" }");
	}
}