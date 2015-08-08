
import static java.lang.Math.*;

import java.util.*;


public class GreedySolver extends Solver {
	
	@Override
	String getTag() {
		return "chokudai";
	}
	
	int[] nums;
	
	double shallowScore(int crtRot, Cell crtPos) {
		double score = 0;
		Board next = new Board(this);
		Unit u = units[sources[source]][crtRot];
		int ls = next.update(u, crtPos);
		score += 100 * (1 + ls) * ls / 2;
		for (int y = 0; y < height; y++) {
			double num = 0;
			for (int x = 0; x < width; x++) {
				int x2 = x * 2 + y % 2;
				if (next.board[y][x2]) {
					num += 1;
				} else {
					for (int d = 0; d < 4; d++) {
						if (next.onBoard(x2 - dx[d], y - dy[d]) && !next.board[y - dy[d]][x2 - dx[d]]) {
							num += (d < 2 ? 0.1 : 0.2);
						}
					}
				}
			}
			score += 100.0 * y / height * num * num / width / width;
		}
//		int[] sum = nums.clone();
//		for (int y = height - 2; y >= 0; y--) sum[y] += sum[y + 1];
//		for (Cell c : units[sources[source]][crtRot].members) {
//			int y = c.y + crtPos.y;
//			score += (double)y * (nums[y] + 1) / (sum[y] - nums[y] + 1);
//		}
		return score;
	}
	
	double shallowScore2(int crtRot, Cell crtPos) {
		double score = 0;
		Board next = new Board(this);
		Unit u = units[sources[source]][crtRot];
		int ls = next.update(u, crtPos);
		score += 100 * (1 + ls) * ls / 2;
		for (int y = 0; y < height; y++) {
			int num = 0;
			boolean dead = false;
			for (int x = 0; x < width; x++) {
				int x2 = x * 2 + y % 2;
				if (next.board[y][x2]) {
					num++;
				} else  {
					if (!next.onBoard(x2  - dx[2], y - dy[2]) || next.board[y - dy[2]][x2 - dx[2]]) {
						if (!next.onBoard(x2 - dx[3], y - dy[3]) || next.board[y - dy[3]][x2 - dx[3]]) {
							dead = true;
						}
					}
				}
			}
			if (!dead) {
				score += (double) 100.0 * y / height * num * num / width / width;
			}
		}
//		int[] sum = nums.clone();
//		for (int y = height - 2; y >= 0; y--) sum[y] += sum[y + 1];
//		for (Cell c : units[sources[source]][crtRot].members) {
//			int y = c.y + crtPos.y;
//			score += (double)y * (nums[y] + 1) / (sum[y] - nums[y] + 1);
//		}
		return score;
	}
	
	boolean hoge = false;
	double deepScore(int crtRot, Cell crtPos) {
		double score = 0;
		Board next = new Board(this);
		Unit u = units[sources[source]][crtRot];
		int ls = next.update(u, crtPos);
		score += 100 * (1 + ls) * ls / 2;
		int[][] can = new int[height][width];
		for (int i = 0; i < 5 && source + 1 + i < sources.length; i++) {
			Unit[] us = units[sources[source + 1 + i]];
			CanMove c = next.canMoveTo(us, 0, init[sources[source + 1 + i]]);
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					for (int r = 0; r < us.length; r++) {
						if (c.can[y][x][r]) {
							int px = x * 2 + y % 2 - us[r].members[0].x, py = y - us[r].members[0].y;
							for (Cell d : us[r].members) {
								can[d.y + py][(d.x + px) / 2]++;
							}
						}
					}
				}
			}
		}
		for (int y = 0; y < height; y++) {
			int num = 0;
			int dead = 2;
			for (int x = 0; x < width; x++) {
				int x2 = x * 2 + y % 2;
				if (next.board[y][x2]) {
					num++;
//					for (int d = -2; d <= 2; d += 4) {
//						if (next.onBoard(x2 + d, y) && !next.board[y][x2 + d]) {
//							score -= 1;
//						}
//					}
				} else  {
					dead = min(dead, can[y][x]);
				}
			}
			if (hoge) Debug.print(y, dead);
			score += (double) 100.0 * y / height * num * num / width / width * pow(dead / 2.0, 0.5);
		}
		return score;
	}
	
	@Override
	void solve() {
		for (;;) {
			nums = new int[height];
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					if (board[y][x * 2 + y % 2]) nums[y]++;
				}
			}
			int crtUnit = sources[source];
			int rotN = units[crtUnit].length;
			vis(0, init[crtUnit]);
			CanMove can = canMoveTo(units[crtUnit], 0, init[crtUnit]);
			ArrayList<Pair<Double, int[]>> list = new ArrayList<Pair<Double,int[]>>();
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					for (int rot = 0; rot < rotN; rot++) {
						if (can.can[y][x][rot] && can.lockCommand[y][x][rot] >= 0) {
							Unit u = units[crtUnit][rot];
							int cy = y - u.members[0].y, cx = (x * 2 + y % 2) - u.members[0].x;
							double score = shallowScore(rot, new Cell(cx, cy));
							list.add(Pair.make(-score, new int[]{cx, cy, rot, can.lockCommand[y][x][rot]}));
						}
					}
				}
			}
			Collections.sort(list);
			double bestScore = -1e10;
			int bestX = 0, bestY = 0, bestRot = 0, lastCommand = 0;
			for (int i = 0; i < 1 && i < list.size(); i++) {
				int[] e = list.get(i).second;
				double score = deepScore(e[2], new Cell(e[0], e[1]));
				if (bestScore < score) {
					bestScore = score;
					bestX = e[0];
					bestY = e[1];
					bestRot = e[2];
					lastCommand = e[3];
				}
			}
//			hoge = true;
//			deepScore(bestRot, new Cell(bestX, bestY));
//			hoge = false;
			if (DEBUG) Debug.print(bestScore);
			StringBuilder sb = new StringBuilder();
			sb.append(COMMANDS[lastCommand][0]);
			int x = bestX, y = bestY, rot = bestRot;
			for (;;) {
				Unit u = units[crtUnit][rot];
				int c = can.prev[u.members[0].y + y][(u.members[0].x + x) / 2][rot];
				if (c < 0) break;
				sb.append(COMMANDS[c][0]);
				x -= dx[c];
				y -= dy[c];
				rot -= dr[c];
				rot %= rotN;
				if (rot < 0) rot += rotN;
			}
			solution += sb.reverse().toString();
			if (update(bestRot, new Cell(bestX, bestY))) break;
		}
	}
	
}
