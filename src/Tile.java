package program;

import java.util.ArrayList;

public class Tile implements Comparable<Tile> {
	public int coordinates[];
    public double fullCost;
    public double costUntilHere;
    public Tile prev = null;
	ArrayList<Tile> neighbors;
	
	// public constructor
	public Tile(int height, int width) {
		// δημιουργεί νεο ArrayList για τους γείτονες
		neighbors = new ArrayList<Tile>();
		// δημιουργεί τον πίνακα για να πάρει τις συντεταγμένες
		coordinates = new int[2];
		// θέτει τη θέση στο ύψος
		coordinates[0] = height;
		// θέτει τη θέση στο πλάτος
		coordinates[1] = width;
	}
	// προσθέτει ένα πλακίδιο στους γείτονες
	public void addNeighbor(Tile neighbor) {
		neighbors.add(neighbor);
	}

	// παίρνει την ευκλειδια απόσταση ως ευριστική του πλακιδίου
	public double getHeuristic(Tile target) {
		return Math.sqrt(Math.pow(this.coordinates[0] - target.coordinates[0], 2) +
				Math.pow(this.coordinates[1] - target.coordinates[1],2));
	}
	
	public boolean equals(Tile tile) {
		return tile.coordinates[0] == this.coordinates[0] &&
				tile.coordinates[1] == this.coordinates[1];
	}
	// Χρησιμοποιείται για την PriorityQueue
	@Override
	public int compareTo(Tile arg0) {
		return Double.compare(fullCost, arg0.fullCost);
	}
}
