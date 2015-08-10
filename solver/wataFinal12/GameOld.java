import static java.lang.Math.*;
import static java.util.Arrays.*;

import java.util.*;


public class GameOld {
	
	boolean DEBUG = false;
	
	String getTag() {
		return "wata5.3";
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
	
	int width;
	int height;
	String[] phrases;
	Cell[] init;
	Unit[][] units;
	int[] sources;
	State initialState;
	
	/************
	 * Initialize
	 ************/
	
	void setup(Unit[] units, int width, int height, Cell[] filled, int[] sources, String[] phrases) {
		this.units = genUnit(units);
		this.width = width;
		this.height = height;
		initialState = new State(filled);
		this.sources = sources;
		this.phrases = phrases;
		init = new Cell[units.length];
		for (int i = 0; i < units.length; i++) {
			init[i] = genInit(this.units[i][0]);
		}
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
	
	int W = 100;
	
	/**
	 * (commands, score)
	 */
	Pair<String, Long> solve() {
		ArrayList<State> crt = new ArrayList<>();
		crt.add(initialState);
		State best = initialState;
		for (int source = 0; source < sources.length && crt.size() > 0; source++) {
			System.err.printf("\r%03d/%03d", source, sources.length);
			ArrayList<State> next = new ArrayList<>();
//			if (DEBUG) System.err.println(crt.get(0).expScore);
//			crt.get(0).vis();
			for (State s : crt) {
				s.nextStates(next, W);
			}
			crt = next;
			for (State s : crt) {
				if (best.finalScore < s.finalScore) {
					best = s;
				}
			}
		}
		StringBuilder commands = new StringBuilder();
		ArrayList<State> list = new ArrayList<GameOld.State>();
		for (State s = best; s != null; s = s.prev) {
			if (s.prev != null) commands.append(s.commands);
			list.add(s);
		}
		if (DEBUG) {
			for (int i = list.size() - 1; i > 0; i--) {
				list.get(i).vis();
			}
		}
		return Pair.make(commands.reverse().toString(), best.finalScore);
	}
	
	class State implements Comparable<State> {
		
		int source;
		boolean[][] bs; // (y,x) 垂直座標
		long finalScore;
		int ls;
		double expScore;
		String commands;
		State prev;
		
		State(Cell[] filled) {
			bs = new boolean[height][width * 2];
			for (Cell c : filled) bs[c.y][c.x * 2 + c.y % 2] = true;
		}
		
		State(State s, int rot, int px, int py) {
			source = s.source;
			bs = Utils.copy(s.bs);
			finalScore = s.finalScore;
			prev = s;
			update(rot, px, py);
			expScore = getScore();
		}
		
		double getScore(int rot, int px, int py) {
			return new State(this, rot, px, py).expScore;
		}
		
		double getScore() {
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
//				expScore += 100.0 * pow((double)y / height, 0.2) * num * num / width / width * (1 + 5.0 * pow((double)(sources.length - source) / sources.length, 0.3));
//				expScore += 100.0 * pow((double)y / height, 0.1) * num * num / width / width * 0.5;//(1 + 5.0 * pow((double)(sources.length - source) / sources.length, 0.3));
				expScore += 100.0 * y / height * num * num / width / width * (1 + 5.0 * pow((double)(sources.length - source) / sources.length, 0.3));
//				expScore += 100.0 * y / height * num * num / width / width * (1 + 6.0 * pow((double)(sources.length - source) / sources.length, 0.2));
			}
			return expScore;
		}
		
		void nextStates(ArrayList<State> list, int W) {
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
							double score = getScore(rot, px, py);
							if (list.size() < W || list.get(W - 1).expScore < score) {
								State s = new State(this, rot, px, py);
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
		
		boolean canPlace(Unit u, int px, int py) {
			for (Cell c : u.members) {
				int x = c.x + px, y = c.y + py;
				if (!onBoard(x, y) || bs[y][x]) return false;
			}
			return true;
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
		
		boolean same(State s) {
			for (int i = 0; i < bs.length; i++) {
				for (int j = 0; j < bs[i].length; j++) {
					if (bs[i][j] != s.bs[i][j]) return false;
				}
			}
			return true;
		}
		
		void vis() {
			if (!DEBUG) return;
			for (int i = 0; i < width; i++) System.out.print("--");
			System.out.println("---");
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
				if (y % 2 != 0) System.out.print(" ");
				System.out.print("|");
				for (int x = 0; x < width; x++) {
					int b = is[y][2 * x + y % 2];
					System.out.print(b == 1 ? "/\\" : b == 2 ? "##" : b == 3 ? "@@" : b == 4  ? "&&" : "  ");
				}
				System.out.println("|");
				if (y % 2 != 0) System.out.print(" ");
				System.out.print("|");
				for (int x = 0; x < width; x++) {
					int b = is[y][2 * x + y % 2];
					System.out.print(b == 1 ? "\\/" : b == 2 ? "##" : b == 3 ? "@@" : b == 4 ? "&&" : "  ");
				}
				System.out.println("|");
			}
			Utils.sleep(200);
		}
		
	}
	
}
