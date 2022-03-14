package program;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Stack;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;


@SuppressWarnings("serial")
public class MyAgent extends Agent{
	String lastAction = "";
	// ουρά για τις θέσεις πελατών που έχουν ανατεθεί στον πράκτορα
	Queue<Integer[]> clientQueue = new LinkedList<Integer[]>();
	// ουρά για τους στόχους πελατών που έχουν ανατεθεί στον πράκτορα
	Queue<Integer[]> clientTargetQueue= new LinkedList<Integer[]>();
	// κρατάει μια αναπαράσταση του περιβάλλοντος
	ArrayList<Tile> tiles;
	// μονοπάτι που πρέπει να ακολουθήσει ο πράκτορας για να φτάσει στον 
	// στόχο του
	ArrayList<Integer> path = new ArrayList<Integer>();
	// συντεταγμένες τελικού στόχου του πράκτορα
	private Integer finalCoords[];
	// τωρινές συντεταγμένες του πράκτορα
	public int coordinates[];
	// κρατάει το τωρινό πλακίδιο του πράκτορα
	public Tile currentTile;
	// κρατάει τις συντεταγμένες του στόχου του πράκτορα
	public Integer[] target;
	// κρατάει το πλακίδιο του πράκτορα
	public Tile targetTile;
	// αληθοτιμή που κρατάει αν ο πράκτορας κουβαλάει
	public boolean carrying = false;
	
	public void setup() {
		this.target = new Integer[] {-5,-5};
        // register agent to directory facilitator
        DFAgentDescription dfd = new DFAgentDescription();
        // agent id
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("agent");
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {DFService.register(this, dfd);}
        catch (FIPAException fe) {fe.printStackTrace();}
        ACLMessage msg = null;
		tiles = new ArrayList<Tile>();
		
		// βάζει όλους τους κόμβους στο arraylist
		for(int h = 0; h < 5; h++ ) {
			for(int w = 0; w < 5; w++ ) {
				tiles.add(new Tile(h,w));
			}
		}
		
		// θέτει όλους τους γείτονες
		for(int h = 0; h < 5; h++ ) {
			for(int w = 0; w < 5; w++ ) {
				// παίρνει το τωρινό πλακίδιο
				Tile cur = tiles.get(h*5+w);
				// θέτει όλους τους γείτονες σε κάθε πλακίδιο
				if(h-1 >= 0) {
					cur.addNeighbor(tiles.get((h-1)*5+w));
				}
				if(h+1 < 5) {
					cur.addNeighbor(tiles.get((h+1)*5+w));
				}
				if(w-1 >= 0) {
					cur.addNeighbor(tiles.get(h*5+(w-1)));
				}
				if(w+1 < 5)  {
					cur.addNeighbor(tiles.get(h*5+(w+1)));
				}
			}
		}
		// διαγράφει τους κατάλληλους γείτονες ώστε να
		// δημιουργηθούν οι τοίχοι μεταξύ των πλακιδίων 
		tiles.get(1).neighbors.remove(tiles.get(2));
		tiles.get(2).neighbors.remove(tiles.get(1));
		tiles.get(6).neighbors.remove(tiles.get(7));
		tiles.get(7).neighbors.remove(tiles.get(6));
		tiles.get(15).neighbors.remove(tiles.get(16));
		tiles.get(16).neighbors.remove(tiles.get(15));
		tiles.get(20).neighbors.remove(tiles.get(21));
		tiles.get(21).neighbors.remove(tiles.get(20));
		tiles.get(17).neighbors.remove(tiles.get(18));
		tiles.get(18).neighbors.remove(tiles.get(17));
		tiles.get(22).neighbors.remove(tiles.get(23));
		tiles.get(23).neighbors.remove(tiles.get(22));
		
    	msg = blockingReceive();
    	// παίρνει την αρχική θέση του πράκτορα
    	if(msg.getContent().equals("Position")) {
    		// δημιουργεί το που θα αποθηκευτούν οι συντεταγμένες
    		coordinates = new int[2];
    		msg = blockingReceive();
    		// λαμβάνει τη θέση στο ύψος
    		coordinates[0] = Integer.parseInt(msg.getContent());
    		// λαμβάνει τη θέση στο πλάτος
    		msg = blockingReceive();
    		coordinates[1] = Integer.parseInt(msg.getContent());
    		finalCoords = new Integer[] {coordinates[0], coordinates[1]};
        }
    	    	
    	// παίρνει το τωρινό tile
    	currentTile = tiles.get(coordinates[0]*5+coordinates[1]);
        
        addBehaviour(new CyclicBehaviour(this) {
			public void action() {
                ACLMessage msg = null;
                while(true) {
                	// waiting to receive message
                	msg = blockingReceive();
                	// αν στέλνει την θέση στον πράκτορα το περιβάλλον
                    if(msg.getContent().equals("Position")){
                    	// παίρνει την πρώτη θέση
                		msg = blockingReceive();
                		coordinates[0] = Integer.parseInt(msg.getContent());
                    	// παίρνει την πρώτη θέση
                		msg = blockingReceive();
                		coordinates[1] = Integer.parseInt(msg.getContent());
                		// ανανεώνει το τωρινό tile του πράκτοα
                		currentTile = tiles.get(coordinates[0]*5+coordinates[1]);
                    } else if(msg.getContent().equals("Pick a Direction")) {
                    	// επιλέγει την επόμενη κίνηση
                    	String content = chooseNextMove();
                    	// ετοιμάζει το επόμενο μήνυμα που θα στείλει
                        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                        // refer to receiver by local name
                        message.addReceiver(new AID("MyGrid", AID.ISLOCALNAME));
                        // στέλνει την κίνηση στο περιβάλλον
                        message.setContent(content);
                        if(content.equals("5")) {
                        	carrying = false;
                    	}
                        send(message);
                        msg = blockingReceive();
                        if(msg.getContent().equals("Conflict")) {
                        	handleConflict(content);
                        }
                    // στέλνει το περιβάλλον συντεταγμένες ενός πελάτη
                    } else if(msg.getContent().equals("Client")) {
                    	// παίρνει τον πελάτη από το περιβάλλον
                		Integer result[][] = getClient();
                		try {
                			// συνεργάζεται με το περιβάλλον για την ανάληψη
                			// πελάτη
							cooperate(result[0], result[1]);
						} catch (IOException e) { e.printStackTrace(); }
                    } else if(msg.getContent().equals("Picked Up Client")) {
                    	// κουβαλάει πελάτη εφόσον τον σήκωσε
                    	carrying = true;
                    	// ανανεώνει τον στόχο                   
                    	target = clientTargetQueue.remove();
                    	// παίρνει το σωστό target tile
                    	targetTile = tiles.get(target[0]*5+target[1]);
                    } else if(msg.getContent().equals("Not Picked Up Client")) {                  	
                    	// διαγράφει τον προηγούμενο στόχο
                    	target[0] = -5;
                    	target[1] = -5;
                    	clientTargetQueue.remove();
                    	carrying = false;

                    } else if(msg.getContent().equals("End")) {
                        System.out.println(getLocalName()+" terminating");
                        // take down from registry
                        takeDown();
                        // terminate
                        doDelete();
                    } // end of else if
                } // end of while true
            } // end of action
        });


	}
	
	
	public void resetPrevs() {
		for(int cnt = 0; cnt < tiles.size(); cnt++) {
			tiles.get(cnt).prev = null;
		}
	}
	
	
	// επιστρέφει την ευκλείδια απόσταση δύο συντεταγμένων
	public double eucledianDistance(Integer[] target2, Integer[] chosen) {
		return(Math.sqrt(Math.pow(target2[0]-chosen[0], 2) +
				Math.sqrt(Math.pow(target2[1]-chosen[1], 2))));
	}

	public Tile aStar(Tile begin, Tile end) {
		// ουρά προτεραιότητας που κρατάει τα κλειστά tiles
	    PriorityQueue<Tile> closedTiles = new PriorityQueue<>();
		// ουρά προτεραιότητας που κρατάει τα ανοιχτά tiles
	    PriorityQueue<Tile> openTiles = new PriorityQueue<>();
	    begin.costUntilHere = 0;
	    // προσθέτει στα ανοιχτά πλακίδια τον αρχικό
	    openTiles.add(begin);
	    // όσο υπάρχουν ακόμα ανοιχτά πλακίδια
	    while(!openTiles.isEmpty()) {
	    	// πάρε αυτό με τη μεγαλύτερη πρωτεραιότητα
	    	Tile cur = openTiles.remove();
	    	// αν είναι ο στόχος επεστρεψε το
			if(cur.equals(end)) {
				return cur;
			}
			// για κάθε γείτονα του
	    	for(Tile neighbor : cur.neighbors) {
	    		// αν δεν έχουμε ξαναδεί το tile
	    		if(!closedTiles.contains(neighbor) && !openTiles.contains(neighbor)) {
	    			// θέτει το προηγούμενο πλακίδιο
	    			neighbor.prev = cur;
	    			neighbor.costUntilHere = cur.costUntilHere+1;
	    			neighbor.fullCost = neighbor.getHeuristic(end) + neighbor.costUntilHere;
	    			openTiles.add(neighbor);
	    		}
	    	}
	    	closedTiles.add(cur);
	    }
		return null;
	}
	
	public ArrayList<Integer> getPath(Tile target) {
		// αποθηκεύει τις συντεταγμένες των πλακιδίων
		int coords[];
		// Stack που έχει τις συντεταγμένες του μονοπατιού
		Stack<Integer[]> pathOfCoords = new Stack<Integer[]>();
		// αποθηκεύει το μονοπάτι που πρέπει να ακολουθήσει ο πράκτορας
		ArrayList<Integer> path = new ArrayList<Integer>();
		// έχει το τωρινό πλακίδιο
		Tile curTile = target;
		// παίρνει τις τελικές συντεταγμένες
		coords = target.coordinates;
		// όσο υπάρχουν κόμβοι
		while(curTile.prev != null) {
			// τις προσθέτει στο μονοπάτι
			pathOfCoords.add(new Integer[] {coords[0], coords[1]});
			// παίρνει τον προηγούμενο
			curTile = curTile.prev;
			// παίρνει τις συντεταγμένες
			coords = curTile.coordinates;
		}
		// προσθέτει στις συντεταγμένες στο μονοπάτι
		pathOfCoords.add(new Integer[] {coords[0], coords[1]});
		// αφαιρεί τους προηγούμενους κόμβους
		resetPrevs();
		// Κρατάει τις τωρινές συντεταγμένες
		Integer current[];
		// παίρνει τις πρώτες συντεταγμένες στη στοίβα
		// που θα είναι οι τωρινές
		current = pathOfCoords.pop();
		// κρατάει τις επόμενες συντεταγμένες
		Integer next[];
		// όσο υπάρχουν συντεταγμένες
		while(!pathOfCoords.isEmpty()) {
			// παίρνει τις επόμενες συντεταγμένες
			next = pathOfCoords.pop();
			// αποφασίζει την ενέργεια από τις συντεταγμένες
			if(next[0] == current[0]-1) {
				path.add(0);
			} else if(next[0] == current[0]+1) {
				path.add(1);
			} else if(next[1] == current[1]-1) {
				path.add(2);
			} else if(next[1] == current[1]+1) {
				path.add(3);
			}
			current = next;
		}
		return path;
	}

	// επιλέγει την επόμενη κίνηση του πράκτορα
	public String chooseNextMove() {
    	String content = "7";
    	// αν την προηγούμενη φορά δεν έχιε κάνει κίνηση
    	if(!lastAction.equals("")) {
    		String action = lastAction;
    		lastAction = "";
    		// επιστρέφει την πράξη που έπρεπε να είχε κάνει
    		return action;
    	}
    	// αν ο πράκτορας είναι πάνω στον στόχο
		if(target[0] == coordinates[0] && target[1] == coordinates[1]) {
			// αν κουβαλάει
			if(carrying) {
				// πρέπει να τον αφήσει
				content = "5";
			} else {
				// αλλιώς πρέπει να τον σηκώσει
				content = "4";
			}
			// διαγράφει τον στόχο
			target[0] = -5;
			target[1] = -5;
			// δημιουργεί καινούργιο μονοπάτι
			path = new ArrayList<Integer>();
		} else {
			// αν δεν έχουμε μονοπάτι
			if(path.size() == 0) {
				// αν δεν κουβαλάει
				if(!carrying) {
					if(clientQueue.size() == 0) {
						content = "7";
					} else {
						// παίρνει ως στόχο την θέση του πελάτη
						target = clientQueue.remove();
						// παίρνει το πλακίδιο του πελάτη ως πλακίδιο στόχο
						targetTile = tiles.get(target[0]*5+target[1]);
						// παίρνει το μονοπάτι που πρέπει να ακολουθήσει ο πράκτορας
	        			path = getPath(aStar(currentTile, targetTile));
	        			// αν ο πράκτορας είναι ήδη στη θέση
	        			if(path.size() == 0) {
	        				// σηκώνει τον πελάτη
	        				content = "4";
	        			} else {
	        				// αλλιώς παει στον πελάτη
	       					content = path.remove(0).toString();
	        			}
					}
				} else {
					targetTile = tiles.get(target[0]*5 + target[1]);
					path = getPath(aStar(currentTile, targetTile));
					content = path.remove(0).toString();
				}
				
			} else {
				content = path.remove(0).toString();
			}
		}
		return content;
	}
	
	public Integer[][] getClient() {
		Integer result[][] = new Integer[2][2];
    	// παίρνει τις πρώτες συντεταγμένες
		ACLMessage msg = blockingReceive();
		result[0][0] = Integer.parseInt(msg.getContent());
    	// παίρνει τις επόμενες συντεταγμένες
		msg = blockingReceive();
		result[0][1] = Integer.parseInt(msg.getContent());
		
		// Στόχος του πελάτη
		msg = blockingReceive();
		result[1][0] = Integer.parseInt(msg.getContent());
		msg = blockingReceive();
		result[1][1] = Integer.parseInt(msg.getContent());
		return result;
	}
	
	// συνεργάζεται με το περιβάλλον για να ανατεθεί 
	public void cooperate(Integer[] clientPos, Integer[] clientTarget) throws IOException {
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
        // refer to receiver by local name
        message.addReceiver(new AID("MyGrid", AID.ISLOCALNAME));
        // υπολογίζει την απόσταση του τελευταίου σημείου από την
        // θέση του πράκτορα
		message.setContent(Integer.toString(clientQueue.size()));
		send(message);
    	ACLMessage msg = blockingReceive();
    	String result = msg.getContent();
    	if(result.equals("Cost")) {
    		// παίρνει το κόστος από τον πελάτη
    		double cost = eucledianDistance(clientPos, finalCoords);
    		// στέλνει το κόστος στο περιβάλλον
			message.setContent(Double.toString(cost));
			send(message);
			msg = blockingReceive();
			// αν το περιβάλλον του έχει δώσει τον πελάτη
			if(msg.getContent().equals("Yours")) {
				// προσθέτει την θέση του στην ουρά
				clientQueue.add(clientPos);
				// προσθέτει τον στόχο του στην ουρά
				clientTargetQueue.add(clientTarget);
				// ανανεώνει τις τελικές συντεταγμένες του πράκτορα
				finalCoords = clientTarget;
			} // τέλος της if 
    	} 
	} // τέλος cooperate
	
	public String handleConflict(String action) {
		String newAction = action;
		double cost = eucledianDistance(new Integer[]{coordinates[0],
					coordinates[1]}, target);
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
        // refer to receiver by local name
        message.addReceiver(new AID("MyGrid", AID.ISLOCALNAME));
        // στέλνει στο περιβάλλον το κόστος
        message.setContent(Double.toString(cost));
        send(message);
        ACLMessage msg = blockingReceive();
    	if(!msg.getContent().equals("Fine")) {
    		newAction = "7";
    		lastAction = action;
    	} else {
    		lastAction = "";
    	}

		return newAction;
	}
}
