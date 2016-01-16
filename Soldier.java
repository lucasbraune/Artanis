package artanis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import battlecode.common.*;

public class Soldier {
	
	private static RobotController rc;
	private static Signal[] signalQueue;
	private static Direction myDirection;
	private static RobotInfo targetRobot;
	
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
		//TODO INCORPORATE THIS INTO THE COMMENTED CODE BELOW
		RobotInfo[] enemyRobots = rc.senseHostileRobots( rc.getLocation(), -1 );
		
		if ( enemyRobots.length > 0 ){
			rc.setIndicatorString(0, "I see the enemy");
			targetRobot = findWeakestEnemy( enemyRobots );
			if ( rc.isWeaponReady() && rc.canAttackLocation( targetRobot.location )){
				rc.attackLocation( targetRobot.location );
			} else if ( rc.isCoreReady() ) {
				repositionAround( targetRobot );
			}
		} else {
			rc.setIndicatorString(0, "No enemy.");
			if ( targetRobot != null && rc.isCoreReady() ) {
				Movement.simpleMove( rc.getLocation().directionTo( targetRobot.location ) );
			}
		}
		
		
//		
//		int mySensorRadius = rc.getType().sensorRadiusSquared;
//		int myAttackRadius = rc.getType().attackRadiusSquared;
//		RobotInfo[] enemyRobots = rc.senseHostileRobots( rc.getLocation(), -1 );
//		RobotInfo[] enemiesInRange = rc.senseHostileRobots( rc.getLocation(), myAttackRadius );
//		//RobotInfo[] enemiesInRange = getEnemiesInRange( enemyRobots );
//		MapLocation weakest, target;
//		
//		signalQueue = rc.emptySignalQueue();
//		target = Communication.getLocation( signalQueue, Archon.TARGET_LOCATION );
//		
//		if( target != null ) {
//			if( enemyRobots.length>0 && 
//					rc.getLocation().distanceSquaredTo( target ) <= 2*mySensorRadius ){
//				if( enemiesInRange.length > 0 && rc.isWeaponReady() ){
//					weakest = Attack.findWeakestEnemy( enemiesInRange );
//					rc.attackLocation( weakest );
//				} else {
//					weakest = Attack.findWeakestEnemy( enemyRobots );
//					myDirection = rc.getLocation().directionTo( weakest );
//					Movement.simpleMove( myDirection );
//				}
//			} else {
//				myDirection = rc.getLocation().directionTo( target );
//				Movement.simpleMove( myDirection );
//			}
//		} else {
//			if( enemyRobots.length>0 ){
//				if( enemiesInRange.length > 0 && rc.isWeaponReady() ){
//					weakest = Attack.findWeakestEnemy( enemiesInRange );
//					rc.attackLocation( weakest );
//				} else {
//					weakest = Attack.findWeakestEnemy( enemyRobots );
//					myDirection = rc.getLocation().directionTo( weakest );
//					Movement.simpleMove( myDirection );
//				}
//			} 
//		}
	}
	
	private static void repositionAround ( RobotInfo enemy ) throws GameActionException {
		
		int radiusSquared = rc.getType().attackRadiusSquared, radius = 0 ;
		int iSquared, jSquared;
		int fromMeToSweetSpot = 0;
		MapLocation sweetSpot = rc.getLocation();
		MapLocation thisLocation;
		
		if ( rc.getLocation().distanceSquaredTo( enemy.location ) > radiusSquared ) {
			Movement.simpleMove( rc.getLocation().directionTo( enemy.location ) );
		} else {
			
			while ( radius*radius < radiusSquared )
				radius++;
			
			for ( int i=-radius; i<=radius; i++ ){
				iSquared = i*i;
				for ( int j=-radius; j<=radius; j++ ){
					jSquared = j*j;
					if (iSquared + jSquared <= radiusSquared ){
						// Begin code to be iterated in a disk around the enemy

						// Here we find a 'sweet spot' from which to attack the enemy.
						// We search in the disk of radius rc.getType().attackRadiusSquared
						// for a place that is as far away from enemy as possible.
						// Between two map location which are the same distance
						// from the enemy, we choose the one that is closest to us.

						thisLocation = enemy.location.add(i, j);
						if (iSquared + jSquared > enemy.location.distanceSquaredTo( sweetSpot ) ){
							sweetSpot = thisLocation;
							fromMeToSweetSpot = rc.getLocation().distanceSquaredTo( sweetSpot );
						} else if ( iSquared + jSquared == enemy.location.distanceSquaredTo( sweetSpot ) ) {
							if ( fromMeToSweetSpot > rc.getLocation().distanceSquaredTo( thisLocation ) ) {
								sweetSpot = thisLocation;
								fromMeToSweetSpot = rc.getLocation().distanceSquaredTo( thisLocation );
							}
						}
						// End code to be iterated in a disk around the enemy
					}
				}
			}
			
			
		}
		
		Direction dir = rc.getLocation().directionTo( sweetSpot );
		Movement.simpleMove( dir );
	}
	
	public static RobotInfo findWeakestEnemy( RobotInfo[] enemyRobots ) {
		int numberOfEnemies = enemyRobots.length;
		
		if( numberOfEnemies > 0 ){
			int i, weakestIndex = 0;
			double weakness = 0, largestWeakness;
	
			largestWeakness = enemyRobots[0].maxHealth - enemyRobots[0].health;
	
			for(i=1; i<numberOfEnemies; i++) {
				weakness = enemyRobots[i].maxHealth - enemyRobots[i].health;
				if ( weakness > largestWeakness ){
					weakestIndex = i;
					largestWeakness = weakness;
				}
			}
			
			return enemyRobots[weakestIndex];
			
		} else {
			return null;
		}
	}
	
	
}