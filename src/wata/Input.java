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
		try (FileReader reader = new FileReader(new File(file))) {
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

class Cell {
	int x;
	int y;
}
