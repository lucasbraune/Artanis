package artanis;

import battlecode.common.*;

public class RobotPlayer {
	
	public static RobotController rc; 
	public static Team myTeam;
	public static Signal[] signalQueue; 
	public static boolean isMasterArchon = false; // Identifies the leading Archon.
	
	private static Direction groupDirection;
	
	public static void run(RobotController rcIn){
		
		rc = rcIn;
		myTeam = rc.getTeam();
		groupDirection = Movement.randomDirection();
		
		try {
			if( rc.getType() == RobotType.ARCHON )
				Communication.electMasterArchon();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
		while(true){
			try{
				signalQueue = rc.emptySignalQueue();
				
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
				}else if(rc.getType()==RobotType.VIPER){
					viperCode();
				}
			}catch ( Exception e ) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}


	private static void soldierCode() throws GameActionException {
		int myAttackRadius = rc.getType().attackRadiusSquared;

		RobotInfo[] enemyRobots = rc.senseHostileRobots( rc.getLocation(), myAttackRadius );
		if(enemyRobots.length>0 && rc.isWeaponReady()){
				rc.attackLocation( Attack.findWeakestEnemy(enemyRobots) );
		} else {
			Movement.simpleMove( Communication.getDirectionOfGroupTarget() );
		}

	}
	

	private static void scoutCode() {
		// TODO Auto-generated method stub
		
	}

	private static void guardCode() throws GameActionException {
		soldierCode();
		
	}
	
	private static void viperCode() throws GameActionException {
		soldierCode();
		
	}

	private static void ttmCode() {
		// TODO Auto-generated method stub
		
	}

	private static void turretCode() {
		// TODO Auto-generated method stub
		
	}

	private static void archonCode() throws GameActionException {
		
		if ( isMasterArchon == true ) {
			// Master archon code:
			
			// Master archon movement = movement of the whole group
			if( rc.getRoundNum() % 4 != 1 ) {
				rc.broadcastMessageSignal( groupDirection.ordinal() , 0, 1000 );
			} else {
				if ( !rc.onTheMap( rc.getLocation().add(groupDirection, 4) ) ) {
					groupDirection = Movement.randomDirection();
				}
				else {
					Movement.simpleMove( groupDirection );
				}
			}
		} else {
			// Normal archon code:
			
			// The following has some bytecode inefficiency because archons
			// end up calling rc.isCoreReady() twice before moving. 
			if(rc.isCoreReady()){
				Direction randomDir = Movement.randomDirection();
				if(rc.canBuild(randomDir, RobotType.SOLDIER)){
					rc.build(randomDir, RobotType.SOLDIER);
				} else {
					Movement.simpleMove( Communication.getDirectionOfGroupTarget() );
				}
			}
		}
		
	}

	
}