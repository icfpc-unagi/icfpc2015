

/**
 * ペア
 */
public class Pair<F, S> implements Comparable<Pair<F, S>> {
	
	public F first;
	public S second;
	
	public Pair(F f, S s) {
		this.first = f;
		this.second = s;
	}
	
	public static <F, S> Pair<F, S> make(F first, S second) {
		return new Pair<>(first, second);
	}
	
	@Override
	public int compareTo(Pair<F, S> o) {
		int comp = ((Comparable<F>)first).compareTo(o.first);
		if (comp != 0) return comp;
		if (o.second instanceof Comparable) {
			return ((Comparable<S>)second).compareTo(o.second);
		} else {
			return 0;
		}
	}
	
	@Override
	public String toString() {
		return String.format("(%s,%s)", first, second);
	}
	
}
