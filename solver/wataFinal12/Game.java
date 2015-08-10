import static java.lang.Math.*;
import static java.util.Arrays.*;

import java.util.*;


public class Game {
	
	boolean DEBUG = false;
	int SOLVE_TYPE = 1; // 0: P, 1: P+N, 2: N->P
	int EVAL_TYPE = 2; // 0: aggressive, 1: chokudai, 2: clear
	
	String getTag() {
		return "wataMiniSAT";
	}
	
	// W, E, SW, SE, CW, CCW
	static int[] dx = {-2, 2, -1, 1, 0, 0};
	static int[] dy = {0, 0, 1, 1, 0, 0};
	static int[] dr = {0, 0, 0, 0, 1, -1};
	static char[][] COMMANDS = {	"p'!.03".toCharArray(),
									"bcefy2".toCharArray(),
									"aghij4".toCharArray(),
									"lmno 5".toCharArray(),
									"dqrvz1".toCharArray(),
									"kstuwx".toCharArray() };
	static int[] DIR = new int[128];
	static {
		fill(DIR, -1);
		for (int i = 0; i < 6; i++) for (char c : COMMANDS[i]) DIR[c] = i;
	}
	
	int width;
	int height;
	String[] phrases;
	Cell[] init;
	Unit[][] units;
	int[] sources;
	State initialState;
	Aho aho;
	char[][][] rCommands;
	boolean[][] death;
	
	int W1 = 10, W2 = 100;
	int maxSize;
	
	/************
	 * Initialize
	 ************/
	
	void setup(Unit[] units, int width, int height, Cell[] filled, int[] sources, String[] phrases, int seedLength) {
		this.units = genUnit(units);
		this.width = width;
		this.height = height;
		this.sources = sources;
		this.phrases = phrases;
		initialState = new State(filled);
		init = new Cell[units.length];
		for (int i = 0; i < units.length; i++) {
			init[i] = genInit(this.units[i][0]);
		}
		aho = new Aho(phrases);
		rCommands = new char[6][aho.N][];
		for (int r = 0; r < 6; r++) {
			for (int a = 0; a < aho.N; a++) {
				char[] cs = new char[6];
				int p = 0;
				boolean[] used = new boolean[aho.N];
				used[0] = true;
				for (char c : COMMANDS[r]) {
					if (!used[aho.to[a][c]]) {
						used[aho.to[a][c]] = true;
						cs[p++] = c;
					}
				}
				if (p == 0) cs[p++] = COMMANDS[r][0];
				rCommands[r][a] = copyOf(cs, p);
			}
		}
		for (int u : sources) maxSize = max(maxSize, this.units[u][0].members.length);
		if (maxSize == 1) System.err.println("one!");
		double avgRot = 0;
		for (int u : sources) avgRot += this.units[u].length;
		avgRot /= sources.length;
		double size = width * height * sources.length * seedLength * avgRot;
//		double size = width * height * sources.length * avgRot;
		W1 = max(1, (int)round(1e6 / size));
		W2 = W1 * 10;
		death = new boolean[height][width];
		for (int i = 0; i < this.units.length; i++) {
			Cell p = init[i];
			for (Cell c : this.units[i][0].members) {
				death[c.y + p.y][(c.x + p.x) / 2] = true;
			}
		}
		System.err.printf("size = %.2e, W1 = %d%n", size, W1);
	}
	
	Unit[][] genUnit(Unit[] units) {
		Unit[][] res = new Unit[units.length][];
		for (int i = 0; i < res.length; i++) res[i] = genUnit(units[i]);
		return res;
	}
	
	Unit[] genUnit(Unit unit) {
		Unit[] res = new Unit[6];
		Unit init = new Unit();
		init.pivot = new Cell(0, 0);
		init.members = new Cell[unit.members.length];
		for (int i = 0; i < unit.members.length; i++) {
			int x = unit.members[i].x * 2 + unit.members[i].y % 2 - (unit.pivot.x * 2 + unit.pivot.y % 2);
			int y = unit.members[i].y - unit.pivot.y;
			init.members[i] = new Cell(x, y);
		}
		Unit crt = init;
		Cell[] inits = init.members.clone();
		sort(inits);
		for (int i = 0; i < 6; i++) {
			res[i] = crt;
			crt = rot(crt);
			Cell[] cells = crt.members.clone();
			sort(cells);
			boolean same = true;
			for (int j = 0; j < cells.length; j++) if (cells[j].compareTo(inits[j]) != 0) {
				same = false;
				break;
			}
			if (same) {
				res = copyOf(res, i + 1);
				break;
			}
		}
		return res;
	}
	
	Cell genInit(Unit unit) {
		int top = 0;
		for (int i = 0; i < unit.members.length; i++) {
			if (unit.members[top].y > unit.members[i].y) {
				top = i;
			}
		}
		int x = 0;
		if (unit.members[top].x % 2 != 0) {
			x = 1;
		}
		Cell c = new Cell(x, -unit.members[top].y);
		for (;;) {
			int left = div2(unit.members[top].x + c.x), right = left;
			for (Cell d : unit.members) {
				left = min(left, div2(d.x + c.x));
				right = max(right, div2(d.x + c.x));
			}
			if (left < width - right - 2) {
				c.x += 2;
			} else if (left > width - right - 1) {
				c.x -= 2;
			} else {
				break;
			}
		}
		return c;
	}
	
	int div2(int x) {
		if (x % 2 != 0) return (x - 1) / 2;
		return x / 2;
	}
	
	Unit rot(Unit unit) {
		Unit ret = new Unit();
		ret.members = new Cell[unit.members.length];
		ret.pivot = new Cell(0, 0);
		for (int i = 0; i < ret.members.length; i++) {
			ret.members[i] = new Cell((unit.members[i].x - 3 * unit.members[i].y) / 2, (unit.members[i].x + unit.members[i].y) / 2);
		}
		return ret;
	}
	
	boolean onBoard(int x, int y) {
		return 0 <= x && x < width * 2 && 0 <= y && y < height;
	}
	
	/************
	 * AI
	 ************/
	
	/**
	 * (commands, score)
	 */
	Pair<String, Long> solve() {
		if (SOLVE_TYPE == 0) return solve0();
		if (SOLVE_TYPE == 1) return solve1();
		if (SOLVE_TYPE == 2) return solve2();
		return null;
	}

	Pair<String, Long> solve0() {
		ArrayList<State> crt = new ArrayList<>();
		crt.add(initialState);
		State best = initialState;
		for (int source = 0; source < sources.length && crt.size() > 0; source++) {
			System.err.printf("\r%03d/%03d %d %.1f %s", source, sources.length, crt.get(0).finalScore, crt.get(0).expScore, crt.get(0).commands);
			ArrayList<State> next = new ArrayList<>();
			crt.get(0).vis();
			for (State s : crt) {
				s.nextStates(next, W1);
			}
			crt = next;
			for (State s : crt) {
				if (best.finalScore < s.finalScore) {
					best = s;
				}
			}
		}
		StringBuilder commands = new StringBuilder();
		ArrayList<State> list = new ArrayList<Game.State>();
		for (State s = best; s != null; s = s.prev) {
			if (s.prev != null) commands.append(s.commands);
			list.add(s);
		}
//		if (DEBUG) {
//			for (int i = list.size() - 1; i > 0; i--) {
//				if (i < list.size() - 1) System.err.println(list.get(i).finalScore + " @ " + new StringBuilder(list.get(i).commands).reverse().toString());
//				list.get(i).vis();
//			}
//		}
		System.err.println("Used phrases: ");
		for (int i = 0; i < phrases.length; i++) if ((best.usedPhrase >> i & 1) != 0) {
			System.err.println(phrases[i]);
		}
		return Pair.make(commands.reverse().toString(), best.finalScore);
	}
	
	Pair<String, Long> solve1() {
		ArrayList<State> crt = new ArrayList<>();
		crt.add(initialState);
		State best = initialState;
		for (int source = 0; source < sources.length && crt.size() > 0; source++) {
			System.err.printf("\r%03d/%03d %d %.1f %s", source, sources.length, crt.get(0).finalScore, crt.get(0).expScore, crt.get(0).commands);
			ArrayList<State> next = new ArrayList<>();
			crt.get(0).vis();
			for (State s : crt) {
				s.nextStatesNoPhrase(next, W2);
			}
			crt.clear();
			for (State s : next) {
				if (!s.prev.used) {
					s.prev.used = true;
					crt.add(s.prev);
				}
				if (crt.size() >= W1) break;
			}
			ArrayList<State> next2 = new ArrayList<State>();
//			next.clear();
			for (State s : crt) {
				s.nextStates(next2, W2);
			}
			for (State s : next) {
				if (!s.prev.used) {
					insert(next2, s);
					if (next2.size() > W2) next2.remove(W2);
				}
			}
			crt = next2;
			for (State s : crt) {
				if (best.finalScore < s.finalScore) {
					best = s;
				}
			}
		}
		StringBuilder commands = new StringBuilder();
		ArrayList<State> list = new ArrayList<Game.State>();
		for (State s = best; s != null; s = s.prev) {
			if (s.prev != null) commands.append(s.commands);
			list.add(s);
		}
//		if (DEBUG) {
//			for (int i = list.size() - 1; i > 0; i--) {
//				if (i < list.size() - 1) System.err.println(list.get(i).finalScore + " @ " + new StringBuilder(list.get(i).commands).reverse().toString());
//				list.get(i).vis();
//			}
//		}
		System.err.println("Used phrases: ");
		for (int i = 0; i < phrases.length; i++) if ((best.usedPhrase >> i & 1) != 0) {
			System.err.println(phrases[i]);
		}
		return Pair.make(commands.reverse().toString(), best.finalScore);
	}
	
	Pair<String, Long> solve2() {
		ArrayList<State> crt = new ArrayList<>();
		crt.add(initialState);
		State best = initialState;
		for (int source = 0; source < sources.length && crt.size() > 0; source++) {
			System.err.printf("\r%03d/%03d %d %.1f %s", source, sources.length, crt.get(0).finalScore, crt.get(0).expScore, source == 0 ? "" : new StringBuilder(crt.get(0).commands).reverse().toString());
			ArrayList<State> next = new ArrayList<>();
			crt.get(0).vis();
			for (State s : crt) {
				s.nextStatesNoPhrase(next, W2);
			}
			crt = next;
			for (State s : crt) {
				if (best.finalScore < s.finalScore) {
					best = s;
				}
			}
		}
		StringBuilder commands = new StringBuilder();
		ArrayList<State> list = new ArrayList<Game.State>();
		for (State s = best; s != null; s = s.prev) {
			list.add(s);
		}
		
		State crtState = initialState;
		for (int source = 0; list.size() - 2 - source >= 0; source++) {
			System.err.printf("\r%02d/%02d", source, list.size() - 1);
			State s = list.get(list.size() - 2 - source);
			int rotN = units[sources[source]].length;
			int toX = init[sources[source]].x, toY = init[sources[source]].y, toRot = 0;
			for (char c : new StringBuilder(s.commands.substring(1)).reverse().toString().toCharArray()) {
				int op = DIR[c];
				toX += dx[op];
				toY += dy[op];
				toRot = (toRot + dr[op] + rotN) % rotN;
			}
			State nextState = crtState.spellPhrases(toX, toY, toRot);
			if (nextState == null) {
				Debug.print("orz");
				nextState = new State(crtState, toRot, toX, toY, 0, 0);
				nextState.commands = s.commands;
			}
			commands.append(new StringBuilder(nextState.commands).reverse().toString());
			crtState = nextState;
//			Debug.print(nextState);
		}
		System.err.println("Used phrases: ");
		for (int i = 0; i < phrases.length; i++) if ((crtState.usedPhrase >> i & 1) != 0) {
			System.err.println(phrases[i]);
		}
		return Pair.make(commands.toString(), crtState.finalScore);
	}
	
	static long A = 1000000007;
	
	class State implements Comparable<State> {
		
		int source;
		boolean[][] bs; // (y,x) 垂直座標
		long hash;
		long finalScore;
		int ls;
		double expScore;
		String commands;
		State prev;
		int ahoID;
		int usedPhrase;
		boolean used;
		
		State(Cell[] filled) {
			bs = new boolean[height][width * 2];
			for (Cell c : filled) bs[c.y][c.x * 2 + c.y % 2] = true;
			hash = getHash();
			usedPhrase = 0;
			ahoID = 0;
		}
		
		State(State s, int rot, int px, int py, int phraseScore, int ahoID) {
			source = s.source;
			bs = Utils.copy(s.bs);
			finalScore = s.finalScore + phraseScore;
			prev = s;
			this.ahoID = ahoID;
			update(rot, px, py);
			expScore = getScore();
			hash = getHash();
			usedPhrase = s.usedPhrase;
		}
		
		double getScore() {
			if (EVAL_TYPE == 0) return getScore0();
			if (EVAL_TYPE == 1) return getScore1();
			if (EVAL_TYPE == 2) return getScore2();
			throw null;
		}
		
		double getScore0() {
			expScore = finalScore;
			for (int y = 0; y < height; y++) {
				double num = 0;
				for (int x = 0; x < width; x++) {
					int x2 = x * 2 + y % 2;
					if (bs[y][x2]) {
						num += 1;
					} else {
						for (int d = 0; d < 4; d++) {
							if (onBoard(x2 - dx[d], y - dy[d]) && !bs[y - dy[d]][x2 - dx[d]]) {
								num += (d < 2 ? 0.1 : 0.2);
							}
						}
					}
				}
//				expScore += 100.0 * y / height * num * num / width / width;
//				expScore += 100.0 * pow((double)y / height, 0.2) * num * num / width / width * (1 + 5.0 * pow((double)(sources.length - source) / sources.length, 0.3));
//				expScore += 100.0 * pow((double)y / height, 0.1) * num * num / width / width * 0.5;//(1 + 5.0 * pow((double)(sources.length - source) / sources.length, 0.3));
//				expScore += 100.0 * y / height * num * num / width / width * (1 + 6.0 * pow((double)(sources.length - source) / sources.length, 0.2));
				if (maxSize == 1) {
					expScore += 100.0 * y / height * num * num / width / width;
				} else {
					expScore += 100.0 * y / height * num * num / width / width * (1 + 5.0 * pow((double)(sources.length - source) / sources.length, 0.3));
				}
			}
//			expScore += aho.subs[ahoID].length();
			return expScore;
		}
		
		double getScore1() {
			expScore = finalScore * 2;
            for (int i = 0; i < height; i++)
            {
                for (int j = 0; j < width * 2; j++)
                {
                    if (i % 2 != j % 2) continue;
                    if (!bs[i][j])
                    {
                        if (i != 0)
                        {
                            int myon = 0;
                            for (int k = -1; k <= 1; k += 2)
                            {
                                int ttx = j + k;
                                int tty = i - 1;
                                //if (!ok(tx, ty)) continue;
                                if (!onBoard(ttx, tty) || bs[tty][ttx])
                                {
                                    expScore -= (height - i) * 3;
                                }
                                else myon++;

                            }
                            if (myon == 0)
                            {
                                expScore -= (height - i) * 3;
                            }
                            for (int k = -2; k <= 2; k += 4)
                            {
                                int ttx = j + k;
                                int tty = i;
                                //if (!ok(tx, ty)) continue;
                                if (!onBoard(ttx, tty) || bs[tty][ttx])
                                {
                                    expScore -= (height - i) * 2;
                                }
                                else myon++;
                            }

                            if (myon == 0)
                            {
                                expScore -= (height - i) * 2;
                            }

                            //nextstate.point -= (H - i) * (H - i);
                        }
                    }
                    else
                    {
                        //nextstate.point -= (H - i);
                        expScore -= (height - i) * (height - i);
                    }
                }
            }
            return expScore;
		}
		
		double getScore2() {
			expScore = finalScore;
			for (int y = height - 1; y >= 0; y--) {
				double num = 0, dead = 1;
				for (int x = 0; x < width; x++) {
					int x2 = x * 2 + y % 2;
					if (bs[y][x2]) {
						num += 1;
//						expScore -= 2.0 / width * (height - y);
						if (death[y][x]) expScore -= 100.0 * height;
					} else {
						int used = 0;
						for (int d = 0; d < 4; d++) {
							if (!onBoard(x2 - dx[d], y - dy[d]) || bs[y - dy[d]][x2 - dx[d]]) {
								if (maxSize > 1) {
									if (d < 2) dead *= 0.95;
									else dead *= 0.9;
								}
								used |= 1 << d;
							}
						}
						if ((used >> 2) == 3) dead *= 0.8;
						if (used == 0) expScore += 2.0 / width * (height - y);
					}
				}
				expScore += 100.0 * num * num / width / width * dead + 20.0 * (width - num) * (width - num) / width / width * (height - y) / height;
			}
//			expScore += aho.subs[ahoID].length();
			return expScore;
		}
		
		double _getScore() {
			expScore = finalScore;
			for (int y = 0; y < height; y++) {
				double num = 0, dead = 1;
				for (int x = 0; x < width; x++) {
					int x2 = x * 2 + y % 2;
					if (bs[y][x2]) {
						num += 1;
						expScore -= 2.0 / width * (height - y);
					} else {
						int used = 0;
						for (int d = 0; d < 4; d++) {
							if (!onBoard(x2 - dx[d], y - dy[d]) || bs[y - dy[d]][x2 - dx[d]]) {
								if (maxSize > 1) {
									if (d < 2) dead *= 0.98;
									else dead *= 0.95;
								}
								used |= 1 << d;
							}
						}
						if ((used >> 2) == 3) dead *= 0.9;
						if (used == 0) expScore += 2.0 / width;
					}
				}
				expScore += 100.0 * num * num / width / width * dead;
			}
//			expScore += aho.subs[ahoID].length();
			return expScore;
		}
		
		class DP {
			int x, y, r, a, dir;
			int score = -1;
			DP prev;
			String command;
			int used;
			boolean finished = false;
			DP(int x, int y, int r, int a, int dir) {
				this.x = x;
				this.y = y;
				this.r = r;
				this.a = a;
				this.dir = dir;
			}
			int ID() {
				return (((r * width + x) * height + y) * aho.N + a) * 3 + dir;
			}
			void update(DP dp, int score2, String command, int used2) {
				if (dp != null) dp.finished = true;
				Debug.check(!finished);
				if (dp != null) Debug.check(score2 >= dp.score);
				if (score < score2) {
					score = score2;
					prev = dp;
					this.command = command;
					used = used2;
				}
			}
		}
		
		DP get(DP[] table, int x, int y, int r, int a, int dir) {
			int id = (((r * width + x) * height + y) * aho.N + a) * 3 + dir;
			if (table[id] == null) table[id] = new DP(x, y, r, a, dir);
			return table[id];
		}
		
		int getScore(DP[] table, int x, int y, int r, int a, int dir) {
			int id = (((r * width + x) * height + y) * aho.N + a) * 3 + dir;
			if (table[id] == null) return -1;
			return table[id].score;
		}
		
		Unit[] us;
		
		void nextStatesNoPhrase(ArrayList<State> list, int W) {
			Unit[] us = units[sources[source]];
			int rotN = us.length;
			Unit u = us[0];
			boolean[][][] can = new boolean[height][width][rotN]; // (y,x) ジグザグ座標
			int[][][] prev = new int[height][width][rotN];
			int[][][] lock = new int[height][width][rotN];
			int px = init[sources[source]].x, py = init[sources[source]].y;
			if (!canPlace(u, px, py)) return;
			can[u.members[0].y + py][(u.members[0].x + px) / 2][0] = true;
			prev[u.members[0].y + py][(u.members[0].x + px) / 2][0] = -1;
			Queue<int[]> que = new LinkedList<>();
			que.offer(new int[]{px, py, 0});
			while (!que.isEmpty()) {
				int[] e = que.poll();
				int x = e[0], y = e[1], rot = e[2];
				int lc = -1;
				for (int command = 0; command < 6; command++) {
					int x2 = x + dx[command], y2 = y + dy[command], rot2 = (rot + dr[command] + rotN) % rotN;
					if (canPlace(us[rot2], x2, y2)) {
						u = us[rot2];
						if (!can[u.members[0].y + y2][(u.members[0].x + x2) / 2][rot2]) {
							can[u.members[0].y + y2][(u.members[0].x + x2) / 2][rot2] = true;
							prev[u.members[0].y + y2][(u.members[0].x + x2) / 2][rot2] = command;
							que.offer(new int[]{x2, y2, rot2});
						}
					} else {
						lc = command;
					}
				}
				lock[us[rot].members[0].y + y][(us[rot].members[0].x + x) / 2][rot] = lc;
			}
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					for (int rot = 0; rot < rotN; rot++) {
						if (can[y][x][rot] && lock[y][x][rot] >= 0) {
							px = x * 2 + y % 2 - us[rot].members[0].x;
							py = y - us[rot].members[0].y;
							State s = new State(this, rot, px, py, 0, 0);
							double score = s.getScore();
							if (list.size() < W || list.get(W - 1).expScore < score) {
								s.commands = COMMANDS[lock[y][x][rot]][0] + getCommand(prev, rot, px, py);
								boolean hasSame = false;
								for (int i = 0; i < list.size(); i++) {
									if (list.get(i).same(s)) {
										hasSame = true;
										if (list.get(i).expScore < score) {
											list.remove(i);
											list.add(s);
											Collections.sort(list);
											break;
										}
									}
								}
								if (!hasSame) {
									list.add(s);
									Collections.sort(list);
									if (list.size() > W) list.remove(W);
								}
							}
						}
					}
				}
			}
		}
		
		String getCommand(int[][][] prev, int rot, int px, int py) {
			StringBuilder sb = new StringBuilder();
			Unit[] us = units[sources[source]];
			for (;;) {
				Unit u = us[rot];
				int c = prev[u.members[0].y + py][(u.members[0].x + px) / 2][rot];
				if (c < 0) break;
				sb.append(COMMANDS[c][0]);
				px -= dx[c];
				py -= dy[c];
				rot -= dr[c];
				rot %= us.length;
				if (rot < 0) rot += us.length;
			}
			return sb.toString();
		}
		
		void nextStates(ArrayList<State> list, int W) {
//			Debug.print(aho.subs[ahoID]);
			Unit[] us = units[sources[source]];
			this.us = us;
			int rotN = us.length;
			Unit u = us[0];
			int px = init[sources[source]].x, py = init[sources[source]].y;
			if (!canPlace(u, px, py)) return;
			DP[] dp = new DP[rotN * width * height * aho.N * 3];
			get(dp, (u.members[0].x + px) / 2, u.members[0].y + py, 0, ahoID, 0).update(null, 0, "", usedPhrase);
			get(dp, (u.members[0].x + px) / 2, u.members[0].y + py, 0, ahoID, 1).update(null, 0, "", usedPhrase);
			int minX = u.members[0].x, maxX = minX;
			for (int i = 0; i < rotN; i++) {
				minX = min(minX, us[i].members[0].x);
				maxX = max(maxX, us[i].members[0].x);
			}
			try (Stat st = new Stat("dp")) {
			for ( ; ; py++) {
				boolean finished = true;
				for (int r = 0; r < rotN; r++) if (us[r].members[0].y + py < height) finished = false;
				if (finished) break;
				for (int dx = -1; dx <= 1; dx += 2) {
					int dir = dx < 0 ? 1 : 0;
					if (dx > 0) {
						px = -maxX;
						if ((px + py) % 2 != 0) px++;
					} else {
						px = width * 2 - minX - 1;
						if ((px + py) % 2 != 0) px--;
					}
					for ( ; 0 <= px + maxX && px + minX < width * 2; px += dx * 2) {
						int[][] dp3 = new int[rotN][aho.N];
						DP[][] prev3 = new DP[rotN][aho.N];
						String[][] cmd3 = new String[rotN][aho.N];
						int[][] used3 = new int[rotN][aho.N];
						for (int r = 0; r < rotN; r++) fill(dp3[r], -1);
						for (int r = 0; r < rotN; r++) {
							int x = div2(us[r].members[0].x + px);
							int y = us[r].members[0].y + py;
							if (x < 0 || x >= width || y < 0 || y >= height) continue;
							for (int t = -1; t <= 1; t += 2) {
								int[][] dp2 = new int[rotN][aho.N];
								for (int i = 0; i < rotN; i++) fill(dp2[i], -1);
								DP[][] prev2 = new DP[rotN][aho.N];
								String[][] cmd2 = new String[rotN][aho.N];
								int[][] used2 = new int[rotN][aho.N];
								for (int a = 0; a < aho.N; a++) if (getScore(dp, x, y, r, a, dir) >= 0) {
									DP e = get(dp, x, y, r, a, dir);
									dp2[0][a] = e.score;
									prev2[0][a] = e;
									cmd2[0][a] = "";
									used2[0][a] = e.used;
								}
								for (int dr = 0; dr + 1 < rotN; dr++) {
									if (!canPlace(us[(r + (dr + 1) * t + rotN) % rotN], px, py)) break;
									for (int a = 0; a < aho.N; a++) if (dp2[dr][a] >= 0) {
										for (char c : rCommands[t < 0 ? 5 : 4][a]) {
											int a2 = aho.to[a][c];
											int[] ps = aho.getScore(a2, used2[dr][a]);
											int score = dp2[dr][a] + ps[0];
											if (dp2[dr + 1][a2] < score) {
												dp2[dr + 1][a2] = score;
												prev2[dr + 1][a2] = prev2[dr][a];
												cmd2[dr + 1][a2] = cmd2[dr][a] + c;
												used2[dr + 1][a2] = ps[1];
											}
										}
									}
								}
								for (int dr = 0; dr < rotN; dr++) {
									for (int a = 0; a < aho.N; a++) if (dp2[dr][a] >= 0) {
										int r2 = (r + dr * t + rotN) % rotN;
										if (dp3[r2][a] < dp2[dr][a]) {
											dp3[r2][a] = dp2[dr][a];
											prev3[r2][a] = prev2[dr][a];
											cmd3[r2][a] = cmd2[dr][a];
											used3[r2][a] = used2[dr][a];
										}
									}
								}
							}
						}
						for (int r = 0; r < rotN; r++) {
							for (int a = 0; a < aho.N; a++) if (dp3[r][a] >= 0) {
								int x2 = (us[r].members[0].x + px) / 2, y2 = us[r].members[0].y + py;
								get(dp, x2, y2, r, a, 2).update(prev3[r][a], dp3[r][a], cmd3[r][a], used3[r][a]);
								if (canPlace(us[r], px + 2 * dx, py)) {
									for (char c : rCommands[1 - dir][a]) {
										int a2 = aho.to[a][c];
										int[] ps = aho.getScore(a2, used3[r][a]);
										get(dp, x2 + dx, y2, r, a2, dir).update(prev3[r][a], dp3[r][a] + ps[0], cmd3[r][a] + c, ps[1]);
									}
								}
							}
						}
					}
				}
				for (int x = 0; x < width; x++) {
					for (int r = 0; r < rotN; r++) {
						int y = us[r].members[0].y + py;
						if (y < 0 || y >= height) continue;
						for (int a = 0; a < aho.N; a++) if (getScore(dp, x, y, r, a, 2) >= 0) {
							DP crt = get(dp, x, y, r, a, 2);
							for (int d = 2; d < 4; d++) {
								int x2 = div2(x * 2 + y % 2 + dx[d]), y2 = y + dy[d];
								int px2 = x2 * 2 + y2 % 2 - us[r].members[0].x;
								int py2 = y2 - us[r].members[0].y;
								if (canPlace(us[r], px2, py2)) {
									for (char c : rCommands[d][a]) {
										int a2 = aho.to[a][c];
										for (int i = 0; i < 2; i++) {
											int[] ps = aho.getScore(a2, crt.used);
											get(dp, x2, y2, r, a2, i).update(crt, crt.score + ps[0], "" + c, ps[1]);
										}
//										// !re -> [2]
//										if (rotN > 1) {
//											int px3 = px2, py3 = py2, r3 = r;
//											int a3 = a2, score = crt.score + aho.getScore(a2, usedPhrase);
//											String command = "" + c;
//											boolean ok = true;
//											for (char c2 : "!re".toCharArray()) {
//												px3 += dx[DIR[c2]];
//												py3 += dy[DIR[c2]];
//												r3 = (r3 + DIR[c2] + rotN) % rotN;
//												if (!canPlace(us[r3], px3, py3)) {
//													ok = false;
//													break;
//												}
//												a3 = aho.to[a3][c2];
//												score += aho.getScore(a3, usedPhrase);
//												command += c2;
//											}
//											if (ok) {
//												get(dp, (us[r3].members[0].x + px3) / 2, us[r3].members[0].y + py3, r3, a3, 2).update(crt, score, command);
//											}
//										}
//										// !ree -> [0]
//										if (rotN > 1) {
//											int px3 = px2, py3 = py2, r3 = r;
//											int a3 = a2, score = crt.score + aho.getScore(a2, usedPhrase);
//											String command = "" + c;
//											boolean ok = true;
//											for (char c2 : "!ree".toCharArray()) {
//												px3 += dx[DIR[c2]];
//												py3 += dy[DIR[c2]];
//												r3 = (r3 + DIR[c2] + rotN) % rotN;
//												if (!canPlace(us[r3], px3, py3)) {
//													ok = false;
//													break;
//												}
//												a3 = aho.to[a3][c2];
//												score += aho.getScore(a3, usedPhrase);
//												command += c2;
//											}
//											if (ok) {
//												get(dp, (us[r3].members[0].x + px3) / 2, us[r3].members[0].y + py3, r3, a3, 0).update(crt, score, command);
//											}
//										}
									}
								}
							}
						}
					}
				}
			}
			}
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					for (int r = 0; r < rotN; r++) {
						px = x * 2 + y % 2 - us[r].members[0].x;
						py = y - us[r].members[0].y;
						int[] dp2 = new int[aho.N];
						fill(dp2, -1);
						DP[] prev2 = new DP[aho.N];
						char[] cmd2 = new char[aho.N];
						int[] used2 = new int[aho.N];
						for (int d = 0; d < 6; d++) {
							if (!canPlace(us[(r + dr[d] + rotN) % rotN], px + dx[d], py + dy[d])) {
								for (int a = 0; a < aho.N; a++) {
									int score = getScore(dp, x, y, r, a, 2);
									if (score >= 0) {
										for (char c : rCommands[d][a]) {
											int a2 = aho.to[a][c];
											int[] ps = aho.getScore(a2, get(dp, x, y, r, a, 2).used);
											if (dp2[a2] < score + ps[0]) {
												dp2[a2] = score + ps[0];
												prev2[a2] = get(dp, x, y, r, a, 2);
												cmd2[a2] = c;
												used2[a2] = ps[1];
											}
										}
									}
								}
							}
						}
						int p = -1;
						for (int a = 0; a < aho.N; a++) if (dp2[a] >= 0 && (p < 0 || dp2[p] < dp2[a])) {
//						for (int a = 0; a < aho.N; a++) if (dp2[a] >= 0 && (p < 0 || dp2[p] < dp2[a] || dp2[p] == dp2[a] && aho.subs[p].length() < aho.subs[a].length())) {
							p = a;
						}
						if (p >= 0) {
							State s = new State(this, r, px, py, dp2[p], p);
							if (list.size() < W || list.get(W - 1).expScore < s.expScore) {
								StringBuilder sb = new StringBuilder();
								sb.append(cmd2[p]);
								DP crt = prev2[p];
								while (crt.prev != null) {
									sb.append(new StringBuilder(crt.command).reverse().toString());
									crt = crt.prev;
								}
								s.commands = sb.toString();
								s.usedPhrase = used2[p];
								boolean hasSame = false;
								for (int i = 0; i < list.size(); i++) {
									if (list.get(i).same(s)) {
										hasSame = true;
										if (list.get(i).expScore < s.expScore) {
											list.remove(i);
											insert(list, s);
											break;
										}
									}
								}
								if (!hasSame) {
									insert(list, s);
									if (list.size() > W) list.remove(W);
								}
							}
						}
					}
				}
			}
		}
		
		State spellPhrases(int toX, int toY, int toRot) {
//			Debug.print(aho.subs[ahoID]);
			Unit[] us = units[sources[source]];
			this.us = us;
			int rotN = us.length;
			Unit u = us[0];
			int px = init[sources[source]].x, py = init[sources[source]].y;
			Debug.check(canPlace(u, px, py));
			DP[] dp = new DP[rotN * width * height * aho.N * 3];
			get(dp, (u.members[0].x + px) / 2, u.members[0].y + py, 0, ahoID, 0).update(null, 0, "", usedPhrase);
			get(dp, (u.members[0].x + px) / 2, u.members[0].y + py, 0, ahoID, 1).update(null, 0, "", usedPhrase);
			int minX = u.members[0].x, maxX = minX;
			for (int i = 0; i < rotN; i++) {
				minX = min(minX, us[i].members[0].x);
				maxX = max(maxX, us[i].members[0].x);
			}
			try (Stat st = new Stat("dp")) {
			for ( ; ; py++) {
				boolean finished = true;
				for (int r = 0; r < rotN; r++) if (us[r].members[0].y + py < height) finished = false;
				if (finished) break;
				for (int dx = -1; dx <= 1; dx += 2) {
					int dir = dx < 0 ? 1 : 0;
					if (dx > 0) {
						px = -maxX;
						if ((px + py) % 2 != 0) px++;
					} else {
						px = width * 2 - minX - 1;
						if ((px + py) % 2 != 0) px--;
					}
					for ( ; 0 <= px + maxX && px + minX < width * 2; px += dx * 2) {
						int[][] dp3 = new int[rotN][aho.N];
						DP[][] prev3 = new DP[rotN][aho.N];
						String[][] cmd3 = new String[rotN][aho.N];
						int[][] used3 = new int[rotN][aho.N];
						for (int r = 0; r < rotN; r++) fill(dp3[r], -1);
						for (int r = 0; r < rotN; r++) {
							int x = div2(us[r].members[0].x + px);
							int y = us[r].members[0].y + py;
							if (x < 0 || x >= width || y < 0 || y >= height) continue;
							for (int t = -1; t <= 1; t += 2) {
								int[][] dp2 = new int[rotN][aho.N];
								for (int i = 0; i < rotN; i++) fill(dp2[i], -1);
								DP[][] prev2 = new DP[rotN][aho.N];
								String[][] cmd2 = new String[rotN][aho.N];
								int[][] used2 = new int[rotN][aho.N];
								for (int a = 0; a < aho.N; a++) if (getScore(dp, x, y, r, a, dir) >= 0) {
									DP e = get(dp, x, y, r, a, dir);
									dp2[0][a] = e.score;
									prev2[0][a] = e;
									cmd2[0][a] = "";
									used2[0][a] = e.used;
								}
								for (int dr = 0; dr + 1 < rotN; dr++) {
									if (!canPlace(us[(r + (dr + 1) * t + rotN) % rotN], px, py)) break;
									for (int a = 0; a < aho.N; a++) if (dp2[dr][a] >= 0) {
										for (char c : rCommands[t < 0 ? 5 : 4][a]) {
											int a2 = aho.to[a][c];
											int[] ps = aho.getScore(a2, used2[dr][a]);
											int score = dp2[dr][a] + ps[0];
											if (dp2[dr + 1][a2] < score) {
												dp2[dr + 1][a2] = score;
												prev2[dr + 1][a2] = prev2[dr][a];
												cmd2[dr + 1][a2] = cmd2[dr][a] + c;
												used2[dr + 1][a2] = ps[1];
											}
										}
									}
								}
								for (int dr = 0; dr < rotN; dr++) {
									for (int a = 0; a < aho.N; a++) if (dp2[dr][a] >= 0) {
										int r2 = (r + dr * t + rotN) % rotN;
										if (dp3[r2][a] < dp2[dr][a]) {
											dp3[r2][a] = dp2[dr][a];
											prev3[r2][a] = prev2[dr][a];
											cmd3[r2][a] = cmd2[dr][a];
											used3[r2][a] = used2[dr][a];
										}
									}
								}
							}
						}
						for (int r = 0; r < rotN; r++) {
							for (int a = 0; a < aho.N; a++) if (dp3[r][a] >= 0) {
								int x2 = (us[r].members[0].x + px) / 2, y2 = us[r].members[0].y + py;
								get(dp, x2, y2, r, a, 2).update(prev3[r][a], dp3[r][a], cmd3[r][a], used3[r][a]);
								if (canPlace(us[r], px + 2 * dx, py)) {
									for (char c : rCommands[1 - dir][a]) {
										int a2 = aho.to[a][c];
										int[] ps = aho.getScore(a2, used3[r][a]);
										get(dp, x2 + dx, y2, r, a2, dir).update(prev3[r][a], dp3[r][a] + ps[0], cmd3[r][a] + c, ps[1]);
									}
								}
							}
						}
					}
				}
				for (int x = 0; x < width; x++) {
					for (int r = 0; r < rotN; r++) {
						int y = us[r].members[0].y + py;
						if (y < 0 || y >= height) continue;
						for (int a = 0; a < aho.N; a++) if (getScore(dp, x, y, r, a, 2) >= 0) {
							DP crt = get(dp, x, y, r, a, 2);
							for (int d = 2; d < 4; d++) {
								int x2 = div2(x * 2 + y % 2 + dx[d]), y2 = y + dy[d];
								int px2 = x2 * 2 + y2 % 2 - us[r].members[0].x;
								int py2 = y2 - us[r].members[0].y;
								if (canPlace(us[r], px2, py2)) {
									for (char c : rCommands[d][a]) {
										int a2 = aho.to[a][c];
										for (int i = 0; i < 2; i++) {
											int[] ps = aho.getScore(a2, crt.used);
											get(dp, x2, y2, r, a2, i).update(crt, crt.score + ps[0], "" + c, ps[1]);
										}
//										// !re -> [2]
//										if (rotN > 1) {
//											int px3 = px2, py3 = py2, r3 = r;
//											int a3 = a2, score = crt.score + aho.getScore(a2, usedPhrase);
//											String command = "" + c;
//											boolean ok = true;
//											for (char c2 : "!re".toCharArray()) {
//												px3 += dx[DIR[c2]];
//												py3 += dy[DIR[c2]];
//												r3 = (r3 + DIR[c2] + rotN) % rotN;
//												if (!canPlace(us[r3], px3, py3)) {
//													ok = false;
//													break;
//												}
//												a3 = aho.to[a3][c2];
//												score += aho.getScore(a3, usedPhrase);
//												command += c2;
//											}
//											if (ok) {
//												get(dp, (us[r3].members[0].x + px3) / 2, us[r3].members[0].y + py3, r3, a3, 2).update(crt, score, command);
//											}
//										}
//										// !ree -> [0]
//										if (rotN > 1) {
//											int px3 = px2, py3 = py2, r3 = r;
//											int a3 = a2, score = crt.score + aho.getScore(a2, usedPhrase);
//											String command = "" + c;
//											boolean ok = true;
//											for (char c2 : "!ree".toCharArray()) {
//												px3 += dx[DIR[c2]];
//												py3 += dy[DIR[c2]];
//												r3 = (r3 + DIR[c2] + rotN) % rotN;
//												if (!canPlace(us[r3], px3, py3)) {
//													ok = false;
//													break;
//												}
//												a3 = aho.to[a3][c2];
//												score += aho.getScore(a3, usedPhrase);
//												command += c2;
//											}
//											if (ok) {
//												get(dp, (us[r3].members[0].x + px3) / 2, us[r3].members[0].y + py3, r3, a3, 0).update(crt, score, command);
//											}
//										}
									}
								}
							}
						}
					}
				}
			}
			}
			px = toX;
			py = toY;
			int r = toRot;
			int x = (us[r].members[0].x + px) / 2;
			int y = us[r].members[0].y + py;
			int[] dp2 = new int[aho.N];
			fill(dp2, -1);
			DP[] prev2 = new DP[aho.N];
			char[] cmd2 = new char[aho.N];
			int[] used2 = new int[aho.N];
			for (int d = 0; d < 6; d++) {
				if (!canPlace(us[(r + dr[d] + rotN) % rotN], px + dx[d], py + dy[d])) {
					for (int a = 0; a < aho.N; a++) {
						int score = getScore(dp, x, y, r, a, 2);
						if (score >= 0) {
							for (char c : rCommands[d][a]) {
								int a2 = aho.to[a][c];
								int[] ps = aho.getScore(a2, get(dp, x, y, r, a, 2).used);
								if (dp2[a2] < score + ps[0]) {
									dp2[a2] = score + ps[0];
									prev2[a2] = get(dp, x, y, r, a, 2);
									cmd2[a2] = c;
									used2[a2] = ps[1];
								}
							}
						}
					}
				}
			}
			int p = -1;
			for (int a = 0; a < aho.N; a++) if (dp2[a] >= 0 && (p < 0 || dp2[p] < dp2[a])) {
//						for (int a = 0; a < aho.N; a++) if (dp2[a] >= 0 && (p < 0 || dp2[p] < dp2[a] || dp2[p] == dp2[a] && aho.subs[p].length() < aho.subs[a].length())) {
				p = a;
			}
			if (p >= 0) {
				StringBuilder sb = new StringBuilder();
				sb.append(cmd2[p]);
				DP crt = prev2[p];
				while (crt.prev != null) {
					sb.append(new StringBuilder(crt.command).reverse().toString());
					crt = crt.prev;
				}
				State s = new State(this, r, px, py, dp2[p], p);
				s.commands = sb.toString();
				s.usedPhrase = used2[p];
				return s;
			}
			return null;
		}
		
		boolean canPlace(Unit u, int px, int py) {
			try (Stat st = new Stat("place")) {
			for (Cell c : u.members) {
				int x = c.x + px, y = c.y + py;
				if (!onBoard(x, y) || bs[y][x]) return false;
			}
			return true;
			}
		}
		
		
		/**
		 * returns the number of cleared rows
		 */
		void update(int rot, int px, int py) {
			Unit u = units[sources[source]][rot];
			for (Cell c : u.members) bs[c.y + py][c.x + px] = true;
			ls = 0;
			boolean[] clear = new boolean[height];
			for (int y = 0; y < height; y++) {
				clear[y] = true;
				for (int x = 0; x < width; x++) {
					if (!bs[y][x * 2 + y % 2]) {
						clear[y] = false;
						break;
					}
				}
				if (clear[y]) ls++;
			}
			int p = height - 1, q = height - 1;
			while (q >= 0) {
				if (!clear[q]) {
					for (int x = 0; x < width; x++) {
						bs[p][x * 2 + p % 2] = bs[q][x * 2 + q % 2];
					}
					p--;
				}
				q--;
			}
			while (p >= 0) {
				fill(bs[p], false);
				p--;
			}
			long points = u.members.length;
			points += 100 * (1 + ls) * ls / 2;
			if (prev != null && prev.ls > 1) points += (prev.ls - 1) * points / 10;
			finalScore += points;
			source++;
		}
		
		@Override
		public int compareTo(State o) {
			return expScore < o.expScore ? 1 : expScore > o.expScore ? -1 : 0;
		}
		
		long getHash() {
			hash = 0;
			for (int i = 0; i < bs.length; i++) {
				for (int j = 0; j < bs[i].length; j++) {
					hash *= A;
					if (bs[i][j]) {
						hash++;
					}
				}
			}
			return hash;
		}
		
		boolean same(State s) {
			if (hash != s.hash) return false;
			for (int i = 0; i < bs.length; i++) {
				for (int j = 0; j < bs[i].length; j++) {
					if (bs[i][j] != s.bs[i][j]) {
						return false;
					}
				}
			}
			return true;
		}
		
		void vis() {
			if (!DEBUG) return;
			System.err.println();
			int[][] is = new int[height][width * 2];
			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width * 2; j++) {
					if (bs[i][j]) is[i][j] = 1;
				}
			}
			Unit u = units[sources[source]][0];
			Cell cp = init[sources[source]];
			if (0 <= cp.x && cp.x < width * 2 && 0 <= cp.y && cp.y < height) {
				is[cp.y][cp.x] = 3;
			}
			for (Cell c : u.members) {
				if (is[c.y + cp.y][c.x + cp.x] == 3) {
					is[c.y + cp.y][c.x + cp.x] = 4;
				} else {
					is[c.y + cp.y][c.x + cp.x] = 2;
				}
			}
			for (int y = 0; y < height; y++) {
				if (y % 2 != 0) System.err.print(" ");
				System.err.print("|");
				for (int x = 0; x < width; x++) {
					int b = is[y][2 * x + y % 2];
					System.err.print(b == 1 ? "/\\" : b == 2 ? "##" : b == 3 ? "@@" : b == 4  ? "&&" : "  ");
				}
				System.err.println("|");
				if (y % 2 != 0) System.err.print(" ");
				System.err.print("|");
				for (int x = 0; x < width; x++) {
					int b = is[y][2 * x + y % 2];
					System.err.print(b == 1 ? "\\/" : b == 2 ? "##" : b == 3 ? "@@" : b == 4 ? "&&" : "  ");
				}
				System.err.println("|");
			}
//			Utils.sleep(200);
		}
		
	}
	
	static void insert(ArrayList<State> list, State s) {
		int p = 0;
		while (p < list.size() && list.get(p).expScore >= s.expScore) p++;
		list.add(p, s);
	}
	
}
