import java.util.*;


public class Aho {
	
	String[] strs;
	int N;
	String[] subs;
	int[][] to;
	int[][] ends;
	
	Aho(String[] strs) {
		this.strs = strs;
		TreeMap<String, Integer> ids = new TreeMap<String, Integer>();
		ids.put("", 0);
		ArrayList<String> subs = new ArrayList<String>();
		subs.add("");
		for (String str : strs) {
			for (int i = 0; i < str.length(); i++) {
				String sub = str.substring(0, i + 1);
				if (!ids.containsKey(sub)) {
					subs.add(sub);
					ids.put(sub, ids.size());
				}
			}
		}
		this.subs = subs.toArray(new String[0]);
		N = ids.size();
		to = new int[N][128];
		for (String s : ids.keySet()) {
			for (int i = 0; i < 128; i++) {
				String t = s + (char)i;
				while (!ids.containsKey(t)) t = t.substring(1);
				to[ids.get(s)][i] = ids.get(t);
			}
		}
		ends = new int[N][];
		for (String s : ids.keySet()) {
			ArrayList<Integer> list = new ArrayList<Integer>();
			for (int i = 0; i < strs.length; i++) {
				if (s.endsWith(strs[i])) {
					list.add(i);
				}
			}
			ends[ids.get(s)] = Utils.toi(list.toArray(new Integer[0]));
		}
	}
	
	int[] getScore(int a, int used) {
		int score = 0, used2 = used;
		for (int e : ends[a]) {
			if ((used2 >> e & 1) == 0) {
				score += 300;
				used2 |= 1 << e;
			}
			score += strs[e].length() * 2;
		}
		return new int[] {score, used2};
	}
	
}
