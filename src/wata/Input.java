import java.io.*;

import com.google.gson.*;
import com.google.gson.stream.*;

public class Input {
	
	int id;
	Unit[] units;
	int width;
	int height;
	Cell[] filled;
	int sourceLength;
	int[] sourceSeeds;
	
	static Input read(String file) {
		Gson gson = new Gson();
		try (Reader reader = new BufferedReader(new FileReader(new File(file)))) {
			return gson.fromJson(new JsonReader(reader), Input.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}

class Unit {
	Cell[] members;
	Cell pivot;
}

class Cell implements Comparable<Cell> {
	int x;
	int y;
	Cell(int x, int y) {
		this.x = x;
		this.y = y;
	}
	@Override
	public int compareTo(Cell o) {
		return x != o.x ? (x - o.x) : (y - o.y);
	}
	Cell copy() {
		return new Cell(x, y);
	}
}
