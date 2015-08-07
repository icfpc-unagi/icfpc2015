
public class Solver {
	
	int[][] dx = {};
	
	Unit[] units;
	int width;
	int height;
	boolean[][] board;
	int[] sources;
	
	long score;
	String seq;
	
	Solver(Unit[] units, int width, int height, Cell[] filled, int[] sources, String[] phrases) {
		this.units = units;
		this.width = width;
		this.height = height;
		board = new boolean[width][height];
		for (Cell c : filled) board[c.x][c.y] = true;
		this.sources = sources;
	}
	
	void solve() {
		vis();
		score = 0;
		seq = "ei!";
	}
	
	void vis() {
		for (int y = 0; y < height; y++) {
			if (y % 2 != 0) System.out.print(" ");
			for (int x = 0; x < width; x++) {
				System.out.print(board[x][y] ? "/\\" : "  ");
			}
			System.out.println();
			if (y % 2 != 0) System.out.print(" ");
			for (int x = 0; x < width; x++) {
				System.out.print(board[x][y] ? "\\/" : "  ");
			}
			System.out.println();
		}
	}
	
}
