package artanis;

import battlecode.common.*;

public class RobotPlayer {

	private static RobotController rc;
	
	public static void run(RobotController rcIn){
		
		rc = rcIn;
		
		while(true){
			try{
				if(rc.getType()==RobotType.ARCHON){
					archonCode();
				}else if(rc.getType()==RobotType.TURRET){
					turretCode();
				}else if(rc.getType()==RobotType.TTM){
					ttmCode();
				}else if(rc.getType()==RobotType.GUARD){
					guardCode();
				}else if(rc.getType()==RobotType.SCOUT){
					scoutCode();
				}else if(rc.getType()==RobotType.SOLDIER){
					soldierCode();
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			Clock.yield();
		}
	}

	private static void soldierCode() {
		// TODO Auto-generated method stub
		
	}

	private static void scoutCode() {
		// TODO Auto-generated method stub
		
	}

	private static void guardCode() {
		// TODO Auto-generated method stub
		
	}

	private static void ttmCode() {
		// TODO Auto-generated method stub
		
	}

	private static void turretCode() {
		// TODO Auto-generated method stub
		
	}

	private static void archonCode() {
		// TODO Auto-generated method stub
		
	}
}