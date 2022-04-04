package program;

import java.util.*;

public class TypeMap extends HashMap<VariableRef, Type> {

	public void display (Functions f) {
		String sep = "  ";
		String sep2 = "";

		for (VariableRef key : keySet() ) {
			if (f != null && f.contains(key)) {
				System.out.print(sep + "<" + key +"(");
				
				Function func = f.get(f.indexOf(key));
				if (func.params != null) {
					for (int a = 0; a < func.params.size(); a++) {
						System.out.print(sep2 + "<" + func.params.get(a).v + ", " + func.params.get(a).t.id + ">");
						sep2 = ", ";
					}
				}
				
				System.out.print("), " + get(key).getId() + ">");
			} else {
				System.out.print(sep + "<" + key + ", " + get(key).getId() + ">");
			}
			sep = ",\n  ";
			sep2 = "";
		}

		System.out.println("\n}\n");
	}
}