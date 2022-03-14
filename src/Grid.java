package program;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

@SuppressWarnings("serial")
public class Grid extends Agent{

	// κρατάει τα ονόματα των πρακτόρων
	private String agents[] = new String[3];
	// κρατάει τους πόντους των πρακτόρων
    private int agentScore[] = new int[3];
    // κρατάει τον τωρινό αριθμό των πελατών
    private int numberOfClients = 0;
    // κρατάει αν ο πράκτορας κουβαλάει κάποιον πελάτη
    private boolean carrying[] = new boolean[3];
    // κρατάει τον αριθμό των πελατών που έχουν παραδοθεί
    private int numberOfDeliveredClients = 0;
    // μέγιστος αριθμός των πελατών
    private int maxNumberOfClients = 8;
    // κρατάει τους πελάτες των πρακτόρων
    private int assignedClient[] = new int[3];
    // κρατάει τις θέσεις και των πελατών
    private Dictionary<Integer, Integer[][]> clients = new Hashtable<Integer, Integer[][]>();
    // κρατάει τους αναθετημένους πελάτες ανά πράκτορα
    private Dictionary<Integer, Queue<Integer>> assigned = new Hashtable<Integer, Queue<Integer>>();
    // κρατάει το αναγνωριστικό του πελάτη
    private int clientId = 0;
    // κρατάει τον αριθμό των προσπαθειών
    int tries = 0;
    // κρατάει τον αριθμό των μέγιστων προσπαθειών
	int maxTries = 60;
	// κρατάει την θέση του κάθε πράκτορα 
	int agentPos[][] = new int[3][2];
	
	public void setup() {		
        try {Thread.sleep(100);} catch (InterruptedException ie) {}      //important
        //search the registry for agents
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("agent");
        template.addServices(sd);
        
        // αποθκεύει το όνομα του κάθε πράκτορα
        for(int cnt = 0; cnt < 3; cnt++) {
        	try {
        		DFAgentDescription[] result = DFService.search(this, template);
        		agents[cnt] = result[cnt].getName().getLocalName();
        		assigned.put(cnt, new LinkedList<Integer>());
        	} catch (FIPAException fe) {fe.printStackTrace();}
        }
        
        // δημιουργεί τις αρχικές θέσεις των πρακτόρων
        generateAgentPositions();

       	// δημιουργεί πέντε πελάτες
        try {
        	for(int cnt = 0; cnt < 5; cnt++) {
        		spawnClient();
        	}
		} catch (IOException | UnreadableException e1) { e1.printStackTrace(); }
        addBehaviour(new CyclicBehaviour(this) {

            public void action(){

                try {Thread.sleep(50);} catch (InterruptedException ie) {}
                
                // εκτυπώνει τους πόντους των πρακτόρων
                System.out.println("Agent Score Is: " + String.valueOf((agentScore[0]+agentScore[1]+agentScore[2])));
                System.out.println("The Number of Delivered Clients is: " + String.valueOf(numberOfDeliveredClients));
                // κρατάει τις επόμενες ενέργειες των πελατών
                String actions[] = new String[3];

                // Όσο έχουμε προσπάθειες 
                if(tries < maxTries) {
                	// αν κάποιος πράκτορας δεν έχει πελάτη
                	if((assigned.get(0).size() == 0
                			|| assigned.get(1).size() == 0 ||
                			assigned.get(2).size() == 0) &&
                			numberOfClients < maxNumberOfClients ) {
                		try {
							spawnClient();
						} catch (IOException | UnreadableException e) { e.printStackTrace(); }
                	}
                	// εκτυπώνει το πλέγμα
                	printGrid();
                	// αυξάνει τις προσπάθειες
                	tries++;
                	// για κάθε ένα από τους πράκτορες παίρνει την δράση του
                	for(int cnt = 0; cnt < 3; cnt++) {
                		ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                    	//refer to receiver by local name
                    	message.addReceiver(new AID(agents[cnt], AID.ISLOCALNAME));
                    	// ενημερώνει τον πράκτορα να κάνει κάτι
                    	message.setContent("Pick a Direction");
                    	send(message);
                    	ACLMessage msg = blockingReceive();
                    	// αποθηκεύει την κίνηση του πράκτορα
                    	actions[cnt] = msg.getContent();
                    	
                	}
                	// λύνει τα conflicts και παίρνει τις ενέργειες
                	String newActions[] = auctionConflict(actions);
                	// για κάθε έναν από τους πράκτορες
                	for(int cnt = 0; cnt < 3; cnt++) {
                    	// κάνει την κίνηση
                		try {
                			// στέλνει Conflict ενω δεν πρέπει μάλλον
							moveAgent(cnt, Integer.parseInt(newActions[cnt]));
						} catch (NumberFormatException | IOException e) { e.printStackTrace(); }
                	}
                    // αν δεν έχουμε αρκετούς πελάτες ή έχει περάσει ο χρόνος
                    if((numberOfClients < 5) || (tries % 5 == 0 && numberOfClients < maxNumberOfClients)) {
                    	try {
                    		// δημιουργεί έναν νέο πελάτη
                    		spawnClient();
						} catch (IOException | UnreadableException e) { e.printStackTrace(); }
                    }
                    
                }
                // αν δεν έχουμε δοκιμές πλέον
                else {
                    try {Thread.sleep(50);} catch (InterruptedException ie) {}
                    ACLMessage messageFinal = new ACLMessage(ACLMessage.INFORM);
                    messageFinal.addReceiver(new AID(agents[0], AID.ISLOCALNAME));
                    messageFinal.setContent("End");
                    send(messageFinal);
                    System.out.println(getLocalName()+" terminating");
                    // terminate
                    doDelete();
                }
            }
        });
	} // end of setup
	
	// δημιουργεί τις θέσεις των πρακτόρων στο περιβάλλον
	private void generateAgentPositions() {
		// δημιουργεί την θέση του πρώτου πράκτορα
		agentPos[0][0] = (byte) Math.round(Math.random() * (4));
		agentPos[0][1] = (byte) Math.round(Math.random() * (4));
		sendAgentPosition(0);
		
		do {
			agentPos[1][0] = (byte) Math.round(Math.random() * (4));
			agentPos[1][1] = (byte) Math.round(Math.random() * (4));
		} while(agentPos[1][0] == agentPos[0][0] && agentPos[1][1] == agentPos[0][1]);
		sendAgentPosition(1);
		
		do {
			agentPos[2][0] = (byte) Math.round(Math.random() * (4));
			agentPos[2][1] = (byte) Math.round(Math.random() * (4));
		} while(agentPos[2][0] == agentPos[0][0] && agentPos[2][1] == agentPos[0][1]&&
				agentPos[2][0] == agentPos[1][0] && agentPos[2][1] == agentPos[1][1]);
		sendAgentPosition(2);
	}

	public void sendClientToAgent(Integer[] clientPos, Integer[] clientTarget, int id) {
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);

    	message.addReceiver(new AID(agents[id], AID.ISLOCALNAME));
	    message.setContent("Client");
	    send(message);
	    message.setContent(String.valueOf(clientPos[0]));
	    send(message);
	    message.setContent(String.valueOf(clientPos[1]));
	    send(message);
	    message.setContent(String.valueOf(clientTarget[0]));
	    send(message);
	    message.setContent(String.valueOf(clientTarget[1]));
	    send(message);

	}
	
	// δημιουργεί έναν νέο πελάτη
	public void spawnClient() throws IOException, UnreadableException {
		// αρχική θέση και τελική θέση του πελάτη
		int pt, pt2;
		// στόχος του πελάτη και θέση του πελάτη
		Integer[] clientTarget, clientPos;
		// αυξάνει τον αριθμό των πρακτόρων
		numberOfClients++;
		// δημιουργεί το σημείο που θα ξεκινήσει ο πελάτης
		pt = (int) Math.round(Math.random() * (4));
		// παίρνει τις συντεταγμένες του σημείου έναρξης
		clientPos = new Integer[] {0,0};
		// ανάλογα με το σημείο του πράκτορα αναθέτει τις συντεταγμένες
		switch(pt) {
		case 1:
			clientPos = new Integer[] {0,4};
			break;
		case 2:
			clientPos = new Integer[] {4,3};
			break;
		case 3:
			clientPos = new Integer[] {4,0};
			break;
		}
		
		// παίρνει ένα από τα σημεία ως σημείο προορισμού του πελάτη
		do {
			pt2 = (int) Math.round(Math.random() * 4);
		} while(pt == pt2);
		// παίρνει τις συντεταγμένες σημείου προορισμού
		clientTarget = new Integer[] {0, 0};
		switch(pt2) {
		case 1:
			clientTarget = new Integer[] {0,4};
			break;
		case 2:
			clientTarget = new Integer[] {4,3};
			break;
		case 3:
			clientTarget = new Integer[] {4,0};
			break;
		}
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
        ACLMessage msg;
		// προσθέτει τον πελάτη στο μητρώο
		clients.put(clientId, new Integer[][] {clientPos, clientTarget});

		int clients[] = new int[3];

        boolean auctionAgents[] = new boolean[3];
        double agentPoints;
        auctionAgents[0] = false;
        auctionAgents[1] = false;
        auctionAgents[2] = false;
        for(int cnt = 0; cnt < 3; cnt++) {
        	// στέλνει τις συντεταγμένες στον κάθε πράκτορα 
        	sendClientToAgent(clientPos, clientTarget, cnt);
        	// παίρνει το κόστος του πράκτορα από τον πελάτη 
        	msg = blockingReceive();
        	int clientSize = Integer.parseInt(msg.getContent());
        	// TODO end for
        	clients[cnt] = clientSize;
        	if(clientSize == 0 ) {
        		auctionAgents[cnt] = true;
        	}
        }

        if(!(auctionAgents[0] || auctionAgents[1] || auctionAgents[2])) {
        	auctionAgents[0] = true;
        	auctionAgents[1] = true;
        	auctionAgents[2] = true;
        }
        double mostPoints = -200;
        int bestAgent = -1;
	    for(int cnt = 0; cnt < 3; cnt++) {
	    	if(auctionAgents[cnt]) {
	    		message = new ACLMessage(ACLMessage.INFORM);
	    		message.addReceiver(new AID(agents[cnt], AID.ISLOCALNAME));
	    		message.setContent("Cost");
	    		send(message);
	    		msg = blockingReceive();
	    		double agentCost = Double.parseDouble(msg.getContent());
	    		agentPoints = agentCost*-10;
	    		agentPoints -= clients[cnt]*5;
	    		if(mostPoints < agentPoints) {
	    			mostPoints = agentPoints;
	    			bestAgent = cnt;
	    		}
	    	}
	    } 

        // ενημερώνει τον πράκτορα για το πιοός έχει πάρει
        // τον πελάτη
        message = new ACLMessage(ACLMessage.INFORM);
        message.addReceiver(new AID(agents[bestAgent], AID.ISLOCALNAME));
        message.setContent("Yours");
        send(message);
        assigned.get(bestAgent).add(clientId);
	        // για όλους τους πράκτορες        
        for(int cnt = 0; cnt < 3; cnt++) {
        	// αν δεν είναι ο πράκτορας που νίκησε
        	if(cnt != bestAgent) {
        		// στέλνει ένα μήνυμα για να τον ενημερώσει
        		message = new ACLMessage(ACLMessage.INFORM);
            	message.addReceiver(new AID(agents[cnt], AID.ISLOCALNAME));
            	message.setContent("Other");
            	send(message);
        	}
        }
        clientId++;
	}
 
	
	
	// αφήνει τον πελάτη
	public void putDownClient(int id) {
		Integer target[];
		Integer client = 0;
		// παίρνει τον πελάτη
		client = assigned.get(id).remove();
		// παίρνει τον στόχο του πελάτη
		target = clients.get(client)[1];
		// αν είναι στον στόχο τότε προσθέτει τους πόντους 
		if(agentPos[id][0] == target[0] && 
				agentPos[id][1] == target[1] && carrying[id]) {
			// προσθέτει στους πόντους του πράκτορα
			agentScore[id] += 20;
        	numberOfDeliveredClients++;
		} else {
			// αλλιώς αφαιρεί τους πόντους
			agentScore[id] -= 10;
		}
		// ο πράκτορας δεν κουβαλάει κάτι πλέον
		carrying[id] = false;
		// αφαιρεί τον πελάτη από το μητρώο
    	clients.remove(client);
		// decrements the client count
		numberOfClients--;
		
	}
	
	// μετακινεί τον πράκτορα ανάλογα με μια κατεύθυνση που θέλει
	public void moveAgent(int id, int dir) throws IOException {
		// switch που αλλάζει τη θέση
		switch(dir) {
		// για κάθε πιθανή κίνηση του πράκτορα ελέγχει αν είναι δυνατή
		// και αλλάζει το σκορ του πράκτορα ανάλογα
		case 0:
			if(agentPos[id][0] > 0) { 
				agentPos[id][0]--;
			} else {
				agentScore[id] -= 100;
			}
			break;

		case 1:
			if(agentPos[id][0] < 4) { 
				agentPos[id][0]++;
			} else {
				agentScore[id] -= 100;
			}

			break;
		case 2:
			if(agentPos[id][1] > 0) { 
				agentPos[id][1]--;
			} else {
				agentScore[id] -= 100;
			}

			break;
		case 3:
			if(agentPos[id][1] < 4) { 
				agentPos[id][1]++;
			} else {
				agentScore[id] -= 100;
			}

			break;
		case 4:
			pickUpClient(id);
			break;
		case 5:
			putDownClient(id);
			break;
		default:
			break;
		}
		// αν ο πράκτορας έχει επιλέξει κίνηση
		if(dir >= 0 && dir <= 3) {
			sendAgentPosition(id);
			if(carrying[id]) {
				// ενημερώνει την θέση του πελάτη
				clients.get(assignedClient[id])[0][0] = agentPos[id][0];
				clients.get(assignedClient[id])[0][1] = agentPos[id][1];
			}
		}
		// μειώνει τους πόντους του πράκτορα
		agentScore[id] -= 1;
	}
	
	// αποστέλλει την θέση του πράκτορα
	public void sendAgentPosition(int id) {
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
    	message.addReceiver(new AID(agents[id], AID.ISLOCALNAME));

       	message.setContent("Position");
        send(message);
        message.setContent(String.valueOf(agentPos[id][0]));
        send(message);
        message.setContent(String.valueOf(agentPos[id][1]));
        send(message);
	}

	// αποστέλλει στον πράκτορα ότι σήκωσε τον πελάτη
	public void sendPickedUpClient(int id) {
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
    	message.addReceiver(new AID(agents[id], AID.ISLOCALNAME));
    	message.setContent("Picked Up Client");
    	send(message);
	}

	// αποστέλλει στον πράκτορα ότι δεν σήκωσε τον πελάτη
	public void sendNotPickedUpClient(int id) throws IOException {
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
    	message.addReceiver(new AID(agents[id], AID.ISLOCALNAME));
		message.setContent("Not Picked Up Client");
    	send(message);
	}
	
 	// καλείται από τον πράκτορα για να σηκώσει τον πελάτη 
	public void pickUpClient(int id) throws IOException {
		
		// δεν βρέθηκε πελάτης
		boolean found = false;
		Integer target[];
		Integer client = 0;
		client = assigned.get(id).peek();

		target = clients.get(client)[0];

		// αν ο πελάτης είναι στην ίδια θέση με τον πράκτορα
		if(agentPos[id][0] == target[0] && agentPos[id][1] == target[1]) {
			assignedClient[id] = assigned.get(id).peek();
			// βρήκε τον πράκτορα
			found = true;
			// ο πράκτορας πλέον κουβαλάει
			carrying[id] = true;
		
		}
		
    	// αν βρήκαμε τον πελάτη
		if(found) {
			// ενημερώνει τον πράκτορα
			sendPickedUpClient(id);
		} else {
			sendNotPickedUpClient(id);
			// αλλιώς ενημερώνει τον πράκτορα και τους πόντους του
        	agentScore[id] -= 10;
		}		

	}

	public int[][] getNewPositions(String actions[]) {
		int newPositions[][] = new int[3][2];
		newPositions[0] = agentPos[0].clone();
		newPositions[1] = agentPos[1].clone();
		newPositions[2] = agentPos[2].clone();
		for(int cnt = 0; cnt < 3; cnt++) {
			switch(actions[cnt]) {
			// για κάθε πιθανή κίνηση του πράκτορα ελέγχει αν είναι δυνατή
			// και αλλάζει το σκορ του πράκτορα ανάλογα
			case "0":
				if(agentPos[cnt][0] > 0) {
					
					newPositions[cnt][0]--;
				}
				break;
			case "1":
				if(agentPos[cnt][0] < 4) { 
					newPositions[cnt][0]++;

				}
				break;
			case "2":
				if(agentPos[cnt][1] > 0) { 
					newPositions[cnt][1]--;

				}

				break;
			case "3":
				if(agentPos[cnt][1] < 4) { 
					newPositions[cnt][1]++;
				}
				break;
			default:
				break;
			}
		}
		return newPositions;
	}
	
	// εκτυπώνει το πλέγμα
	public void printGrid() {
		System.out.println("At Try: " + String.valueOf(tries));
		for(int h = 0; h < 5; h++ ) {
			for(int w = 0; w < 5; w++ ) {
				System.out.print('[');
				boolean client = false;
				boolean agent = false;
				if(agentPos[0][0] == h && agentPos[0][1] == w ||
						agentPos[1][0] == h && agentPos[1][1] == w ||
						agentPos[2][0] == h && agentPos[2][1] == w) {
					agent = true;
					Enumeration<Integer> enu = clients.keys();
					Integer currentKey;
					while(enu.hasMoreElements()) {
						currentKey = (Integer) enu.nextElement();
						Integer clientPos[] = clients.get(currentKey)[0];
						if(clientPos[0] == h && clientPos[1] == w) {
							client = true;
							break;
						}

					}
					if(client) {
						System.out.print("B");
					} else {
						System.out.print("A");
					}
				} else {
					Enumeration<Integer> enu = clients.keys();
					Integer currentKey;
					while(enu.hasMoreElements()) {
						currentKey = (Integer) enu.nextElement();
						Integer clientPos[] = clients.get(currentKey)[0];
						if(clientPos[0] == h && clientPos[1] == w) {
							System.out.print("C");
							client = true;
							break;
						}

					}
				}
				if(!client && !agent) {
					System.out.print(" ");
				}
				System.out.print(']');
			}
			System.out.println();
		}
		System.out.println("----------------------------------");
	}
	
	public String[] auctionConflict(String[] actions) {
		// αντιγράφει τις ενέργειες που θέλουν να κάνουν οι πράκτορες
		String[] newActions = actions.clone();
		// κρατάει τις νέες θέσεις των πρακτόρων ώστε να βρει συγκρούσεις
		int[][] newPositions = getNewPositions(newActions);;
		// κρατάει τους πράκτορες που έχουν σύγκρουση
		int[] conflictingAgents =  getConflictingAgents(newPositions);
		// κρατάει αν πρέπει να γίνουν ορισμένες κινήσεις οπωσδήποτε 
		boolean fixed = false;
		// σηματοδοτεί τον νικητή της δημοπρασίας
		int winner = -1;
		// αν έχουμε σύγκρουση δύο πρακτόρων υπάρχει ενδεχόμενο να υπάρχει
		// σύγκρουση και στο αποτέλεσμα 
		if(conflictingAgents.length == 2) {
			boolean conflictFirst = false;
			if(!(newActions[conflictingAgents[0]].equals("4") ||
					newActions[conflictingAgents[0]].equals("5"))) {
				// έχουμε δύο εναλλακτικές, να μην κάνει πράξη ο ένας ή ο άλλος
				String[] alternativeFirst = actions.clone();
				alternativeFirst[conflictingAgents[0]] = "7";
				alternativeFirst[conflictingAgents[1]] = actions[conflictingAgents[1]];
				conflictFirst = getConflictingAgents(getNewPositions(alternativeFirst)).length > 0; 
			}
			boolean conflictSecond = false;
			if(!(newActions[conflictingAgents[1]].equals("4") ||
					newActions[conflictingAgents[1]].equals("5"))) {
				String[] alternativeSecond = actions.clone();
				alternativeSecond[conflictingAgents[0]] = actions[conflictingAgents[0]];
				alternativeSecond[conflictingAgents[1]] = "7";
				conflictSecond = getConflictingAgents(getNewPositions(alternativeSecond)).length > 0;
			}
			
			// αν υπάρχει σύγκρουση με την κίνηση του πρώτου αυτόματα νικάει
			// την δημοπρασία ο δεύτερος πριν ξεκινήσει
			if(conflictFirst) {
				winner = 1;
				fixed = true;
			// αν υπάρχει σύγκρουση με την κίνηση του δεύτερου αυτόματα νικάει
			// την δημοπρασία ο πρώτος πριν ξεκινήσει
			} else if(conflictSecond) {
				winner = 0;
				fixed = true;
			}
		}

		// κρατάει τους πόντους του νικητή
		double mostPoints = -100;

		for(int cnt = 0; cnt < conflictingAgents.length; cnt++) {
    		ACLMessage message = new ACLMessage(ACLMessage.INFORM);
        	//refer to receiver by local name
        	message.addReceiver(new AID(agents[conflictingAgents[cnt]], AID.ISLOCALNAME));
        	// αποστέλλει στον πράκτορα ότι υπάρχει σύγκρουση 
        	message.setContent("Conflict");
	       	send(message);
		}
		
		// για κάθε έναν από τους πράκτορες που έχουν σύγκρουση
		for(int cnt = 0; cnt < conflictingAgents.length; cnt++) {
			// αν ένας πράκτορας φορτώνει η ξεφορτώνει τότε κερδίζει αυτόματα 
			if(actions[conflictingAgents[cnt]] == "5" ||
					actions[conflictingAgents[cnt]] == "4") {
				winner = cnt;
				fixed = true;
			}
        	// λαμβάνει το κόστος του πράκτορα
        	ACLMessage msg = blockingReceive();
        	double cost = Double.parseDouble(msg.getContent());
        	// αρχικοποιεί τους πόντους του
        	double points = 0;
        	// αφαιρεί το κλιμακωμένο κόστος από τους πόντους 
        	points -= cost*10;
        	// αν ο πράκτορας κουβαλάει τότε έχει επιπλέον πόντους
			if(carrying[conflictingAgents[cnt]]) {
				points += 20;
			}
			// αν έχει περισσότερους πόντους από τον καλύτερο
			if(points > mostPoints && !fixed) {
				// αντιγράφονται οι πόντοι του
				mostPoints = points;
				// ο προσωρινός νικιτής είναι άλλος πράκτορας
				winner = conflictingAgents[cnt];
			}
		} // τέλος της for
		// για κάθε πράκτορα
		for(int cnt = 0; cnt < 3; cnt++) {
			// υποθέτουμε ότι δεν χρειάζεται να αλλάξει κατεύθυνση
			boolean clear = true; 
			// για κάθε πράκτορα που είχε conflict
			for(int i = 0; i < conflictingAgents.length; i++) {
				// αν ο πράκτορας είχε conflict και δεν είναι αυτός που κέρδισε
				if(conflictingAgents[i] == cnt && !(winner != cnt)) {
					// θέτει την αληθοτιμή ως false
					clear = false;
				}
			}
			// αν ο πράκτορας κέρδισε του αποστέλλει μήνυμα να τον ενημερώσει
			if(clear) {
	    		ACLMessage message = new ACLMessage(ACLMessage.INFORM);
	        	//refer to receiver by local name
	        	message.addReceiver(new AID(agents[cnt], AID.ISLOCALNAME));
	        	message.setContent("Fine");
	        	send(message);
			} else {
				// αλλιώς δεν κέρδισε
	    		ACLMessage message = new ACLMessage(ACLMessage.INFORM);
	        	//refer to receiver by local name
	        	message.addReceiver(new AID(agents[cnt], AID.ISLOCALNAME));
	        	message.setContent("Not Fine");
	        	send(message);
	        	newActions[cnt] = "7";
			}
		} // end of for
	
		return newActions;
	}
	
	public int[] getConflictingAgents(int[][] newPositions) {

		int[] conflictingAgents = new int[0];
		if(newPositions[0][0] == newPositions[1][0] &&
				newPositions[0][1] == newPositions[1][1]) {

			// αν ο πρώτος πράκτορας έχει σύγκρουση με τον τρίτο
			if(newPositions[0][0] == newPositions[2][0] &&
					newPositions[0][1] == newPositions[2][1]) {
				// έχουμε σύκγρουση και των τριών
				conflictingAgents = new int[]{0,1,2};
			} else {
				// αλλιώς έχουμε σύγκρουση μεταξύ του πρώτου και του
				// δεύτερου
				conflictingAgents = new int[]{0,1};
			}

		// αν έχουμε σύγκρουση μεταξύ του πρώτου και του τρίτου
		} else if(newPositions[0][0] == newPositions[2][0] &&
				  newPositions[0][1] == newPositions[2][1]) {
			conflictingAgents = new int[]{0,2};
		// αν έχουμε σύγκρουση μεταξύ του δεύτερου και του τρίτου
		} else if(newPositions[1][0] == newPositions[2][0] &&
				  newPositions[1][1] == newPositions[2][1]) {
			conflictingAgents = new int[]{1,2};
		}
		return conflictingAgents;
	}
		
}
