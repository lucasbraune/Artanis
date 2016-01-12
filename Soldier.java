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
		int mySensorRadius = rc.getType().sensorRadiusSquared, myAttackRadius = rc.getType().attackRadiusSquared, toTarget;
		RobotInfo[] enemyRobots = rc.senseHostileRobots( rc.getLocation(), mySensorRadius );
		MapLocation target, weakest;
		
		signalQueue = rc.emptySignalQueue();
		target = Communication.getLocation( signalQueue, Archon.TARGET_LOCATION );
		toTarget = rc.getLocation().distanceSquaredTo( target );
		
		if( enemyRobots.length>0 && toTarget <= mySensorRadius ) {
			weakest = Attack.findWeakestEnemy(enemyRobots);
			if( toTarget <= myAttackRadius ) {
				rc.attackLocation( weakest );
			} else {
				myDirection = rc.getLocation().directionTo( weakest );
				Movement.simpleMove( myDirection );
			}
			Movement.simpleMove( myDirection );
		} else {
			myDirection = rc.getLocation().directionTo( target );
			Movement.simpleMove( myDirection );
		}
	}
}