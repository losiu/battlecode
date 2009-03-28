package team006;

import java.util.HashMap;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

public class Archon {
	
	private final RobotController myRC;

	private ArchonStatus status;
	
	private Direction myDirection = Direction.NORTH;

	private Clock clock;
	
	private int roundNumNotNearest;
	
	private int roundOfLastSpawn = clock.getRoundNum();
	private Direction spawnDirection = Direction.NORTH;
	
	private HashMap<Integer, Integer> workers = new HashMap<Integer, Integer>();
	private int workersNum = 0;
				
	public Archon(RobotController rc) {
		myRC = rc;
		status = ArchonStatus.LOOKING_FOR_NEAREST_DEPOSIT;
	}
			
	public void nextTurn() throws Exception{
		
		ArchonStatus turnStatus = status;
		
		if (turnStatus == ArchonStatus.CAPTURING_DEPOSIT){
		

			workersNum = 0;
			
			double maxRoundsWithoutTransfer = RobotType.WORKER.maxEnergon() / RobotType.WORKER.energonUpkeep();
			
			for (int key: workers.keySet()){
				if (workers.get(key) + maxRoundsWithoutTransfer < clock.getRoundNum())
					workersNum++;
			}
				
			if (clock.getRoundNum() > roundOfLastSpawn + 10
				&& workersNum < 10){

				if (myRC.getDirection() != spawnDirection)
					myRC.setDirection(spawnDirection);

				if (myRC.getEnergonLevel() > RobotType.WORKER.spawnCost()){
					myRC.spawn(RobotType.WORKER);
					roundOfLastSpawn = clock.getRoundNum();
				}
			}
			
			Robot[] groundRobot = myRC.senseNearbyGroundRobots();

			// energon will be transfered to one of ground robots
			if (groundRobot.length > 0){
			
				// id of the robot with minimal energon level
				int idRobot = groundRobot.length;
				
				double minEnergon = 0;
				
				for (int i = 0; i < groundRobot.length; i++) {
					
					if (myRC.canSenseObject(groundRobot[i])){
						
						workers.remove(groundRobot[i].getID());
						
						workers.put(groundRobot[i].getID(), clock.getRoundNum());

						RobotInfo robotInfo = myRC.senseRobotInfo(groundRobot[i]);

						if (myRC.getLocation().isAdjacentTo(robotInfo.location)){
						
							double energon = robotInfo.eventualEnergon;
							
							if (energon < minEnergon || idRobot == groundRobot.length){
								minEnergon = energon;
								idRobot = i;
							}
				
						}
						
					}
					
				}
				
				if (idRobot < groundRobot.length) {
					RobotInfo robotInfo = myRC.senseRobotInfo(groundRobot[idRobot]);
					
					double amountOfEnergon = myRC.getEnergonLevel();
					if (amountOfEnergon > ENERGON_TRANSFER_RATE)
						amountOfEnergon = ENERGON_TRANSFER_RATE;
					if (amountOfEnergon > robotInfo.maxEnergon - robotInfo.eventualEnergon)
						amountOfEnergon = robotInfo.maxEnergon - robotInfo.eventualEnergon;
					
				
					if (amountOfEnergon > 0
							&& myRC.senseGroundRobotAtLocation(robotInfo.location) != null )
						myRC.transferEnergon(amountOfEnergon, robotInfo.location, RobotLevel.ON_GROUND);
				
				}

				
			}
						
		}
		
		
		if (turnStatus == ArchonStatus.DEPOSIT_FOUND){
			
			MapLocation[] blocksLocations = myRC.senseNearbyBlocks();
			
			int idOfBlock = 0; //the nearest block if some exists
			
			if (blocksLocations.length > 0) {
				
				double minDistance = myRC.getLocation().distanceSquaredTo(blocksLocations[0]);
				
				for (int i = 0; i < blocksLocations.length; i++){
					
					if (minDistance > myRC.getLocation().distanceSquaredTo(blocksLocations[i])) {
						minDistance = myRC.getLocation().distanceSquaredTo(blocksLocations[i]);
						idOfBlock = i;
					}					
					
				}
				
				spawnDirection = myRC.getLocation().directionTo(blocksLocations[idOfBlock]);
							
			}
			
			status = ArchonStatus.CAPTURING_DEPOSIT;
		}
		
			
		if (turnStatus == ArchonStatus.LOOKING_FOR_NEAREST_DEPOSIT
				|| turnStatus == ArchonStatus.LOOKING_FOR_SOME_DEPOSIT) {
			
			while(myRC.isMovementActive()) {
                myRC.yield();
            }
			
			if (turnStatus == ArchonStatus.LOOKING_FOR_NEAREST_DEPOSIT)
					roundNumNotNearest = clock.getRoundNum();
			
			MapLocation[] archonLocations = myRC.senseAlliedArchons();
			FluxDeposit[] fluxDeposits = myRC.senseNearbyFluxDeposits();
			MapLocation[] depositsLocations = new MapLocation[fluxDeposits.length];
			boolean[] archonIsFixed = new boolean[archonLocations.length];
			int notFixedarchonsNum = archonLocations.length;
			
			for (int i = 0; i < fluxDeposits.length; i++) {
				FluxDepositInfo depositInfo = myRC.senseFluxDepositInfo(fluxDeposits[i]);
				depositsLocations[i] = depositInfo.location;
			}
			
			// there are more archons then deposits
			// every deposit should has its fixed archon and not fixed 
			// archons should move in some different directions  
 			if (depositsLocations.length < archonLocations.length){
 				
 				for (int i = 0; i < archonLocations.length; i++) 
 	 	     		archonIsFixed[i] = false;
 	 			
 	 			for (int i = 0; i < depositsLocations.length; i++) {
 	 					
 	 				// idOfFixedArchon == archonLocations.length
 	 				// means then any archon is fixed
 	 				int idOfFixedArchon = archonLocations.length; 
 	 				int minDistance = 0;
 	 	     		
 	 				// looking for the nearest archon to this fluxDeposit 
 	 				for (int j = 0; j < archonLocations.length; j++){
 	 					int distance = depositsLocations[i].distanceSquaredTo(archonLocations[j]);
 	 					if (!archonIsFixed[j] && (idOfFixedArchon == archonLocations.length 
 	 						|| distance < minDistance)){
 	 	     					minDistance = distance;
 	 	     					idOfFixedArchon = j;
 	 					}
 	 	     				
 	 				}
 	 	     			
 	 				if (idOfFixedArchon < archonLocations.length) {
 	 					archonIsFixed[idOfFixedArchon] = true;
 	 					notFixedarchonsNum --;
 	 				}
 	 	     				 	 	     			 					
 	 			}
 	 			
 	 			// gets statistics of not fixed archons's locations	
 	 			boolean first = true;
 	 			int maxX = 0; int maxY = 0;
 	 			int minX = 0; int minY = 0;
 				
 	 			for (int i = 0; i < archonLocations.length; i++) {
 	 				if (!archonIsFixed[i]){
 	 					if (first || maxX < archonLocations[i].getX())
 	 						maxX = archonLocations[i].getX();
 	 					
 	 					if (first || minX > archonLocations[i].getX())
 	 						minX = archonLocations[i].getX();
 	 				
 	 					if (first || maxY < archonLocations[i].getY())
 	 						maxY = archonLocations[i].getY();
 	 				
 	 					if (first || minY > archonLocations[i].getY())
 	 						minY = archonLocations[i].getY();
 	 				}
 	 				first = false;
 	 			}
 	 			
 	 			int myX = myRC.getLocation().getX();
 	 			int myY = myRC.getLocation().getY();
 	 		
 	 			// there are not deposits, but some archon should be fixed
 	 			// to move in senseDirectionToUnownedFluxDeposit
 	 			if (depositsLocations.length == 0 && archonLocations.length > 0){
 	 				for (int i = 0; i < archonLocations.length; i++) {
 	 					if (myX == maxX && myY == maxY
 	 							&& archonLocations[i].getX() == myX
 	 							&& archonLocations[i].getY() == myY){
 	 						archonIsFixed[i] = true;
 	 						notFixedarchonsNum --;
 	 					}	
 	 				}
 	 			}
 	 		
 	 			// if there are more then one not fixed archons,
 	 			// they should move in some different directions
 	 			if (notFixedarchonsNum > 1){
 	 				if (myY == maxY && myX != maxX)
 	 					myDirection = Direction.SOUTH;
 	  				
 	 				if (myY == minY && myX != maxX)
 	 					myDirection = Direction.NORTH;
 	 	 			
 	 				if (myX == maxX && myY != maxY)
 	 					myDirection = Direction.EAST;
 	  				
 	 				if (myX == minX && myY != maxY)
 	 					myDirection = Direction.WEST;
 	 	 		
 	 				if (myX == maxX && myY == maxY)
 	 					myDirection = Direction.SOUTH_EAST;
 	  				
 	 				if (myX == maxX && myY == minY)
 	 					myDirection = Direction.NORTH_EAST;

 	 				if (myX == minX && myY == maxY)
 	 	 			myDirection = Direction.SOUTH_WEST;
 	  				
 	 				if (myX == minX && myY == minY)
 	 					myDirection = Direction.NORTH_WEST;
 	 	 			
 	 			}
 	 			  				
 	 			status = ArchonStatus.LOOKING_FOR_SOME_DEPOSIT;
 	 			
 	 			for (int i = 0; i < archonLocations.length; i++) {
 	 				if (archonIsFixed[i]
 	 				    && archonLocations[i].getX() == myRC.getLocation().getX()
 	 				    && archonLocations[i].getY() == myRC.getLocation().getY()){
 	 						status = ArchonStatus.LOOKING_FOR_NEAREST_DEPOSIT;
 	 				}
 	 				
 	 			}
 					
 			}
 			
 			Direction direction;
			
 			if (turnStatus == ArchonStatus.LOOKING_FOR_NEAREST_DEPOSIT) {
 				direction = myRC.senseDirectionToUnownedFluxDeposit();
 			} else {
 				direction = myDirection;
 				if (clock.getRoundNum() > roundNumNotNearest + ROUNDS_TO_CAPTURE / 2){
 					status = ArchonStatus.LOOKING_FOR_NEAREST_DEPOSIT;
 				}
					
 			}
    		
 			if (direction == Direction.OMNI) {
 				
 				status = ArchonStatus.DEPOSIT_FOUND;
    			
 			} else {
    			
 				if (myRC.canMove(direction) && direction != myRC.getDirection()) {
 					myRC.setDirection(direction);
 				}
 				else {
 					if(myRC.canMove(myRC.getDirection()))
 						myRC.moveForward();
 					else
 						myRC.setDirection(myRC.getDirection().rotateRight());
 				}
    			
 				myRC.yield();
    			
 			}
    						
		}
		
	}

}
