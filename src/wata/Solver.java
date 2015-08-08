
import static java.lang.Math.*;
import static java.util.Arrays.*;

import java.util.*;


public class Solver extends Board {
	
	boolean DEBUG = true;//false;
	
	String getTag() {
		return "random";
	}
	
	// W, E, SW, SE, CW, CCW
	static char[][] COMMANDS = {	"p'!.03".toCharArray(),
							"bcefy2".toCharArray(),
							"aghij4".toCharArray(),
							"lmno 5".toCharArray(),
			 				"dqrvz1".toCharArray(),
			 				"kstuwx".toCharArray() };
	
	String[] phrases;
	
	int source;
	int ls_old;
	
	long score;
	String solution;
	
	/*******************
	 * init
	 ******************/
	
	void setup(Unit[] units, int width, int height, Cell[] filled, int[] sources, String[] phrases) {	
		this.units = genUnit(units);
		this.width = width;
		this.height = height;
		board = new boolean[height][width * 2];
		for (Cell c : filled) board[c.y][c.x * 2 + c.y % 2] = true;
		this.sources = sources;
		this.phrases = phrases;
		init = new Cell[units.length];
		for (int i = 0; i < units.length; i++) {
			init[i] = genInit(this.units[i][0]);
		}
		source = 0;
		ls_old = 0;
		score = 0;
		solution = "";
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
	
	/*******************
	 * utils
	 ******************/
	
	boolean canMove(int crtRot, Cell crtPos, int command) {
		return canMove(units[sources[source]], crtRot, crtPos, command);
	}
	
	/**
	 * returns true when the game ends
	 */
	boolean update(int crtRot, Cell crtPos) {
		Unit u = units[sources[source]][crtRot];
		int ls = update(u, crtPos);
		long points = u.members.length;
		points += 100 * (1 + ls) * ls / 2;
		if (ls_old > 1) points += (ls_old - 1) * points / 10;
		score += points;
		ls_old = ls;
		source++;
		if (source >= sources.length) return true;
		u = units[sources[source]][0];
		crtPos = init[sources[source]];
		for (Cell c : u.members) {
			int x = c.x + crtPos.x, y = c.y + crtPos.y;
			if (x < 0 || x >= 2 * width || y < 0 || y >= height || board[y][x]) return true;
		}
		return false;
	}
	
	/*******************
	 * AI
	 ******************/
	
	void solve() {
		int SLEEP = 50;
		int crtRot = 0;
		int crtUnit = sources[source];
		Cell crtPos = init[crtUnit].copy();
		System.out.println(score);
		vis(crtRot, crtPos);
		Utils.sleep(SLEEP);
		Random rand = new Random(743982);
		for (;;) {
			int command = rand.nextInt(6);
			solution += COMMANDS[command][0];
			if (canMove(crtRot, crtPos, command)) {
				crtRot = (crtRot + dr[command] + units[crtUnit].length) % units[crtUnit].length;
				crtPos.x += dx[command];
				crtPos.y += dy[command];
			} else {
				if (update(crtRot, crtPos)) {
					break;
				}
				crtUnit = sources[source];
				crtRot = 0;
				crtPos = init[sources[source]].copy();
			}
			System.out.printf("%d (%d/%d)%n", score, source, sources.length);
			vis(crtRot, crtPos);
			Utils.sleep(SLEEP);
		}
	}
	
	void vis(int crtRot, Cell crtPos) {
		if (!DEBUG) return;
		for (int i = 0; i < width; i++) System.out.print("--");
		System.out.println("---");
		int crtUnit = sources[source];
		int[][] bs = new int[height][width * 2];
		for (int i = 0; i < board.length; i++) {
			for (int j = 0; j < board[i].length; j++) {
				if (board[i][j]) bs[i][j] = 1;
			}
		}
		if (0 <= crtPos.x && crtPos.x < width * 2 && 0 <= crtPos.y && crtPos.y < height) {
			bs[crtPos.y][crtPos.x] = 3;
		}
		for (Cell c : units[crtUnit][crtRot].members) {
			if (bs[c.y + crtPos.y][c.x + crtPos.x] == 3) {
				bs[c.y + crtPos.y][c.x + crtPos.x] = 4;
			} else {
				bs[c.y + crtPos.y][c.x + crtPos.x] = 2;
			}
		}
		for (int y = 0; y < height; y++) {
			if (y % 2 != 0) System.out.print(" ");
			System.out.print("|");
			for (int x = 0; x < width; x++) {
				int b = bs[y][2 * x + y % 2];
				System.out.print(b == 1 ? "/\\" : b == 2 ? "##" : b == 3 ? "@@" : b == 4  ? "&&" : "  ");
			}
			System.out.println("|");
			if (y % 2 != 0) System.out.print(" ");
			System.out.print("|");
			for (int x = 0; x < width; x++) {
				int b = bs[y][2 * x + y % 2];
				System.out.print(b == 1 ? "\\/" : b == 2 ? "##" : b == 3 ? "@@" : b == 4 ? "&&" : "  ");
			}
			System.out.println("|");
		}
		Utils.sleep(200);
	}
	
}
