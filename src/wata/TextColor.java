

/**
 * コンソール出力に色を付ける
 */
public class TextColor {
	
	public static final int BLACK = 0;
	public static final int RED = 1;
	public static final int GREEN = 2;
	public static final int YELLOW = 3;
	public static final int BLUE = 4;
	public static final int MAGENTA = 5;
	public static final int CYAN = 6;
	public static final int WHITE = 7;
	
	private String val = "";
	
	/**
	 * テキストの色を変更する
	 */
	public TextColor color(int c) {
		set(30 + c);
		return this;
	}
	
	/**
	 * 背景の色を変更する
	 */
	public TextColor colorB(int c) {
		set(40 + c);
		return this;
	}
	
	/**
	 * 太字に変更する
	 */
	public TextColor bold() {
		set(1);
		return this;
	}
	
	/**
	 * 下線を引く
	 */
	public TextColor underscore() {
		set(4);
		return this;
	}
	
	/**
	 * 色を反転する
	 */
	public TextColor reverse() {
		set(7);
		return this;
	}
	
	private void set(int id) {
		if (val.length() > 0) val += ";";
		val += id;
	}
	
	/**
	 * テキストに色の変更を適用した文字列を返す
	 */
	public String apply(String text) {
		return String.format("\u001b[%sm%s\u001b[m", val, text);
	}
	
}
