package team006;

import battlecode.common.*;
//import static battlecode.common.GameConstants.*;

public class RobotPlayer implements Runnable {

   private final RobotController myRC;
   private Archon archon; // if RobotPlayer's type is ARCHON
   private Worker worker; // if RobotPlayer's type is WORKER
   
   public RobotPlayer(RobotController rc) {
   
	  myRC = rc;
	   
      if (rc.getRobotType() == RobotType.ARCHON){
    	  archon = new Archon(rc);
      }
      
      if (rc.getRobotType() == RobotType.WORKER){
    	  worker = new Worker(rc);
      }
  
   }

   public void run() {
      while(true){
         try{
            /*** beginning of main loop ***/

        	 if (myRC.getRobotType() == RobotType.ARCHON){
        		 archon.nextTurn();
        	 }
        	 
        	 if (myRC.getRobotType() == RobotType.WORKER){
        		 worker.nextTurn();
        	 }
        	 
            /*** end of main loop ***/
         }catch(Exception e) {
            System.out.println("caught exception:");
            e.printStackTrace();
         }
      }
   }
}
