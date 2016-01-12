package artanis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

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
	
	// I am having trouble extracting enemies in range from all enemies sensed.
	// This should be easy.
	private static void repeat() throws GameActionException {
		int mySensorRadius = rc.getType().sensorRadiusSquared;
		int myAttackRadius = rc.getType().attackRadiusSquared;
		RobotInfo[] enemyRobots = rc.senseHostileRobots( rc.getLocation(), -1 );
		RobotInfo[] enemiesInRange = rc.senseHostileRobots( rc.getLocation(), myAttackRadius );
		//RobotInfo[] enemiesInRange = getEnemiesInRange( enemyRobots );
		MapLocation weakest, target;
		
		signalQueue = rc.emptySignalQueue();
		target = Communication.getLocation( signalQueue, Archon.TARGET_LOCATION );
		
		if( target != null ) {
			if( enemyRobots.length>0 && 
					rc.getLocation().distanceSquaredTo( target ) <= 2*mySensorRadius ){
				if( enemiesInRange.length > 0 && rc.isWeaponReady() ){
					weakest = Attack.findWeakestEnemy( enemiesInRange );
					rc.attackLocation( weakest );
				} else {
					weakest = Attack.findWeakestEnemy( enemyRobots );
					myDirection = rc.getLocation().directionTo( weakest );
					Movement.simpleMove( myDirection );
				}
			} else {
				myDirection = rc.getLocation().directionTo( target );
				Movement.simpleMove( myDirection );
			}
		} else {
			if( enemyRobots.length>0 ){
				if( enemiesInRange.length > 0 && rc.isWeaponReady() ){
					weakest = Attack.findWeakestEnemy( enemiesInRange );
					rc.attackLocation( weakest );
				} else {
					weakest = Attack.findWeakestEnemy( enemyRobots );
					myDirection = rc.getLocation().directionTo( weakest );
					Movement.simpleMove( myDirection );
				}
			} 
		}
	}
//	
//	private static RobotInfo[] getEnemiesInRange( RobotInfo[] enemies ){
//		int removed = 0, totalRemoved = 0, myAttackRadius = rc.getType().attackRadiusSquared;
//		RobotInfo[] inRange;
//		for (int i=0; i<enemies.length; i++){
//			if( rc.getLocation().distanceSquaredTo( enemies[i].location ) > myAttackRadius ) {
//				totalRemoved++;
//			}
//		}
//		inRange = new RobotInfo[ enemies.length-totalRemoved ];
//		for (int i=0; i<enemies.length-totalRemoved; i++){
//			if( rc.getLocation().distanceSquaredTo( enemies[i].location ) <= myAttackRadius ) {
//				inRange[ i ] = enemies[i+removed];
//			} else {
//				removed++;
//				i--;
//			}
//		}
//		return inRange;
//	}
}