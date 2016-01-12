package artanis;

import battlecode.common.*;

public class Soldier {
	
	private static RobotController rc;
	private static Signal[] signalQueue;
	private static Direction myDirection;

	public static void code( ){
		rc = RobotPlayer.rc;		
		myDirection = Movement.randomDirection();
		
		try {
			runsOnce();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		while(true){
			try {
				repeat();
			} catch ( Exception e ) {
				e.printStackTrace();
			}
			Clock.yield();
		}	
	}
	
	private static void runsOnce() throws GameActionException {
		return;
	}
	
	private static void repeat() throws GameActionException {
		int myAttackRadius = rc.getType().attackRadiusSquared;
		RobotInfo[] enemyRobots = rc.senseHostileRobots( rc.getLocation(), myAttackRadius );
		
		if(enemyRobots.length>0 && rc.isWeaponReady()){
				rc.attackLocation( Attack.findWeakestEnemy(enemyRobots) );
		} else {
			signalQueue = rc.emptySignalQueue();
			myDirection = rc.getLocation().directionTo( Communication.getLocation( signalQueue, Archon.TARGET_LOCATION ) );
			Movement.simpleMove( myDirection );
		}
	}
}