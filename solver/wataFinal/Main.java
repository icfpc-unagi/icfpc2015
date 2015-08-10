import static java.lang.Math.*;
import static java.util.Arrays.*;

import java.util.*;

import com.google.gson.*;

public class Main {
	Scanner sc = new Scanner(System.in);
	
	double sTime;
	
	boolean DEBUG = false;
	
	String input;
	int tle = 60;
	int mle = 10000;
	ArrayList<String> phrases = new ArrayList<String>();
	
	void run() {
		Input in = Input.read(input);
		System.err.printf("caseN = %d%n", in.sourceSeeds.length);
		ArrayList<Output> output = new ArrayList<Output>();
		long sum = 0;
		for (int seed : in.sourceSeeds) {
			LCG lcg = new LCG(seed);
			int[] sources = new int[in.sourceLength];
			for (int i = 0; i < sources.length; i++) {
				sources[i] = lcg.next() % in.units.length;
			}
			Game game = new Game();
			game.TLE = max(1, (double)(tle - (Utils.getTime() - sTime) - 0.3) / in.sourceSeeds.length);
			System.err.printf("TLE = %.3f%n", game.TLE);
			game.DEBUG = DEBUG;
			game.setup(in.units, in.width, in.height, in.filled, sources, phrases.toArray(new String[0]));
			try (Stat st = new Stat("solve")) {
				Pair<String, Long> res = game.solve();
				System.err.printf("Score = %d%n", res.second);
				output.add(new Output(in.id, seed, game.getTag() + "@" + res.second, res.first));
				sum += res.second;
			}
		}
		Gson gson = new Gson();
		System.out.println(gson.toJson(output));
		System.err.println(round((double)sum / in.sourceSeeds.length));
		System.err.printf("Time = %.3f%n", Utils.getTime() - sTime);
	}
	
	public static void main(String[] args) {
		Main main = new Main();
		main.sTime = Utils.getTime();
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
				main.phrases.add(s.toLowerCase());
			} else if (args[i].equals("-d")) {
				main.DEBUG = true;
			} else {
				System.err.printf("illegal parameter: %n", args[i]);
			}
		}
		main.run();
	}
}
