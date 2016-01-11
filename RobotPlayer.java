package artanis;

import battlecode.common.*;

public class RobotPlayer {
	
	public static RobotController rc; 
	
	private static Signal[] signalQueue;
	private static Direction myDirection;
	public static boolean isMasterArchon = false;
	
	public static void run(RobotController rcIn){
		
		rc = rcIn;
		myDirection = Movement.randomDirection();
		
		try {
			if( rc.getType() == RobotType.ARCHON )
				Communication.electMasterArchon();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
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
			signalQueue = rc.emptySignalQueue();
			myDirection = rc.getLocation().directionTo( Communication.readTargetLocation( signalQueue ) );
			Movement.simpleMove( myDirection );
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
		MapLocation aFewStepsAheadOfMe;
		if ( isMasterArchon == true ) {
			// Master archon code:
			// Master archon movement = movement of the whole group
			if( rc.getRoundNum() % 4 != 1 ) {
				aFewStepsAheadOfMe = Movement.getLocationAhead( myDirection );
				Communication.broadcastTargetLocation( aFewStepsAheadOfMe );
			} else {
				if ( !rc.onTheMap( rc.getLocation().add(myDirection, 4) ) ) {
					myDirection = Movement.randomDirection();
					// this changes directions if current one leads out of the map.
				}
				else {
					Movement.simpleMove( myDirection );
				}
			}
		} else {
			// Normal archon code
			// (The following has some bytecode inefficiency because archons
			// end up calling rc.isCoreReady() twice before moving.) 
			if(rc.isCoreReady()){
				Direction randomDir = Movement.randomDirection();
				if(rc.canBuild(randomDir, RobotType.SOLDIER)){
					rc.build(randomDir, RobotType.SOLDIER);
				} else {
					signalQueue = rc.emptySignalQueue();
					myDirection = rc.getLocation().directionTo( Communication.readTargetLocation( signalQueue ) );
					Movement.simpleMove( myDirection );
				}
			}
		}
		
	}

	
}