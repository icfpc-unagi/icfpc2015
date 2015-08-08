import static java.lang.Math.*;
import static java.util.Arrays.*;

import java.util.*;

import com.google.gson.*;

public class Main {
	Scanner sc = new Scanner(System.in);
	
	boolean DEBUG = false;
	
	String input;
	int tle = 10000;
	int mle = 10000;
	ArrayList<String> phrases = new ArrayList<String>();
	
	void run() {
		Input in = Input.read(input);
		ArrayList<Output> output = new ArrayList<Output>();
		long sum = 0;
		for (int seed : in.sourceSeeds) {
			LCG lcg = new LCG(seed);
			int[] sources = new int[in.sourceLength];
			for (int i = 0; i < sources.length; i++) {
				sources[i] = lcg.next() % in.units.length;
			}
			Game game = new Game();
			game.DEBUG = DEBUG;
			game.setup(in.units, in.width, in.height, in.filled, sources, phrases.toArray(new String[0]));
			Pair<String, Long> res = game.solve();
			System.err.printf("Score = %d%n", res.second);
			output.add(new Output(in.id, seed, game.getTag() + "@" + res.second, res.first));
			sum += res.second;
		}
		Gson gson = new Gson();
		System.out.println(gson.toJson(output));
		System.err.println(round((double)sum / in.sourceSeeds.length));
	}
	
	public static void main(String[] args) {
		Main main = new Main();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-f")) {
				main.input = args[++i];
			} else if (args[i].equals("-t")) {
				main.tle = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-m")) {
				main.mle = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-p")) {
				String s = args[++i];
				if (s.startsWith("\"")) s = s.substring(1, s.length() - 1);
				main.phrases.add(s);
			} else if (args[i].equals("-d")) {
				main.DEBUG = true;
			} else {
				System.err.printf("illegal parameter: %n", args[i]);
			}
		}
		main.run();
	}
}
