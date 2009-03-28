package team006;

import java.util.HashSet;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

public class Worker {
	
	private final RobotController myRC;
	
	WorkerStatus status = WorkerStatus.CREATING_ME;
	
	HashSet<MapLocation> locationsOfUnloadedBlocks = new HashSet<MapLocation>();
	
	//private MapLocation fixedBlockLocation;
	private MapLocation depositLocation;
	
	//boolean unloadIfPossible = false;
	
	private Direction lookingForDirection;
	int wrongDirectionTurns = 0;
	
	public Worker(RobotController rc) {
		myRC = rc;
	}
	
	private boolean isLocationOfBuilding(MapLocation location) throws Exception{
		
		Direction direction = location.directionTo(depositLocation);
		
		if (direction == Direction.OMNI)
			return true;
		
		if (locationsOfUnloadedBlocks.contains(location))
			return true;
		
		direction = Direction.EAST;
		
		for(int i = 0; i < 8; i++){
			
			MapLocation nextLocation = location.subtract(direction.opposite());
			
			if (!myRC.canSenseSquare(nextLocation))
				return false;
			
			int height = myRC.senseHeightOfLocation(location);
			int nextHeight = myRC.senseHeightOfLocation(nextLocation);
			
			int delta = nextHeight - height;
			
			if ((delta > 0)
				&& (delta <= WORKER_MAX_HEIGHT_DELTA)
				&& isLocationOfBuilding(nextLocation)) {
				
					if (!locationsOfUnloadedBlocks.contains(location))
						locationsOfUnloadedBlocks.add(location);
					return true;
			}
				
			
			direction = direction.rotateRight();
		}
		
		return false;

	}
	
private boolean isLocationOfBuildingOrNeighbourhood(MapLocation location) throws Exception{
		
		Direction direction = location.directionTo(depositLocation);
		
		if (direction == Direction.OMNI)
			return true;
	
		//direction = Direction.EAST;
		
		//for(int i = 0; i < 8; i++){
			
			MapLocation nextLocation = location.subtract(direction.opposite());
			
			if (locationsOfUnloadedBlocks.contains(nextLocation))
				return true;
			
			if (!myRC.canSenseSquare(nextLocation))
				return false;
			
			int height = myRC.senseHeightOfLocation(location);
			int nextHeight = myRC.senseHeightOfLocation(nextLocation);
			
			int delta = nextHeight - height;
			
			if ((delta >= 0)
				&& (delta <= WORKER_MAX_HEIGHT_DELTA)
				&& isLocationOfBuilding(nextLocation))
					return true;
			
			//direction = direction.rotateRight();
		//}
		
		return false;

	}
	
	public void nextTurn() throws Exception{
		
		WorkerStatus turnStatus = status;
		
		if (turnStatus != WorkerStatus.CREATING_ME){
		
			if (myRC.getLocation().isAdjacentTo(depositLocation)){
				
				double eventualEnergon = myRC.senseRobotInfo(myRC.getRobot()).eventualEnergon;
				double maxEnergon = myRC.senseRobotInfo(myRC.getRobot()).maxEnergon;
				
				if (eventualEnergon < maxEnergon / 2)
					myRC.yield();
				
			}
					
			Robot[] groundRobot = myRC.senseNearbyGroundRobots();
			
			// id of the robot with minimal energon level
			int idRobot = groundRobot.length;
			
			double minEnergon = 0;
			
			for (int i = 0; i < groundRobot.length; i++) {
			
				RobotInfo robotInfo = myRC.senseRobotInfo(groundRobot[i]);

				if (myRC.getLocation().isAdjacentTo(robotInfo.location)
						|| myRC.getLocation().equals(robotInfo.location)){
				
					double energon = robotInfo.eventualEnergon;
					
					if (energon < minEnergon || idRobot == groundRobot.length){
						minEnergon = energon;
						idRobot = i;
					}
				}
			}
			
			if (idRobot < groundRobot.length) {
				RobotInfo robotInfo = myRC.senseRobotInfo(groundRobot[idRobot]);
				RobotInfo myInfo = myRC.senseRobotInfo(myRC.getRobot());
				
				if (robotInfo.eventualEnergon < robotInfo.maxEnergon / 3
						&& myInfo.eventualEnergon > 2 * myInfo.maxEnergon / 3)
				myRC.transferEnergon(ENERGON_TRANSFER_RATE, robotInfo.location,RobotLevel.ON_GROUND);
				myRC.yield();
			}
			
		}
			
		if (turnStatus == WorkerStatus.CREATING_ME){
		
			System.out.println("Powstał WORKER, loc: " + myRC.getLocation() + "id: " + myRC.getRobot().getID());

			MapLocation[] archonLocations = myRC.senseAlliedArchons();
			
			int idOfArchon = archonLocations.length;
			
			int minDistance = 0;
			
			// looking for the nearest archon to this worker 
			for (int j = 0; j < archonLocations.length; j++){
				int distance = myRC.getLocation().distanceSquaredTo(archonLocations[j]);
				if ((idOfArchon == archonLocations.length 
					|| distance < minDistance)){
	     				minDistance = distance;
	     				idOfArchon = j;
				}		
			}
			
			// it is also a deposit location
			depositLocation = archonLocations[idOfArchon];
			
			lookingForDirection = myRC.getLocation().directionTo(archonLocations[idOfArchon]).opposite();
			
			status = WorkerStatus.LOOKING_FOR_BLOCK;
			
		}
		
		if (turnStatus == WorkerStatus.LOOKING_FOR_BLOCK){
			
			while(myRC.isMovementActive()) {
                myRC.yield();
            }
	
			Direction direction;
			
			MapLocation[] allBlocksLocations = myRC.senseNearbyBlocks();
			int blocksLocationsNum = 0;
			
			for (int i = 1; i < allBlocksLocations.length; i++){
				if (!isLocationOfBuilding(allBlocksLocations[i])){
					blocksLocationsNum++;
				}
			}
			
			MapLocation[] blocksLocations = new MapLocation[blocksLocationsNum];
			int j = 0;
			
			for (int i = 1; i < allBlocksLocations.length; i++){
				if (!isLocationOfBuilding(allBlocksLocations[i])){
					blocksLocations[j] = allBlocksLocations[i];
					j++;
				}
			}

			boolean blockCanBeLoad = false;
			
			//the block which can be loaded or the nearest block
			int idOfBlock = 0;
			
			if (blocksLocations.length > 0) {
				
				double minDistance = myRC.getLocation().distanceSquaredTo(blocksLocations[0]);
				
				for (int i = 0; i < blocksLocations.length; i++){
					
					double distance = myRC.getLocation().distanceSquaredTo(blocksLocations[i]);
					
					if (minDistance > distance) {
						minDistance = distance;
						idOfBlock = i;
					}
					
					if (myRC.canLoadBlockFromLocation(blocksLocations[i])){
						blockCanBeLoad = true;
						idOfBlock = i;
						minDistance = 0;
					}
					
					
				}
				
				direction = myRC.getLocation().directionTo(blocksLocations[idOfBlock]);
				
			} else {
				
			
				if (lookingForDirection != myRC.getDirection()) {
					wrongDirectionTurns++;
					if (wrongDirectionTurns > 10)
						lookingForDirection = lookingForDirection.opposite();
				}
				
				direction = lookingForDirection;
			}
			
			if (blockCanBeLoad){
				
				myRC.loadBlockFromLocation(blocksLocations[idOfBlock]);
				status = WorkerStatus.MODIFYING_TERRAIN;
				
			} else {
				
				if (myRC.canMove(direction) && direction != myRC.getDirection()) {
					wrongDirectionTurns = 0;
					myRC.setDirection(direction);
				} else {
					if(myRC.canMove(myRC.getDirection()))
	            		myRC.moveForward();
	            	else {
	            		myRC.setDirection(myRC.getDirection().rotateRight());
	            	}

				}
							
				myRC.yield();

			}

					                                    
		}
		
		if (turnStatus == WorkerStatus.MODIFYING_TERRAIN){
	
            while(myRC.isMovementActive()) {
                myRC.yield();
             }
            
        	MapLocation firstNextLocation = myRC.getLocation().subtract(myRC.getLocation().directionTo(depositLocation).opposite());
			Direction nextDirection = firstNextLocation.directionTo(depositLocation);
			MapLocation secondNextLocation = firstNextLocation.subtract(nextDirection.opposite());
			
			if (nextDirection == Direction.OMNI
				|| ((myRC.senseHeightOfLocation(secondNextLocation) >= myRC.senseHeightOfLocation(firstNextLocation) + GameConstants.WORKER_MAX_HEIGHT_DELTA))
					&& isLocationOfBuildingOrNeighbourhood(firstNextLocation)
					&& isLocationOfBuilding(secondNextLocation)){
				/* 
				 * firstNextLocation is deposit location or difference
				 * between the first of next locations and the second one
				 * is too high - block should be unload at firstNextLocation 
				 */
				
				if (myRC.canUnloadBlockToLocation(firstNextLocation)){
					// it is possible to unload the block and we do it
					System.out.println("unloadBlockToLocation");
					
					while(myRC.isMovementActive()) {
						myRC.yield();
			        }
					
					myRC.unloadBlockToLocation(firstNextLocation);
					locationsOfUnloadedBlocks.add(firstNextLocation);
						
					status = WorkerStatus.LOOKING_FOR_BLOCK;	
				} else {
					// it is impossible to unload the block here
					myRC.setDirection(myRC.getDirection().rotateRight());
					
					myRC.yield();
					
					if(myRC.canMove(myRC.getDirection()))
						myRC.moveForward();
	
				}
				
			} else {
				
				// unloading the block is not necessary 
				
				 while(myRC.isMovementActive()) {
					 myRC.yield();
		         }
							
				Direction direction = myRC.getLocation().directionTo(depositLocation);
			
				if (myRC.canMove(direction) && direction != myRC.getDirection()) {
					myRC.setDirection(direction);
				} else {
				
					if(myRC.canMove(myRC.getDirection()))
						myRC.moveForward();
					else
						myRC.setDirection(myRC.getDirection().rotateRight());
							
				}
				
			}
			myRC.yield();
					
			/*
			if (unloadIfPossible) {
				
				MapLocation locationOfUnload = myRC.getLocation().subtract(myRC.getDirection().opposite());
				
				if (myRC.canUnloadBlockToLocation(locationOfUnload)){
					myRC.unloadBlockToLocation(locationOfUnload);
					unloadIfPossible = false;
					//status =
				} else {
					
					myRC.moveForward();
					
				}
				
			} else {
				
				Direction direction = myRC.getLocation().directionTo(depositFixed);
				
			}
			*/
			
			/*unloadIfPossible
			if (direction == Direction.OMNI){
				
				if 
				myRC.unloadBlockToLocation(myRC.getLocation());
				status = WorkerStatus.LOOKING_FOR_BLOCK;
				
			} else {

				if (myRC.canMove(direction) && direction != myRC.getDirection()) {
					myRC.setDirection(direction);
				}
				else {
					if(myRC.canMove(myRC.getDirection()))
						myRC.moveForward();
					else {
						MapLocation location = myRC.getLocation();
						MapLocation nextLocation = location.subtract(myRC.getDirection());
			
						if (myRC.senseHeightOfLocation(nextLocation) > myRC.senseHeightOfLocation(location) + WORKER_MAX_HEIGHT_DELTA){
							myRC.setDirection(myRC.getDirection().opposite());
							if(myRC.canMove(myRC.getDirection()))
								myRC.moveForward();
							myRC.unloadBlockToLocation(myRC.getLocation());
							status = WorkerStatus.LOOKING_FOR_BLOCK;
						} else 
							myRC.setDirection(myRC.getDirection().rotateRight());
					}
						
				}
			
				myRC.yield();
				
			}
			*/
			
		}
		
		/*
		if (status == WorkerStatus.BLOCK_FIXED){
			
			while(myRC.isMovementActive()) {
                myRC.yield();
            }
			
			MapLocation[] blocksLocations = myRC.senseNearbyBlocks();
			
			boolean blockAtLocation = false;
			
			for (int i = 0; i < blocksLocations.length; i++){
				if (blocksLocations[i].getX() == myRC.getLocation().getX()
					&& blocksLocations[i].getY() == myRC.getLocation().getY())
					blockAtLocation = true;
			}
			
			if (blockAtLocation) {
				status = WorkerStatus.BLOCK_FOUND;
				System.out.println("Fixed block is found");
				myRC.spawn(RobotType.WORKER);
			}
			else {
	
				Direction direction = myRC.getLocation().directionTo(fixedBlockLocation);
				
									
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
		*/
		
	}
		
}
