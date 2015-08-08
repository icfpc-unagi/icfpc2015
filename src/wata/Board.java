
import static java.util.Arrays.*;

import java.util.*;


public class Board {
	
	// W, E, SW, SE, CW, CCW
	static int[] dx = {-2, 2, -1, 1, 0, 0};
	static int[] dy = {0, 0, 1, 1, 0, 0};
	static int[] dr = {0, 0, 0, 0, 1, -1};
	
	int width;
	int height;
	boolean[][] board; // (y,x)
	Cell[] init;
	Unit[][] units;
	int[] sources;
	
	Board() {
	}
	
	Board(Board b) {
		width = b.width;
		height = b.height;
		board = new boolean[height][];
		for (int i = 0; i < height; i++) board[i] = b.board[i].clone();
	}
	
	boolean onBoard(int x, int y) {
		return 0 <= x && x < width * 2 && 0 <= y && y < height;
	}
	
	boolean canMove(Unit[] us, int crtRot, Cell crtPos, int command) {
		int rot = (crtRot + dr[command] + us.length) % us.length;
		int px = crtPos.x + dx[command], py = crtPos.y + dy[command];
		return canPlace(us[rot], px, py);
	}
	
	boolean canPlace(Unit u, int px, int py) {
		for (Cell c : u.members) {
			int x = c.x + px, y = c.y + py;
			if (!onBoard(x, y) || board[y][x]) return false;
		}
		return true;
	}
	
	static class CanMove {
		// member[0]の座標系 (xに1/2足していない)
		boolean[][][] can;
		int[][][] prev;
		int[][][] lockCommand;
		CanMove(int height, int width, int rotN) {
			can = new boolean[height][width][rotN];
			prev = new int[height][width][rotN];
			lockCommand = new int[height][width][rotN];
		}
	}
	
	CanMove canMoveTo(Unit[] us, int crtRot, Cell crtPos) {
		int rotN = us.length;
		Unit u = us[crtRot];
		CanMove res = new CanMove(height, width, rotN);
		if (!canPlace(u, crtPos.x, crtPos.y)) return res;
		res.can[u.members[0].y + crtPos.y][(u.members[0].x + crtPos.x) / 2][crtRot] = true;
		res.prev[u.members[0].y + crtPos.y][(u.members[0].x + crtPos.x) / 2][crtRot] = -1;
		Queue<int[]> que = new LinkedList<>();
		que.offer(new int[]{crtPos.x, crtPos.y, 0});
		while (!que.isEmpty()) {
			int[] e = que.poll();
			int x = e[0], y = e[1], rot = e[2];
			int lock = -1;
			for (int command = 0; command < 6; command++) {
				if (canMove(us, rot, new Cell(e[0], e[1]), command)) {
					int x2 = x + Solver.dx[command], y2 = y + Solver.dy[command], rot2 = (rot + Solver.dr[command] + rotN) % rotN;
					u = us[rot2];
					if (!res.can[u.members[0].y + y2][(u.members[0].x + x2) / 2][rot2]) {
						res.can[u.members[0].y + y2][(u.members[0].x + x2) / 2][rot2] = true;
						res.prev[u.members[0].y + y2][(u.members[0].x + x2) / 2][rot2] = command;
						que.offer(new int[]{x2, y2, rot2});
					}
				} else {
					lock = command;
				}
			}
			res.lockCommand[us[rot].members[0].y + y][(us[rot].members[0].x + x) / 2][rot] = lock;
		}
		return res;
	}
	
	/**
	 * returns the number of cleared rows
	 */
	int update(Unit u, Cell crtPos) {
		for (Cell c : u.members) board[c.y + crtPos.y][c.x + crtPos.x] = true;
		int ls = 0;
		boolean[] clear = new boolean[height];
		for (int y = 0; y < height; y++) {
			clear[y] = true;
			for (int x = 0; x < width; x++) {
				if (!board[y][x * 2 + y % 2]) {
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
					board[p][x * 2 + p % 2] = board[q][x * 2 + q % 2];
				}
				p--;
			}
			q--;
		}
		while (p >= 0) {
			fill(board[p], false);
			p--;
		}
		return ls;
	}
	
}
